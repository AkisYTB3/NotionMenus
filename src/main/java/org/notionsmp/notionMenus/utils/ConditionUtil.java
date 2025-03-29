package org.notionsmp.notionMenus.utils;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.notionsmp.notionMenus.NotionMenus;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class ConditionUtil {
    public static boolean checkMoneyCondition(Player player, int amount) {
        if (NotionMenus.getInstance().getEconomy() == null) return false;
        return NotionMenus.getInstance().getEconomy().has(player, amount);
    }

    public static boolean checkItemCondition(Player player, String condition) {
        String[] parts = condition.split("\\[|\\]");
        if (parts.length < 3) return false;
        String itemName = parts[2].trim();
        String[] itemParts = itemName.split(":");
        if (itemParts.length < 2) return false;
        String materialName = itemParts[0];
        int amount = Integer.parseInt(itemParts[1]);
        Material material = Material.matchMaterial(materialName);
        if (material == null) return false;
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count >= amount;
    }

    public static boolean checkMetaCondition(Player player, String condition) {
        String[] parts = condition.split("\\[|\\]");
        if (parts.length < 3) return false;
        String metaValue = parts[2].trim();
        String[] metaParts = metaValue.split(":");
        if (metaParts.length < 2) return false;
        String key = metaParts[0];
        String expectedValue = metaParts[1];
        String actualValue = player.getPersistentDataContainer().get(
                new NamespacedKey(NotionMenus.getInstance(), key),
                PersistentDataType.STRING
        );
        return Objects.equals(expectedValue, actualValue);
    }

    public static boolean checkNearCondition(Player player, String condition) {
        String[] parts = condition.split("\\[|\\]");
        if (parts.length < 3) return false;
        String nearValue = parts[2].trim();
        String[] nearParts = nearValue.split(":");
        if (nearParts.length < 3) return false;
        String targetName = nearParts[0];
        int distance = Integer.parseInt(nearParts[1]);
        int requiredCount = Integer.parseInt(nearParts[2]);
        int count = 0;
        for (Player nearby : player.getWorld().getPlayers()) {
            if (nearby != player &&
                    nearby.getName().equalsIgnoreCase(targetName) &&
                    nearby.getLocation().distance(player.getLocation()) <= distance) {
                count++;
            }
        }
        return count >= requiredCount;
    }

    public static boolean checkEqualsCondition(String condition) {
        String[] parts = condition.split("\\[|\\]");
        if (parts.length < 3) return false;
        String compareValue = parts[2].trim();
        String[] compareParts = compareValue.split(":");
        if (compareParts.length < 2) return false;
        return compareParts[0].equals(compareParts[1]);
    }

    public static boolean checkContainsCondition(String condition) {
        String[] parts = condition.split("\\[|\\]");
        if (parts.length < 3) return false;
        String compareValue = parts[2].trim();
        String[] compareParts = compareValue.split(":");
        if (compareParts.length < 2) return false;
        return compareParts[0].contains(compareParts[1]);
    }

    public static boolean checkRegexCondition(String condition) {
        String[] parts = condition.split("\\[|\\]");
        if (parts.length < 3) return false;
        String regexValue = parts[2].trim();
        String[] regexParts = regexValue.split(":");
        if (regexParts.length < 2) return false;
        return Pattern.compile(regexParts[1]).matcher(regexParts[0]).matches();
    }

    public static boolean checkCompareCondition(String condition) {
        String[] parts = condition.split("\\[|\\]");
        if (parts.length < 3) return false;
        String compareValue = parts[2].trim();
        String[] compareParts = compareValue.split(":");
        if (compareParts.length < 2) return false;
        try {
            double num1 = Double.parseDouble(compareParts[0]);
            double num2 = Double.parseDouble(compareParts[1]);
            return num1 > num2;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean checkPermissionCondition(Player player, String permission) {
        return player.hasPermission(permission);
    }

    public static boolean checkGameModeCondition(Player player, String gameMode) {
        try {
            GameMode targetMode = GameMode.valueOf(gameMode.toUpperCase());
            return player.getGameMode() == targetMode;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean checkWorldCondition(Player player, String worldName) {
        return player.getWorld().getName().equalsIgnoreCase(worldName);
    }

    public static boolean checkXPCondition(Player player, String xpValue) {
        if (xpValue.endsWith("l")) {
            int levels = Integer.parseInt(xpValue.substring(0, xpValue.length() - 1));
            return player.getLevel() >= levels;
        } else if (xpValue.endsWith("p")) {
            int points = Integer.parseInt(xpValue.substring(0, xpValue.length() - 1));
            return player.getTotalExperience() >= points;
        }
        return false;
    }

    public static boolean checkConditions(List<String> conditions, Player player) {
        if (conditions == null || conditions.isEmpty()) return true;
        for (String condition : conditions) {
            if (condition == null || condition.isEmpty()) continue;
            String[] parts = condition.split("\\[|\\]");
            if (parts.length < 2) continue;
            String conditionType = parts[1].toLowerCase();
            String conditionValue = condition.substring(condition.indexOf(']') + 1).trim();
            boolean result;
            switch (conditionType) {
                case "money":
                    result = checkMoneyCondition(player, Integer.parseInt(conditionValue));
                    break;
                case "!money":
                    result = !checkMoneyCondition(player, Integer.parseInt(conditionValue));
                    break;
                case "item":
                    result = checkItemCondition(player, condition);
                    break;
                case "!item":
                    result = !checkItemCondition(player, condition);
                    break;
                case "meta":
                    result = checkMetaCondition(player, condition);
                    break;
                case "!meta":
                    result = !checkMetaCondition(player, condition);
                    break;
                case "near":
                    result = checkNearCondition(player, condition);
                    break;
                case "!near":
                    result = !checkNearCondition(player, condition);
                    break;
                case "equals":
                    result = checkEqualsCondition(condition);
                    break;
                case "!equals":
                    result = !checkEqualsCondition(condition);
                    break;
                case "contains":
                    result = checkContainsCondition(condition);
                    break;
                case "!contains":
                    result = !checkContainsCondition(condition);
                    break;
                case "regex":
                    result = checkRegexCondition(condition);
                    break;
                case "!regex":
                    result = !checkRegexCondition(condition);
                    break;
                case "compare":
                    result = checkCompareCondition(condition);
                    break;
                case "!compare":
                    result = !checkCompareCondition(condition);
                    break;
                case "permission":
                    result = checkPermissionCondition(player, conditionValue);
                    break;
                case "!permission":
                    result = !checkPermissionCondition(player, conditionValue);
                    break;
                case "gamemode":
                    result = checkGameModeCondition(player, conditionValue);
                    break;
                case "!gamemode":
                    result = !checkGameModeCondition(player, conditionValue);
                    break;
                case "world":
                    result = checkWorldCondition(player, conditionValue);
                    break;
                case "!world":
                    result = !checkWorldCondition(player, conditionValue);
                    break;
                case "xp":
                    result = checkXPCondition(player, conditionValue);
                    break;
                case "!xp":
                    result = !checkXPCondition(player, conditionValue);
                    break;
                default:
                    result = true;
                    break;
            }
            if (!result) {
                return false;
            }
        }
        return true;
    }
}