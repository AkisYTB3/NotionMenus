package org.notionsmp.notionMenus.listeners;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.notionsmp.notionMenus.NotionMenus;
import org.notionsmp.notionMenus.gui.GuiConfig;
import org.notionsmp.notionMenus.utils.ActionUtil;
import org.notionsmp.notionMenus.utils.ConditionUtil;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

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
                }
            }
        }
    }

    private void executeActions(GuiConfig.ClickAction clickAction, InventoryClickEvent event) {
        if (clickAction == null || clickAction.actions().isEmpty()) return;

        Player player = (Player) event.getWhoClicked();

        if (!checkConditions(clickAction.conditions(), player)) {
            for (String denyAction : clickAction.denyActions()) {
                ActionUtil.executeAction(denyAction, player, event);
            }
            return;
        }

        for (String action : clickAction.actions()) {
            ActionUtil.executeAction(action, player, event);
        }
    }

    private boolean checkConditions(List<String> conditions, Player player) {
        if (conditions == null || conditions.isEmpty()) return true;

        for (String condition : conditions) {
            if (condition.startsWith("[money]")) {
                int amount = Integer.parseInt(condition.substring(8).trim());
                if (!ConditionUtil.checkMoneyCondition(player, amount)) return false;
            } else if (condition.startsWith("[!money]")) {
                int amount = Integer.parseInt(condition.substring(9).trim());
                if (ConditionUtil.checkMoneyCondition(player, amount)) return false;
            } else if (condition.startsWith("[item]")) {
                if (!ConditionUtil.checkItemCondition(player, condition)) return false;
            } else if (condition.startsWith("[!item]")) {
                if (ConditionUtil.checkItemCondition(player, condition)) return false;
            } else if (condition.startsWith("[meta]")) {
                if (!ConditionUtil.checkMetaCondition(player, condition)) return false;
            } else if (condition.startsWith("[!meta]")) {
                if (ConditionUtil.checkMetaCondition(player, condition)) return false;
            } else if (condition.startsWith("[near]")) {
                if (!ConditionUtil.checkNearCondition(player, condition)) return false;
            } else if (condition.startsWith("[!near]")) {
                if (ConditionUtil.checkNearCondition(player, condition)) return false;
            } else if (condition.startsWith("[equals]")) {
                if (!ConditionUtil.checkEqualsCondition(condition)) return false;
            } else if (condition.startsWith("[!equals]")) {
                if (ConditionUtil.checkEqualsCondition(condition)) return false;
            } else if (condition.startsWith("[contains]")) {
                if (!ConditionUtil.checkContainsCondition(condition)) return false;
            } else if (condition.startsWith("[!contains]")) {
                if (ConditionUtil.checkContainsCondition(condition)) return false;
            } else if (condition.startsWith("[regex]")) {
                if (!ConditionUtil.checkRegexCondition(condition)) return false;
            } else if (condition.startsWith("[!regex]")) {
                if (ConditionUtil.checkRegexCondition(condition)) return false;
            } else if (condition.startsWith("[compare]")) {
                if (!ConditionUtil.checkCompareCondition(condition)) return false;
            } else if (condition.startsWith("[!compare]")) {
                if (ConditionUtil.checkCompareCondition(condition)) return false;
            }
        }

        return true;
    }

    private String replacePlaceholders(Player player, String text, InventoryClickEvent event) {
        return text.replace("<player>", player.getName())
                .replace("<location>", player.getLocation().toString())
                .replace("<playerX>", String.valueOf(player.getLocation().getX()))
                .replace("<playerY>", String.valueOf(player.getLocation().getY()))
                .replace("<playerZ>", String.valueOf(player.getLocation().getZ()))
                .replace("<player_health>", String.valueOf(player.getHealth()))
                .replace("<player_food>", String.valueOf(player.getFoodLevel()))
                .replace("<slot_clicked>", String.valueOf(event.getSlot()))
                .replace("<raw_slot_clicked>", String.valueOf(event.getRawSlot()));
    }
}