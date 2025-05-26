package org.notionsmp.notionMenus.listeners;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
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
        if (!(event.getView().getTopInventory().getHolder() instanceof CustomInventoryHolder holder)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        GuiConfig guiConfig = NotionMenus.getInstance().getGuiManager().getGuis().get(holder.getGuiId());
        if (guiConfig == null) return;

        event.setCancelled(true);

        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory == null) {

            if (!guiConfig.isLockInventory() && event.getClick().isKeyboardClick()) {
                event.setCancelled(false);
            }
            return;
        }

        if (clickedInventory.getType() == InventoryType.PLAYER) {

            if (!guiConfig.isLockInventory()) {

                event.setCancelled(false);

                if (event.getClick().isShiftClick()) {
                    event.setCancelled(true);
                    executeClickActions(event, holder, guiConfig, player);
                }
            }
            return;
        }

        if (clickedInventory.equals(event.getView().getTopInventory())) {
            executeClickActions(event, holder, guiConfig, player);
        }
    }

    private void executeClickActions(InventoryClickEvent event, CustomInventoryHolder holder,
                                     GuiConfig guiConfig, Player player) {
        int slot = event.getSlot();
        Map<String, GuiConfig.ClickAction> clickActions = guiConfig.getClickActions(slot, player);

        executeActions(clickActions.get("all"), event);

        String clickType = getClickType(event);
        if (clickType != null) {
            executeActions(clickActions.get(clickType), event);
        }
    }

    private String getClickType(InventoryClickEvent event) {
        return switch (event.getClick()) {
            case LEFT -> "left";
            case RIGHT -> "right";
            case SHIFT_LEFT -> "shift_left";
            case SHIFT_RIGHT -> "shift_right";
            case MIDDLE -> "middle";
            case DOUBLE_CLICK -> "double";
            case DROP -> "drop";
            case CONTROL_DROP -> "ctrl_drop";
            case NUMBER_KEY -> "number_key";
            case SWAP_OFFHAND -> "swap_offhand";
            default -> null;
        };
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