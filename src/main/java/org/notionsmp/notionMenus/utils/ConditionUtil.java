package org.notionsmp.notionMenus.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.notionsmp.notionMenus.NotionMenus;
import org.notionsmp.notionMenus.gui.GuiConfig;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConditionUtil {
    public static boolean checkMoneyCondition(Player player, String amountStr) {
        if (NotionMenus.getInstance().getEconomy() == null) return false;
        String processedAmount = processConditionValue(amountStr, player);
        try {
            int amount = Integer.parseInt(processedAmount);
            return NotionMenus.getInstance().getEconomy().has(player, amount);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean checkItemCondition(Player player, String condition) {
        String processed = processConditionValue(condition, player);
        String[] parts = processed.split("\\[|\\]");
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
        String processed = processConditionValue(condition, player);
        String[] parts = processed.split("\\[|\\]");
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
        String processed = processConditionValue(condition, player);
        String[] parts = processed.split("\\[|\\]");
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

    public static boolean checkContainsCondition(Player player, String condition) {
        String processed = processConditionValue(condition, player);
        String[] parts = processed.split("\\[contains\\]", 2);
        if (parts.length < 2) return false;

        String[] components = parts[1].trim().split("\\s+", 2);
        if (components.length < 2) return false;

        return components[0].contains(components[1]);
    }

    public static boolean checkRegexCondition(Player player, String condition) {
        String processed = processConditionValue(condition, player);
        String[] parts = processed.split("\\[regex\\]", 2);
        if (parts.length < 2) return false;

        String[] components = parts[1].trim().split("\\s+", 2);
        if (components.length < 2) return false;

        String text = components[0];
        String regex = components[1];
        return Pattern.compile(regex).matcher(text).find();
    }

    public static boolean checkPlaceholderCondition(Player player, String condition) {
        String processed = processConditionValue(condition, player);
        String[] parts = processed.split("\\[placeholder\\]", 2);
        if (parts.length < 2) return false;

        String placeholder = parts[1].trim();
        String value = NotionMenus.getInstance().isPAPILoaded()
                ? PlaceholderAPI.setPlaceholders(player, placeholder)
                : placeholder;

        return Boolean.parseBoolean(value) || "true".equalsIgnoreCase(value);
    }

    public static boolean checkLengthCondition(Player player, String condition) {
        String processed = processConditionValue(condition, player);
        String[] parts = processed.split("\\[length\\]", 2);
        if (parts.length < 2) return false;

        String[] components = parts[1].trim().split("\\s+", 3);
        if (components.length < 3) return false;

        try {
            int min = Integer.parseInt(components[0]);
            int max = Integer.parseInt(components[1]);
            String input = components[2];
            int length = input.length();
            return length >= min && length <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean checkCompareCondition(Player player, String condition) {
        String processed = processConditionValue(condition, player);
        String[] parts = processed.split("\\[compare\\]", 2);
        if (parts.length < 2) return false;

        String comparison = parts[1].trim();

        if (comparison.contains("==")) {
            String[] operands = comparison.split("==", 2);
            if (operands.length == 2) {
                return operands[0].trim().equals(operands[1].trim());
            }
        }

        Pattern pattern = Pattern.compile("^(-?\\d+\\.?\\d*)\\s*([<>]=?|==)\\s*(-?\\d+\\.?\\d*)$");
        Matcher matcher = pattern.matcher(comparison);

        if (matcher.find()) {
            try {
                double num1 = Double.parseDouble(matcher.group(1).trim());
                double num2 = Double.parseDouble(matcher.group(3).trim());
                String operator = matcher.group(2).trim();

                return switch (operator) {
                    case "<" -> num1 < num2;
                    case ">" -> num1 > num2;
                    case "<=" -> num1 <= num2;
                    case ">=" -> num1 >= num2;
                    case "==" -> num1 == num2;
                    default -> false;
                };
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    public static boolean checkPermissionCondition(Player player, String permission) {
        String processed = processConditionValue(permission, player);
        return player.hasPermission(processed);
    }

    public static boolean checkGameModeCondition(Player player, String gameMode) {
        String processed = processConditionValue(gameMode, player);
        try {
            GameMode targetMode = GameMode.valueOf(processed.toUpperCase());
            return player.getGameMode() == targetMode;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean checkWorldCondition(Player player, String worldName) {
        String processed = processConditionValue(worldName, player);
        return player.getWorld().getName().equalsIgnoreCase(processed);
    }

    public static boolean checkXPCondition(Player player, String xpValue) {
        String processed = processConditionValue(xpValue, player);
        if (processed.endsWith("l")) {
            int levels = Integer.parseInt(processed.substring(0, processed.length() - 1));
            return player.getLevel() >= levels;
        } else if (processed.endsWith("p")) {
            int points = Integer.parseInt(processed.substring(0, processed.length() - 1));
            return player.getTotalExperience() >= points;
        }
        return false;
    }

    private static String processConditionValue(String value, Player player) {

        String processed = value;
        processed = processed.replace("<player>", player.getName())
                .replace("<location>", player.getLocation().toString())
                .replace("<playerX>", String.valueOf(player.getLocation().getX()))
                .replace("<playerY>", String.valueOf(player.getLocation().getY()))
                .replace("<playerZ>", String.valueOf(player.getLocation().getZ()))
                .replace("<player_health>", String.valueOf(player.getHealth()))
                .replace("<player_food>", String.valueOf(player.getFoodLevel()));

        return GuiConfig.parsePlaceholders(player, processed);
    }

    public static boolean checkConditions(List<String> conditions, Player player) {
        if (conditions == null || conditions.isEmpty()) return true;
        for (String condition : conditions) {
            if (condition == null || condition.isEmpty()) continue;

            String processedCondition = processConditionValue(condition, player);
            String[] parts = processedCondition.split("\\[|\\]");
            if (parts.length < 2) continue;

            String conditionType = parts[1].toLowerCase();
            String conditionValue = processedCondition.substring(processedCondition.indexOf(']') + 1).trim();
            boolean result;

            switch (conditionType) {
                case "money":
                    result = checkMoneyCondition(player, conditionValue);
                    break;
                case "!money":
                    result = !checkMoneyCondition(player, conditionValue);
                    break;
                case "item":
                    result = checkItemCondition(player, processedCondition);
                    break;
                case "!item":
                    result = !checkItemCondition(player, processedCondition);
                    break;
                case "meta":
                    result = checkMetaCondition(player, processedCondition);
                    break;
                case "!meta":
                    result = !checkMetaCondition(player, processedCondition);
                    break;
                case "near":
                    result = checkNearCondition(player, processedCondition);
                    break;
                case "!near":
                    result = !checkNearCondition(player, processedCondition);
                    break;
                case "contains":
                    result = checkContainsCondition(player, processedCondition);
                    break;
                case "!contains":
                    result = !checkContainsCondition(player, processedCondition);
                    break;
                case "regex":
                    result = checkRegexCondition(player, processedCondition);
                    break;
                case "!regex":
                    result = !checkRegexCondition(player, processedCondition);
                    break;
                case "placeholder":
                    result = checkPlaceholderCondition(player, processedCondition);
                    break;
                case "!placeholder":
                    result = !checkPlaceholderCondition(player, processedCondition);
                    break;
                case "length":
                    result = checkLengthCondition(player, processedCondition);
                    break;
                case "!length":
                    result = !checkLengthCondition(player, processedCondition);
                    break;
                case "compare":
                    result = checkCompareCondition(player, processedCondition);
                    break;
                case "!compare":
                    result = !checkCompareCondition(player, processedCondition);
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