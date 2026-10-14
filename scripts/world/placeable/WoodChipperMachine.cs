using System;
using LumberjackAttack.Core;
using LumberjackAttack.Items;

namespace LumberjackAttack.World.Placeable;

/// <summary>One wood chipper: 1 pine_log → 5 wood_chips over 5 game minutes.</summary>
public sealed class WoodChipperMachine
{
	public const string MachineItemId = "wood_chipper";
	public const string InputItemId = "pine_log";
	public const string OutputItemId = "wood_chips";
	public const int ChipsPerLog = 5;

	public static double ProcessDurationMs =>
		GameConfig.WoodChipperProcessGameMinutes * (double)GameConfig.RealMsPerGameMinute;

	private ItemStack _input = ItemStack.Empty;
	private ItemStack _output = ItemStack.Empty;
	private double _progressMs;
	private bool _processing;

	public ItemStack InputStack => _input;
	public ItemStack OutputStack => _output;
	public double ProgressMs => _progressMs;
	public bool IsProcessing => _processing;

	public double ProgressRatio =>
		ProcessDurationMs <= 0 ? 0 : Math.Min(1.0, _progressMs / ProcessDurationMs);

	public bool IsEmpty => _input.IsEmpty && _output.IsEmpty;

	public void SetInput(ItemStack stack)
	{
		_input = stack.IsEmpty ? ItemStack.Empty : stack;
		NormalizeState();
	}

	public void SetOutput(ItemStack stack)
	{
		_output = stack.IsEmpty ? ItemStack.Empty : stack;
	}

	public void Restore(
		string? inputItemId,
		int inputQty,
		string? outputItemId,
		int outputQty,
		double progressMs,
		bool processing
	)
	{
		_input = ToStack(inputItemId, inputQty);
		_output = ToStack(outputItemId, outputQty);
		_progressMs = Math.Max(0, progressMs);
		_processing = processing;
		NormalizeState();
	}

	public void Update(double elapsedMs, Func<string, int> maxStackOf)
	{
		if (elapsedMs <= 0)
		{
			return;
		}

		NormalizeState();

		if (_processing)
		{
			if (!CanAcceptOutputBatch(maxStackOf) || !HasProcessableInput())
			{
				return;
			}

			_progressMs += elapsedMs;
			while (_progressMs >= ProcessDurationMs)
			{
				if (!CompleteOneBatch(maxStackOf))
				{
					_progressMs = Math.Min(_progressMs, ProcessDurationMs);
					break;
				}

				_progressMs -= ProcessDurationMs;
			}

			return;
		}

		if (HasProcessableInput() && CanAcceptOutputBatch(maxStackOf))
		{
			_processing = true;
			_progressMs = 0;
		}
	}

	public bool TryInsertOneLog()
	{
		if (!_input.IsEmpty && _input.ItemId != InputItemId)
		{
			return false;
		}

		if (_input.IsEmpty)
		{
			_input = ItemStack.Of(InputItemId, 1);
		}
		else
		{
			_input = _input.WithQuantity(_input.Quantity + 1);
		}

		NormalizeState();
		return true;
	}

	public ItemStack TakeAllOutput()
	{
		if (_output.IsEmpty)
		{
			return ItemStack.Empty;
		}

		var taken = _output;
		_output = ItemStack.Empty;
		return taken;
	}

	private bool CompleteOneBatch(Func<string, int> maxStackOf)
	{
		if (!HasProcessableInput() || !CanAcceptOutputBatch(maxStackOf))
		{
			_processing = false;
			return false;
		}

		_input = _input.Quantity <= 1
			? ItemStack.Empty
			: _input.WithQuantity(_input.Quantity - 1);

		_output = _output.IsEmpty
			? ItemStack.Of(OutputItemId, ChipsPerLog)
			: _output.WithQuantity(_output.Quantity + ChipsPerLog);

		_processing = HasProcessableInput() && CanAcceptOutputBatch(maxStackOf);
		if (!_processing)
		{
			_progressMs = 0;
		}

		return true;
	}

	private bool HasProcessableInput() =>
		!_input.IsEmpty && _input.ItemId == InputItemId;

	private bool CanAcceptOutputBatch(Func<string, int> maxStackOf)
	{
		if (_output.IsEmpty)
		{
			return true;
		}

		if (_output.ItemId != OutputItemId)
		{
			return false;
		}

		return _output.Quantity + ChipsPerLog <= maxStackOf(OutputItemId);
	}

	private void NormalizeState()
	{
		if (_input.IsEmpty)
		{
			_input = ItemStack.Empty;
		}

		if (_output.IsEmpty)
		{
			_output = ItemStack.Empty;
		}

		if (!HasProcessableInput())
		{
			_processing = false;
			_progressMs = 0;
		}
	}

	private static ItemStack ToStack(string? itemId, int quantity) =>
		string.IsNullOrWhiteSpace(itemId) || quantity <= 0
			? ItemStack.Empty
			: ItemStack.Of(itemId, quantity);
}
