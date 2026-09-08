package lumberjack.interaction;

import lumberjack.combat.CombatDamage;
import lumberjack.combat.EffectiveCombatStats;
import lumberjack.entity.Player;
import lumberjack.entity.enemy.WorldEnemy;
import lumberjack.game.GameSession;
import lumberjack.inventory.Inventory;
import lumberjack.item.ItemDefinition;
import lumberjack.item.ItemStack;

/**
 * Handles player melee attacks against world enemies.
 */
public final class CombatService {

    public boolean tryAttackAtWorldPosition(GameSession session, int worldX, int worldY) {
        Player player = session.getPlayer();
        if (player.isActionLocked()) {
            return false;
        }

        WorldEnemy target = session.getEnemyManager().findAttackableEnemy(session, worldX, worldY);
        if (target == null) {
            return false;
        }

        boolean holdingAxe = isHoldingAxe(session.getInventory(), session.getItemRegistry());
        if (holdingAxe && !session.trySpendActionEnergy()) {
            return true;
        }

        EffectiveCombatStats stats = session.getCombatState().getEffectiveStats(
                session.getInventory(),
                session.getItemRegistry()
        );
        CombatDamage.AttackResult attack = CombatDamage.rollPlayerAttack(stats);
        session.getEnemyManager().damageEnemy(target, attack.damage(), session);

        player.faceToward(target.getX() + target.getWidth() / 2, target.getY() + target.getHeight() / 2);
        if (holdingAxe) {
            player.triggerAxeSwing();
        }

        return true;
    }

    private boolean isHoldingAxe(Inventory inventory, lumberjack.item.ItemRegistry itemRegistry) {
        ItemStack held = inventory.getSelectedHotbarStack();
        if (held.isEmpty()) {
            return false;
        }

        ItemDefinition item = itemRegistry.get(held.getItemId());
        return item.isAxe();
    }
}
