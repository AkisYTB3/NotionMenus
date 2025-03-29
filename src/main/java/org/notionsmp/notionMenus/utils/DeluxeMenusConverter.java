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

            convertMenuFile(menuId, deluxeMenuFile, notionMenusDir);
        }

        config.set("converted", true);
        NotionMenus.getInstance().saveConfig();
        NotionMenus.getInstance().getGuiManager().reloadGuis();
        NotionMenus.getInstance().getLogger().info("DeluxeMenus conversion completed.");
    }

    private void convertMenuFile(String menuId, File deluxeMenuFile, File notionMenusDir) {
        FileConfiguration deluxeMenuConfig = YamlConfiguration.loadConfiguration(deluxeMenuFile);
        File notionMenuFile = new File(notionMenusDir, "dm_" + menuId + ".yml");
        FileConfiguration notionMenuConfig = new YamlConfiguration();

        notionMenuConfig.set("id", menuId);
        notionMenuConfig.set("title", convertLegacyColors(deluxeMenuConfig.getString("menu_title")));
        notionMenuConfig.set("size", deluxeMenuConfig.getInt("size"));

        convertOpenCommands(deluxeMenuConfig, notionMenuConfig);
        convertOpenRequirements(deluxeMenuConfig, notionMenuConfig);
        convertItems(deluxeMenuConfig, notionMenuConfig);

        try {
            notionMenuConfig.save(notionMenuFile);
            NotionMenus.getInstance().getLogger().info("Converted menu " + menuId + " to " + notionMenuFile.getName());
        } catch (IOException e) {
            NotionMenus.getInstance().getLogger().warning("Failed to save converted menu " + menuId + ": " + e.getMessage());
        }
    }

    private void convertOpenCommands(FileConfiguration deluxeMenuConfig, FileConfiguration notionMenuConfig) {
        if (deluxeMenuConfig.contains("open_command")) {
            List<String> openCommands = deluxeMenuConfig.getStringList("open_command");
            if (!openCommands.isEmpty()) {
                Object command = openCommands.size() == 1 ?
                        convertLegacyColors(openCommands.getFirst()) :
                        convertStringList(openCommands);
                notionMenuConfig.set("command", command);
            }
        }

        if (deluxeMenuConfig.contains("open_commands")) {
            notionMenuConfig.set("open_actions.actions", convertActions(deluxeMenuConfig.getStringList("open_commands")));
        }
    }

    private void convertOpenRequirements(FileConfiguration deluxeMenuConfig, FileConfiguration notionMenuConfig) {
        if (deluxeMenuConfig.contains("open_requirement")) {
            ConfigurationSection openReqSection = deluxeMenuConfig.getConfigurationSection("open_requirement.requirements");
            if (openReqSection != null) {
                List<String> openConditions = new ArrayList<>();
                for (String reqKey : openReqSection.getKeys(false)) {
                    convertRequirement(openReqSection, reqKey, openConditions);
                }
                notionMenuConfig.set("open_conditions", openConditions);
            }
        }
    }

    private void convertItems(FileConfiguration deluxeMenuConfig, FileConfiguration notionMenuConfig) {
        ConfigurationSection itemsSection = deluxeMenuConfig.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String itemKey : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
                if (itemSection != null) {
                    convertItem(itemKey, itemSection, notionMenuConfig);
                }
            }
        }
    }

    private void convertItem(String itemKey, ConfigurationSection itemSection, FileConfiguration notionMenuConfig) {
        Map<String, Object> itemMap = new HashMap<>();
        convertItemProperties(itemSection, itemMap);
        convertItemRequirements(itemSection, itemMap);
        notionMenuConfig.set("items." + itemKey, itemMap);
    }

    private void convertItemProperties(ConfigurationSection itemSection, Map<String, Object> itemMap) {
        String material = itemSection.getString("material");
        if (material != null) {
            itemMap.put("material", convertMaterial(material));
        }

        itemMap.put("itemname", convertLegacyColors(itemSection.getString("display_name")));

        if (itemSection.contains("model_data")) {
            itemMap.put("custom_model_data", itemSection.getInt("model_data"));
        }

        if (itemSection.contains("slot") || itemSection.contains("slots")) {
            itemMap.put("slot", convertSlots(itemSection.contains("slot") ?
                    itemSection.get("slot") :
                    itemSection.get("slots")));
        }

        if (itemSection.contains("lore")) {
            itemMap.put("lore", convertStringList(itemSection.getStringList("lore")));
        }

        if (itemSection.contains("item_flags")) {
            itemMap.put("item_flags", itemSection.getStringList("item_flags"));
        }

        if (itemSection.contains("enchantments")) {
            itemMap.put("enchantments", convertEnchantments(itemSection.getStringList("enchantments")));
        }

        if (itemSection.contains("banner_meta")) {
            itemMap.put("banner_meta", convertStringList(itemSection.getStringList("banner_meta")));
        }

        if (itemSection.contains("potion_effects")) {
            itemMap.put("potion_effects", convertStringList(itemSection.getStringList("potion_effects")));
        }

        if (itemSection.contains("rgb")) {
            String rgb = itemSection.getString("rgb");
            if (rgb != null) {
                itemMap.put("color", rgb.replace(" ", ""));
            }
        }
    }

    private void convertItemRequirements(ConfigurationSection itemSection, Map<String, Object> itemMap) {
        convertViewRequirements(itemSection, itemMap);
        convertClickRequirements(itemSection, itemMap, "left");
        convertClickRequirements(itemSection, itemMap, "right");
    }

    private void convertViewRequirements(ConfigurationSection itemSection, Map<String, Object> itemMap) {
        if (itemSection.contains("view_requirement")) {
            ConfigurationSection viewReqSection = itemSection.getConfigurationSection("view_requirement.requirements");
            if (viewReqSection != null) {
                List<String> viewConditions = new ArrayList<>();
                for (String reqKey : viewReqSection.getKeys(false)) {
                    convertRequirement(viewReqSection, reqKey, viewConditions);
                }
                itemMap.put("view_conditions", viewConditions);
            }
        }
    }

    private void convertClickRequirements(ConfigurationSection itemSection, Map<String, Object> itemMap, String clickType) {
        String commandsKey = clickType + "_click_commands";
        String requirementKey = clickType + "_click_requirement";

        if (itemSection.contains(commandsKey)) {
            Map<String, Object> clickActions = new HashMap<>();
            clickActions.put("actions", convertActions(itemSection.getStringList(commandsKey)));

            if (itemSection.contains(requirementKey)) {
                ConfigurationSection reqSection = itemSection.getConfigurationSection(requirementKey + ".requirements");
                if (reqSection != null) {
                    List<String> conditions = new ArrayList<>();
                    for (String reqKey : reqSection.getKeys(false)) {
                        convertRequirement(reqSection, reqKey, conditions);
                    }
                    clickActions.put("conditions", conditions);
                }
            }

            itemMap.put("click_actions." + clickType, clickActions);
        }
    }

    private void convertRequirement(ConfigurationSection reqSection, String reqKey, List<String> conditions) {
        ConfigurationSection requirement = reqSection.getConfigurationSection(reqKey);
        if (requirement == null) return;

        String type = requirement.getString("type");
        if (type == null) return;

        switch (type) {
            case "has permission" -> conditions.add("[permission] " + requirement.getString("permission"));
            case "has money" -> conditions.add("[money] " + requirement.getInt("amount"));
            case "has item" -> {
                ConfigurationSection itemSection = requirement.getConfigurationSection("item");
                if (itemSection != null) {
                    conditions.add("[item] " + convertHasItem(itemSection));
                }
            }
            case "is near" -> {
                ConfigurationSection nearSection = requirement.getConfigurationSection("location");
                if (nearSection != null) {
                    conditions.add("[near] " + convertIsNear(nearSection));
                }
            }
            case "string equals" -> conditions.add("[equals] " + requirement.getString("input") + "," + requirement.getString("output"));
            case "string equals ignorecase" -> conditions.add("[equals] " + requirement.getString("input") + "," + requirement.getString("output") + ",true");
            case "string contains" -> conditions.add("[contains] " + requirement.getString("input") + "," + requirement.getString("output"));
            case "regex matches" -> conditions.add("[regex] " + requirement.getString("input") + "," + requirement.getString("regex"));
            case "javascript" -> conditions.add("[compare] " + requirement.getString("expression"));
            case "has meta" -> {
                ConfigurationSection metaSection = requirement.getConfigurationSection("meta");
                if (metaSection != null) {
                    conditions.add("[meta] " + convertHasMeta(metaSection));
                }
            }
            case "has exp" -> conditions.add("[xp] " + requirement.getInt("amount") + (requirement.getBoolean("level") ? "l" : "p"));
        }
    }

    private String convertMaterial(String material) {
        return switch (material.toLowerCase()) {
            case "air" -> "empty";
            case "main_hand", "placeholder-main_hand" -> "mainhand";
            case "off_hand", "placeholder-off_hand" -> "offhand";
            default -> material.startsWith("placeholder-armor_") ?
                    material.replace("placeholder-armor_", "") :
                    material.startsWith("texture-") ?
                            material + " # Not implemented yet" :
                            material;
        };
    }

    private List<String> convertEnchantments(List<String> enchantments) {
        List<String> converted = new ArrayList<>();
        for (String enchantment : enchantments) {
            converted.add(enchantment.replace(";", ":"));
        }
        return converted;
    }

    private List<String> convertActions(List<String> actions) {
        List<String> convertedActions = new ArrayList<>();
        for (String action : actions) {
            convertedActions.add(convertAction(action));
        }
        return convertedActions;
    }

    private String convertAction(String action) {
        if (!action.startsWith("[")) return convertLegacyColors(action);

        String converted = action;
        String actionType = action.substring(1, action.indexOf(']'));

        converted = switch (actionType) {
            case "commandevent" -> action.replace("[commandevent]", "[player]");
            case "minimessage" -> action.replace("[minimessage]", "[message]");
            case "minibroadcast" -> action.replace("[minibroadcast]", "[broadcast]");
            case "openguimenu" -> action.replace("[openguimenu]", "[opengui]");
            case "connect" -> action.replace("[connect]", "[server]");
            case "broadcastsound" -> action.replace("[broadcastsound]", "[soundall]") + " true";
            case "broadcastsoundworld" -> action.replace("[broadcastsoundworld]", "[soundall]") + " false";
            case "givepermission" -> action.replace("[givepermission]", "[giveperm]");
            case "takepermission" -> action.replace("[takepermission]", "[takeperm]");
            case "giveexp" -> action.replace("[giveexp]", "[givexp]").replace("L", "l");
            case "takeexp" -> action.replace("[takeexp]", "[takexp]").replace("L", "l");
            case "sound" -> convertSoundAction(action);
            default -> action;
        };

        if (converted.contains(" dm open ")) {
            converted = converted.replace(" dm open ", " nm gui ");
        }

        return convertLegacyColors(converted);
    }

    private String convertSoundAction(String action) {
        String sound = action.substring(7).trim();
        return "[sound] " + sound.toLowerCase().replace("_", ".") +
                " # Note: Some sounds like smithing_table may need manual correction";
    }

    private List<String> convertStringList(List<String> list) {
        List<String> converted = new ArrayList<>();
        for (String str : list) {
            converted.add(convertLegacyColors(str));
        }
        return converted;
    }

    private String convertLegacyColors(String text) {
        if (text == null) return null;

        return text.replaceAll("(?i)&([0-9a-fk-or])", "<$1>")
                .replaceAll("(?i)&#([0-9a-f]{6})", "<#$1>");
    }

    private Object convertSlots(Object slots) {
        if (slots instanceof Integer) return slots;
        if (slots instanceof String slotStr) return slotStr.replace("-", "..");
        if (slots instanceof List) {
            List<String> converted = new ArrayList<>();
            for (Object slot : (List<?>) slots) {
                if (slot instanceof String s) converted.add(s.replace("-", ".."));
                else if (slot instanceof Integer i) converted.add(i.toString());
            }
            return converted;
        }
        return null;
    }

    private String convertHasItem(ConfigurationSection hasItemSection) {
        StringBuilder builder = new StringBuilder();
        builder.append("material:").append(hasItemSection.getString("material"));
        if (hasItemSection.contains("amount")) builder.append(",amount:").append(hasItemSection.getInt("amount"));
        if (hasItemSection.contains("name")) builder.append(",itemname:").append(convertLegacyColors(hasItemSection.getString("name")));
        if (hasItemSection.contains("lore")) builder.append(",lore:").append(String.join("\\n", convertStringList(hasItemSection.getStringList("lore"))));
        if (hasItemSection.getBoolean("armor", false)) builder.append(",armor:true");
        if (hasItemSection.getBoolean("offhand", false)) builder.append(",offhand:true");
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