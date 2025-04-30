package org.notionsmp.notionMenus.listeners;

import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.notionsmp.notionMenus.NotionMenus;
import org.notionsmp.notionMenus.gui.CustomInventoryHolder;
import org.notionsmp.notionMenus.gui.GuiConfig;
import org.notionsmp.notionMenus.utils.ActionUtil;
import org.notionsmp.notionMenus.utils.ConditionUtil;

import java.util.Map;

@RequiredArgsConstructor
public class GuiClickListener implements Listener {
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) return;

        if (!(clickedInventory.getHolder() instanceof CustomInventoryHolder holder)) return;

        Player player = (Player) event.getWhoClicked();
        GuiConfig guiConfig = NotionMenus.getInstance().getGuiManager().getGuis().get(holder.getGuiId());
        if (guiConfig == null) return;

        event.setCancelled(true);
        int slot = event.getSlot();
        Map<String, GuiConfig.ClickAction> clickActions = guiConfig.getClickActions(slot, player);
        ItemStack clickedItem = event.getCurrentItem();

        executeActions(clickActions.get("all"), event);

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            switch (event.getClick()) {
                case LEFT -> executeActions(clickActions.get("left"), event);
                case RIGHT -> executeActions(clickActions.get("right"), event);
                case SHIFT_LEFT -> executeActions(clickActions.get("shift_left"), event);
                case SHIFT_RIGHT -> executeActions(clickActions.get("shift_right"), event);
                case MIDDLE -> executeActions(clickActions.get("middle"), event);
                case DOUBLE_CLICK -> executeActions(clickActions.get("double"), event);
                case DROP -> executeActions(clickActions.get("drop"), event);
                case CONTROL_DROP -> executeActions(clickActions.get("ctrl_drop"), event);
                case NUMBER_KEY -> executeActions(clickActions.get("number_key"), event);
                case SWAP_OFFHAND -> executeActions(clickActions.get("swap_offhand"), event);
            }
            return;
        }

        switch (event.getClick()) {
            case LEFT -> executeActions(clickActions.get("left"), event);
            case RIGHT -> executeActions(clickActions.get("right"), event);
            case SHIFT_LEFT -> executeActions(clickActions.get("shift_left"), event);
            case SHIFT_RIGHT -> executeActions(clickActions.get("shift_right"), event);
            case MIDDLE -> executeActions(clickActions.get("middle"), event);
            case DOUBLE_CLICK -> executeActions(clickActions.get("double"), event);
            case DROP -> executeActions(clickActions.get("drop"), event);
            case CONTROL_DROP -> executeActions(clickActions.get("ctrl_drop"), event);
        }
    }

    private void executeActions(GuiConfig.ClickAction clickAction, InventoryClickEvent event) {
        if (clickAction == null || clickAction.actions().isEmpty()) return;
        Player player = (Player) event.getWhoClicked();
        if (!ConditionUtil.checkConditions(clickAction.conditions(), player)) {
            for (String denyAction : clickAction.denyActions()) {
                ActionUtil.executeAction(denyAction, player, event);
            }
            return;
        }
        for (String action : clickAction.actions()) {
            ActionUtil.executeAction(action, player, event);
        }
    }
}