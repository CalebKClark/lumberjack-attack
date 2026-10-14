using System;
using System.Collections.Generic;
using Godot;
using LumberjackAttack.Core;
using LumberjackAttack.Time;
using LumberjackAttack.Ui;
using LumberjackAttack.World;
using LumberjackAttack.World.Placeable;

namespace LumberjackAttack.Interaction;

/// <summary>Step onto a bed tile or placed bed → Yes/No sleep prompt (Java SleepService).</summary>
public sealed class SleepService
{
	private readonly TileMapLayer _ground;
	private readonly IReadOnlyDictionary<int, TileDef> _catalog;
	private readonly GameClock _clock;
	private readonly PlayerEnergyState _energy;
	private readonly PlaceableLayer _placeables;
	private readonly Func<string> _mapId;
	private readonly Action<int> _onSlept;
	private readonly ConfirmationDialog _dialog;
	private bool _wasOnBed;
	private bool _promptOpen;

	public SleepService(
		TileMapLayer ground,
		IReadOnlyDictionary<int, TileDef> catalog,
		GameClock clock,
		PlayerEnergyState energy,
		PlaceableLayer placeables,
		Func<string> mapId,
		Node host,
		Action<int> onSlept
	)
	{
		_ground = ground;
		_catalog = catalog;
		_clock = clock;
		_energy = energy;
		_placeables = placeables;
		_mapId = mapId;
		_onSlept = onSlept;

		_dialog = new ConfirmationDialog
		{
			Title = "Sleep",
			DialogText = "Go to sleep until morning?",
			OkButtonText = "Yes",
			CancelButtonText = "No",
		};
		host.AddChild(_dialog);
		_dialog.Confirmed += OnConfirmed;
		_dialog.Canceled += OnCanceled;
		GameFont.Apply(_dialog);
		MenuChrome.ApplyDialog(_dialog);
	}

	public void Update(Vector2 playerCenter)
	{
		var cell = _ground.LocalToMap(_ground.ToLocal(playerCenter));
		var onBed = false;
		var tileId = _ground.GetCellSourceId(cell);
		if (tileId >= 0 && _catalog.TryGetValue(tileId, out var def))
		{
			onBed = def.Bed;
		}

		if (!onBed)
		{
			var placed = _placeables.FindAt(_mapId(), cell.X, cell.Y);
			onBed = placed != null && BedPlaceable.IsBed(placed.ItemId);
		}

		if (!onBed)
		{
			_wasOnBed = false;
			if (_promptOpen)
			{
				_dialog.Hide();
				_promptOpen = false;
			}

			return;
		}

		if (_wasOnBed || _promptOpen)
		{
			return;
		}

		_promptOpen = true;
		_dialog.PopupCentered();
	}

	private void OnConfirmed()
	{
		_promptOpen = false;
		_wasOnBed = true;
		var skipped = _clock.AdvanceToNextMorning();
		_energy.ResetToFull();
		_onSlept(skipped);
	}

	private void OnCanceled()
	{
		_promptOpen = false;
		_wasOnBed = true;
	}
}
