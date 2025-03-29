package org.notionsmp.notionMenus.utils;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.notionsmp.notionMenus.NotionMenus;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DeluxeMenusConverter {

    private final FileConfiguration config;

    public DeluxeMenusConverter() {
        this.config = NotionMenus.getInstance().getConfig();
    }

    public void convertIfNeeded() {
        if (config.getBoolean("converted", false)) {
            NotionMenus.getInstance().getLogger().info("DeluxeMenus conversion already completed.");
            return;
        }

        NotionMenus.getInstance().getLogger().info("Starting DeluxeMenus conversion...");

        File pluginsFolder = NotionMenus.getInstance().getDataFolder().getParentFile();
        if (pluginsFolder == null || !pluginsFolder.exists()) {
            NotionMenus.getInstance().getLogger().warning("Plugins directory not found. Skipping conversion.");
            return;
        }

        File dmFolder = new File(pluginsFolder, "DeluxeMenus");
        if (!dmFolder.exists()) {
            NotionMenus.getInstance().getLogger().warning("DeluxeMenus directory not found. Skipping conversion.");
            return;
        }

        File guiMenusFolder = new File(dmFolder, "gui_menus");
        if (!guiMenusFolder.exists()) {
            NotionMenus.getInstance().getLogger().warning("DeluxeMenus gui_menus directory not found. Skipping conversion.");
            return;
        }

        File notionMenusDir = new File(NotionMenus.getInstance().getDataFolder(), "menus");

        File deluxeConfigFile = new File(dmFolder, "config.yml");
        if (!deluxeConfigFile.exists()) {
            NotionMenus.getInstance().getLogger().warning("DeluxeMenus config.yml not found. Skipping conversion.");
            return;
        }

        FileConfiguration deluxeConfig = YamlConfiguration.loadConfiguration(deluxeConfigFile);
        ConfigurationSection guiMenusSection = deluxeConfig.getConfigurationSection("gui_menus");
        if (guiMenusSection == null) {
            NotionMenus.getInstance().getLogger().warning("No GUI menus found in DeluxeMenus config. Skipping conversion.");
            return;
        }

        for (String menuId : guiMenusSection.getKeys(false)) {
            String fileName = guiMenusSection.getString(menuId + ".file");
            if (fileName == null) {
                NotionMenus.getInstance().getLogger().warning("No file specified for menu " + menuId + ". Skipping.");
                continue;
            }

            File deluxeMenuFile = new File(guiMenusFolder, fileName);
            if (!deluxeMenuFile.exists()) {
                NotionMenus.getInstance().getLogger().warning("File " + fileName + " for menu " + menuId + " not found. Skipping.");
                continue;
            }

            FileConfiguration deluxeMenuConfig = YamlConfiguration.loadConfiguration(deluxeMenuFile);
            File notionMenuFile = new File(notionMenusDir, "dm_" + menuId + ".yml");
            FileConfiguration notionMenuConfig = new YamlConfiguration();

            String title = deluxeMenuConfig.getString("menu_title");
            notionMenuConfig.set("id", menuId);
            notionMenuConfig.set("title", title != null ? convertLegacyColors(title) : null);
            notionMenuConfig.set("size", deluxeMenuConfig.getInt("size"));

            if (deluxeMenuConfig.contains("open_command")) {
                List<String> openCommands = deluxeMenuConfig.getStringList("open_command");
                if (!openCommands.isEmpty()) {
                    Object command = openCommands.size() == 1 ?
                            convertLegacyColors(openCommands.get(0)) :
                            convertStringList(openCommands);
                    notionMenuConfig.set("command", command);
                }
            }

            if (deluxeMenuConfig.contains("open_commands")) {
                notionMenuConfig.set("open_actions.actions", convertActions(deluxeMenuConfig.getStringList("open_commands")));
            }

            if (deluxeMenuConfig.contains("open_requirement")) {
                ConfigurationSection openReqSection = deluxeMenuConfig.getConfigurationSection("open_requirement.requirements");
                if (openReqSection != null) {
                    List<String> openConditions = new ArrayList<>();
                    for (String reqKey : openReqSection.getKeys(false)) {
                        String type = openReqSection.getString(reqKey + ".type");
                        if (type != null) {
                            switch (type) {
                                case "has permission" -> openConditions.add("[permission] " + openReqSection.getString(reqKey + ".permission"));
                                case "has money" -> openConditions.add("[money] " + openReqSection.getInt(reqKey + ".amount"));
                                case "has item" -> openConditions.add("[item] " + convertHasItem(openReqSection.getConfigurationSection(reqKey)));
                                case "is near" -> openConditions.add("[near] " + convertIsNear(openReqSection.getConfigurationSection(reqKey)));
                                case "string equals" -> openConditions.add("[equals] " + openReqSection.getString(reqKey + ".input") + "," + openReqSection.getString(reqKey + ".output"));
                                case "string equals ignorecase" -> openConditions.add("[equals] " + openReqSection.getString(reqKey + ".input") + "," + openReqSection.getString(reqKey + ".output") + ",true");
                                case "string contains" -> openConditions.add("[contains] " + openReqSection.getString(reqKey + ".input") + "," + openReqSection.getString(reqKey + ".output"));
                                case "regex matches" -> openConditions.add("[regex] " + openReqSection.getString(reqKey + ".input") + "," + openReqSection.getString(reqKey + ".regex"));
                                case "javascript" -> openConditions.add("[compare] " + openReqSection.getString(reqKey + ".expression"));
                                case "has meta" -> openConditions.add("[meta] " + convertHasMeta(openReqSection.getConfigurationSection(reqKey)));
                                case "has exp" -> openConditions.add("[xp] " + openReqSection.getInt(reqKey + ".amount") + (openReqSection.getBoolean(reqKey + ".level") ? "l" : "p"));
                            }
                        }
                    }
                    notionMenuConfig.set("open_conditions", openConditions);
                }
            }

            ConfigurationSection itemsSection = deluxeMenuConfig.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String itemKey : itemsSection.getKeys(false)) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
                    if (itemSection != null) {
                        convertItem(itemKey, itemSection, notionMenuConfig);
                    }
                }
            }

            try {
                notionMenuConfig.save(notionMenuFile);
                NotionMenus.getInstance().getLogger().info("Converted menu " + menuId + " to " + notionMenuFile.getName());
            } catch (IOException e) {
                NotionMenus.getInstance().getLogger().warning("Failed to save converted menu " + menuId + ": " + e.getMessage());
            }
        }

        config.set("converted", true);
        NotionMenus.getInstance().saveConfig();
        NotionMenus.getInstance().getGuiManager().reloadGuis();
        NotionMenus.getInstance().getLogger().info("DeluxeMenus conversion completed.");
    }

    private void convertItem(String itemKey, ConfigurationSection itemSection, FileConfiguration notionMenuConfig) {
        Map<String, Object> itemMap = new HashMap<>();

        String material = itemSection.getString("material");
        if (material != null) {
            switch (material.toLowerCase()) {
                case "air" -> itemMap.put("material", "empty");
                case "main_hand", "placeholder-main_hand" -> itemMap.put("material", "mainhand");
                case "off_hand", "placeholder-off_hand" -> itemMap.put("material", "offhand");
                default -> {
                    if (material.startsWith("placeholder-armor_")) {
                        itemMap.put("material", material.replace("placeholder-armor_", ""));
                    } else if (material.startsWith("texture-")) {
                        itemMap.put("material", material + " # Not implemented yet");
                    } else {
                        itemMap.put("material", material);
                    }
                }
            }
        }

        String displayName = itemSection.getString("display_name");
        itemMap.put("itemname", displayName != null ? convertLegacyColors(displayName) : "");

        if (itemSection.contains("model_data")) {
            itemMap.put("custom_model_data", itemSection.getInt("model_data"));
        }

        if (itemSection.contains("slot")) {
            itemMap.put("slot", convertSlots(itemSection.get("slot")));
        } else if (itemSection.contains("slots")) {
            itemMap.put("slot", convertSlots(itemSection.get("slots")));
        }

        if (itemSection.contains("lore")) {
            List<String> lore = itemSection.getStringList("lore");
            itemMap.put("lore", convertStringList(lore));
        }

        if (itemSection.contains("item_flags")) {
            itemMap.put("item_flags", itemSection.getStringList("item_flags"));
        }

        if (itemSection.contains("enchantments")) {
            List<String> enchantments = new ArrayList<>();
            for (String enchantment : itemSection.getStringList("enchantments")) {
                enchantments.add(enchantment.replace(";", ":"));
            }
            itemMap.put("enchantments", enchantments);
        }

        if (itemSection.contains("banner_meta")) {
            List<String> bannerMeta = itemSection.getStringList("banner_meta");
            itemMap.put("banner_meta", convertStringList(bannerMeta));
        }

        if (itemSection.contains("potion_effects")) {
            List<String> potionEffects = itemSection.getStringList("potion_effects");
            itemMap.put("potion_effects", convertStringList(potionEffects));
        }

        if (itemSection.contains("rgb")) {
            String rgb = itemSection.getString("rgb");
            if (rgb != null) {
                itemMap.put("color", rgb.replace(" ", ""));
            }
        }

        if (itemSection.contains("view_requirement")) {
            ConfigurationSection viewReqSection = itemSection.getConfigurationSection("view_requirement.requirements");
            if (viewReqSection != null) {
                List<String> viewConditions = new ArrayList<>();
                for (String reqKey : viewReqSection.getKeys(false)) {
                    String type = viewReqSection.getString(reqKey + ".type");
                    if (type != null) {
                        switch (type) {
                            case "has permission" -> viewConditions.add("[permission] " + viewReqSection.getString(reqKey + ".permission"));
                            case "has money" -> viewConditions.add("[money] " + viewReqSection.getInt(reqKey + ".amount"));
                            case "has item" -> viewConditions.add("[item] " + convertHasItem(viewReqSection.getConfigurationSection(reqKey)));
                            case "is near" -> viewConditions.add("[near] " + convertIsNear(viewReqSection.getConfigurationSection(reqKey)));
                            case "string equals" -> viewConditions.add("[equals] " + viewReqSection.getString(reqKey + ".input") + "," + viewReqSection.getString(reqKey + ".output"));
                            case "string equals ignorecase" -> viewConditions.add("[equals] " + viewReqSection.getString(reqKey + ".input") + "," + viewReqSection.getString(reqKey + ".output") + ",true");
                            case "string contains" -> viewConditions.add("[contains] " + viewReqSection.getString(reqKey + ".input") + "," + viewReqSection.getString(reqKey + ".output"));
                            case "regex matches" -> viewConditions.add("[regex] " + viewReqSection.getString(reqKey + ".input") + "," + viewReqSection.getString(reqKey + ".regex"));
                            case "javascript" -> viewConditions.add("[compare] " + viewReqSection.getString(reqKey + ".expression"));
                            case "has meta" -> viewConditions.add("[meta] " + convertHasMeta(viewReqSection.getConfigurationSection(reqKey)));
                            case "has exp" -> viewConditions.add("[xp] " + viewReqSection.getInt(reqKey + ".amount") + (viewReqSection.getBoolean(reqKey + ".level") ? "l" : "p"));
                        }
                    }
                }
                itemMap.put("view_conditions", viewConditions);
            }
        }

        if (itemSection.contains("left_click_commands")) {
            Map<String, Object> leftClickActions = new HashMap<>();
            leftClickActions.put("actions", convertActions(itemSection.getStringList("left_click_commands")));

            if (itemSection.contains("left_click_requirement")) {
                ConfigurationSection leftReqSection = itemSection.getConfigurationSection("left_click_requirement.requirements");
                if (leftReqSection != null) {
                    List<String> leftConditions = new ArrayList<>();
                    for (String reqKey : leftReqSection.getKeys(false)) {
                        String type = leftReqSection.getString(reqKey + ".type");
                        if (type != null) {
                            switch (type) {
                                case "has permission" -> leftConditions.add("[permission] " + leftReqSection.getString(reqKey + ".permission"));
                                case "has money" -> leftConditions.add("[money] " + leftReqSection.getInt(reqKey + ".amount"));
                                case "has item" -> leftConditions.add("[item] " + convertHasItem(leftReqSection.getConfigurationSection(reqKey)));
                                case "is near" -> leftConditions.add("[near] " + convertIsNear(leftReqSection.getConfigurationSection(reqKey)));
                                case "string equals" -> leftConditions.add("[equals] " + leftReqSection.getString(reqKey + ".input") + "," + leftReqSection.getString(reqKey + ".output"));
                                case "string equals ignorecase" -> leftConditions.add("[equals] " + leftReqSection.getString(reqKey + ".input") + "," + leftReqSection.getString(reqKey + ".output") + ",true");
                                case "string contains" -> leftConditions.add("[contains] " + leftReqSection.getString(reqKey + ".input") + "," + leftReqSection.getString(reqKey + ".output"));
                                case "regex matches" -> leftConditions.add("[regex] " + leftReqSection.getString(reqKey + ".input") + "," + leftReqSection.getString(reqKey + ".regex"));
                                case "javascript" -> leftConditions.add("[compare] " + leftReqSection.getString(reqKey + ".expression"));
                                case "has meta" -> leftConditions.add("[meta] " + convertHasMeta(leftReqSection.getConfigurationSection(reqKey)));
                                case "has exp" -> leftConditions.add("[xp] " + leftReqSection.getInt(reqKey + ".amount") + (leftReqSection.getBoolean(reqKey + ".level") ? "l" : "p"));
                            }
                        }
                    }
                    leftClickActions.put("conditions", leftConditions);
                }
            }

            itemMap.put("click_actions.left", leftClickActions);
        }

        if (itemSection.contains("right_click_commands")) {
            Map<String, Object> rightClickActions = new HashMap<>();
            rightClickActions.put("actions", convertActions(itemSection.getStringList("right_click_commands")));

            if (itemSection.contains("right_click_requirement")) {
                ConfigurationSection rightReqSection = itemSection.getConfigurationSection("right_click_requirement.requirements");
                if (rightReqSection != null) {
                    List<String> rightConditions = new ArrayList<>();
                    for (String reqKey : rightReqSection.getKeys(false)) {
                        String type = rightReqSection.getString(reqKey + ".type");
                        if (type != null) {
                            switch (type) {
                                case "has permission" -> rightConditions.add("[permission] " + rightReqSection.getString(reqKey + ".permission"));
                                case "has money" -> rightConditions.add("[money] " + rightReqSection.getInt(reqKey + ".amount"));
                                case "has item" -> rightConditions.add("[item] " + convertHasItem(rightReqSection.getConfigurationSection(reqKey)));
                                case "is near" -> rightConditions.add("[near] " + convertIsNear(rightReqSection.getConfigurationSection(reqKey)));
                                case "string equals" -> rightConditions.add("[equals] " + rightReqSection.getString(reqKey + ".input") + "," + rightReqSection.getString(reqKey + ".output"));
                                case "string equals ignorecase" -> rightConditions.add("[equals] " + rightReqSection.getString(reqKey + ".input") + "," + rightReqSection.getString(reqKey + ".output") + ",true");
                                case "string contains" -> rightConditions.add("[contains] " + rightReqSection.getString(reqKey + ".input") + "," + rightReqSection.getString(reqKey + ".output"));
                                case "regex matches" -> rightConditions.add("[regex] " + rightReqSection.getString(reqKey + ".input") + "," + rightReqSection.getString(reqKey + ".regex"));
                                case "javascript" -> rightConditions.add("[compare] " + rightReqSection.getString(reqKey + ".expression"));
                                case "has meta" -> rightConditions.add("[meta] " + convertHasMeta(rightReqSection.getConfigurationSection(reqKey)));
                                case "has exp" -> rightConditions.add("[xp] " + rightReqSection.getInt(reqKey + ".amount") + (rightReqSection.getBoolean(reqKey + ".level") ? "l" : "p"));
                            }
                        }
                    }
                    rightClickActions.put("conditions", rightConditions);
                }
            }

            itemMap.put("click_actions.right", rightClickActions);
        }

        notionMenuConfig.set("items." + itemKey, itemMap);
    }

    private List<String> convertActions(List<String> actions) {
        List<String> convertedActions = new ArrayList<>();
        for (String action : actions) {
            String convertedAction = action;
            if (action.startsWith("[")) {
                String actionType = action.substring(1, action.indexOf(']'));
                switch (actionType) {
                    case "commandevent" -> convertedAction = action.replace("[commandevent]", "[player]");
                    case "minimessage" -> convertedAction = action.replace("[minimessage]", "[message]");
                    case "minibroadcast" -> convertedAction = action.replace("[minibroadcast]", "[broadcast]");
                    case "openguimenu" -> convertedAction = action.replace("[openguimenu]", "[opengui]");
                    case "connect" -> convertedAction = action.replace("[connect]", "[server]");
                    case "broadcastsound" -> convertedAction = action.replace("[broadcastsound]", "[soundall]") + " true";
                    case "broadcastsoundworld" -> convertedAction = action.replace("[broadcastsoundworld]", "[soundall]") + " false";
                    case "givepermission" -> convertedAction = action.replace("[givepermission]", "[giveperm]");
                    case "takepermission" -> convertedAction = action.replace("[takepermission]", "[takeperm]");
                    case "giveexp" -> convertedAction = action.replace("[giveexp]", "[givexp]").replace("L", "l");
                    case "takeexp" -> convertedAction = action.replace("[takeexp]", "[takexp]").replace("L", "l");
                    case "sound" -> {
                        String sound = action.substring(7).trim();
                        String convertedSound = sound.toLowerCase().replace("_", ".");
                        convertedAction = "[sound] " + convertedSound + "# Note: Some sounds like smithing_table may need manual correction";
                    }
                }
            }

            if (convertedAction.contains(" dm open ")) {
                convertedAction = convertedAction.replace(" dm open ", " nm gui ");
            }

            convertedAction = convertLegacyColors(convertedAction);

            convertedActions.add(convertedAction);
        }
        return convertedActions;
    }

    private List<String> convertStringList(List<String> list) {
        List<String> converted = new ArrayList<>();
        for (String str : list) {
            converted.add(convertLegacyColors(str));
        }
        return converted;
    }

    private String convertLegacyColors(String text) {
        if (text == null) {
            return null;
        }

        String converted = text.replaceAll("(?i)&0", "<black>")
                .replaceAll("(?i)&1", "<dark_blue>")
                .replaceAll("(?i)&2", "<dark_green>")
                .replaceAll("(?i)&3", "<dark_aqua>")
                .replaceAll("(?i)&4", "<dark_red>")
                .replaceAll("(?i)&5", "<dark_purple>")
                .replaceAll("(?i)&6", "<gold>")
                .replaceAll("(?i)&7", "<gray>")
                .replaceAll("(?i)&8", "<dark_gray>")
                .replaceAll("(?i)&9", "<blue>")
                .replaceAll("(?i)&a", "<green>")
                .replaceAll("(?i)&b", "<aqua>")
                .replaceAll("(?i)&c", "<red>")
                .replaceAll("(?i)&d", "<light_purple>")
                .replaceAll("(?i)&e", "<yellow>")
                .replaceAll("(?i)&f", "<white>");

        converted = converted.replaceAll("(?i)&k", "<obf>")
                .replaceAll("(?i)&l", "<b>")
                .replaceAll("(?i)&m", "<st>")
                .replaceAll("(?i)&n", "<u>")
                .replaceAll("(?i)&o", "<i>")
                .replaceAll("(?i)&r", "<r>");

        converted = converted.replaceAll("(?i)&#([0-9a-f]{6})", "<#$1>");

        return converted;
    }

    private Object convertSlots(Object slots) {
        if (slots instanceof Integer) {
            return slots;
        } else if (slots instanceof List) {
            List<String> convertedSlots = new ArrayList<>();
            for (Object slot : (List<?>) slots) {
                if (slot instanceof String slotStr) {
                    convertedSlots.add(slotStr.contains("-") ?
                            slotStr.replace("-", "..") : slotStr);
                } else if (slot instanceof Integer) {
                    convertedSlots.add(slot.toString());
                }
            }
            return convertedSlots;
        } else if (slots instanceof String slotStr) {
            return slotStr.contains("-") ? slotStr.replace("-", "..") : slotStr;
        }
        return null;
    }

    private String convertHasItem(ConfigurationSection hasItemSection) {
        StringBuilder builder = new StringBuilder();
        builder.append("material:").append(hasItemSection.getString("material"));
        if (hasItemSection.contains("amount")) {
            builder.append(",amount:").append(hasItemSection.getInt("amount"));
        }
        if (hasItemSection.contains("name")) {
            builder.append(",itemname:").append(convertLegacyColors(hasItemSection.getString("name")));
        }
        if (hasItemSection.contains("lore")) {
            builder.append(",lore:").append(String.join("\\n", convertStringList(hasItemSection.getStringList("lore"))));
        }
        if (hasItemSection.getBoolean("armor", false)) {
            builder.append(",armor:true");
        }
        if (hasItemSection.getBoolean("offhand", false)) {
            builder.append(",offhand:true");
        }
        return builder.toString();
    }

    private String convertIsNear(ConfigurationSection isNearSection) {
        return isNearSection.getString("location") + "," + isNearSection.getDouble("distance");
    }

    private String convertHasMeta(ConfigurationSection hasMetaSection) {
        return hasMetaSection.getString("key") + "," +
                hasMetaSection.getString("meta_type") + "," +
                hasMetaSection.getString("value");
    }
}