using System;
using System.Collections.Generic;
using LumberjackAttack.Core;

namespace LumberjackAttack.Fishing;

/// <summary>Underwater hook minigame — descend, A/D steer, catch what you touch (Java FishingMinigame).</summary>
public sealed class FishingMinigame
{
	public const int HookWidth = 14;
	public const int HookHeight = 14;
	public const int EntityWidth = 36;
	public const int EntityHeight = 22;

	private readonly IReadOnlyList<FishSpawnDef> _fishTable;
	private readonly List<MinigameEntity> _entities = new();
	private bool _moveLeft;
	private bool _moveRight;
	private double _nextSpawnDepth = GameConfig.FishingEntitySpawnInterval;

	public FishingMinigame(IReadOnlyList<FishSpawnDef> fishTable)
	{
		_fishTable = fishTable;
		LineX = GameConfig.FishingMinigamePanelWidth / 2.0;
	}

	public double LineX { get; private set; }
	public double Depth { get; private set; }
	public bool Finished { get; private set; }
	public string? CaughtItemId { get; private set; }
	public IReadOnlyList<MinigameEntity> Entities => _entities;

	public int PanelWidth => GameConfig.FishingMinigamePanelWidth;
	public int PanelHeight => GameConfig.FishingMinigamePanelHeight;
	public int PlayAreaTop => GameConfig.FishingPlayAreaTop;
	public int HookScreenY => GameConfig.FishingHookScreenY;

	public void SetMoveLeft(bool value) => _moveLeft = value;
	public void SetMoveRight(bool value) => _moveRight = value;

	public double WorldYToScreenY(double worldY) => HookScreenY + (worldY - Depth);

	public void Update(double elapsedMs)
	{
		if (Finished || elapsedMs <= 0)
		{
			return;
		}

		var seconds = elapsedMs / 1000.0;
		if (_moveLeft)
		{
			LineX -= GameConfig.FishingHookMoveSpeed * seconds;
		}

		if (_moveRight)
		{
			LineX += GameConfig.FishingHookMoveSpeed * seconds;
		}

		LineX = Math.Clamp(LineX, HookWidth / 2.0, PanelWidth - HookWidth / 2.0);
		Depth += ResolveDescendSpeed() * seconds;

		while (_nextSpawnDepth <= Depth + GameConfig.FishingSpawnLookahead)
		{
			SpawnEntity(_nextSpawnDepth);
			_nextSpawnDepth += GameConfig.FishingEntitySpawnInterval;
		}

		foreach (var entity in _entities)
		{
			if (!entity.Stationary)
			{
				entity.Update(seconds, PanelWidth);
			}
		}

		var hook = GetHookBounds();
		foreach (var entity in _entities)
		{
			if (hook.Intersects(entity.GetBounds()))
			{
				CaughtItemId = entity.ItemId;
				Finished = true;
				return;
			}
		}

		_entities.RemoveAll(e => e.Y < Depth - GameConfig.FishingDespawnBehind);
	}

	private double ResolveDescendSpeed()
	{
		var tier = (int)(Depth / GameConfig.FishingLineSpeedDepthTier);
		return GameConfig.FishingLineDescendSpeed + tier * GameConfig.FishingLineDescendSpeedPerTier;
	}

	private RectD GetHookBounds() => new(
		LineX - HookWidth / 2.0,
		Depth - HookHeight / 2.0,
		HookWidth,
		HookHeight
	);

	private void SpawnEntity(double spawnDepth)
	{
		var trash = Random.Shared.NextDouble() < GameConfig.FishingTrashSpawnChance;
		var spawn = trash
			? FishCatalog.RollTrash(_fishTable)
			: FishCatalog.RollFishAtDepth(_fishTable, spawnDepth);

		var x = Random.Shared.NextDouble() * (PanelWidth - EntityWidth * 2) + EntityWidth;
		var velocityX = 0.0;
		if (!trash)
		{
			var referenceSpeed = GameConfig.FishingBaseSpeedMin
				+ Random.Shared.NextDouble() * (GameConfig.FishingBaseSpeedMax - GameConfig.FishingBaseSpeedMin);
			var speedScale = spawn.Speed / (double)GameConfig.FishingReferenceSpeedStat;
			var direction = Random.Shared.Next(2) == 0 ? 1 : -1;
			velocityX = referenceSpeed * speedScale * direction;
		}

		_entities.Add(new MinigameEntity(
			spawn.ItemId,
			x,
			spawnDepth,
			EntityWidth,
			EntityHeight,
			velocityX,
			stationary: trash
		));
	}

	public readonly record struct RectD(double X, double Y, double W, double H)
	{
		public bool Intersects(RectD other) =>
			X < other.X + other.W && X + W > other.X && Y < other.Y + other.H && Y + H > other.Y;
	}

	public sealed class MinigameEntity
	{
		public string ItemId { get; }
		public double X { get; private set; }
		public double Y { get; }
		public int Width { get; }
		public int Height { get; }
		public bool Stationary { get; }
		private double _velocityX;

		public MinigameEntity(
			string itemId,
			double x,
			double y,
			int width,
			int height,
			double velocityX,
			bool stationary
		)
		{
			ItemId = itemId;
			X = x;
			Y = y;
			Width = width;
			Height = height;
			_velocityX = velocityX;
			Stationary = stationary;
		}

		public void Update(double seconds, int panelWidth)
		{
			X += _velocityX * seconds;
			if (X <= 0)
			{
				X = 0;
				_velocityX = Math.Abs(_velocityX);
			}
			else if (X + Width >= panelWidth)
			{
				X = panelWidth - Width;
				_velocityX = -Math.Abs(_velocityX);
			}
		}

		public RectD GetBounds() => new(X, Y, Width, Height);
	}
}
