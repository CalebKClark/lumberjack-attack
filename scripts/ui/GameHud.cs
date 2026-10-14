using System;
using System.Collections.Generic;
using System.Text;
using Godot;
using LumberjackAttack.Collection;
using LumberjackAttack.Combat;
using LumberjackAttack.Core;
using LumberjackAttack.Crafting;
using LumberjackAttack.Economy;
using LumberjackAttack.Fishing;
using LumberjackAttack.Inventory;
using LumberjackAttack.Items;
using LumberjackAttack.Skills;
using LumberjackAttack.Settings;
using LumberjackAttack.Time;
using LumberjackAttack.World.Placeable;

namespace LumberjackAttack.Ui;

/// <summary>Hotbar, unified E-menu (inventory/skills/craft/stats/collections), chests, chipper, tooltips.</summary>
public partial class GameHud : CanvasLayer
{
	private PlayerInventory _inventory = null!;
	private IReadOnlyDictionary<string, ItemDef> _items = null!;
	private GameClock _clock = null!;
	private PlayerEnergyState _energy = null!;
	private InventoryCursor _cursor = null!;
	private CraftingService _crafting = null!;
	private PlayerDiscoveries _discoveries = null!;
	private PlayerBank _bank = null!;
	private PlayerSkills _skills = null!;
	private InputSettings _inputSettings = null!;
	private Func<bool>? _tryOpenHoveredPlaceable;
	private IReadOnlyList<FishSpawnDef> _fishTable = Array.Empty<FishSpawnDef>();
	private Action? _beforeRefresh;
	private PlayerHealthState? _health;

	private Label _statusLabel = null!;
	private NotificationFeed _feed = new();
	private Control _feedRoot = null!;
	private VBoxContainer _feedList = null!;
	private Control _xpRoot = null!;
	private VBoxContainer _xpList = null!;
	private Control _hotbar = null!;
	/// <summary>Health + energy bars (bottom-right).</summary>
	private Control _vitalsHud = null!;
	private ColorRect _healthDeplete = null!;
	private ColorRect _energyDeplete = null!;
	private Label _healthValueLabel = null!;
	private Label _energyValueLabel = null!;
	private float _vitalsFillStartX;
	private float _vitalsFillWidth;
	private float _vitalsBarHeight;
	private ColorRect _menuDim = null!;
	private PanelContainer _playerMenu = null!;
	private Control _backpack = null!;
	private Control _craftHost = null!;
	private Control _skillsHost = null!;
	private Control _collectionsHost = null!;
	private Control _statsHost = null!;
	private VBoxContainer _statsList = null!;
	private Label _statsHeldLabel = null!;
	private Label _statsHpLabel = null!;
	private readonly Dictionary<PlayerMenuTab, Button> _tabButtons = new();
	private PanelContainer _storageOverlay = null!;
	private Label _storageTitle = null!;
	private Control _storageChestHost = null!;
	private Control _storageChipperHost = null!;
	private Panel[] _chestSlots = null!;
	private ItemContainer? _openChest;
	private Panel _chipperInputSlot = null!;
	private Panel _chipperOutputSlot = null!;
	private ProgressBar _chipperProgress = null!;
	private Label _chipperStatus = null!;
	private WoodChipperMachine? _openChipper;
	private VBoxContainer _craftList = null!;
	private VBoxContainer _skillsList = null!;
	private VBoxContainer _collectionsList = null!;
	private Label _collectionsTitle = null!;
	private CollectionCategory? _collectionsFocus;
	private Panel[] _hotbarSlots = null!;
	private Panel[] _inventoryGridSlots = null!;
	private Panel[] _storageInventorySlots = null!;
	private Control _cursorGhost = null!;
	private TextureRect _cursorIcon = null!;
	private Label _cursorQty = null!;
	private PanelContainer _tooltip = null!;
	private VBoxContainer _tooltipLines = null!;
	private bool _menuOpen;
	private PlayerMenuTab _tab = PlayerMenuTab.Inventory;

	public bool IsPlayerMenuOpen => _menuOpen;
	public bool IsBackpackOpen =>
		(_menuOpen && _tab == PlayerMenuTab.Inventory) || _openChest != null || _openChipper != null;
	public bool IsCraftOpen => _menuOpen && _tab == PlayerMenuTab.Crafting;
	public bool IsSkillsOpen => _menuOpen && _tab == PlayerMenuTab.Skills;
	public bool IsCollectionsOpen => _menuOpen && _tab == PlayerMenuTab.Collections;
	public bool IsChestOpen => _openChest != null;
	public bool IsChipperOpen => _openChipper != null;
	public bool IsPointerOverUi { get; private set; }

	public void Configure(
		PlayerInventory inventory,
		IReadOnlyDictionary<string, ItemDef> items,
		GameClock clock,
		PlayerEnergyState energy,
		CraftingService crafting,
		PlayerDiscoveries discoveries,
		PlayerBank bank,
		PlayerSkills skills,
		PlayerHealthState? health = null,
		InputSettings? inputSettings = null,
		Func<bool>? tryOpenHoveredPlaceable = null,
		IReadOnlyList<FishSpawnDef>? fishTable = null,
		Action? beforeRefresh = null
	)
	{
		_inventory = inventory;
		_items = items;
		_clock = clock;
		_energy = energy;
		_crafting = crafting;
		_discoveries = discoveries;
		_bank = bank;
		_skills = skills;
		_health = health;
		_inputSettings = inputSettings ?? InputSettings.Load();
		_tryOpenHoveredPlaceable = tryOpenHoveredPlaceable;
		_fishTable = fishTable ?? Array.Empty<FishSpawnDef>();
		_beforeRefresh = beforeRefresh;
		_cursor = new InventoryCursor(items);
	}

	public override void _Ready()
	{
		Layer = 20;
		GameFont.ApplyToCanvasLayer(this);

		_statusLabel = new Label
		{
			Position = new Vector2(8, 8),
			Size = new Vector2(780, 48),
			MouseFilter = Control.MouseFilterEnum.Ignore,
		};
		_statusLabel.AddThemeFontSizeOverride("font_size", 14);
		_statusLabel.AddThemeColorOverride("font_outline_color", Colors.Black);
		_statusLabel.AddThemeConstantOverride("outline_size", 4);
		AddChild(_statusLabel);

		_feedRoot = new Control
		{
			Name = "NotificationFeed",
			MouseFilter = Control.MouseFilterEnum.Ignore,
		};
		AddChild(_feedRoot);
		_feedList = new VBoxContainer
		{
			MouseFilter = Control.MouseFilterEnum.Ignore,
		};
		_feedList.AddThemeConstantOverride("separation", 2);
		_feedRoot.AddChild(_feedList);

		_xpRoot = new Control
		{
			Name = "XpPopups",
			MouseFilter = Control.MouseFilterEnum.Ignore,
		};
		AddChild(_xpRoot);
		_xpList = new VBoxContainer
		{
			MouseFilter = Control.MouseFilterEnum.Ignore,
			Alignment = BoxContainer.AlignmentMode.Center,
		};
		_xpList.AddThemeConstantOverride("separation", 4);
		_xpRoot.AddChild(_xpList);

		_menuDim = new ColorRect
		{
			Name = "MenuDim",
			Color = new Color(0f, 0f, 0f, 0.45f),
			MouseFilter = Control.MouseFilterEnum.Stop,
			Visible = false,
			ZIndex = 0,
		};
		_menuDim.SetAnchorsPreset(Control.LayoutPreset.FullRect);
		AddChild(_menuDim);

		_hotbar = BuildSlotRow(GameConfig.HotbarSlots, out _hotbarSlots, hotbar: true);
		_hotbar.ZIndex = 2;
		AddChild(_hotbar);

		_vitalsHud = BuildVitalsHud();
		_vitalsHud.ZIndex = 2;
		AddChild(_vitalsHud);

		_playerMenu = BuildPlayerMenu();
		_playerMenu.Visible = false;
		_playerMenu.ZIndex = 1;
		AddChild(_playerMenu);

		_storageOverlay = BuildStorageOverlay();
		_storageOverlay.Visible = false;
		_storageOverlay.ZIndex = 1;
		AddChild(_storageOverlay);

		_cursorGhost = BuildCursorGhost();
		_cursorGhost.Visible = false;
		AddChild(_cursorGhost);

		_tooltip = BuildTooltip();
		_tooltip.Visible = false;
		AddChild(_tooltip);

		GetViewport().SizeChanged += LayoutBars;
		LayoutBars();
		Refresh();
	}

	public override void _Process(double delta)
	{
		var mouse = GetViewport().GetMousePosition();
		IsPointerOverUi = IsMouseOverHud(mouse);

		if (_statusLabel != null)
		{
			_statusLabel.Text = FormatStatus();
		}

		_feed.Update(delta * 1000.0);
		PaintNotificationFeed();

		if (_cursor.IsCarrying)
		{
			_cursorGhost.Visible = true;
			_cursorGhost.Position = mouse + new Vector2(8, 8);
			PaintCursorGhost();
		}
		else
		{
			_cursorGhost.Visible = false;
		}

		if (_openChipper != null)
		{
			PaintChipper();
		}

		UpdateTooltip(mouse);
	}

	public override void _UnhandledInput(InputEvent @event)
	{
		if (@event is InputEventMouseButton { Pressed: true } wheel
			&& wheel.ButtonIndex is MouseButton.WheelUp or MouseButton.WheelDown)
		{
			if (!IsPointerOverScrollContainer())
			{
				var delta = wheel.ButtonIndex == MouseButton.WheelUp ? -1 : 1;
				var next = (_inventory.SelectedHotbarSlot + delta + GameConfig.HotbarSlots)
					% GameConfig.HotbarSlots;
				_inventory.SelectHotbarSlot(next);
				Refresh();
				GetViewport().SetInputAsHandled();
			}

			return;
		}

		if (@event is InputEventKey { Pressed: true, Echo: false } key)
		{
			var invKey = _inputSettings.GetBinding(GameAction.Inventory);
			if (key.Keycode == invKey)
			{
				if (_openChest != null)
				{
					CloseChest();
					GetViewport().SetInputAsHandled();
					return;
				}

				if (_openChipper != null)
				{
					CloseChipper();
					GetViewport().SetInputAsHandled();
					return;
				}

				if (_menuOpen)
				{
					TogglePlayerMenu();
					GetViewport().SetInputAsHandled();
					return;
				}

				if (_tryOpenHoveredPlaceable?.Invoke() == true)
				{
					GetViewport().SetInputAsHandled();
					return;
				}

				TogglePlayerMenu();
				GetViewport().SetInputAsHandled();
				return;
			}

			foreach (GameAction action in Enum.GetValues(typeof(GameAction)))
			{
				var slot = action.HotbarIndex();
				if (slot < 0 || key.Keycode != _inputSettings.GetBinding(action))
				{
					continue;
				}

				_inventory.SelectHotbarSlot(slot);
				Refresh();
				GetViewport().SetInputAsHandled();
				return;
			}
		}
	}

	public NotificationFeed Feed => _feed;

	public void ShowToast(string message, double durationMs = 2500)
	{
		_feed.NotifyMessage(message);
	}

	public void NotifyItemsAdded(string itemId, int quantity)
	{
		if (quantity <= 0)
		{
			return;
		}

		var name = _items.TryGetValue(itemId, out var def) ? def.Name : itemId;
		_feed.NotifyItemsAdded(itemId, name, quantity);
	}

	private void PaintNotificationFeed()
	{
		if (_feedList == null)
		{
			return;
		}

		while (_feedList.GetChildCount() > _feed.Lines.Count)
		{
			var last = _feedList.GetChild(_feedList.GetChildCount() - 1);
			_feedList.RemoveChild(last);
			last.Free();
		}

		while (_feedList.GetChildCount() < _feed.Lines.Count)
		{
			var label = new Label { MouseFilter = Control.MouseFilterEnum.Ignore };
			label.AddThemeFontSizeOverride("font_size", 14);
			label.AddThemeColorOverride("font_outline_color", Colors.Black);
			label.AddThemeConstantOverride("outline_size", 4);
			_feedList.AddChild(label);
		}

		for (var i = 0; i < _feed.Lines.Count; i++)
		{
			var line = _feed.Lines[i];
			var label = (Label)_feedList.GetChild(i);
			label.Text = line.Text;
			var a = line.Alpha;
			label.AddThemeColorOverride("font_color", new Color(1f, 0.9f, 0.27f, a));
			label.Modulate = new Color(1, 1, 1, a);
		}

		while (_xpList.GetChildCount() > _feed.XpPopups.Count)
		{
			var last = _xpList.GetChild(_xpList.GetChildCount() - 1);
			_xpList.RemoveChild(last);
			last.Free();
		}

		while (_xpList.GetChildCount() < _feed.XpPopups.Count)
		{
			var label = new Label
			{
				MouseFilter = Control.MouseFilterEnum.Ignore,
				HorizontalAlignment = HorizontalAlignment.Center,
			};
			label.AddThemeFontSizeOverride("font_size", 14);
			label.AddThemeColorOverride("font_outline_color", Colors.Black);
			label.AddThemeConstantOverride("outline_size", 4);
			_xpList.AddChild(label);
		}

		for (var i = 0; i < _feed.XpPopups.Count; i++)
		{
			var popup = _feed.XpPopups[i];
			var label = (Label)_xpList.GetChild(i);
			label.Text = popup.Text;
			var a = popup.Alpha;
			label.AddThemeColorOverride("font_color", new Color(1f, 0.9f, 0.27f, a));
			label.Modulate = new Color(1, 1, 1, a);
		}

		LayoutNotificationFeed();
	}

	private void LayoutNotificationFeed()
	{
		if (_feedRoot == null || _hotbar == null)
		{
			return;
		}

		var size = GetViewport().GetVisibleRect().Size;
		var hotbarTop = _hotbar.Position.Y;
		var feedH = Math.Max(20f, _feedList.GetCombinedMinimumSize().Y);
		_feedRoot.Position = new Vector2(24, hotbarTop - feedH - 12);
		_feedList.CustomMinimumSize = new Vector2(420, 0);

		var xpH = Math.Max(20f, _xpList.GetCombinedMinimumSize().Y);
		var maxDrift = 0.0;
		foreach (var p in _feed.XpPopups)
		{
			maxDrift = Math.Max(maxDrift, p.OffsetY);
		}

		_xpRoot.Position = new Vector2(size.X / 2f - 80, hotbarTop - xpH - 18 - (float)maxDrift);
		_xpList.CustomMinimumSize = new Vector2(160, 0);
	}

	public void OpenChest(ItemContainer chest)
	{
		CloseChipper();
		if (_menuOpen)
		{
			ClosePlayerMenu(stowCursor: false);
		}

		_openChest = chest;
		_storageTitle.Text = "Chest";
		_storageChestHost.Visible = true;
		_storageChipperHost.Visible = false;
		_storageOverlay.Visible = true;
		SetMenuDimVisible(true);
		Refresh();
	}

	public void CloseChest()
	{
		if (_openChest == null)
		{
			return;
		}

		_cursor.StowInto(_inventory, _bank);
		_openChest = null;
		_storageOverlay.Visible = false;
		if (!_menuOpen)
		{
			SetMenuDimVisible(false);
		}

		Refresh();
	}

	public void OpenChipper(WoodChipperMachine machine)
	{
		CloseChest();
		if (_menuOpen)
		{
			ClosePlayerMenu(stowCursor: false);
		}

		_openChipper = machine;
		_storageTitle.Text = "Wood Chipper";
		_storageChestHost.Visible = false;
		_storageChipperHost.Visible = true;
		_storageOverlay.Visible = true;
		SetMenuDimVisible(true);
		Refresh();
	}

	public void CloseChipper()
	{
		if (_openChipper == null)
		{
			return;
		}

		_cursor.StowInto(_inventory, _bank);
		_openChipper = null;
		_storageOverlay.Visible = false;
		if (!_menuOpen)
		{
			SetMenuDimVisible(false);
		}

		Refresh();
	}

	public void Refresh()
	{
		if (_statusLabel == null)
		{
			return;
		}

		_beforeRefresh?.Invoke();

		_statusLabel.Text = FormatStatus();
		UpdateVitalsBars();

		for (var i = 0; i < GameConfig.HotbarSlots; i++)
		{
			PaintSlot(_hotbarSlots[i], _inventory.GetSlot(i), selected: i == _inventory.SelectedHotbarSlot, hotbarKeySlot: i);
		}

		PaintInventoryGrid(_inventoryGridSlots);
		PaintInventoryGrid(_storageInventorySlots);

		if (_openChest != null)
		{
			for (var i = 0; i < _chestSlots.Length; i++)
			{
				PaintSlot(_chestSlots[i], _openChest.GetSlot(i), selected: false);
			}
		}

		if (_openChipper != null)
		{
			PaintChipper();
		}

		if (IsCraftOpen)
		{
			UpdateCraftRows();
		}

		if (IsSkillsOpen)
		{
			RebuildSkillsList();
		}

		if (IsCollectionsOpen)
		{
			RebuildCollectionsList();
		}

		if (_menuOpen && _tab == PlayerMenuTab.Stats)
		{
			RebuildStatsPanel();
		}

		LayoutBars();
	}

	private void PaintInventoryGrid(Panel[] slots)
	{
		for (var i = 0; i < slots.Length; i++)
		{
			var selected = i < GameConfig.HotbarSlots && i == _inventory.SelectedHotbarSlot;
			var keySlot = i < GameConfig.HotbarSlots ? i : (int?)null;
			PaintSlot(slots[i], _inventory.GetSlot(i), selected, keySlot);
		}
	}

	private string FormatStatus()
	{
		var invKey = InputSettings.DisplayName(_inputSettings.GetBinding(GameAction.Inventory));
		var hp = _health == null
			? ""
			: $"HP {(int)_health.CurrentHealth}/{_health.MaxHealth}  |  ";
		var chips = _bank == null ? "" : $"Chips {_bank.WoodChips}  |  ";
		return $"{_clock.FormatHudText()}\n{hp}{chips}Energy {(int)_energy.CurrentEnergy}/{_energy.MaxEnergy}  |  {invKey} = interact/menu | scroll = hotbar | Esc = pause | RMB = place";
	}

	private void TogglePlayerMenu()
	{
		if (_menuOpen)
		{
			ClosePlayerMenu(stowCursor: true);
		}
		else
		{
			OpenPlayerMenu(PlayerMenuTab.Inventory, stowCursor: false);
		}

		Refresh();
	}

	private void OpenPlayerMenu(PlayerMenuTab tab, bool stowCursor)
	{
		if (_openChest != null)
		{
			CloseChest();
		}

		if (_openChipper != null)
		{
			CloseChipper();
		}

		if (stowCursor)
		{
			_cursor.StowInto(_inventory, _bank);
		}

		_menuOpen = true;
		_playerMenu.Visible = true;
		SetMenuDimVisible(true);
		SetTab(tab);
	}

	private void ClosePlayerMenu(bool stowCursor)
	{
		if (!_menuOpen)
		{
			return;
		}

		if (stowCursor)
		{
			_cursor.StowInto(_inventory, _bank);
		}

		_menuOpen = false;
		_playerMenu.Visible = false;
		_collectionsFocus = null;
		if (_openChest == null && _openChipper == null)
		{
			SetMenuDimVisible(false);
		}
	}

	private void SetMenuDimVisible(bool visible)
	{
		_menuDim.Visible = visible;
	}

	private void SetTab(PlayerMenuTab tab)
	{
		_tab = tab;
		_backpack.Visible = tab == PlayerMenuTab.Inventory;
		_craftHost.Visible = tab == PlayerMenuTab.Crafting;
		_skillsHost.Visible = tab == PlayerMenuTab.Skills;
		_collectionsHost.Visible = tab == PlayerMenuTab.Collections;
		_statsHost.Visible = tab == PlayerMenuTab.Stats;

		foreach (var (t, btn) in _tabButtons)
		{
			ApplyTabButtonStyle(btn, active: t == tab);
		}

		if (tab == PlayerMenuTab.Crafting)
		{
			RebuildCraftList();
		}
		else if (tab == PlayerMenuTab.Skills)
		{
			RebuildSkillsList();
		}
		else if (tab == PlayerMenuTab.Collections)
		{
			RebuildCollectionsList();
		}
		else if (tab == PlayerMenuTab.Stats)
		{
			RebuildStatsPanel();
		}
	}

	private void RebuildStatsPanel()
	{
		foreach (var child in _statsList.GetChildren())
		{
			_statsList.RemoveChild(child);
			child.Free();
		}

		var stats = EffectivePlayerStats.Resolve(_inventory, _items);
		var combat = stats.Combat;
		var currentHp = _health?.CurrentHealth ?? combat.MaxHealth;

		_statsHeldLabel.Text = combat.EquipmentName != null
			? $"Held: {combat.EquipmentName}"
			: "Held: Empty hands";
		_statsHeldLabel.AddThemeColorOverride("font_color", new Color(0.75f, 0.78f, 0.82f));

		AddStatsSection("Vitals", new Color(0.86f, 0.47f, 0.47f), new[]
		{
			("Health", $"{(int)Math.Ceiling(currentHp)} / {combat.MaxHealth}",
				FormatBonus(combat.Equipment.BonusMaxHealth, "")),
		});

		AddStatsSection("Combat", new Color(0.82f, 0.37f, 0.37f), new[]
		{
			("Damage", combat.Damage.ToString(), FormatBonus(combat.Equipment.BonusDamage, "")),
			("Crit Chance", FormatPercent(combat.CritChance),
				FormatBonus(combat.Equipment.BonusCritChance, "%")),
			("Crit Multiplier", FormatMultiplier(combat.CritMultiplier),
				FormatBonus(combat.Equipment.BonusCritMultiplier, "x")),
			("Defense", FormatPercent(combat.Defense),
				FormatBonus(combat.Equipment.BonusDefense, "%")),
		});

		AddStatsSection("Fishing", new Color(0.35f, 0.61f, 0.88f), new[]
		{
			("Fishing Speed", FormatPercent(stats.FishingSpeed),
				FormatBonus(combat.Equipment.BonusFishingSpeed, "%")),
			("Bite Wait",
				$"{FormatSeconds(stats.BiteWaitMinMs)}-{FormatSeconds(stats.BiteWaitMaxMs)} s",
				""),
		});

		_statsList.AddChild(MutedLabel($"Fishing speed cap: {(int)GameConfig.FishingSpeedCap}%"));
		if (combat.HasEquipmentBonus)
		{
			_statsList.AddChild(MutedLabel("Bonuses apply from your selected hotbar item."));
		}
	}

	private void AddStatsSection(string title, Color accent, (string label, string value, string bonus)[] rows)
	{
		var header = new HBoxContainer();
		header.AddThemeConstantOverride("separation", 8);
		_statsList.AddChild(header);

		var bar = new ColorRect
		{
			CustomMinimumSize = new Vector2(6, 18),
			Color = accent,
		};
		header.AddChild(bar);
		var titleLabel = new Label { Text = title };
		titleLabel.AddThemeFontSizeOverride("font_size", 13);
		titleLabel.AddThemeColorOverride("font_color", new Color(0.78f, 0.78f, 0.8f));
		header.AddChild(titleLabel);

		foreach (var (label, value, bonus) in rows)
		{
			var row = new HBoxContainer();
			row.AddThemeConstantOverride("separation", 8);
			_statsList.AddChild(row);

			var name = new Label
			{
				Text = label,
				CustomMinimumSize = new Vector2(140, 0),
			};
			name.AddThemeFontSizeOverride("font_size", 12);
			name.AddThemeColorOverride("font_color", new Color(0.71f, 0.71f, 0.73f));
			row.AddChild(name);

			var val = new Label
			{
				Text = value,
				CustomMinimumSize = new Vector2(100, 0),
			};
			val.AddThemeFontSizeOverride("font_size", 12);
			row.AddChild(val);

			if (!string.IsNullOrEmpty(bonus))
			{
				var bonusLabel = new Label { Text = bonus };
				bonusLabel.AddThemeFontSizeOverride("font_size", 12);
				bonusLabel.AddThemeColorOverride("font_color", new Color(0.47f, 0.86f, 0.55f));
				row.AddChild(bonusLabel);
			}
		}

		_statsList.AddChild(new Control { CustomMinimumSize = new Vector2(0, 6) });
	}

	private static Label MutedLabel(string text)
	{
		var label = new Label { Text = text };
		label.AddThemeFontSizeOverride("font_size", 11);
		label.AddThemeColorOverride("font_color", new Color(0.78f, 0.78f, 0.78f));
		return label;
	}

	private static string FormatBonus(double bonus, string suffix)
	{
		if (bonus == 0)
		{
			return "";
		}

		var sign = bonus > 0 ? "+" : "";
		return sign + TrimNum(bonus) + suffix;
	}

	private static string FormatPercent(double value) => TrimNum(value) + "%";

	private static string FormatMultiplier(double value) => TrimNum(value) + "x";

	private static string FormatSeconds(double ms)
	{
		var seconds = ms / 1000.0;
		return seconds == (long)seconds
			? ((long)seconds).ToString()
			: seconds.ToString("0.0", System.Globalization.CultureInfo.InvariantCulture);
	}

	private static string TrimNum(double value) =>
		value == (long)value
			? ((long)value).ToString()
			: value.ToString(System.Globalization.CultureInfo.InvariantCulture);

	private void RebuildCraftList()
	{
		foreach (var child in _craftList.GetChildren())
		{
			_craftList.RemoveChild(child);
			child.Free();
		}

		if (_crafting.Recipes.Count == 0)
		{
			_craftList.AddChild(new Label { Text = "No recipes loaded (check data/recipes.csv)." });
			return;
		}

		foreach (var recipe in _crafting.Recipes)
		{
			var unlocked = _crafting.IsRecipeUnlocked(recipe, _discoveries);
			var can = unlocked && _crafting.CanCraft(recipe, _inventory, _discoveries);
			var row = new Button
			{
				Text = FormatRecipeRow(recipe, unlocked, can),
				Disabled = !unlocked || !can,
				CustomMinimumSize = new Vector2(420, 40),
				Alignment = HorizontalAlignment.Left,
				FocusMode = Control.FocusModeEnum.None,
			};
			ApplyCraftButtonStyle(row, unlocked && can);
			var captured = recipe;
			row.Pressed += () =>
			{
				if (_crafting.TryCraft(captured, _inventory, _discoveries))
				{
					GD.Print($"Crafted {captured.DisplayName}");
					RebuildCraftList();
					Refresh();
				}
			};
			row.SetMeta("recipe_id", recipe.Id);
			_craftList.AddChild(row);
		}
	}

	/// <summary>Update labels/enabled without recreating buttons (clicks survive HUD refresh).</summary>
	private void UpdateCraftRows()
	{
		foreach (var child in _craftList.GetChildren())
		{
			if (child is not Button row || !row.HasMeta("recipe_id"))
			{
				continue;
			}

			var id = row.GetMeta("recipe_id").AsString();
			RecipeDef? recipe = null;
			foreach (var r in _crafting.Recipes)
			{
				if (r.Id == id)
				{
					recipe = r;
					break;
				}
			}

			if (recipe == null)
			{
				continue;
			}

			var unlocked = _crafting.IsRecipeUnlocked(recipe, _discoveries);
			var can = unlocked && _crafting.CanCraft(recipe, _inventory, _discoveries);
			row.Text = FormatRecipeRow(recipe, unlocked, can);
			row.Disabled = !unlocked || !can;
			ApplyCraftButtonStyle(row, unlocked && can);
		}
	}

	private static void ApplyCraftButtonStyle(Button row, bool can)
	{
		var normal = new StyleBoxFlat
		{
			BgColor = can ? new Color(0.22f, 0.35f, 0.22f, 0.95f) : new Color(0.18f, 0.18f, 0.2f, 0.95f),
			BorderColor = can ? new Color(0.55f, 0.85f, 0.45f) : new Color(0.4f, 0.4f, 0.45f),
			BorderWidthLeft = 2,
			BorderWidthTop = 2,
			BorderWidthRight = 2,
			BorderWidthBottom = 2,
			ContentMarginLeft = 8,
			ContentMarginRight = 8,
			ContentMarginTop = 6,
			ContentMarginBottom = 6,
		};
		row.AddThemeStyleboxOverride("normal", normal);
		row.AddThemeStyleboxOverride("hover", normal);
		row.AddThemeStyleboxOverride("pressed", normal);
		row.AddThemeStyleboxOverride("disabled", normal);
		row.AddThemeColorOverride("font_color", can ? Colors.White : new Color(0.7f, 0.7f, 0.7f));
		row.AddThemeColorOverride("font_disabled_color", new Color(0.65f, 0.65f, 0.65f));
	}

	private string FormatRecipeRow(RecipeDef recipe, bool unlocked, bool can)
	{
		if (!unlocked)
		{
			return "[Locked] ???";
		}

		var sb = new StringBuilder();
		sb.Append(can ? "[Craft] " : "[Need] ");
		sb.Append(recipe.DisplayName);
		sb.Append("  ←  ");
		for (var i = 0; i < recipe.Ingredients.Count; i++)
		{
			if (i > 0)
			{
				sb.Append(", ");
			}

			var ing = recipe.Ingredients[i];
			var owned = _inventory.CountItem(ing.ItemId);
			var name = _items.TryGetValue(ing.ItemId, out var def) ? def.Name : ing.ItemId;
			sb.Append($"{owned}/{ing.Quantity} {name}");
		}

		return sb.ToString();
	}

	private void OnSlotGuiInput(InputEvent @event, int slotIndex)
	{
		if (@event is not InputEventMouseButton { Pressed: true } mouse)
		{
			return;
		}

		if (mouse.ButtonIndex is not (MouseButton.Left or MouseButton.Right))
		{
			return;
		}

		if (!IsBackpackOpen && slotIndex < GameConfig.HotbarSlots && !_cursor.IsCarrying
			&& mouse.ButtonIndex == MouseButton.Left)
		{
			_inventory.SelectHotbarSlot(slotIndex);
		}

		_cursor.HandleClick(_inventory, slotIndex, rightClick: mouse.ButtonIndex == MouseButton.Right);
		Refresh();
		GetViewport().SetInputAsHandled();
	}

	private void OnChestSlotGuiInput(InputEvent @event, int slotIndex)
	{
		if (_openChest == null || @event is not InputEventMouseButton { Pressed: true } mouse)
		{
			return;
		}

		if (mouse.ButtonIndex is not (MouseButton.Left or MouseButton.Right))
		{
			return;
		}

		_cursor.HandleClick(_openChest, slotIndex, rightClick: mouse.ButtonIndex == MouseButton.Right);
		Refresh();
		GetViewport().SetInputAsHandled();
	}

	private void OnChipperInputGuiInput(InputEvent @event)
	{
		if (_openChipper == null || @event is not InputEventMouseButton { Pressed: true } mouse)
		{
			return;
		}

		if (mouse.ButtonIndex is not (MouseButton.Left or MouseButton.Right))
		{
			return;
		}

		var updated = _cursor.HandleFilteredClick(
			_openChipper.InputStack,
			WoodChipperMachine.InputItemId,
			rightClick: mouse.ButtonIndex == MouseButton.Right
		);
		_openChipper.SetInput(updated);
		Refresh();
		GetViewport().SetInputAsHandled();
	}

	private void OnChipperOutputGuiInput(InputEvent @event)
	{
		if (_openChipper == null || @event is not InputEventMouseButton { Pressed: true } mouse)
		{
			return;
		}

		if (mouse.ButtonIndex is not (MouseButton.Left or MouseButton.Right))
		{
			return;
		}

		var output = _openChipper.OutputStack;
		if (output.IsEmpty || output.ItemId != WoodChipperMachine.OutputItemId)
		{
			return;
		}

		if (mouse.ButtonIndex == MouseButton.Right)
		{
			_bank.DepositWoodChips(1);
			_openChipper.SetOutput(
				output.Quantity <= 1
					? ItemStack.Empty
					: ItemStack.Of(output.ItemId!, output.Quantity - 1)
			);
			NotifyItemsAdded(PlayerBank.WoodChipsId, 1);
		}
		else
		{
			_bank.DepositWoodChips(output.Quantity);
			NotifyItemsAdded(PlayerBank.WoodChipsId, output.Quantity);
			_openChipper.SetOutput(ItemStack.Empty);
		}

		Refresh();
		GetViewport().SetInputAsHandled();
	}

	private bool IsMouseOverHud(Vector2 mouse)
	{
		if (_menuDim.Visible)
		{
			return true;
		}

		if (_hotbar.GetGlobalRect().HasPoint(mouse))
		{
			return true;
		}

		if (_menuOpen && _playerMenu.GetGlobalRect().HasPoint(mouse))
		{
			return true;
		}

		if (_storageOverlay.Visible && _storageOverlay.GetGlobalRect().HasPoint(mouse))
		{
			return true;
		}

		return false;
	}

	private bool IsPointerOverScrollContainer()
	{
		var hovered = GetViewport().GuiGetHoveredControl();
		while (hovered != null)
		{
			if (hovered is ScrollContainer)
			{
				return true;
			}

			hovered = hovered.GetParent() as Control;
		}

		return false;
	}

	private void LayoutBars()
	{
		var size = GetViewport().GetVisibleRect().Size;
		var barWidth = GameConfig.HotbarSlots * GameConfig.HudSlotSize
			+ (GameConfig.HotbarSlots - 1) * GameConfig.HudSlotGap;
		_hotbar.Position = new Vector2(
			(size.X - barWidth) / 2f,
			size.Y - GameConfig.HudSlotSize - GameConfig.HudHotbarBottomMargin
		);
		LayoutVitalsHud(size);
		LayoutNotificationFeed();

		var menuSize = _playerMenu.GetCombinedMinimumSize();
		if (menuSize.X < 10)
		{
			menuSize = new Vector2(460, 320);
		}

		_playerMenu.Position = new Vector2(
			(size.X - menuSize.X) / 2f,
			Math.Max(8f, size.Y / 2f - menuSize.Y / 2f - 20)
		);

		var overlaySize = _storageOverlay.GetCombinedMinimumSize();
		if (overlaySize.X < 10)
		{
			overlaySize = new Vector2(
				9 * GameConfig.HudSlotSize + 8 * GameConfig.HudSlotGap + 40,
				7 * GameConfig.HudSlotSize + 6 * GameConfig.HudSlotGap + 96
			);
		}

		var hotbarTop = _hotbar.Position.Y;
		var topMargin = 12f;
		var availableH = Math.Max(overlaySize.Y, hotbarTop - topMargin - 8f);
		var overlayY = topMargin + Math.Max(0f, (availableH - overlaySize.Y) / 2f);
		if (overlayY + overlaySize.Y > hotbarTop - 8f)
		{
			overlayY = Math.Max(topMargin, hotbarTop - overlaySize.Y - 8f);
		}

		_storageOverlay.Position = new Vector2(
			(size.X - overlaySize.X) / 2f,
			overlayY
		);
	}

	private void LayoutVitalsHud(Vector2 viewportSize)
	{
		if (_vitalsHud == null)
		{
			return;
		}

		// Keep clear of the centered hotbar and screen edges.
		const float margin = 20f;
		const float clearGap = 12f;
		var x = viewportSize.X - _vitalsHud.Size.X - margin;
		var y = viewportSize.Y - _vitalsHud.Size.Y - margin;

		var hotbarWidth = GameConfig.HotbarSlots * GameConfig.HudSlotSize
			+ (GameConfig.HotbarSlots - 1) * GameConfig.HudSlotGap;
		var hotbarRight = _hotbar.Position.X + hotbarWidth;
		var hotbarTop = _hotbar.Position.Y;

		// If doubled bars would overlap the hotbar horizontally, lift them above it.
		if (x < hotbarRight + clearGap && y + _vitalsHud.Size.Y > hotbarTop)
		{
			y = hotbarTop - _vitalsHud.Size.Y - clearGap;
		}

		_vitalsHud.Position = new Vector2(Math.Max(margin, x), Math.Max(margin, y));
	}

	/// <summary>
	/// Stacked health (top) + energy (bottom) bars for the bottom-right HUD.
	/// Fill track is 20 source px → 10 slices of 2 px each.
	/// </summary>
	private Control BuildVitalsHud()
	{
		const float scale = 6f; // 75% of the previous 8x size
		const float gap = 6f;
		const int srcW = 31;
		const int srcH = 8;
		// Inner fill (not icon / not outer border): source x 10..29 inclusive.
		const int srcFillStart = 10;
		const int srcFillWidth = 20;

		var barW = srcW * scale;
		var barH = srcH * scale;
		_vitalsFillStartX = srcFillStart * scale;
		_vitalsFillWidth = srcFillWidth * scale;
		_vitalsBarHeight = barH;

		var root = new Control
		{
			Name = "VitalsHud",
			MouseFilter = Control.MouseFilterEnum.Ignore,
			Size = new Vector2(barW, barH * 2 + gap),
		};

		BuildVitalsRow(
			root,
			"res://assets/sprites/ui/health_bar.png",
			y: 0,
			barW,
			barH,
			out _healthDeplete,
			out _healthValueLabel
		);
		BuildVitalsRow(
			root,
			"res://assets/sprites/ui/energy_bar.png",
			y: barH + gap,
			barW,
			barH,
			out _energyDeplete,
			out _energyValueLabel
		);

		return root;
	}

	private void BuildVitalsRow(
		Control parent,
		string texturePath,
		float y,
		float barW,
		float barH,
		out ColorRect deplete,
		out Label valueLabel
	)
	{
		var row = new Control
		{
			Position = new Vector2(0, y),
			Size = new Vector2(barW, barH),
			MouseFilter = Control.MouseFilterEnum.Ignore,
		};
		parent.AddChild(row);

		var sprite = new TextureRect
		{
			Texture = GD.Load<Texture2D>(texturePath),
			ExpandMode = TextureRect.ExpandModeEnum.IgnoreSize,
			StretchMode = TextureRect.StretchModeEnum.Scale,
			TextureFilter = CanvasItem.TextureFilterEnum.Nearest,
			MouseFilter = Control.MouseFilterEnum.Ignore,
			Size = new Vector2(barW, barH),
		};
		row.AddChild(sprite);

		// Covers empty fill slices from the right (blacked-out portion of the track).
		deplete = new ColorRect
		{
			Color = Colors.Black,
			MouseFilter = Control.MouseFilterEnum.Ignore,
			Visible = false,
		};
		row.AddChild(deplete);

		valueLabel = new Label
		{
			HorizontalAlignment = HorizontalAlignment.Left,
			VerticalAlignment = VerticalAlignment.Center,
			MouseFilter = Control.MouseFilterEnum.Ignore,
			// Sit on the start of the fill track (red/green bar), not the icon.
			Position = new Vector2(_vitalsFillStartX + 2f, 0),
			Size = new Vector2(_vitalsFillWidth - 2f, barH),
		};
		valueLabel.AddThemeFontSizeOverride("font_size", 20);
		valueLabel.AddThemeColorOverride("font_color", new Color(1f, 0.9f, 0.27f));
		valueLabel.AddThemeColorOverride("font_outline_color", Colors.Black);
		valueLabel.AddThemeConstantOverride("outline_size", 3);
		row.AddChild(valueLabel);
	}

	private void UpdateVitalsBars()
	{
		if (_vitalsHud == null || _healthDeplete == null)
		{
			return;
		}

		var hpCurrent = _health?.CurrentHealth ?? 0;
		var hpMax = Math.Max(1, _health?.MaxHealth ?? GameConfig.BaseMaxHealth);
		ApplyVitalsFill(_healthDeplete, _healthValueLabel, hpCurrent, hpMax);

		var enCurrent = _energy.CurrentEnergy;
		var enMax = Math.Max(1, _energy.MaxEnergy);
		ApplyVitalsFill(_energyDeplete, _energyValueLabel, enCurrent, enMax);
	}

	private void ApplyVitalsFill(ColorRect deplete, Label valueLabel, double current, int max)
	{
		var slices = FilledVitalsSlices(current, max);
		var filledWidth = slices / 10f * _vitalsFillWidth;
		var emptyWidth = _vitalsFillWidth - filledWidth;

		if (emptyWidth <= 0.01f)
		{
			deplete.Visible = false;
		}
		else
		{
			deplete.Visible = true;
			deplete.Position = new Vector2(_vitalsFillStartX + filledWidth, 0);
			deplete.Size = new Vector2(emptyWidth, _vitalsBarHeight);
		}

		valueLabel.Text = $"{(int)Math.Ceiling(current)}/{max}";
	}

	/// <summary>
	/// 10 discrete slices from percentage: 50/50 → 10, 49/50 → 9, 44/50 → 8.
	/// </summary>
	private static int FilledVitalsSlices(double current, int max)
	{
		if (current <= 0 || max <= 0)
		{
			return 0;
		}

		if (current >= max)
		{
			return 10;
		}

		return (int)Math.Floor(current / max * 10.0);
	}

	private PanelContainer BuildPlayerMenu()
	{
		var root = new PanelContainer { Name = "PlayerMenu" };
		MenuChrome.ApplyPanel(root);

		var vbox = new VBoxContainer();
		vbox.AddThemeConstantOverride("separation", 10);
		root.AddChild(vbox);

		var tabBar = new HBoxContainer();
		tabBar.AddThemeConstantOverride("separation", 6);
		vbox.AddChild(tabBar);

		foreach (PlayerMenuTab tab in Enum.GetValues(typeof(PlayerMenuTab)))
		{
			var captured = tab;
			var btn = new Button
			{
				Text = tab.Label(),
				FocusMode = Control.FocusModeEnum.None,
				CustomMinimumSize = new Vector2(0, 36),
				SizeFlagsHorizontal = Control.SizeFlags.ExpandFill,
			};
			ApplyTabButtonStyle(btn, active: tab == PlayerMenuTab.Inventory);
			btn.Pressed += () =>
			{
				if (!_menuOpen)
				{
					return;
				}

				SetTab(captured);
				Refresh();
			};
			tabBar.AddChild(btn);
			_tabButtons[tab] = btn;
		}

		var contentSize = new Vector2(
			9 * GameConfig.HudSlotSize + 8 * GameConfig.HudSlotGap + 8,
			4 * GameConfig.HudSlotSize + 3 * GameConfig.HudSlotGap + 24
		);
		var contentScroll = new ScrollContainer
		{
			CustomMinimumSize = contentSize,
			HorizontalScrollMode = ScrollContainer.ScrollMode.Disabled,
			VerticalScrollMode = ScrollContainer.ScrollMode.Auto,
		};
		vbox.AddChild(contentScroll);

		var content = new VBoxContainer
		{
			SizeFlagsHorizontal = Control.SizeFlags.ExpandFill,
		};
		contentScroll.AddChild(content);

		_backpack = BuildInventoryGrid(out _inventoryGridSlots);
		_backpack.Visible = true;
		content.AddChild(_backpack);

		_craftHost = BuildCraftHost();
		_craftHost.Visible = false;
		content.AddChild(_craftHost);

		_skillsHost = BuildSkillsHost();
		_skillsHost.Visible = false;
		content.AddChild(_skillsHost);

		_collectionsHost = BuildCollectionsHost();
		_collectionsHost.Visible = false;
		content.AddChild(_collectionsHost);

		_statsHost = BuildStatsHost();
		_statsHost.Visible = false;
		content.AddChild(_statsHost);

		return root;
	}

	private static void ApplyTabButtonStyle(Button btn, bool active)
	{
		MenuChrome.ApplyTabButton(btn, active);
	}

	private Control BuildCraftHost()
	{
		var root = new VBoxContainer { Name = "CraftHost" };
		root.SizeFlagsHorizontal = Control.SizeFlags.ExpandFill;
		var hint = new Label { Text = "Chipper: 25 logs · Chest: 50 logs" };
		hint.AddThemeFontSizeOverride("font_size", 12);
		root.AddChild(hint);

		_craftList = new VBoxContainer();
		_craftList.SizeFlagsHorizontal = Control.SizeFlags.ExpandFill;
		root.AddChild(_craftList);
		return root;
	}

	private Control BuildSkillsHost()
	{
		var root = new VBoxContainer { Name = "SkillsHost" };
		root.SizeFlagsHorizontal = Control.SizeFlags.ExpandFill;
		var hint = new Label { Text = "Foraging: chop trees · Fishing: catch fish · Combat: later" };
		hint.AddThemeFontSizeOverride("font_size", 11);
		root.AddChild(hint);

		_skillsList = new VBoxContainer();
		_skillsList.AddThemeConstantOverride("separation", 8);
		_skillsList.SizeFlagsHorizontal = Control.SizeFlags.ExpandFill;
		root.AddChild(_skillsList);
		return root;
	}

	private Control BuildCollectionsHost()
	{
		var root = new VBoxContainer { Name = "CollectionsHost" };
		root.SizeFlagsHorizontal = Control.SizeFlags.ExpandFill;
		_collectionsTitle = new Label { Text = "Collections" };
		GameFont.ApplyDisplay(_collectionsTitle, 18);
		root.AddChild(_collectionsTitle);

		var hint = new Label { Text = "Discoveries unlock as you gather items and defeat enemies" };
		hint.AddThemeFontSizeOverride("font_size", 11);
		root.AddChild(hint);

		_collectionsList = new VBoxContainer();
		_collectionsList.AddThemeConstantOverride("separation", 6);
		_collectionsList.SizeFlagsHorizontal = Control.SizeFlags.ExpandFill;
		root.AddChild(_collectionsList);
		return root;
	}

	private Control BuildStatsHost()
	{
		var root = new VBoxContainer { Name = "StatsHost" };
		root.AddThemeConstantOverride("separation", 8);
		root.SizeFlagsHorizontal = Control.SizeFlags.ExpandFill;

		var title = new Label { Text = "Stats" };
		GameFont.ApplyDisplay(title, 18);
		root.AddChild(title);

		_statsHeldLabel = new Label { Text = "Held: Empty hands" };
		_statsHeldLabel.AddThemeFontSizeOverride("font_size", 13);
		root.AddChild(_statsHeldLabel);

		// Kept for field init compatibility; section rows replace the old HP line.
		_statsHpLabel = new Label { Visible = false };

		_statsList = new VBoxContainer();
		_statsList.AddThemeConstantOverride("separation", 2);
		_statsList.SizeFlagsHorizontal = Control.SizeFlags.ExpandFill;
		root.AddChild(_statsList);
		return root;
	}

	private void RebuildSkillsList()
	{
		foreach (var child in _skillsList.GetChildren())
		{
			_skillsList.RemoveChild(child);
			child.Free();
		}

		foreach (SkillType skill in Enum.GetValues(typeof(SkillType)))
		{
			var level = _skills.GetLevel(skill);
			var xp = _skills.GetTotalXp(skill);
			var toNext = _skills.GetXpToNextLevel(skill);
			var progress = _skills.GetProgressInLevel(skill);

			var row = new VBoxContainer();
			var label = new Label
			{
				Text = level >= SkillXpTable.MaxLevel
					? $"{skill.DisplayName()}  Lv {level}  (MAX)  ·  {xp} XP"
					: $"{skill.DisplayName()}  Lv {level}  ·  {xp} XP  ({toNext} to next)",
			};
			label.AddThemeFontSizeOverride("font_size", 14);
			row.AddChild(label);

			var bar = new ProgressBar
			{
				MinValue = 0,
				MaxValue = 1,
				Value = progress,
				ShowPercentage = false,
				CustomMinimumSize = new Vector2(320, 14),
			};
			row.AddChild(bar);
			_skillsList.AddChild(row);
		}
	}

	private void RebuildCollectionsList()
	{
		foreach (var child in _collectionsList.GetChildren())
		{
			_collectionsList.RemoveChild(child);
			child.Free();
		}

		if (_collectionsFocus is { } focus)
		{
			_collectionsTitle.Text = focus.DisplayName();

			var back = new Button
			{
				Text = "← Categories",
				CustomMinimumSize = new Vector2(160, 32),
				FocusMode = Control.FocusModeEnum.None,
			};
			back.Pressed += () =>
			{
				_collectionsFocus = null;
				RebuildCollectionsList();
			};
			_collectionsList.AddChild(back);

			var count = new Label
			{
				Text = $"Unlocked: {_discoveries.GetDiscoveredCount(focus)}/{_discoveries.GetTotalCount(focus)}",
			};
			count.AddThemeFontSizeOverride("font_size", 12);
			_collectionsList.AddChild(count);

			foreach (var entry in _discoveries.GetEntries(focus))
			{
				var discovered = _discoveries.IsEntryDiscovered(entry.EntryId);
				var row = new PanelContainer();
				var rowStyle = new StyleBoxFlat
				{
					BgColor = discovered ? CategoryAccent(focus) * new Color(1, 1, 1, 0.35f) : new Color(0.14f, 0.14f, 0.16f, 0.9f),
					ContentMarginLeft = 8,
					ContentMarginRight = 8,
					ContentMarginTop = 6,
					ContentMarginBottom = 6,
					BorderColor = discovered ? CategoryAccent(focus) : new Color(0.35f, 0.35f, 0.4f),
					BorderWidthLeft = 2,
					BorderWidthTop = 2,
					BorderWidthRight = 2,
					BorderWidthBottom = 2,
				};
				row.AddThemeStyleboxOverride("panel", rowStyle);

				var h = new HBoxContainer();
				h.AddThemeConstantOverride("separation", 10);
				row.AddChild(h);

				var icon = new TextureRect
				{
					CustomMinimumSize = new Vector2(28, 28),
					ExpandMode = TextureRect.ExpandModeEnum.IgnoreSize,
					StretchMode = TextureRect.StretchModeEnum.KeepAspectCentered,
					TextureFilter = CanvasItem.TextureFilterEnum.Nearest,
				};
				if (discovered && entry.DiscoveryType == DiscoveryType.Item && entry.DiscoveryId != null)
				{
					var path = $"res://assets/sprites/items/{entry.DiscoveryId}.png";
					if (ResourceLoader.Exists(path))
					{
						icon.Texture = GD.Load<Texture2D>(path);
					}
				}

				h.AddChild(icon);

				var textCol = new VBoxContainer();
				textCol.SizeFlagsHorizontal = Control.SizeFlags.ExpandFill;
				h.AddChild(textCol);

				var name = new Label
				{
					Text = discovered ? entry.DisplayName : "???",
				};
				name.AddThemeFontSizeOverride("font_size", 13);
				name.AddThemeColorOverride("font_color", discovered ? Colors.White : new Color(0.55f, 0.55f, 0.6f));
				textCol.AddChild(name);

				if (discovered)
				{
					var tipText = FormatCollectionTooltip(entry);
					if (!string.IsNullOrWhiteSpace(tipText))
					{
						var tip = new Label
						{
							Text = tipText,
							AutowrapMode = TextServer.AutowrapMode.WordSmart,
							CustomMinimumSize = new Vector2(320, 0),
						};
						tip.AddThemeFontSizeOverride("font_size", 11);
						tip.AddThemeColorOverride("font_color", new Color(0.75f, 0.8f, 0.85f));
						textCol.AddChild(tip);
					}
				}

				_collectionsList.AddChild(row);
			}

			return;
		}

		_collectionsTitle.Text = "Collections";

		foreach (CollectionCategory category in Enum.GetValues(typeof(CollectionCategory)))
		{
			var discovered = _discoveries.GetDiscoveredCount(category);
			var total = _discoveries.GetTotalCount(category);
			var captured = category;
			var btn = new Button
			{
				Text = $"{category.DisplayName()}  ·  Unlocked {discovered}/{total}",
				CustomMinimumSize = new Vector2(380, 44),
				Alignment = HorizontalAlignment.Left,
				FocusMode = Control.FocusModeEnum.None,
			};
			var accent = CategoryAccent(category);
			var normal = new StyleBoxFlat
			{
				BgColor = new Color(accent.R * 0.35f, accent.G * 0.35f, accent.B * 0.35f, 0.95f),
				BorderColor = accent,
				BorderWidthLeft = 3,
				BorderWidthTop = 2,
				BorderWidthRight = 2,
				BorderWidthBottom = 2,
				ContentMarginLeft = 12,
				ContentMarginRight = 12,
				ContentMarginTop = 8,
				ContentMarginBottom = 8,
			};
			btn.AddThemeStyleboxOverride("normal", normal);
			btn.AddThemeStyleboxOverride("hover", normal);
			btn.AddThemeStyleboxOverride("pressed", normal);
			btn.Pressed += () =>
			{
				_collectionsFocus = captured;
				RebuildCollectionsList();
			};
			_collectionsList.AddChild(btn);
		}
	}

	private string FormatCollectionTooltip(CollectionEntryDef entry)
	{
		var tip = new StringBuilder(entry.Tooltip ?? "");
		if (entry.Category == CollectionCategory.Fish
			&& entry.DiscoveryType == DiscoveryType.Item
			&& FishCatalog.FindByItemId(_fishTable, entry.DiscoveryId) is { } fish
			&& !fish.IsTrash)
		{
			if (tip.Length > 0)
			{
				tip.Append(' ');
			}

			tip.Append("Speed: ")
				.Append(fish.Speed)
				.Append(", Weight: ")
				.Append(TrimWeight(fish.Weight))
				.Append(", Min depth: ")
				.Append(fish.MinDepth);
		}

		return tip.ToString();
	}

	private static string TrimWeight(double weight) =>
		weight == (long)weight ? ((long)weight).ToString() : weight.ToString(System.Globalization.CultureInfo.InvariantCulture);

	private static Color CategoryAccent(CollectionCategory category) => category switch
	{
		CollectionCategory.Wood => new Color(0.55f, 0.35f, 0.17f),
		CollectionCategory.Combat => new Color(0.82f, 0.37f, 0.37f),
		CollectionCategory.Fish => new Color(0.35f, 0.61f, 0.88f),
		_ => new Color(0.5f, 0.5f, 0.55f),
	};

	private Control BuildSlotRow(int count, out Panel[] slots, bool hotbar)
	{
		var root = new Control { Name = hotbar ? "Hotbar" : "Slots" };
		slots = new Panel[count];
		for (var i = 0; i < count; i++)
		{
			var slotIndex = i;
			var panel = MakeSlotPanel();
			panel.Position = new Vector2(i * (GameConfig.HudSlotSize + GameConfig.HudSlotGap), 0);
			panel.GuiInput += e => OnSlotGuiInput(e, slotIndex);
			root.AddChild(panel);
			slots[i] = panel;
		}

		return root;
	}

	private Control BuildInventoryGrid(out Panel[] slots)
	{
		var root = new Control { Name = "InventoryGrid" };
		slots = new Panel[GameConfig.InventorySlots];
		for (var i = 0; i < slots.Length; i++)
		{
			var slotIndex = i;
			var col = i % 9;
			var row = i / 9;
			var panel = MakeSlotPanel();
			panel.Position = new Vector2(
				col * (GameConfig.HudSlotSize + GameConfig.HudSlotGap),
				row * (GameConfig.HudSlotSize + GameConfig.HudSlotGap)
			);
			panel.GuiInput += e => OnSlotGuiInput(e, slotIndex);
			root.AddChild(panel);
			slots[i] = panel;
		}

		root.CustomMinimumSize = new Vector2(
			9 * GameConfig.HudSlotSize + 8 * GameConfig.HudSlotGap,
			4 * GameConfig.HudSlotSize + 3 * GameConfig.HudSlotGap
		);
		return root;
	}

	private PanelContainer BuildStorageOverlay()
	{
		var root = new PanelContainer { Name = "StorageOverlay" };
		MenuChrome.ApplyPanel(root);

		var vbox = new VBoxContainer();
		vbox.AddThemeConstantOverride("separation", 12);
		root.AddChild(vbox);

		_storageTitle = new Label { Text = "Chest" };
		GameFont.ApplyDisplay(_storageTitle, 18);
		vbox.AddChild(_storageTitle);

		_storageChestHost = BuildChestSection(out _chestSlots);
		vbox.AddChild(_storageChestHost);

		_storageChipperHost = BuildChipperSection();
		_storageChipperHost.Visible = false;
		vbox.AddChild(_storageChipperHost);

		vbox.AddChild(new Control { CustomMinimumSize = new Vector2(0, 4) });

		var invLabel = new Label { Text = "Your Inventory" };
		invLabel.AddThemeFontSizeOverride("font_size", 14);
		vbox.AddChild(invLabel);

		var invGrid = BuildInventoryGrid(out _storageInventorySlots);
		vbox.AddChild(invGrid);

		return root;
	}

	private Control BuildChestSection(out Panel[] slots)
	{
		var root = new Control { Name = "ChestSlots" };
		slots = new Panel[GameConfig.ChestSlotCount];
		for (var i = 0; i < slots.Length; i++)
		{
			var slotIndex = i;
			var col = i % GameConfig.ChestColumns;
			var row = i / GameConfig.ChestColumns;
			var panel = MakeSlotPanel();
			panel.Position = new Vector2(
				col * (GameConfig.HudSlotSize + GameConfig.HudSlotGap),
				row * (GameConfig.HudSlotSize + GameConfig.HudSlotGap)
			);
			panel.GuiInput += e => OnChestSlotGuiInput(e, slotIndex);
			root.AddChild(panel);
			slots[i] = panel;
		}

		root.CustomMinimumSize = new Vector2(
			9 * GameConfig.HudSlotSize + 8 * GameConfig.HudSlotGap,
			3 * GameConfig.HudSlotSize + 2 * GameConfig.HudSlotGap
		);
		return root;
	}

	private Control BuildChipperSection()
	{
		var root = new VBoxContainer { Name = "ChipperSection" };
		root.AddThemeConstantOverride("separation", 8);

		var row = new HBoxContainer();
		row.AddThemeConstantOverride("separation", 12);
		root.AddChild(row);

		_chipperInputSlot = MakeSlotPanel();
		_chipperInputSlot.GuiInput += OnChipperInputGuiInput;
		row.AddChild(_chipperInputSlot);

		var mid = new VBoxContainer();
		mid.CustomMinimumSize = new Vector2(140, GameConfig.HudSlotSize);
		mid.SizeFlagsVertical = Control.SizeFlags.ShrinkCenter;
		row.AddChild(mid);

		_chipperProgress = new ProgressBar
		{
			MinValue = 0,
			MaxValue = 1,
			Value = 0,
			ShowPercentage = false,
			CustomMinimumSize = new Vector2(140, 18),
		};
		mid.AddChild(_chipperProgress);

		_chipperStatus = new Label { Text = "1 pine log → 5 chips" };
		_chipperStatus.AddThemeFontSizeOverride("font_size", 11);
		mid.AddChild(_chipperStatus);

		_chipperOutputSlot = MakeSlotPanel();
		_chipperOutputSlot.GuiInput += OnChipperOutputGuiInput;
		row.AddChild(_chipperOutputSlot);

		return root;
	}

	private void PaintChipper()
	{
		if (_openChipper == null)
		{
			return;
		}

		PaintSlot(_chipperInputSlot, _openChipper.InputStack, selected: false);
		PaintSlot(_chipperOutputSlot, _openChipper.OutputStack, selected: false);
		_chipperProgress.Value = _openChipper.ProgressRatio;
		var pct = (int)(_openChipper.ProgressRatio * 100);
		_chipperStatus.Text = _openChipper.IsProcessing
			? $"Chipping… {pct}%"
			: _openChipper.InputStack.IsEmpty
				? "Insert pine logs"
				: _openChipper.OutputStack.IsEmpty
					? $"Ready · {pct}%"
					: "Click output → bank";
	}

	private Control BuildCursorGhost()
	{
		var root = new Control
		{
			Name = "CursorGhost",
			MouseFilter = Control.MouseFilterEnum.Ignore,
			ZIndex = 100,
		};
		_cursorIcon = new TextureRect
		{
			Size = new Vector2(32, 32),
			ExpandMode = TextureRect.ExpandModeEnum.IgnoreSize,
			StretchMode = TextureRect.StretchModeEnum.KeepAspectCentered,
			TextureFilter = CanvasItem.TextureFilterEnum.Nearest,
			MouseFilter = Control.MouseFilterEnum.Ignore,
		};
		root.AddChild(_cursorIcon);
		_cursorQty = new Label
		{
			Position = new Vector2(18, 20),
			Size = new Vector2(24, 14),
			MouseFilter = Control.MouseFilterEnum.Ignore,
		};
		_cursorQty.AddThemeFontSizeOverride("font_size", 11);
		_cursorQty.AddThemeColorOverride("font_outline_color", Colors.Black);
		_cursorQty.AddThemeConstantOverride("outline_size", 3);
		root.AddChild(_cursorQty);
		return root;
	}

	private PanelContainer BuildTooltip()
	{
		var root = new PanelContainer
		{
			Name = "ItemTooltip",
			MouseFilter = Control.MouseFilterEnum.Ignore,
			ZIndex = 90,
			Visible = false,
		};
		var style = new StyleBoxFlat
		{
			BgColor = ItemTooltip.BgColor,
			ContentMarginLeft = 10,
			ContentMarginRight = 10,
			ContentMarginTop = 8,
			ContentMarginBottom = 8,
			BorderColor = new Color(0.35f, 0.35f, 0.4f),
			BorderWidthLeft = 1,
			BorderWidthTop = 1,
			BorderWidthRight = 1,
			BorderWidthBottom = 1,
		};
		root.AddThemeStyleboxOverride("panel", style);

		_tooltipLines = new VBoxContainer
		{
			MouseFilter = Control.MouseFilterEnum.Ignore,
		};
		_tooltipLines.AddThemeConstantOverride("separation", 2);
		root.AddChild(_tooltipLines);
		return root;
	}

	private void UpdateTooltip(Vector2 mouse)
	{
		if (_cursor.IsCarrying)
		{
			_tooltip.Visible = false;
			return;
		}

		if (!TryGetHoveredItem(mouse, out var def))
		{
			_tooltip.Visible = false;
			return;
		}

		foreach (var child in _tooltipLines.GetChildren())
		{
			_tooltipLines.RemoveChild(child);
			child.Free();
		}

		foreach (var line in ItemTooltip.BuildLines(def))
		{
			var label = new Label
			{
				Text = line.Text,
				MouseFilter = Control.MouseFilterEnum.Ignore,
			};
			GameFont.ApplyTooltip(label, line.FontSize);
			label.AddThemeColorOverride("font_color", line.Color);
			_tooltipLines.AddChild(label);
		}

		_tooltip.Visible = true;
		_tooltip.ResetSize();
		var tipSize = _tooltip.GetCombinedMinimumSize();
		var view = GetViewport().GetVisibleRect().Size;
		var pos = mouse + new Vector2(16, 16);
		if (pos.X + tipSize.X > view.X)
		{
			pos.X = mouse.X - tipSize.X - 8;
		}

		if (pos.Y + tipSize.Y > view.Y)
		{
			pos.Y = mouse.Y - tipSize.Y - 8;
		}

		_tooltip.Position = pos;
	}

	private bool TryGetHoveredItem(Vector2 mouse, out ItemDef def)
	{
		def = null!;

		for (var i = 0; i < _hotbarSlots.Length; i++)
		{
			if (!_hotbarSlots[i].GetGlobalRect().HasPoint(mouse))
			{
				continue;
			}

			return TryResolveStack(_inventory.GetSlot(i), out def);
		}

		// Parent Visible can stay true after the menu closes — require _menuOpen too.
		if (_menuOpen && _backpack.Visible && TryGetHoveredInventorySlot(mouse, _inventoryGridSlots, out def))
		{
			return true;
		}

		if (_storageOverlay.Visible && TryGetHoveredInventorySlot(mouse, _storageInventorySlots, out def))
		{
			return true;
		}

		if (_openChest != null)
		{
			for (var i = 0; i < _chestSlots.Length; i++)
			{
				if (!_chestSlots[i].GetGlobalRect().HasPoint(mouse))
				{
					continue;
				}

				return TryResolveStack(_openChest.GetSlot(i), out def);
			}
		}

		if (_openChipper != null)
		{
			if (_chipperInputSlot.GetGlobalRect().HasPoint(mouse))
			{
				return TryResolveStack(_openChipper.InputStack, out def);
			}

			if (_chipperOutputSlot.GetGlobalRect().HasPoint(mouse))
			{
				return TryResolveStack(_openChipper.OutputStack, out def);
			}
		}

		return false;
	}

	private bool TryGetHoveredInventorySlot(Vector2 mouse, Panel[] slots, out ItemDef def)
	{
		def = null!;
		for (var i = 0; i < slots.Length; i++)
		{
			if (!slots[i].GetGlobalRect().HasPoint(mouse))
			{
				continue;
			}

			return TryResolveStack(_inventory.GetSlot(i), out def);
		}

		return false;
	}

	private bool TryResolveStack(ItemStack stack, out ItemDef def)
	{
		def = null!;
		if (stack.IsEmpty || stack.ItemId == null)
		{
			return false;
		}

		return _items.TryGetValue(stack.ItemId, out def!);
	}

	private void PaintCursorGhost()
	{
		var stack = _cursor.Carried;
		if (stack.IsEmpty)
		{
			return;
		}

		var path = $"res://assets/sprites/items/{stack.ItemId}.png";
		_cursorIcon.Texture = ResourceLoader.Exists(path) ? GD.Load<Texture2D>(path) : null;
		_cursorQty.Text = stack.Quantity > 1 ? stack.Quantity.ToString() : "";
	}

	private static Panel MakeSlotPanel()
	{
		var panel = new Panel
		{
			CustomMinimumSize = new Vector2(GameConfig.HudSlotSize, GameConfig.HudSlotSize),
			Size = new Vector2(GameConfig.HudSlotSize, GameConfig.HudSlotSize),
			MouseFilter = Control.MouseFilterEnum.Stop,
		};
		panel.AddThemeStyleboxOverride("panel", SlotStyle(false));

		var key = new Label
		{
			Name = "Key",
			Position = new Vector2(3, 1),
			Size = new Vector2(GameConfig.HudSlotSize - 6, 12),
			HorizontalAlignment = HorizontalAlignment.Left,
			MouseFilter = Control.MouseFilterEnum.Ignore,
			Visible = false,
		};
		key.AddThemeFontSizeOverride("font_size", 10);
		key.AddThemeColorOverride("font_color", new Color(1f, 1f, 1f, 0.7f));
		panel.AddChild(key);

		var icon = new TextureRect
		{
			Name = "Icon",
			Position = new Vector2(6, 6),
			Size = new Vector2(GameConfig.HudSlotSize - 12, GameConfig.HudSlotSize - 18),
			ExpandMode = TextureRect.ExpandModeEnum.IgnoreSize,
			StretchMode = TextureRect.StretchModeEnum.KeepAspectCentered,
			TextureFilter = CanvasItem.TextureFilterEnum.Nearest,
			MouseFilter = Control.MouseFilterEnum.Ignore,
		};
		panel.AddChild(icon);

		var qty = new Label
		{
			Name = "Qty",
			Position = new Vector2(2, GameConfig.HudSlotSize - 16),
			Size = new Vector2(GameConfig.HudSlotSize - 4, 14),
			HorizontalAlignment = HorizontalAlignment.Right,
			MouseFilter = Control.MouseFilterEnum.Ignore,
		};
		qty.AddThemeFontSizeOverride("font_size", 11);
		panel.AddChild(qty);
		return panel;
	}

	private void PaintSlot(Panel panel, ItemStack stack, bool selected, int? hotbarKeySlot = null)
	{
		panel.AddThemeStyleboxOverride("panel", SlotStyle(selected));
		var icon = panel.GetNode<TextureRect>("Icon");
		var qty = panel.GetNode<Label>("Qty");
		var key = panel.GetNodeOrNull<Label>("Key");
		if (key != null)
		{
			if (hotbarKeySlot is >= 0 and < GameConfig.HotbarSlots)
			{
				var action = (GameAction)hotbarKeySlot.Value;
				key.Text = InputSettings.DisplayName(_inputSettings.GetBinding(action));
				key.Visible = true;
			}
			else
			{
				key.Text = "";
				key.Visible = false;
			}
		}

		if (stack.IsEmpty)
		{
			icon.Texture = null;
			qty.Text = "";
			return;
		}

		var path = $"res://assets/sprites/items/{stack.ItemId}.png";
		icon.Texture = ResourceLoader.Exists(path) ? GD.Load<Texture2D>(path) : null;
		qty.Text = stack.Quantity > 1 ? stack.Quantity.ToString() : "";
	}

	private static StyleBoxFlat SlotStyle(bool selected) => new()
	{
		BgColor = selected ? new Color(0.35f, 0.32f, 0.18f, 0.95f) : new Color(0.12f, 0.12f, 0.14f, 0.9f),
		BorderColor = selected ? new Color(0.95f, 0.85f, 0.35f) : new Color(0.45f, 0.45f, 0.5f),
		BorderWidthLeft = 2,
		BorderWidthTop = 2,
		BorderWidthRight = 2,
		BorderWidthBottom = 2,
	};
}
