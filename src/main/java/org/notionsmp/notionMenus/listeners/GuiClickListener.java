package org.notionsmp.notionMenus.listeners;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.notionsmp.notionMenus.NotionMenus;
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
        Player player = (Player) event.getWhoClicked();
        for (GuiConfig guiConfig : NotionMenus.getInstance().getGuiManager().getGuis().values()) {
            String parsedTitle = GuiConfig.replacePlaceholders(player, GuiConfig.parsePlaceholders(player, guiConfig.getTitle()));
            Component parsedTitleComponent = MiniMessage.miniMessage().deserialize(parsedTitle);
            if (clickedInventory.getSize() == guiConfig.getSize() &&
                    event.getView().title().equals(parsedTitleComponent)) {
                event.setCancelled(true);
                int slot = event.getSlot();
                Map<String, GuiConfig.ClickAction> clickActions = guiConfig.getClickActions(slot, player);
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                    switch (event.getClick()) {
                        case LEFT:
                            executeActions(clickActions.get("left"), event);
                            break;
                        case RIGHT:
                            executeActions(clickActions.get("right"), event);
                            break;
                        case SHIFT_LEFT:
                            executeActions(clickActions.get("shift_left"), event);
                            break;
                        case SHIFT_RIGHT:
                            executeActions(clickActions.get("shift_right"), event);
                            break;
                        case MIDDLE:
                            executeActions(clickActions.get("middle"), event);
                            break;
                        case DOUBLE_CLICK:
                            executeActions(clickActions.get("double"), event);
                            break;
                        case DROP:
                            executeActions(clickActions.get("drop"), event);
                            break;
                        case CONTROL_DROP:
                            executeActions(clickActions.get("ctrl_drop"), event);
                            break;
                    }
                    return;
                }
                switch (event.getClick()) {
                    case LEFT:
                        executeActions(clickActions.get("left"), event);
                        break;
                    case RIGHT:
                        executeActions(clickActions.get("right"), event);
                        break;
                    case SHIFT_LEFT:
                        executeActions(clickActions.get("shift_left"), event);
                        break;
                    case SHIFT_RIGHT:
                        executeActions(clickActions.get("shift_right"), event);
                        break;
                    case MIDDLE:
                        executeActions(clickActions.get("middle"), event);
                        break;
                    case DOUBLE_CLICK:
                        executeActions(clickActions.get("double"), event);
                        break;
                    case DROP:
                        executeActions(clickActions.get("drop"), event);
                        break;
                    case CONTROL_DROP:
                        executeActions(clickActions.get("ctrl_drop"), event);
                        break;
                }
            }
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