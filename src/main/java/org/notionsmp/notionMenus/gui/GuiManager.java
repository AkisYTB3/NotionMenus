package org.notionsmp.notionMenus.gui;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.notionsmp.notionMenus.NotionMenus;
import org.notionsmp.notionMenus.utils.ActionUtil;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class GuiManager {
    private final Map<String, GuiConfig> guis = new HashMap<>();
    private final Map<String, String> commandToGuiIdMap = new HashMap<>();

    public GuiManager() {
        loadGuis();
    }

    public void registerGuiCommands(GuiConfig guiConfig) {
        for (String command : guiConfig.getCommands()) {
            if (command != null) {
                commandToGuiIdMap.put(command.toLowerCase(), guiConfig.getId());
            }
        }
    }

    public String getGuiIdByCommand(String command) {
        return commandToGuiIdMap.get(command.toLowerCase());
    }

    public void loadGuis() {
        File menusDir = new File(NotionMenus.getInstance().getDataFolder(), "menus");
        if (!menusDir.exists()) {
            boolean dirsCreated = menusDir.mkdirs();
            if (!dirsCreated) {
                NotionMenus.getInstance().getLogger().severe("Failed to create menus directory: " + menusDir.getAbsolutePath());
                return;
            }

            try {
                NotionMenus.getInstance().saveResource("menus/example_menu.yml", false);
            } catch (Exception e) {
                NotionMenus.getInstance().getLogger().warning("Failed to create example menu: " + e.getMessage());
            }
        }

        File[] files = menusDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            if (!config.contains("id")) {
                NotionMenus.getInstance().getLogger().warning("Skipping " + file.getName() + " because it does not contain an id.");
                continue;
            }

            NotionMenus.getInstance().getLogger().info(ANSIComponentSerializer.ansi().serialize(
                    MiniMessage.miniMessage().deserialize("<green>Loaded GUI <dark_green>" + config.getString("id") +
                            "</dark_green> from file <dark_green>" + file.getName() + "</dark_green>.")));

            GuiConfig guiConfig = new GuiConfig(config);
            guis.put(guiConfig.getId(), guiConfig);
            registerGuiCommands(guiConfig);
        }
    }

    public void openGui(String guiId, Player player) {
        GuiConfig guiConfig = guis.get(guiId);
        if (guiConfig == null) {
            player.sendMessage(NotionMenus.NotionString(String.format("<red>GUI not found! (%s)", guiId)));
            return;
        }

        if (!checkConditions(guiConfig.getOpenConditions(), player)) {
            executeActions(guiConfig.getOpenActions(), player, false);
            return;
        }

        String title = GuiConfig.replacePlaceholders(player,
                GuiConfig.parsePlaceholders(player, guiConfig.getTitle()));
        Inventory gui = Bukkit.createInventory(null, guiConfig.getSize(),
                MiniMessage.miniMessage().deserialize(title));

        for (Map.Entry<Integer, List<ConfigurationSection>> entry : guiConfig.getItemConfigs().entrySet()) {
            int slot = entry.getKey();
            ItemStack item = guiConfig.createItemFromConfig(slot, player);
            if (item != null && item.getType() != Material.AIR) {
                gui.setItem(slot, item);
            }
        }
        player.openInventory(gui);
        executeActions(guiConfig.getOpenActions(), player, true);
    }

    private boolean checkConditions(List<String> conditions, Player player) {
        if (conditions == null || conditions.isEmpty()) return true;

        for (String condition : conditions) {
            String[] parts = condition.split("\\[|\\]");
            if (parts.length < 2) continue;

            String conditionType = parts[1];
            String conditionValue = condition.substring(conditionType.length() + 2).trim();

            switch (conditionType) {
                case "permission" -> {
                    if (!player.hasPermission(conditionValue)) return false;
                }
                case "!permission" -> {
                    if (player.hasPermission(conditionValue)) return false;
                }
                case "gamemode" -> {
                    if (!player.getGameMode().name().equalsIgnoreCase(conditionValue)) return false;
                }
                case "!gamemode" -> {
                    if (player.getGameMode().name().equalsIgnoreCase(conditionValue)) return false;
                }
                case "world" -> {
                    if (!player.getWorld().getName().equalsIgnoreCase(conditionValue)) return false;
                }
                case "xp" -> {
                    if (conditionValue.endsWith("l")) {
                        int levels = Integer.parseInt(conditionValue.substring(0, conditionValue.length() - 1));
                        if (player.getLevel() < levels) return false;
                    } else if (conditionValue.endsWith("p")) {
                        int points = Integer.parseInt(conditionValue.substring(0, conditionValue.length() - 1));
                        if (player.getTotalExperience() < points) return false;
                    }
                }
            }
        }
        return true;
    }

    private void executeActions(GuiConfig.ClickAction clickAction, Player player, boolean shouldExecuteActions) {
        if (clickAction == null) return;

        List<String> actions = shouldExecuteActions ? clickAction.actions() : clickAction.denyActions();
        for (String action : actions) {
            ActionUtil.executeAction(action, player);
        }
    }

    public void reloadGuis() {
        guis.clear();
        commandToGuiIdMap.clear();
        loadGuis();
    }
}