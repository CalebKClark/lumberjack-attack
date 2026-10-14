using System;
using LumberjackAttack.Core;

namespace LumberjackAttack.Entities;

/// <summary>Persisted player attributes (Java PlayerAttributes) — magnet radius, etc.</summary>
public sealed class PlayerAttributes
{
	/// <summary>Pickup magnet radius in tiles (gap between player and drop hitboxes).</summary>
	public double PickupMagnetTiles { get; private set; } = GameConfig.DefaultPickupMagnetTiles;

	public float PickupMagnetPixels => (float)(PickupMagnetTiles * GameConfig.TileSize);

	public void SetPickupMagnetTiles(double tiles) =>
		PickupMagnetTiles = Math.Max(0, tiles);

	public void Reset() => PickupMagnetTiles = GameConfig.DefaultPickupMagnetTiles;
}
