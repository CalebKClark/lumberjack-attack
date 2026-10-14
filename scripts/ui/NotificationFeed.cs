using System;
using System.Collections.Generic;
using LumberjackAttack.Core;

namespace LumberjackAttack.Ui;

/// <summary>
/// Bottom-left stacked feed (Java GameNotificationManager) with pickup consolidation:
/// repeated +item pickups within a line's lifetime update that line instead of stacking clones.
/// </summary>
public sealed class NotificationFeed
{
	public sealed class FeedLine
	{
		public string Text { get; set; } = "";
		public string? MergeKey { get; set; }
		public int Quantity { get; set; }
		public double RemainingMs { get; set; }

		public float Alpha
		{
			get
			{
				if (RemainingMs >= GameConfig.NotificationFadeMs)
				{
					return 1f;
				}

				return (float)Math.Max(0, RemainingMs / GameConfig.NotificationFadeMs);
			}
		}
	}

	public sealed class XpPopup
	{
		public string Text { get; set; } = "";
		public double RemainingMs { get; set; }
		public double OffsetY { get; set; }

		public float Alpha
		{
			get
			{
				if (RemainingMs >= GameConfig.NotificationFadeMs)
				{
					return 1f;
				}

				return (float)Math.Max(0, RemainingMs / GameConfig.NotificationFadeMs);
			}
		}
	}

	private readonly List<FeedLine> _lines = new();
	private readonly List<XpPopup> _xp = new();

	public bool Suppressed { get; set; }
	public IReadOnlyList<FeedLine> Lines => _lines;
	public IReadOnlyList<XpPopup> XpPopups => _xp;

	public void Clear()
	{
		_lines.Clear();
		_xp.Clear();
	}

	public void Update(double elapsedMs)
	{
		if (elapsedMs <= 0)
		{
			return;
		}

		for (var i = _lines.Count - 1; i >= 0; i--)
		{
			_lines[i].RemainingMs -= elapsedMs;
			if (_lines[i].RemainingMs <= 0)
			{
				_lines.RemoveAt(i);
			}
		}

		for (var i = _xp.Count - 1; i >= 0; i--)
		{
			_xp[i].RemainingMs -= elapsedMs;
			_xp[i].OffsetY += GameConfig.NotificationXpDriftSpeed * (elapsedMs / 1000.0);
			if (_xp[i].RemainingMs <= 0)
			{
				_xp.RemoveAt(i);
			}
		}
	}

	public void NotifyMessage(string text)
	{
		if (Suppressed || string.IsNullOrWhiteSpace(text))
		{
			return;
		}

		AddFeedLine(text.Trim(), mergeKey: null, quantity: 0);
	}

	public void NotifyItemsAdded(string itemId, string displayName, int quantity)
	{
		if (Suppressed || quantity <= 0 || string.IsNullOrEmpty(itemId))
		{
			return;
		}

		var key = "add:" + itemId;
		for (var i = _lines.Count - 1; i >= 0; i--)
		{
			var line = _lines[i];
			if (line.MergeKey != key)
			{
				continue;
			}

			line.Quantity += quantity;
			line.Text = FormatItemChange("+", displayName, line.Quantity);
			line.RemainingMs = GameConfig.NotificationFeedDurationMs;
			return;
		}

		AddFeedLine(FormatItemChange("+", displayName, quantity), key, quantity);
	}

	public void NotifyItemsRemoved(string itemId, string displayName, int quantity)
	{
		if (Suppressed || quantity <= 0 || string.IsNullOrEmpty(itemId))
		{
			return;
		}

		var key = "rem:" + itemId;
		for (var i = _lines.Count - 1; i >= 0; i--)
		{
			var line = _lines[i];
			if (line.MergeKey != key)
			{
				continue;
			}

			line.Quantity += quantity;
			line.Text = FormatItemChange("-", displayName, line.Quantity);
			line.RemainingMs = GameConfig.NotificationFeedDurationMs;
			return;
		}

		AddFeedLine(FormatItemChange("-", displayName, quantity), key, quantity);
	}

	public void NotifyXp(int amount)
	{
		if (Suppressed || amount <= 0)
		{
			return;
		}

		_xp.Add(new XpPopup
		{
			Text = $"+{amount} XP",
			RemainingMs = GameConfig.NotificationXpDurationMs,
		});
		while (_xp.Count > GameConfig.NotificationMaxXpPopups)
		{
			_xp.RemoveAt(0);
		}
	}

	public void NotifyLevelUp(string skillName, int level) =>
		NotifyMessage($"You are now {skillName} level {level}");

	public void NotifyRecipeUnlocked(string recipeName) =>
		NotifyMessage($"New recipe unlocked: {recipeName}");

	public void NotifyCollection(string entryName) =>
		NotifyMessage($"Collection: {entryName}");

	public void NotifyGameSaved() => NotifyMessage("Game saved");

	private void AddFeedLine(string text, string? mergeKey, int quantity)
	{
		_lines.Add(new FeedLine
		{
			Text = text,
			MergeKey = mergeKey,
			Quantity = quantity,
			RemainingMs = GameConfig.NotificationFeedDurationMs,
		});
		while (_lines.Count > GameConfig.NotificationMaxFeedLines)
		{
			_lines.RemoveAt(0);
		}
	}

	private static string FormatItemChange(string sign, string itemName, int quantity) =>
		$"{sign} {itemName} x{quantity}";
}
