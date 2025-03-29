package org.notionsmp.notionMenus.utils;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.notionsmp.notionMenus.NotionMenus;

import java.util.*;

public class ConditionUtil {
    public static boolean checkMetaCondition(Player player, String condition) {
        String[] parts = condition.substring(condition.indexOf(']') + 1).trim().split(",");
        String key = parts[0].split(":")[1].trim().replace("\"", "");
        String value = parts[1].split(":")[1].trim().replace("\"", "");

        NamespacedKey namespacedKey = new NamespacedKey(NotionMenus.getInstance(), key);
        PersistentDataContainer pdc = player.getPersistentDataContainer();

        if (!pdc.has(namespacedKey, getDataType(value))) {
            return false;
        }

        return switch (value.charAt(value.length() - 1)) {
            case 'b' -> {
                boolean expected = switch (value) {
                    case "1b" -> true;
                    case "0b" -> false;
                    default -> throw new IllegalArgumentException("Invalid boolean value: " + value);
                };
                yield pdc.get(namespacedKey, PersistentDataType.BYTE) == (byte) (expected ? 1 : 0);
            }
            case 'l' -> pdc.get(namespacedKey, PersistentDataType.LONG) >= Long.parseLong(value.substring(0, value.length() - 1));
            case 'd' -> pdc.get(namespacedKey, PersistentDataType.DOUBLE) >= Double.parseDouble(value.substring(0, value.length() - 1));
            default -> {
                if (value.contains(".")) {
                    yield pdc.get(namespacedKey, PersistentDataType.DOUBLE) >= Double.parseDouble(value);
                } else {
                    yield pdc.get(namespacedKey, PersistentDataType.INTEGER) >= Integer.parseInt(value);
                }
            }
        };
    }

    private static PersistentDataType<?, ?> getDataType(String value) {
        return switch (value.charAt(value.length() - 1)) {
            case 'b' -> PersistentDataType.BYTE;
            case 'l' -> PersistentDataType.LONG;
            case 'd' -> PersistentDataType.DOUBLE;
            default -> value.contains(".") ? PersistentDataType.DOUBLE : PersistentDataType.INTEGER;
        };
    }

    public static boolean checkNearCondition(Player player, String condition) {
        String[] parts = condition.substring(condition.indexOf(']') + 1).trim().split(",");
        String worldName = parts[0].trim();
        Location targetLoc = new Location(
                player.getWorld(),
                Double.parseDouble(parts[1].trim()),
                Double.parseDouble(parts[2].trim()),
                Double.parseDouble(parts[3].trim())
        );
        double maxDistance = Double.parseDouble(parts[4].trim());

        return player.getWorld().getName().equalsIgnoreCase(worldName) &&
                player.getLocation().distance(targetLoc) <= maxDistance;
    }

    public static boolean checkEqualsCondition(String condition) {
        String[] parts = condition.substring(condition.indexOf(']') + 1).trim().split(",");
        String input = parts[0].trim().replace("\"", "");
        String output = parts[1].trim().replace("\"", "");
        boolean ignoreCase = parts.length > 2 && Boolean.parseBoolean(parts[2].trim());

        return ignoreCase ? input.equalsIgnoreCase(output) : input.equals(output);
    }

    public static boolean checkContainsCondition(String condition) {
        String[] parts = condition.substring(condition.indexOf(']') + 1).trim().split(",");
        String input = parts[0].trim().replace("\"", "");
        String output = parts[1].trim().replace("\"", "");
        boolean ignoreCase = parts.length > 2 && Boolean.parseBoolean(parts[2].trim());

        return ignoreCase ? input.toLowerCase().contains(output.toLowerCase()) : input.contains(output);
    }

    public static boolean checkRegexCondition(String condition) {
        String[] parts = condition.substring(condition.indexOf(']') + 1).trim().split(",");
        String input = parts[0].trim().replace("\"", "");
        String regex = parts[1].trim().replace("\"", "");

        return input.matches(regex);
    }

    public static boolean checkCompareCondition(String condition) {
        String[] parts = condition.substring(condition.indexOf(']') + 1).trim().split(",");
        double input = Double.parseDouble(parts[0].trim());
        String operator = parts[1].trim();
        double output = Double.parseDouble(parts[2].trim());

        return switch (operator) {
            case "==" -> input == output;
            case ">=" -> input >= output;
            case "<=" -> input <= output;
            case "!=" -> input != output;
            case ">" -> input > output;
            case "<" -> input < output;
            default -> false;
        };
    }

    public static boolean checkMoneyCondition(Player player, int amount) {
        NotionMenus plugin = NotionMenus.getInstance();
        if (!plugin.isVaultEnabled() || plugin.getEconomy() == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>Vault is not installed. Money-related features are disabled."));
            return false;
        }
        return plugin.getEconomy().has(player, amount);
    }

    public static boolean checkItemCondition(Player player, String condition) {
        Map<String, String> params = parseItemConditionParams(condition);

        Material material = Material.matchMaterial(params.getOrDefault("material", "STONE"));
        int customModelData = Integer.parseInt(params.getOrDefault("custom_model_data", "-1"));
        int amount = Integer.parseInt(params.getOrDefault("amount", "1"));
        String itemName = params.getOrDefault("itemname", "");
        boolean nameContains = Boolean.parseBoolean(params.getOrDefault("name_contains", "false"));
        boolean nameIgnoreCase = Boolean.parseBoolean(params.getOrDefault("name_ignorecase", "false"));
        boolean loreContains = Boolean.parseBoolean(params.getOrDefault("lore_contains", "false"));
        boolean loreIgnoreCase = Boolean.parseBoolean(params.getOrDefault("lore_ignorecase", "false"));
        boolean strict = Boolean.parseBoolean(params.getOrDefault("strict", "false"));
        boolean armor = Boolean.parseBoolean(params.getOrDefault("armor", "false"));
        boolean offhand = Boolean.parseBoolean(params.getOrDefault("offhand", "false"));
        List<String> lore = parseLore(params.get("lore"));

        return checkInventoryItems(player, material, customModelData, amount, itemName, lore,
                nameContains, nameIgnoreCase, loreContains, loreIgnoreCase, strict, armor, offhand);
    }

    private static Map<String, String> parseItemConditionParams(String condition) {
        String[] parts = condition.substring(condition.indexOf(']') + 1).trim().split(",");
        Map<String, String> params = new HashMap<>();
        for (String part : parts) {
            String[] keyValue = part.split(":", 2);
            if (keyValue.length == 2) {
                params.put(keyValue[0].trim().toLowerCase(), keyValue[1].trim());
            }
        }
        return params;
    }

    private static List<String> parseLore(String loreString) {
        if (loreString == null) return Collections.emptyList();

        loreString = loreString.replace("[", "").replace("]", "").trim();
        if (loreString.isEmpty()) return Collections.emptyList();

        return Arrays.stream(loreString.split(","))
                .map(line -> line.trim().replace("\"", ""))
                .toList();
    }

    private static boolean checkInventoryItems(Player player, Material material, int customModelData,
                                               int amount, String itemName, List<String> lore, boolean nameContains,
                                               boolean nameIgnoreCase, boolean loreContains, boolean loreIgnoreCase,
                                               boolean strict, boolean armor, boolean offhand) {

        if (checkItems(player.getInventory().getContents(), material, customModelData, amount,
                itemName, lore, nameContains, nameIgnoreCase, loreContains,
                loreIgnoreCase, strict)) {
            return true;
        }

        if (armor && checkItems(player.getInventory().getArmorContents(), material,
                customModelData, amount, itemName, lore, nameContains,
                nameIgnoreCase, loreContains, loreIgnoreCase, strict)) {
            return true;
        }

        if (offhand) {
            ItemStack offhandItem = player.getInventory().getItemInOffHand();
            return offhandItem != null && matchesItem(offhandItem, customModelData, amount,
                    itemName, lore, nameContains, nameIgnoreCase, loreContains,
                    loreIgnoreCase, strict);
        }

        return false;
    }

    private static boolean checkItems(ItemStack[] items, Material material, int customModelData,
                                      int amount, String itemName, List<String> lore, boolean nameContains,
                                      boolean nameIgnoreCase, boolean loreContains, boolean loreIgnoreCase,
                                      boolean strict) {

        for (ItemStack item : items) {
            if (item != null && item.getType() == material &&
                    matchesItem(item, customModelData, amount, itemName, lore,
                            nameContains, nameIgnoreCase, loreContains, loreIgnoreCase, strict)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesItem(ItemStack item, int customModelData, int amount,
                                       String itemName, List<String> lore, boolean nameContains,
                                       boolean nameIgnoreCase, boolean loreContains, boolean loreIgnoreCase,
                                       boolean strict) {

        if (item == null) return false;

        if (strict && (item.getItemMeta().hasCustomModelData() ||
                item.getItemMeta().hasDisplayName() || item.getItemMeta().hasLore())) {
            return false;
        }

        if (amount > 0 && item.getAmount() < amount) {
            return false;
        }

        if (customModelData != -1 && item.getItemMeta().hasCustomModelData() &&
                item.getItemMeta().getCustomModelData() != customModelData) {
            return false;
        }

        if (!itemName.isEmpty() && !matchesName(item, itemName, nameContains, nameIgnoreCase)) {
            return false;
        }

        return lore.isEmpty() || matchesLore(item, lore, loreContains, loreIgnoreCase);
    }

    private static boolean matchesName(ItemStack item, String itemName,
                                       boolean nameContains, boolean nameIgnoreCase) {
        if (!item.getItemMeta().hasDisplayName()) return false;

        String displayName = item.getItemMeta().getDisplayName();
        if (nameContains) {
            return nameIgnoreCase ?
                    displayName.toLowerCase().contains(itemName.toLowerCase()) :
                    displayName.contains(itemName);
        } else {
            return nameIgnoreCase ?
                    displayName.equalsIgnoreCase(itemName) :
                    displayName.equals(itemName);
        }
    }

    private static boolean matchesLore(ItemStack item, List<String> lore,
                                       boolean loreContains, boolean loreIgnoreCase) {
        if (!item.getItemMeta().hasLore()) return false;

        List<String> itemLore = item.getItemMeta().getLore();
        for (String loreLine : lore) {
            boolean found = false;
            for (String itemLoreLine : itemLore) {
                if (loreContains) {
                    if (loreIgnoreCase && itemLoreLine.toLowerCase().contains(loreLine.toLowerCase())) {
                        found = true;
                        break;
                    } else if (itemLoreLine.contains(loreLine)) {
                        found = true;
                        break;
                    }
                } else {
                    if (loreIgnoreCase && itemLoreLine.equalsIgnoreCase(loreLine)) {
                        found = true;
                        break;
                    } else if (itemLoreLine.equals(loreLine)) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }
}