package org.notionsmp.notionMenus.gui;

import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.notionsmp.notionMenus.NotionMenus;
import org.notionsmp.notionMenus.hooks.*;
import org.notionsmp.notionMenus.utils.ConditionUtil;

import java.util.*;
import java.util.regex.Matcher;

@Getter
public class GuiConfig {
    private final String id;
    private final String title;
    private final int size;
    private final List<String> commands;
    private final Map<Integer, List<ConfigurationSection>> itemConfigs;
    private final Map<Integer, Map<String, ClickAction>> clickActions;
    private final Map<String, ItemHook> itemHooks = new HashMap<>();
    private final ClickAction openActions;
    private final List<String> openConditions;
    private static final Random random = new Random();

    public GuiConfig(FileConfiguration config) {
        this.id = config.getString("id");
        this.title = config.getString("title");
        this.size = config.getInt("size", 54);
        if (config.isList("command")) {
            this.commands = config.getStringList("command");
        } else {
            String singleCommand = config.getString("command");
            this.commands = singleCommand != null ? Collections.singletonList(singleCommand) : Collections.emptyList();
        }
        ConfigurationSection openActionsSection = config.getConfigurationSection("open_actions");
        if (openActionsSection != null) {
            List<String> conditions = openActionsSection.getStringList("conditions");
            List<String> actions = openActionsSection.getStringList("actions");
            List<String> denyActions = openActionsSection.getStringList("deny_actions");
            this.openActions = new ClickAction(conditions, actions, denyActions);
        } else {
            this.openActions = new ClickAction(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }
        this.openConditions = config.getStringList("open_conditions");
        this.itemConfigs = new HashMap<>();
        this.clickActions = new HashMap<>();
        if (Bukkit.getPluginManager().isPluginEnabled("Nexo")) {
            this.itemHooks.put("nexo", new NexoHook());
        }
        if (Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) {
            this.itemHooks.put("itemsadder", new ItemsAdderHook());
        }
        this.itemHooks.put("basehead", new BaseheadHook());
        this.itemHooks.put("head", new HeadHook());
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String itemKey : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
                if (itemSection != null) {
                    addItemToSlot(itemSection);
                }
            }
        }
    }

    private void addItemToSlot(ConfigurationSection itemSection) {
        Object slotObject = itemSection.get("slot");
        if (slotObject == null) return;
        List<Integer> slots = parseSlots(slotObject);
        for (int slot : slots) {
            if (slot != -1) {
                itemConfigs.computeIfAbsent(slot, k -> new ArrayList<>()).add(itemSection);
                ConfigurationSection clickActionsSection = itemSection.getConfigurationSection("click_actions");
                if (clickActionsSection != null) {
                    Map<String, ClickAction> actions = new HashMap<>();
                    for (String clickType : clickActionsSection.getKeys(false)) {
                        List<String> conditions = clickActionsSection.getStringList(clickType + ".conditions");
                        List<String> actionList = clickActionsSection.getStringList(clickType + ".actions");
                        List<String> denyActions = clickActionsSection.getStringList(clickType + ".deny_actions");
                        actions.put(clickType, new ClickAction(conditions, actionList, denyActions));
                    }
                    clickActions.put(slot, actions);
                }
            }
        }
    }

    private List<Integer> parseSlots(Object slotObject) {
        List<Integer> slots = new ArrayList<>();
        if (slotObject instanceof Integer) {
            slots.add((Integer) slotObject);
        } else if (slotObject instanceof List) {
            for (Object entry : (List<?>) slotObject) {
                if (entry instanceof Integer) {
                    slots.add((Integer) entry);
                } else if (entry instanceof String range) {
                    if (range.contains("..")) {
                        String[] parts = range.split("\\.\\.");
                        if (parts.length == 2) {
                            try {
                                int start = Integer.parseInt(parts[0]);
                                int end = Integer.parseInt(parts[1]);
                                for (int i = start; i <= end; i++) {
                                    slots.add(i);
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    } else {
                        try {
                            slots.add(Integer.parseInt(range));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } else if (slotObject instanceof String range) {
            if (range.contains("..")) {
                String[] parts = range.split("\\.\\.");
                if (parts.length == 2) {
                    try {
                        int start = Integer.parseInt(parts[0]);
                        int end = Integer.parseInt(parts[1]);
                        for (int i = start; i <= end; i++) {
                            slots.add(i);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            } else {
                try {
                    slots.add(Integer.parseInt(range));
                } catch (NumberFormatException ignored) {}
            }
        }
        return slots;
    }

    public ItemStack createItemFromConfig(int slot, Player player) {
        List<ConfigurationSection> itemsForSlot = itemConfigs.get(slot);
        if (itemsForSlot == null || itemsForSlot.isEmpty()) {
            return new ItemStack(Material.AIR);
        }
        itemsForSlot.sort((item1, item2) -> {
            int priority1 = item1.getInt("priority", 0);
            int priority2 = item2.getInt("priority", 0);
            return Integer.compare(priority2, priority1);
        });
        for (ConfigurationSection itemSection : itemsForSlot) {
            if (checkViewConditions(itemSection.getStringList("view_conditions"), player)) {
                return createItemFromConfig(itemSection, player);
            }
        }
        return new ItemStack(Material.AIR);
    }

    private ItemStack createItemFromConfig(ConfigurationSection itemSection, Player player) {
        if (itemSection.contains("view_conditions")) {
            List<String> viewConditions = itemSection.getStringList("view_conditions");
            if (!checkViewConditions(viewConditions, player)) {
                return new ItemStack(Material.AIR);
            }
        }
        String materialName = itemSection.getString("material");
        Material material = Material.STONE;
        if (player != null && materialName != null) {
            materialName = replacePlaceholders(player, materialName);
        }
        ItemStack item = null;
        if (materialName != null && materialName.contains("-")) {
            String[] parts = materialName.split("-", 2);
            String hookPrefix = parts[0];
            String hookId = parts[1];
            if (itemHooks.containsKey(hookPrefix)) {
                item = itemHooks.get(hookPrefix).getItem(hookId, player);
            }
        }
        if (item == null && materialName != null) {
            switch (materialName.toLowerCase()) {
                case "empty":
                    material = Material.AIR;
                    item = new ItemStack(material);
                    break;
                case "offhand":
                    if (player != null) {
                        item = player.getInventory().getItemInOffHand();
                        if (item.getType() == Material.AIR) {
                            item = new ItemStack(Material.AIR);
                        } else {
                            item = item.clone();
                        }
                    } else {
                        item = new ItemStack(Material.AIR);
                    }
                    break;
                case "mainhand":
                    if (player != null) {
                        item = player.getInventory().getItemInMainHand();
                        if (item.getType() == Material.AIR) {
                            item = new ItemStack(Material.AIR);
                        } else {
                            item = item.clone();
                        }
                    } else {
                        item = new ItemStack(Material.AIR);
                    }
                    break;
                case "helmet":
                    if (player != null) {
                        item = player.getInventory().getHelmet();
                        if (item == null || item.getType() == Material.AIR) {
                            item = new ItemStack(Material.AIR);
                        } else {
                            item = item.clone();
                        }
                    } else {
                        item = new ItemStack(Material.AIR);
                    }
                    break;
                case "chestplate":
                    if (player != null) {
                        item = player.getInventory().getChestplate();
                        if (item == null || item.getType() == Material.AIR) {
                            item = new ItemStack(Material.AIR);
                        } else {
                            item = item.clone();
                        }
                    } else {
                        item = new ItemStack(Material.AIR);
                    }
                    break;
                case "leggings":
                    if (player != null) {
                        item = player.getInventory().getLeggings();
                        if (item == null || item.getType() == Material.AIR) {
                            item = new ItemStack(Material.AIR);
                        } else {
                            item = item.clone();
                        }
                    } else {
                        item = new ItemStack(Material.AIR);
                    }
                    break;
                case "boots":
                    if (player != null) {
                        item = player.getInventory().getBoots();
                        if (item == null || item.getType() == Material.AIR) {
                            item = new ItemStack(Material.AIR);
                        } else {
                            item = item.clone();
                        }
                    } else {
                        item = new ItemStack(Material.AIR);
                    }
                    break;
                case "water_bottle":
                    material = Material.POTION;
                    item = new ItemStack(material);
                    break;
                default:
                    material = Material.matchMaterial(materialName);
                    if (material == null) material = Material.STONE;
                    item = new ItemStack(material);
                    break;
            }
        }
        if (item == null || item.getType() == Material.AIR) {
            return item != null ? item : new ItemStack(Material.AIR);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(item.getType());
        }
        if (itemSection.contains("itemname")) {
            String itemName = itemSection.getString("itemname");
            if (player != null && itemName != null) {
                itemName = replacePlaceholders(player, itemName);
            }
            if (itemName != null) {
                meta.displayName(MiniMessage.miniMessage().deserialize(itemName));
            }
        }
        if (itemSection.contains("custom_model_data")) {
            meta.setCustomModelData(itemSection.getInt("custom_model_data"));
        }
        if (itemSection.contains("lore")) {
            List<Component> lore = new ArrayList<>();
            for (String line : itemSection.getStringList("lore")) {
                if (player != null && line != null) {
                    line = replacePlaceholders(player, line);
                }
                if (line != null) {
                    lore.add(MiniMessage.miniMessage().deserialize(line));
                }
            }
            meta.lore(lore);
        }
        if (itemSection.contains("item_flags")) {
            for (String flag : itemSection.getStringList("item_flags")) {
                try {
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.valueOf(flag));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        if (itemSection.contains("enchantments")) {
            ConfigurationSection enchantmentsSection = itemSection.getConfigurationSection("enchantments");
            if (enchantmentsSection != null) {
                for (String enchantmentKey : enchantmentsSection.getKeys(false)) {
                    Enchantment enchantment = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchantmentKey.toLowerCase()));
                    if (enchantment != null) {
                        int level = enchantmentsSection.getInt(enchantmentKey);
                        meta.addEnchant(enchantment, level, true);
                    }
                }
            }
        }
        if (itemSection.contains("amount")) {
            String amount = itemSection.getString("amount");
            if (amount != null) {
                try {
                    int amountValue = Integer.parseInt(amount);
                    item.setAmount(amountValue);
                } catch (NumberFormatException e) {
                    if (player != null) {
                        amount = replacePlaceholders(player, amount);
                        try {
                            int amountValue = Integer.parseInt(amount);
                            item.setAmount(amountValue);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }
        if (itemSection.contains("color")) {
            String color = itemSection.getString("color");
            if (color != null) {
                if (meta instanceof LeatherArmorMeta leatherArmorMeta) {
                    if (color.startsWith("#")) {
                        leatherArmorMeta.setColor(Color.fromRGB(Integer.parseInt(color.substring(1), 16)));
                    } else {
                        String[] rgb = color.split(",");
                        if (rgb.length == 3) {
                            leatherArmorMeta.setColor(Color.fromRGB(
                                    Integer.parseInt(rgb[0]),
                                    Integer.parseInt(rgb[1]),
                                    Integer.parseInt(rgb[2])
                            ));
                        }
                    }
                } else if (meta instanceof PotionMeta potionMeta) {
                    if (color.startsWith("#")) {
                        potionMeta.setColor(Color.fromRGB(Integer.parseInt(color.substring(1), 16)));
                    } else {
                        String[] rgb = color.split(",");
                        if (rgb.length == 3) {
                            potionMeta.setColor(Color.fromRGB(
                                    Integer.parseInt(rgb[0]),
                                    Integer.parseInt(rgb[1]),
                                    Integer.parseInt(rgb[2])
                            ));
                        }
                    }
                }
            }
        }
        if (itemSection.contains("banner_meta")) {
            if (meta instanceof BannerMeta bannerMeta) {
                List<String> patterns = itemSection.getStringList("banner_meta");
                for (String pattern : patterns) {
                    if (pattern != null) {
                        String[] parts = pattern.split(";");
                        if (parts.length == 2) {
                            try {
                                DyeColor dyeColor = DyeColor.valueOf(parts[0].toUpperCase());
                                PatternType patternType = Bukkit.getRegistry(PatternType.class).get(org.bukkit.NamespacedKey.minecraft(parts[1].toLowerCase()));
                                if (patternType != null) {
                                    bannerMeta.addPattern(new Pattern(dyeColor, patternType));
                                }
                            } catch (IllegalArgumentException ignored) {}
                        }
                    }
                }
            }
        }
        if (material == Material.SHIELD && itemSection.contains("base_color")) {
            if (meta instanceof ShieldMeta shieldMeta) {
                String baseColor = itemSection.getString("base_color");
                if (baseColor != null) {
                    try {
                        DyeColor dyeColor = DyeColor.valueOf(baseColor.toUpperCase());
                        shieldMeta.setBaseColor(dyeColor);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
        if ((material == Material.POTION ||
                material == Material.SPLASH_POTION ||
                material == Material.TIPPED_ARROW ||
                material == Material.LINGERING_POTION) &&
                itemSection.contains("potion_effects")) {
            if (meta instanceof PotionMeta potionMeta) {
                List<String> potionEffects = itemSection.getStringList("potion_effects");
                for (String effect : potionEffects) {
                    if (effect != null) {
                        String[] parts = effect.split(";");
                        if (parts.length == 3) {
                            try {
                                PotionEffectType effectType = PotionEffectType.getByName(parts[0].toUpperCase());
                                if (effectType != null) {
                                    int duration = Integer.parseInt(parts[1]);
                                    int amplifier = Integer.parseInt(parts[2]);
                                    potionMeta.addCustomEffect(new PotionEffect(effectType, duration, amplifier), true);
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
        }
        item.setItemMeta(meta);
        return item;
    }

    private boolean checkViewConditions(List<String> viewConditions, Player player) {
        return ConditionUtil.checkConditions(viewConditions, player);
    }

    public boolean canPlayerOpen(Player player) {
        return ConditionUtil.checkConditions(openConditions, player);
    }

    public Map<String, ClickAction> getClickActions(int slot, Player player) {
        ConfigurationSection itemSection = getHighestPriorityItemForSlot(slot, player);
        if (itemSection == null) {
            return new HashMap<>();
        }
        Map<String, ClickAction> actions = clickActions.getOrDefault(slot, new HashMap<>());
        if (actions.containsKey("any")) {
            Map<String, ClickAction> mergedActions = new HashMap<>();
            ClickAction anyAction = actions.get("any");
            for (Map.Entry<String, ClickAction> entry : actions.entrySet()) {
                if (!entry.getKey().equals("any")) {
                    List<String> mergedConditions = new ArrayList<>(anyAction.conditions());
                    mergedConditions.addAll(entry.getValue().conditions());
                    List<String> mergedActionsList = new ArrayList<>(anyAction.actions());
                    mergedActionsList.addAll(entry.getValue().actions());
                    List<String> mergedDenyActions = new ArrayList<>(anyAction.denyActions());
                    mergedDenyActions.addAll(entry.getValue().denyActions());
                    mergedActions.put(entry.getKey(),
                            new ClickAction(mergedConditions, mergedActionsList, mergedDenyActions));
                }
            }
            mergedActions.put("any", anyAction);
            return mergedActions;
        }
        return actions;
    }

    private ConfigurationSection getHighestPriorityItemForSlot(int slot, Player player) {
        List<ConfigurationSection> itemsForSlot = itemConfigs.get(slot);
        if (itemsForSlot == null || itemsForSlot.isEmpty()) {
            return null;
        }
        itemsForSlot.sort((item1, item2) -> {
            int priority1 = item1.getInt("priority", 0);
            int priority2 = item2.getInt("priority", 0);
            return Integer.compare(priority2, priority1);
        });
        for (ConfigurationSection itemSection : itemsForSlot) {
            if (checkViewConditions(itemSection.getStringList("view_conditions"), player)) {
                return itemSection;
            }
        }
        return null;
    }

    public record ClickAction(List<String> conditions, List<String> actions, List<String> denyActions) {}

    public static String parsePlaceholders(Player player, String text) {
        if (NotionMenus.getInstance().isPAPILoaded()) return PlaceholderAPI.setPlaceholders(player, text);
        return text;
    }

    public static String replacePlaceholders(Player player, String text) {
        if (player == null || text == null) {
            return text;
        }
        java.util.regex.Pattern randomPattern = java.util.regex.Pattern.compile("<random:(-?\\d+),(-?\\d+)>");
        Matcher randomMatcher = randomPattern.matcher(text);
        while (randomMatcher.find()) {
            int min = Integer.parseInt(randomMatcher.group(1));
            int max = Integer.parseInt(randomMatcher.group(2));
            int randomNum = random.nextInt(max - min + 1) + min;
            text = text.replace(randomMatcher.group(0), String.valueOf(randomNum));
        }
        return text.replace("<player>", player.getName())
                .replace("<location>", player.getLocation().toString())
                .replace("<playerX>", String.valueOf(player.getLocation().getX()))
                .replace("<playerY>", String.valueOf(player.getLocation().getY()))
                .replace("<playerZ>", String.valueOf(player.getLocation().getZ()))
                .replace("<player_health>", String.valueOf(Math.round(player.getHealth())))
                .replace("<player_food>", String.valueOf(player.getFoodLevel()));
    }
}