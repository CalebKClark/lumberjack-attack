using System;
using System.Collections.Generic;
using Godot;
using LumberjackAttack.Core;
using LumberjackAttack.Ui;
using LumberjackAttack.World;
using LumberjackAttack.World.Placeable;

namespace LumberjackAttack.Editor;

/// <summary>
/// Title-menu world editor. Paint, rotate, expand/shrink, undo, load/save .map files.
/// Gated by <see cref="GameConfig.EnableWorldEdit"/> — safe to leave out of shipping builds.
/// </summary>
public partial class WorldEditScreen : Control
{
	private const int TopBarH = 56;
	private const int BottomBarH = 120;
	private const int SidePad = 52;
	private const int PaletteSlot = 48;
	private const int PaletteGap = 6;
	private const int MaxUndo = 40;
	private const float MinZoom = 0.5f;
	private const float MaxZoom = 4f;

	private readonly List<EditSnapshot> _undo = new();
	private IReadOnlyDictionary<int, TileDef> _catalog = null!;
	private List<TileDef> _palette = null!;
	private EditableTileMap _map = null!;

	private SubViewportContainer _viewHost = null!;
	private SubViewport _viewport = null!;
	private Node2D _world = null!;
	private Node2D _tileLayer = null!;
	private Node2D _markerLayer = null!;
	private Node2D _buildingLayer = null!;
	private Camera2D _camera = null!;
	private TileMapLayer _logicGround = null!;

	private Label _status = null!;
	private Label _info = null!;
	private HBoxContainer _paletteRow = null!;
	private LineEdit? _nameEdit;
	private PanelContainer? _loadPanel;
	private VBoxContainer? _loadList;

	private int _selectedTileId;
	private int _paintRotation;
	private int _paletteScroll;
	private bool _strokeUndoPushed;
	private bool _dirty;
	private string _statusText = "LMB paint · RMB erase · Q/E rotate · Scroll zoom · WASD pan · Ctrl+drag pan · Z undo";

	private sealed class EditSnapshot
	{
		public required int[][] Tiles { get; init; }
		public required int[][] Rotations { get; init; }
		public required bool[][] Markers { get; init; }
	}

	public override void _Ready()
	{
		SetAnchorsPreset(LayoutPreset.FullRect);
		MouseFilter = MouseFilterEnum.Stop;
		GameFont.Apply(this);

		_catalog = TileCatalog.Load();
		_palette = new List<TileDef>(_catalog.Values);
		_palette.Sort((a, b) => a.Id.CompareTo(b.Id));
		_selectedTileId = _palette.Count > 0 ? _palette[0].Id : GameConfig.BlankTileId;
		_map = EditableTileMap.CreateDefault();

		var bg = new ColorRect
		{
			Color = new Color(0.1f, 0.11f, 0.13f),
			MouseFilter = MouseFilterEnum.Ignore,
		};
		bg.SetAnchorsPreset(LayoutPreset.FullRect);
		AddChild(bg);

		BuildTopBar();
		BuildViewport();
		BuildEdgeButtons();
		BuildBottomBar();
		BuildLoadPanel();
		BuildNamePrompt();

		GetViewport().SizeChanged += OnViewportSizeChanged;
		OnViewportSizeChanged();
		ReloadWorldVisuals();
		CenterCamera();
		RefreshChrome();
	}

	public override void _ExitTree()
	{
		if (GetViewport() != null)
		{
			GetViewport().SizeChanged -= OnViewportSizeChanged;
		}
	}

	public override void _Process(double delta)
	{
		HandleCameraPan((float)delta);
		if (_dirty)
		{
			_dirty = false;
			ReloadWorldVisuals();
			RefreshChrome();
		}
	}

	public override void _UnhandledInput(InputEvent @event)
	{
		if (_nameEdit is { Visible: true } || _loadPanel is { Visible: true })
		{
			if (@event is InputEventKey { Pressed: true, Keycode: Key.Escape })
			{
				HideOverlays();
				GetViewport().SetInputAsHandled();
			}

			return;
		}

		if (@event is InputEventKey { Pressed: true, Echo: false } key)
		{
			switch (key.Keycode)
			{
				case Key.Escape:
					GoBack();
					GetViewport().SetInputAsHandled();
					return;
				case Key.Z:
					Undo();
					GetViewport().SetInputAsHandled();
					return;
				case Key.Q:
					NudgeRotation(-1);
					GetViewport().SetInputAsHandled();
					return;
				case Key.E:
					NudgeRotation(1);
					GetViewport().SetInputAsHandled();
					return;
			}
		}

		if (@event is InputEventMouseButton mb && PointInMapArea(mb.Position))
		{
			if (mb.ButtonIndex == MouseButton.WheelUp && mb.Pressed)
			{
				AdjustZoom(1.1f);
				GetViewport().SetInputAsHandled();
			}
			else if (mb.ButtonIndex == MouseButton.WheelDown && mb.Pressed)
			{
				AdjustZoom(1f / 1.1f);
				GetViewport().SetInputAsHandled();
			}
		}
	}

	public override void _GuiInput(InputEvent @event)
	{
		// Map painting is handled on the viewport host (OnMapGuiInput).
	}

	private void OnMapGuiInput(InputEvent @event)
	{
		if (_nameEdit is { Visible: true } || _loadPanel is { Visible: true })
		{
			return;
		}

		var ctrl = Input.IsKeyPressed(Key.Ctrl);
		if (@event is InputEventMouseMotion motion && ctrl && Input.IsMouseButtonPressed(MouseButton.Left))
		{
			_camera.Position -= motion.Relative / Math.Max(0.01f, _camera.Zoom.X);
			_viewHost.AcceptEvent();
			return;
		}

		// Positions are local to the viewport host.
		if (@event is InputEventMouseButton { Pressed: true } press
			&& !ctrl
			&& press.ButtonIndex is MouseButton.Left or MouseButton.Right)
		{
			BeginStroke();
			PaintAtLocal(press.Position, erase: press.ButtonIndex == MouseButton.Right);
			_viewHost.AcceptEvent();
		}
		else if (@event is InputEventMouseMotion
			&& (Input.IsMouseButtonPressed(MouseButton.Left) || Input.IsMouseButtonPressed(MouseButton.Right))
			&& !ctrl)
		{
			var local = _viewHost.GetLocalMousePosition();
			PaintAtLocal(local, erase: Input.IsMouseButtonPressed(MouseButton.Right));
			_viewHost.AcceptEvent();
		}
		else if (@event is InputEventMouseButton { Pressed: false })
		{
			_strokeUndoPushed = false;
		}
	}

	private void BuildTopBar()
	{
		var bar = new HBoxContainer
		{
			Position = new Vector2(12, 8),
			CustomMinimumSize = new Vector2(0, 40),
		};
		bar.AddThemeConstantOverride("separation", 8);
		AddChild(bar);

		bar.AddChild(ToolButton("New", PromptNew));
		bar.AddChild(ToolButton("Load", OpenLoadPanel));
		bar.AddChild(ToolButton("Save", BeginSave));
		bar.AddChild(ToolButton("Undo", Undo));
		bar.AddChild(ToolButton("Rot L", () => NudgeRotation(-1)));
		bar.AddChild(ToolButton("Rot R", () => NudgeRotation(1)));
		bar.AddChild(ToolButton("Back", GoBack));

		_info = new Label { Text = "", SizeFlagsHorizontal = SizeFlags.ExpandFill };
		_info.AddThemeFontSizeOverride("font_size", 13);
		bar.AddChild(_info);

		_status = new Label
		{
			Position = new Vector2(12, TopBarH - 22),
			Size = new Vector2(900, 20),
			MouseFilter = MouseFilterEnum.Ignore,
		};
		_status.AddThemeFontSizeOverride("font_size", 12);
		_status.AddThemeColorOverride("font_color", new Color(1f, 0.85f, 0.45f));
		AddChild(_status);
	}

	private void BuildViewport()
	{
		_viewHost = new SubViewportContainer
		{
			Stretch = true,
			MouseFilter = MouseFilterEnum.Stop,
			FocusMode = FocusModeEnum.All,
		};
		_viewHost.GuiInput += OnMapGuiInput;
		AddChild(_viewHost);

		_viewport = new SubViewport
		{
			TransparentBg = false,
			HandleInputLocally = false,
			RenderTargetUpdateMode = SubViewport.UpdateMode.Always,
		};
		_viewHost.AddChild(_viewport);

		var worldBg = new ColorRect
		{
			Color = new Color(0.07f, 0.08f, 0.1f),
			Size = new Vector2(4096, 4096),
			Position = new Vector2(-2048, -2048),
		};
		_viewport.AddChild(worldBg);

		_logicGround = new TileMapLayer
		{
			Visible = false,
			TileSet = WorldTileSetBuilder.Build(_catalog),
		};
		_viewport.AddChild(_logicGround);

		_world = new Node2D();
		_viewport.AddChild(_world);
		_tileLayer = new Node2D { Name = "Tiles" };
		_markerLayer = new Node2D { Name = "Markers" };
		_buildingLayer = new Node2D { Name = "Buildings" };
		_world.AddChild(_tileLayer);
		_world.AddChild(_buildingLayer);
		_world.AddChild(_markerLayer);

		_camera = new Camera2D { Enabled = true };
		_viewport.AddChild(_camera);
	}

	private void BuildEdgeButtons()
	{
		var upPlus = MakeEdge("+↑", () => ResizeMap(() => _map.ExtendUp(GameConfig.BlankTileId), "Extended up"));
		upPlus.Name = "EdgeUpPlus";
		AddChild(upPlus);
		var upMinus = MakeEdge("-↑", () => ResizeMap(() => _map.ShrinkUp(), "Shrunk top", allowFail: true));
		upMinus.Name = "EdgeUpMinus";
		AddChild(upMinus);

		var downPlus = MakeEdge("+↓", () => ResizeMap(() => _map.ExtendDown(GameConfig.BlankTileId), "Extended down"));
		downPlus.Name = "EdgeDownPlus";
		AddChild(downPlus);
		var downMinus = MakeEdge("-↓", () => ResizeMap(() => _map.ShrinkDown(), "Shrunk bottom", allowFail: true));
		downMinus.Name = "EdgeDownMinus";
		AddChild(downMinus);

		var leftPlus = MakeEdge("+←", () => ResizeMap(() =>
		{
			_map.ExtendLeft(GameConfig.BlankTileId);
			_camera.Position += new Vector2(GameConfig.TileSize, 0);
		}, "Extended left"));
		leftPlus.Name = "EdgeLeftPlus";
		AddChild(leftPlus);
		var leftMinus = MakeEdge("-←", () => ResizeMap(() =>
		{
			if (_map.ShrinkLeft())
			{
				_camera.Position -= new Vector2(GameConfig.TileSize, 0);
				return true;
			}

			return false;
		}, "Shrunk left", allowFail: true));
		leftMinus.Name = "EdgeLeftMinus";
		AddChild(leftMinus);

		var rightPlus = MakeEdge("+→", () => ResizeMap(() => _map.ExtendRight(GameConfig.BlankTileId), "Extended right"));
		rightPlus.Name = "EdgeRightPlus";
		AddChild(rightPlus);
		var rightMinus = MakeEdge("-→", () => ResizeMap(() => _map.ShrinkRight(), "Shrunk right", allowFail: true));
		rightMinus.Name = "EdgeRightMinus";
		AddChild(rightMinus);
	}

	private static Button MakeEdge(string text, Action action)
	{
		var btn = new Button
		{
			Text = text,
			CustomMinimumSize = new Vector2(44, 32),
			FocusMode = FocusModeEnum.None,
		};
		btn.Pressed += action;
		return btn;
	}

	private void BuildBottomBar()
	{
		var host = new HBoxContainer
		{
			Name = "PaletteBar",
		};
		host.AddThemeConstantOverride("separation", 8);
		AddChild(host);

		var prev = new Button { Text = "<", CustomMinimumSize = new Vector2(36, PaletteSlot), FocusMode = FocusModeEnum.None };
		prev.Pressed += () =>
		{
			_paletteScroll = Math.Max(0, _paletteScroll - 1);
			RebuildPalette();
		};
		host.AddChild(prev);

		_paletteRow = new HBoxContainer();
		_paletteRow.AddThemeConstantOverride("separation", PaletteGap);
		host.AddChild(_paletteRow);

		var next = new Button { Text = ">", CustomMinimumSize = new Vector2(36, PaletteSlot), FocusMode = FocusModeEnum.None };
		next.Pressed += () =>
		{
			_paletteScroll = Math.Min(Math.Max(0, _palette.Count - 1), _paletteScroll + 1);
			RebuildPalette();
		};
		host.AddChild(next);

		RebuildPalette();
	}

	private void RebuildPalette()
	{
		foreach (var child in _paletteRow.GetChildren())
		{
			child.QueueFree();
		}

		var viewW = GetViewportRect().Size.X;
		var visible = Math.Max(1, (int)((viewW - 160) / (PaletteSlot + PaletteGap)));
		for (var i = 0; i < visible; i++)
		{
			var index = _paletteScroll + i;
			if (index >= _palette.Count)
			{
				break;
			}

			var def = _palette[index];
			var slot = new Button
			{
				CustomMinimumSize = new Vector2(PaletteSlot, PaletteSlot),
				TooltipText = $"{def.Name} ({def.Id})",
				FocusMode = FocusModeEnum.None,
				Flat = true,
			};
			var captured = def.Id;
			slot.Pressed += () => SelectTile(captured);

			var texPath = $"res://assets/sprites/tiles/{def.Name}.png";
			if (def.Id == GameConfig.TransitionMarkerTileId)
			{
				slot.Text = "T";
			}
			else if (ResourceLoader.Exists(texPath))
			{
				var icon = new TextureRect
				{
					Texture = GD.Load<Texture2D>(texPath),
					ExpandMode = TextureRect.ExpandModeEnum.IgnoreSize,
					StretchMode = TextureRect.StretchModeEnum.KeepAspectCentered,
					TextureFilter = TextureFilterEnum.Nearest,
					CustomMinimumSize = new Vector2(32, 32),
					SizeFlagsHorizontal = SizeFlags.ShrinkCenter,
					SizeFlagsVertical = SizeFlags.ShrinkCenter,
					MouseFilter = MouseFilterEnum.Ignore,
				};
				slot.AddChild(icon);
			}
			else
			{
				slot.Text = def.Id.ToString();
			}

			if (def.Id == _selectedTileId)
			{
				slot.Modulate = new Color(1.2f, 1.15f, 0.7f);
			}

			_paletteRow.AddChild(slot);
		}
	}

	private void BuildLoadPanel()
	{
		_loadPanel = new PanelContainer
		{
			Visible = false,
			CustomMinimumSize = new Vector2(420, 400),
		};
		var style = new StyleBoxFlat
		{
			BgColor = new Color(0.14f, 0.15f, 0.18f, 0.98f),
			ContentMarginLeft = 16,
			ContentMarginRight = 16,
			ContentMarginTop = 12,
			ContentMarginBottom = 12,
		};
		_loadPanel.AddThemeStyleboxOverride("panel", style);
		AddChild(_loadPanel);

		var vbox = new VBoxContainer();
		vbox.AddThemeConstantOverride("separation", 8);
		_loadPanel.AddChild(vbox);

		var header = new HBoxContainer();
		header.AddChild(new Label { Text = "Load map", SizeFlagsHorizontal = SizeFlags.ExpandFill });
		var close = new Button { Text = "Close", FocusMode = FocusModeEnum.None };
		close.Pressed += () => _loadPanel!.Visible = false;
		header.AddChild(close);
		vbox.AddChild(header);

		var scroll = new ScrollContainer
		{
			CustomMinimumSize = new Vector2(380, 320),
			SizeFlagsVertical = SizeFlags.ExpandFill,
		};
		vbox.AddChild(scroll);
		_loadList = new VBoxContainer();
		_loadList.AddThemeConstantOverride("separation", 4);
		scroll.AddChild(_loadList);
	}

	private void BuildNamePrompt()
	{
		_nameEdit = new LineEdit
		{
			Visible = false,
			PlaceholderText = "map_id",
			CustomMinimumSize = new Vector2(320, 36),
			MaxLength = 40,
		};
		_nameEdit.TextSubmitted += OnNameSubmitted;
		AddChild(_nameEdit);
	}

	private void OnViewportSizeChanged()
	{
		var size = GetViewportRect().Size;
		_viewHost.Position = new Vector2(SidePad, TopBarH);
		_viewHost.Size = new Vector2(size.X - SidePad * 2, size.Y - TopBarH - BottomBarH);
		_viewport.Size = new Vector2I((int)_viewHost.Size.X, (int)_viewHost.Size.Y);

		var midX = size.X * 0.5f;
		var midY = TopBarH + _viewHost.Size.Y * 0.5f;
		PlaceNamed("EdgeUpPlus", new Vector2(midX - 48, TopBarH + 4));
		PlaceNamed("EdgeUpMinus", new Vector2(midX + 4, TopBarH + 4));
		PlaceNamed("EdgeDownPlus", new Vector2(midX - 48, size.Y - BottomBarH - 40));
		PlaceNamed("EdgeDownMinus", new Vector2(midX + 4, size.Y - BottomBarH - 40));
		PlaceNamed("EdgeLeftPlus", new Vector2(4, midY - 40));
		PlaceNamed("EdgeLeftMinus", new Vector2(4, midY + 4));
		PlaceNamed("EdgeRightPlus", new Vector2(size.X - 48, midY - 40));
		PlaceNamed("EdgeRightMinus", new Vector2(size.X - 48, midY + 4));

		if (GetNodeOrNull("PaletteBar") is Control palette)
		{
			palette.Position = new Vector2(16, size.Y - BottomBarH + 16);
			palette.Size = new Vector2(size.X - 32, PaletteSlot + 8);
		}

		if (_loadPanel != null)
		{
			_loadPanel.Position = (size - _loadPanel.CustomMinimumSize) * 0.5f;
		}

		if (_nameEdit != null)
		{
			_nameEdit.Position = new Vector2(size.X * 0.5f - 160, size.Y * 0.5f - 20);
		}

		_status.Size = new Vector2(size.X - 24, 20);
		RebuildPalette();
	}

	private void PlaceNamed(string name, Vector2 pos)
	{
		if (GetNodeOrNull(name) is Control c)
		{
			c.Position = pos;
		}
	}

	private void ReloadWorldVisuals()
	{
		foreach (var child in _tileLayer.GetChildren())
		{
			child.QueueFree();
		}

		foreach (var child in _markerLayer.GetChildren())
		{
			child.QueueFree();
		}

		foreach (var child in _buildingLayer.GetChildren())
		{
			child.QueueFree();
		}

		_logicGround.Clear();
		for (var row = 0; row < _map.Height; row++)
		{
			for (var col = 0; col < _map.Width; col++)
			{
				var id = _map.GetTile(col, row);
				TileRotation.SetCell(_logicGround, new Vector2I(col, row), id);
			}
		}

		for (var row = 0; row < _map.Height; row++)
		{
			for (var col = 0; col < _map.Width; col++)
			{
				var id = _map.GetTile(col, row);
				if (!_catalog.TryGetValue(id, out var def) || def.Name == "transition_marker")
				{
					continue;
				}

				var basePath = ResolveSpritePath(col, row, id, def);
				var rot = id == GameConfig.WaterTileId
					|| OverlayTiles.NeedsDrawnOverlay(id)
					|| TallTreeSprites.IsTallTree(id)
					? 0
					: _map.GetRotation(col, row);

				AddWorldSprite(basePath, col, row, rot);

				// Trees: show stump only (no canopy). Stumps/beds/bridges: base + overlay.
				if (TallTreeSprites.IsTallTree(id))
				{
					var stumpId = TallTreeSprites.StumpTileIdForTree(id);
					var stumpName = stumpId == GameConfig.AspenStumpTileId ? "aspen_stump" : "pine_stump";
					AddWorldSprite($"res://assets/sprites/tiles/{stumpName}.png", col, row, 0);
				}
				else if (TallTreeSprites.IsTreeStump(id) || OverlayTiles.IsBridge(id) || OverlayTiles.IsBed(id))
				{
					AddWorldSprite(OverlayTiles.OverlaySpritePath(id, def.Name), col, row, 0);
				}

				if (_map.HasMarker(col, row))
				{
					var mark = new ColorRect
					{
						Color = new Color(1f, 0.85f, 0.15f, 0.55f),
						Size = new Vector2(GameConfig.TileSize - 4, GameConfig.TileSize - 4),
						Position = new Vector2(col * GameConfig.TileSize + 2, row * GameConfig.TileSize + 2),
						MouseFilter = MouseFilterEnum.Ignore,
					};
					_markerLayer.AddChild(mark);
				}
			}
		}

		// Map bounds wash
		var outline = new ColorRect
		{
			Color = new Color(1f, 1f, 1f, 0.08f),
			Size = new Vector2(_map.Width * GameConfig.TileSize, _map.Height * GameConfig.TileSize),
			MouseFilter = MouseFilterEnum.Ignore,
			ZIndex = -1,
		};
		_tileLayer.AddChild(outline);
		_tileLayer.MoveChild(outline, 0);

		DrawBuildingGuides();
	}

	private void AddWorldSprite(string path, int col, int row, int rotationQuarters)
	{
		if (!ResourceLoader.Exists(path))
		{
			return;
		}

		var sprite = new Sprite2D
		{
			Texture = GD.Load<Texture2D>(path),
			Centered = true,
			TextureFilter = TextureFilterEnum.Nearest,
			Position = new Vector2(
				col * GameConfig.TileSize + GameConfig.TileSize / 2f,
				row * GameConfig.TileSize + GameConfig.TileSize / 2f
			),
			RotationDegrees = TileRotation.Degrees(rotationQuarters),
		};
		_tileLayer.AddChild(sprite);
	}

	/// <summary>Red dotted footprints for buildings that aren't map tiles (cabin on homestead).</summary>
	private void DrawBuildingGuides()
	{
		if (!string.Equals(_map.MapId, "homestead", StringComparison.OrdinalIgnoreCase))
		{
			return;
		}

		// CabinBuilding: 80×96 sprite, footprint cols 14–18, bottom row 10 → covers rows 5–10.
		const int footCol = 14;
		const int footBottomRow = 10;
		const int spriteW = 80;
		const int spriteH = 96;
		var rect = new Rect2(
			footCol * GameConfig.TileSize,
			(footBottomRow + 1) * GameConfig.TileSize - spriteH,
			spriteW,
			spriteH
		);
		DrawDottedRect(rect, new Color(1f, 0.22f, 0.22f, 0.95f));
	}

	private void DrawDottedRect(Rect2 rect, Color color)
	{
		const float dash = 3f;
		const float gap = 2f;
		const float thick = 1.5f;

		void HLine(float y, float x0, float x1)
		{
			for (var x = x0; x < x1; x += dash + gap)
			{
				var w = Math.Min(dash, x1 - x);
				_buildingLayer.AddChild(new ColorRect
				{
					Color = color,
					Position = new Vector2(x, y),
					Size = new Vector2(w, thick),
					MouseFilter = MouseFilterEnum.Ignore,
				});
			}
		}

		void VLine(float x, float y0, float y1)
		{
			for (var y = y0; y < y1; y += dash + gap)
			{
				var h = Math.Min(dash, y1 - y);
				_buildingLayer.AddChild(new ColorRect
				{
					Color = color,
					Position = new Vector2(x, y),
					Size = new Vector2(thick, h),
					MouseFilter = MouseFilterEnum.Ignore,
				});
			}
		}

		var x0 = rect.Position.X;
		var y0 = rect.Position.Y;
		var x1 = rect.Position.X + rect.Size.X;
		var y1 = rect.Position.Y + rect.Size.Y;
		HLine(y0, x0, x1);
		HLine(y1 - thick, x0, x1);
		VLine(x0, y0, y1);
		VLine(x1 - thick, y0, y1);
	}

	private string ResolveSpritePath(int col, int row, int tileId, TileDef def)
	{
		if (tileId == GameConfig.WaterTileId || OverlayTiles.IsBridge(tileId))
		{
			return WaterTileSprites.ResolveSpritePath(_logicGround, col, row);
		}

		return OverlayTiles.BaseSpritePath(tileId) ?? $"res://assets/sprites/tiles/{def.Name}.png";
	}

	private void PaintAtLocal(Vector2 localPos, bool erase)
	{
		if (!TryLocalToCell(localPos, out var col, out var row) || !_map.InBounds(col, row))
		{
			return;
		}

		if (_selectedTileId == GameConfig.TransitionMarkerTileId)
		{
			_map.SetMarker(col, row, !erase);
		}
		else if (erase)
		{
			_map.SetTile(col, row, GameConfig.BlankTileId, 0);
			_map.SetMarker(col, row, false);
		}
		else
		{
			var rot = _selectedTileId == GameConfig.WaterTileId ? 0 : _paintRotation;
			_map.SetTile(col, row, _selectedTileId, rot);
			if (_selectedTileId == GameConfig.BedTileId)
			{
				var head = BedPlaceable.HeadRow(row);
				if (_map.InBounds(col, head))
				{
					_map.SetTile(col, head, GameConfig.BedTileId, rot);
				}
			}
		}

		_dirty = true;
	}

	private bool TryLocalToCell(Vector2 localPos, out int col, out int row)
	{
		col = 0;
		row = 0;
		if (localPos.X < 0 || localPos.Y < 0 || localPos.X >= _viewHost.Size.X || localPos.Y >= _viewHost.Size.Y)
		{
			return false;
		}

		var world = _camera.Position + (localPos - _viewHost.Size * 0.5f) / _camera.Zoom;
		col = (int)Math.Floor(world.X / GameConfig.TileSize);
		row = (int)Math.Floor(world.Y / GameConfig.TileSize);
		return true;
	}

	private bool PointInMapArea(Vector2 screenPos)
	{
		var r = new Rect2(_viewHost.Position, _viewHost.Size);
		return r.HasPoint(screenPos);
	}

	private void BeginStroke()
	{
		if (_strokeUndoPushed)
		{
			return;
		}

		PushUndo();
		_strokeUndoPushed = true;
	}

	private void PushUndo()
	{
		_undo.Add(new EditSnapshot
		{
			Tiles = _map.CopyTiles(),
			Rotations = _map.CopyRotations(),
			Markers = _map.CopyMarkers(),
		});
		while (_undo.Count > MaxUndo)
		{
			_undo.RemoveAt(0);
		}
	}

	private void Undo()
	{
		if (_undo.Count == 0)
		{
			SetStatus("Nothing to undo");
			return;
		}

		var snap = _undo[^1];
		_undo.RemoveAt(_undo.Count - 1);
		_map.Restore(snap.Tiles, snap.Rotations, snap.Markers);
		_dirty = true;
		SetStatus("Undid last change");
	}

	private void NudgeRotation(int delta)
	{
		_paintRotation = (_paintRotation + delta) & 3;
		SetStatus($"Brush rotation: {_paintRotation * 90}°");
		RefreshChrome();
	}

	private void SelectTile(int id)
	{
		_selectedTileId = id;
		if (id == GameConfig.TransitionMarkerTileId)
		{
			SetStatus("Transfer marker (T): LMB place · RMB clear · invisible in-game");
		}
		else if (_catalog.TryGetValue(id, out var def))
		{
			SetStatus($"Selected: {def.Name} ({id})");
		}

		RebuildPalette();
		RefreshChrome();
	}

	private void ResizeMap(Action mutate, string okMessage) =>
		ResizeMap(() =>
		{
			mutate();
			return true;
		}, okMessage, allowFail: false);

	private void ResizeMap(Func<bool> mutate, string okMessage, bool allowFail)
	{
		PushUndo();
		if (!mutate())
		{
			_undo.RemoveAt(_undo.Count - 1);
			SetStatus(allowFail ? "Map already at minimum size" : "Resize failed");
			return;
		}

		_dirty = true;
		SetStatus($"{okMessage} ({_map.Width}x{_map.Height})");
	}

	private void HandleCameraPan(float dt)
	{
		var speed = 220f / Math.Max(0.01f, _camera.Zoom.X);
		var dir = Vector2.Zero;
		if (Input.IsKeyPressed(Key.W) || Input.IsKeyPressed(Key.Up))
		{
			dir.Y -= 1;
		}

		if (Input.IsKeyPressed(Key.S) || Input.IsKeyPressed(Key.Down))
		{
			dir.Y += 1;
		}

		if (Input.IsKeyPressed(Key.A) || Input.IsKeyPressed(Key.Left))
		{
			dir.X -= 1;
		}

		if (Input.IsKeyPressed(Key.D) || Input.IsKeyPressed(Key.Right))
		{
			dir.X += 1;
		}

		if (dir != Vector2.Zero)
		{
			_camera.Position += dir.Normalized() * speed * dt;
		}
	}

	private void AdjustZoom(float factor)
	{
		var z = Mathf.Clamp(_camera.Zoom.X * factor, MinZoom, MaxZoom);
		_camera.Zoom = new Vector2(z, z);
	}

	private void CenterCamera()
	{
		_camera.Position = new Vector2(
			_map.Width * GameConfig.TileSize * 0.5f,
			_map.Height * GameConfig.TileSize * 0.5f
		);
		_camera.Zoom = new Vector2(2f, 2f);
	}

	private void PromptNew()
	{
		HideOverlays();
		_nameEdit!.Visible = true;
		_nameEdit.Text = "untitled";
		_nameEdit.GrabFocus();
		_nameEdit.SetMeta("mode", "new");
		SetStatus("Enter id for new map (or leave untitled)");
	}

	private void BeginSave()
	{
		HideOverlays();
		if (EditorMapIo.IsValidMapId(_map.MapId) && _map.MapId != "untitled")
		{
			SaveCurrent();
			return;
		}

		_nameEdit!.Visible = true;
		_nameEdit.Text = _map.MapId == "untitled" ? "" : _map.MapId;
		_nameEdit.GrabFocus();
		_nameEdit.SetMeta("mode", "save");
		SetStatus("Enter map id to save");
	}

	private void OnNameSubmitted(string text)
	{
		var mode = _nameEdit!.HasMeta("mode") ? _nameEdit.GetMeta("mode").AsString() : "save";
		_nameEdit.Visible = false;
		if (string.IsNullOrWhiteSpace(text))
		{
			if (mode == "new")
			{
				PushUndo();
				_map = EditableTileMap.CreateDefault();
				_undo.Clear();
				CenterCamera();
				_dirty = true;
				SetStatus("New 5x5 blank map");
			}

			return;
		}

		if (!EditorMapIo.IsValidMapId(text))
		{
			SetStatus("Invalid id — use letters/numbers/underscore, start with a letter");
			return;
		}

		var id = EditorMapIo.NormalizeMapId(text);
		if (mode == "new")
		{
			PushUndo();
			_map = EditableTileMap.CreateDefault(id);
			_undo.Clear();
			CenterCamera();
			_dirty = true;
			SetStatus($"New map '{id}' (5x5 blank)");
			return;
		}

		_map.MapId = id;
		SaveCurrent();
	}

	private void SaveCurrent()
	{
		try
		{
			EditorMapIo.Save(_map);
			SetStatus($"Saved {_map.MapId}.map ({_map.Width}x{_map.Height})");
		}
		catch (Exception ex)
		{
			SetStatus($"Save failed: {ex.Message}");
		}
	}

	private void OpenLoadPanel()
	{
		HideOverlays();
		if (_loadList == null || _loadPanel == null)
		{
			return;
		}

		_loadList.ClearChildrenSafe();
		var ids = EditorMapIo.ListMapIds();
		if (ids.Count == 0)
		{
			SetStatus("No maps in data/");
			return;
		}

		foreach (var id in ids)
		{
			var captured = id;
			var btn = new Button
			{
				Text = id,
				Alignment = HorizontalAlignment.Left,
				FocusMode = FocusModeEnum.None,
				CustomMinimumSize = new Vector2(360, 32),
			};
			btn.Pressed += () =>
			{
				LoadMap(captured);
				_loadPanel.Visible = false;
			};
			_loadList.AddChild(btn);
		}

		_loadPanel.Visible = true;
	}

	private void LoadMap(string id)
	{
		try
		{
			PushUndo();
			_map = EditorMapIo.Load(id);
			_undo.Clear();
			CenterCamera();
			_dirty = true;
			SetStatus($"Loaded {id} ({_map.Width}x{_map.Height})");
		}
		catch (Exception ex)
		{
			SetStatus($"Load failed: {ex.Message}");
		}
	}

	private void HideOverlays()
	{
		if (_nameEdit != null)
		{
			_nameEdit.Visible = false;
		}

		if (_loadPanel != null)
		{
			_loadPanel.Visible = false;
		}
	}

	private void GoBack()
	{
		GetTree().ChangeSceneToFile("res://scenes/Title.tscn");
	}

	private void SetStatus(string text)
	{
		_statusText = text;
		_status.Text = text;
	}

	private void RefreshChrome()
	{
		_status.Text = _statusText;
		var sel = _catalog.TryGetValue(_selectedTileId, out var def) ? def.Name : "?";
		_info.Text = $"  {_map.MapId}  {_map.Width}x{_map.Height}  |  brush {sel}  rot {_paintRotation * 90}°";
	}

	private static Button ToolButton(string text, Action action)
	{
		var btn = new Button
		{
			Text = text,
			CustomMinimumSize = new Vector2(72, 36),
			FocusMode = FocusModeEnum.None,
		};
		btn.Pressed += action;
		return btn;
	}
}

internal static class EditorUiExtensions
{
	public static void ClearChildrenSafe(this Node node)
	{
		foreach (var child in node.GetChildren())
		{
			node.RemoveChild(child);
			child.QueueFree();
		}
	}
}
