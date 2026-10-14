using System.Collections.Generic;
using Godot;
using LumberjackAttack.Core;
using LumberjackAttack.Items;
using LumberjackAttack.Ui;

namespace LumberjackAttack.Fishing;

/// <summary>Canvas overlay for the underwater fishing minigame.</summary>
public partial class FishingMinigameView : CanvasLayer
{
	private FishingMinigame? _minigame;
	private IReadOnlyDictionary<string, ItemDef> _items = null!;
	private Control _root = null!;
	private ColorRect _dim = null!;
	private PanelContainer _panel = null!;
	private Label _title = null!;
	private Label _depth = null!;
	private Control _play = null!;
	private ColorRect _line = null!;
	private ColorRect _hook = null!;
	private Label _result = null!;
	private readonly List<Control> _entityNodes = new();
	private readonly Dictionary<string, Texture2D> _textures = new();

	public bool IsOpen => _minigame != null;

	public void Configure(IReadOnlyDictionary<string, ItemDef> items)
	{
		_items = items;
	}

	public override void _Ready()
	{
		Layer = 40;
		Visible = false;
		GameFont.ApplyToCanvasLayer(this);

		_root = new Control
		{
			Name = "FishingMinigameRoot",
			MouseFilter = Control.MouseFilterEnum.Stop,
			AnchorsPreset = (int)Control.LayoutPreset.FullRect,
		};
		AddChild(_root);

		_dim = new ColorRect
		{
			Color = new Color(0, 0, 0, 0.67f),
			MouseFilter = Control.MouseFilterEnum.Stop,
			AnchorsPreset = (int)Control.LayoutPreset.FullRect,
		};
		_root.AddChild(_dim);

		_panel = new PanelContainer { Name = "Panel" };
		var style = new StyleBoxFlat
		{
			BgColor = new Color(0.06f, 0.18f, 0.33f, 1f),
			BorderColor = new Color(0.78f, 0.9f, 1f),
			BorderWidthLeft = 2,
			BorderWidthTop = 2,
			BorderWidthRight = 2,
			BorderWidthBottom = 2,
			ContentMarginLeft = 0,
			ContentMarginTop = 0,
			ContentMarginRight = 0,
			ContentMarginBottom = 0,
		};
		_panel.AddThemeStyleboxOverride("panel", style);
		_panel.CustomMinimumSize = new Vector2(
			GameConfig.FishingMinigamePanelWidth,
			GameConfig.FishingMinigamePanelHeight
		);
		_root.AddChild(_panel);

		_play = new Control
		{
			Name = "Play",
			CustomMinimumSize = new Vector2(
				GameConfig.FishingMinigamePanelWidth,
				GameConfig.FishingMinigamePanelHeight
			),
			MouseFilter = Control.MouseFilterEnum.Ignore,
		};
		_panel.AddChild(_play);

		_title = new Label
		{
			Text = "Fishing — A / D to move · Esc cancel",
			Position = new Vector2(16, 12),
			Size = new Vector2(440, 24),
			MouseFilter = Control.MouseFilterEnum.Ignore,
		};
		_title.AddThemeFontSizeOverride("font_size", 16);
		_title.AddThemeColorOverride("font_color", new Color(0.7f, 0.82f, 0.94f));
		_play.AddChild(_title);

		_depth = new Label
		{
			Text = "Depth: 0",
			Position = new Vector2(16, 36),
			Size = new Vector2(200, 24),
			MouseFilter = Control.MouseFilterEnum.Ignore,
		};
		_depth.AddThemeFontSizeOverride("font_size", 14);
		_depth.AddThemeColorOverride("font_color", new Color(0.7f, 0.82f, 0.94f));
		_play.AddChild(_depth);

		_line = new ColorRect
		{
			Color = new Color(0.82f, 0.82f, 0.82f),
			Size = new Vector2(2, 1),
			MouseFilter = Control.MouseFilterEnum.Ignore,
		};
		_play.AddChild(_line);

		_hook = new ColorRect
		{
			Color = Colors.White,
			Size = new Vector2(FishingMinigame.HookWidth, FishingMinigame.HookHeight),
			MouseFilter = Control.MouseFilterEnum.Ignore,
		};
		_play.AddChild(_hook);

		_result = new Label
		{
			Visible = false,
			HorizontalAlignment = HorizontalAlignment.Center,
			VerticalAlignment = VerticalAlignment.Center,
			Size = new Vector2(GameConfig.FishingMinigamePanelWidth, 80),
			Position = new Vector2(0, GameConfig.FishingMinigamePanelHeight / 2f - 40),
			MouseFilter = Control.MouseFilterEnum.Ignore,
		};
		_result.AddThemeFontSizeOverride("font_size", 22);
		_result.AddThemeColorOverride("font_color", new Color(1f, 0.92f, 0.45f));
		_result.AddThemeColorOverride("font_outline_color", Colors.Black);
		_result.AddThemeConstantOverride("outline_size", 4);
		GameFont.ApplyDisplay(_result, 22);
		_play.AddChild(_result);

		GetViewport().SizeChanged += LayoutPanel;
		LayoutPanel();
	}

	public void Open(FishingMinigame minigame)
	{
		_minigame = minigame;
		Visible = true;
		_result.Visible = false;
		ClearEntities();
		LayoutPanel();
		Redraw();
	}

	public void Close()
	{
		_minigame = null;
		Visible = false;
		ClearEntities();
	}

	public void Redraw()
	{
		if (_minigame == null)
		{
			return;
		}

		_depth.Text = $"Depth: {(int)_minigame.Depth}";

		var hookX = (float)_minigame.LineX;
		var hookY = _minigame.HookScreenY;
		_line.Position = new Vector2(hookX - 1, _minigame.PlayAreaTop);
		_line.Size = new Vector2(2, Math.Max(1, hookY - _minigame.PlayAreaTop));
		_hook.Position = new Vector2(hookX - FishingMinigame.HookWidth / 2f, hookY - FishingMinigame.HookHeight / 2f);

		SyncEntities();

		if (_minigame.Finished && _minigame.CaughtItemId != null)
		{
			var name = _items.TryGetValue(_minigame.CaughtItemId, out var def)
				? def.Name
				: _minigame.CaughtItemId;
			_result.Text = $"Caught {name}!";
			_result.Visible = true;
		}
		else
		{
			_result.Visible = false;
		}
	}

	private void SyncEntities()
	{
		if (_minigame == null)
		{
			return;
		}

		var entities = _minigame.Entities;
		while (_entityNodes.Count < entities.Count)
		{
			var icon = new TextureRect
			{
				ExpandMode = TextureRect.ExpandModeEnum.IgnoreSize,
				StretchMode = TextureRect.StretchModeEnum.KeepAspectCentered,
				TextureFilter = CanvasItem.TextureFilterEnum.Nearest,
				MouseFilter = Control.MouseFilterEnum.Ignore,
				Size = new Vector2(FishingMinigame.EntityWidth, FishingMinigame.EntityHeight),
			};
			_play.AddChild(icon);
			_play.MoveChild(icon, _hook.GetIndex());
			_entityNodes.Add(icon);
		}

		for (var i = 0; i < _entityNodes.Count; i++)
		{
			var node = _entityNodes[i];
			if (i >= entities.Count)
			{
				node.Visible = false;
				continue;
			}

			var e = entities[i];
			var screenY = (float)_minigame.WorldYToScreenY(e.Y);
			if (screenY + e.Height < _minigame.PlayAreaTop || screenY > _minigame.PanelHeight)
			{
				node.Visible = false;
				continue;
			}

			node.Visible = true;
			node.Position = new Vector2((float)e.X, screenY);
			if (node is TextureRect tr)
			{
				tr.Texture = GetItemTexture(e.ItemId);
			}
		}
	}

	private void ClearEntities()
	{
		foreach (var node in _entityNodes)
		{
			node.QueueFree();
		}

		_entityNodes.Clear();
	}

	private Texture2D? GetItemTexture(string itemId)
	{
		if (_textures.TryGetValue(itemId, out var cached))
		{
			return cached;
		}

		var path = $"res://assets/sprites/items/{itemId}.png";
		if (!ResourceLoader.Exists(path))
		{
			return null;
		}

		var tex = GD.Load<Texture2D>(path);
		_textures[itemId] = tex;
		return tex;
	}

	private void LayoutPanel()
	{
		var size = GetViewport().GetVisibleRect().Size;
		_panel.Position = new Vector2(
			(size.X - GameConfig.FishingMinigamePanelWidth) / 2f,
			(size.Y - GameConfig.FishingMinigamePanelHeight) / 2f
		);
	}
}
