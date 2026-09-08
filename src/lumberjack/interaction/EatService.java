package lumberjack.interaction;

import lumberjack.game.GameSession;
import lumberjack.inventory.Inventory;
import lumberjack.item.ItemDefinition;
import lumberjack.item.ItemStack;

/**
 * Confirms and applies eating a held edible item (fish, etc.).
 */
public final class EatService {

    private final EatPrompt eatPrompt;

    public EatService(EatPrompt eatPrompt) {
        this.eatPrompt = eatPrompt;
    }

    /**
     * Starts an eat prompt when the player clicks while holding edible food.
     *
     * @return true if this click was consumed by the eat flow
     */
    public boolean tryBeginEatOnClick(GameSession session) {
        if (eatPrompt.isEatPromptVisible()) {
            return true;
        }

        ItemStack held = session.getInventory().getSelectedHotbarStack();
        if (held.isEmpty()) {
            return false;
        }

        ItemDefinition item = session.getItemRegistry().get(held.getItemId());
        if (!item.isEdible()) {
            return false;
        }

        Boolean decision = eatPrompt.confirmEat(item.getName());
        if (decision == null) {
            return true;
        }
        if (decision) {
            applyEat(session, item);
        }
        return true;
    }

    /**
     * Polls an already-open eat prompt (same pattern as sleep).
     *
     * @return true if the player ate
     */
    public boolean updateOpenPrompt(GameSession session) {
        if (!eatPrompt.isEatPromptVisible()) {
            return false;
        }

        ItemStack held = session.getInventory().getSelectedHotbarStack();
        if (held.isEmpty()) {
            eatPrompt.cancelEatPrompt();
            return false;
        }

        ItemDefinition item = session.getItemRegistry().get(held.getItemId());
        if (!item.isEdible()) {
            eatPrompt.cancelEatPrompt();
            return false;
        }

        Boolean decision = eatPrompt.confirmEat(item.getName());
        if (decision == null) {
            return false;
        }
        if (decision) {
            applyEat(session, item);
            return true;
        }
        return false;
    }

    public void reset() {
        eatPrompt.cancelEatPrompt();
    }

    public boolean isPromptVisible() {
        return eatPrompt.isEatPromptVisible();
    }

    private void applyEat(GameSession session, ItemDefinition item) {
        Inventory inventory = session.getInventory();
        int slot = inventory.getSelectedHotbarSlot();
        ItemStack held = inventory.getSlot(slot);
        if (held.isEmpty() || !held.getItemId().equals(item.getId())) {
            return;
        }

        if (item.getFoodHealth() > 0) {
            session.getCombatState().heal(
                    item.getFoodHealth(),
                    inventory,
                    session.getItemRegistry()
            );
        }
        if (item.getFoodEnergy() > 0) {
            session.getEnergyState().restore(item.getFoodEnergy());
        }

        if (held.getQuantity() <= 1) {
            inventory.setSlot(slot, ItemStack.empty());
        } else {
            held.setQuantity(held.getQuantity() - 1);
        }
    }
}
