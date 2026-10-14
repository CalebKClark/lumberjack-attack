using System;
using LumberjackAttack.Core;
using LumberjackAttack.Time;

namespace LumberjackAttack.Interaction;

/// <summary>When HP hits 0, warp to the cabin bed and restore HP/energy (Java DeathService).</summary>
public sealed class DeathService
{
	private readonly PlayerHealthState _health;
	private readonly PlayerEnergyState _energy;
	private readonly Action _respawnAtBed;
	private readonly Action? _onRespawned;

	public DeathService(
		PlayerHealthState health,
		PlayerEnergyState energy,
		Action respawnAtBed,
		Action? onRespawned = null
	)
	{
		_health = health;
		_energy = energy;
		_respawnAtBed = respawnAtBed;
		_onRespawned = onRespawned;
	}

	/// <summary>Returns true if a death→respawn happened this tick.</summary>
	public bool CheckAndRespawn()
	{
		if (!_health.IsDead)
		{
			return false;
		}

		_respawnAtBed();
		_health.ResetToFull();
		_energy.ResetToFull();
		_onRespawned?.Invoke();
		return true;
	}
}
