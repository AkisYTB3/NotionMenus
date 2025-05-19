package org.notionsmp.notionMenus.gui;

import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.notionsmp.notionMenus.NotionMenus;
import org.notionsmp.notionMenus.utils.ActionUtil;
import org.notionsmp.notionMenus.utils.ConditionUtil;

import java.io.File;
import java.util.*;

@Getter
public class GuiManager {
    private final Map<String, GuiConfig> guis = new HashMap<>();
    private final Map<String, String> commandToGuiIdMap = new HashMap<>();
    private final Map<Player, Map<String, BukkitTask>> refreshTasks = new HashMap<>();

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
        loadGuis(false);
    }

    public void loadGuis(boolean quiet) {
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
                if (!quiet) {
                    NotionMenus.getInstance().getLogger().warning("Skipping " + file.getName() + " because it does not contain an id.");
                }
                continue;
            }
            if (!quiet) {
                NotionMenus.getInstance().getLogger().info(ANSIComponentSerializer.ansi().serialize(
                        MiniMessage.miniMessage().deserialize("<green>Loaded GUI <dark_green>" + config.getString("id") +
                                "</dark_green> from file <dark_green>" + file.getName() + "</dark_green>.")));
            }
            GuiConfig guiConfig = new GuiConfig(config);
            guis.put(guiConfig.getId(), guiConfig);
            registerGuiCommands(guiConfig);
        }
    }

    public void openGui(String guiId, Player player) {
        openGui(guiId, player, new ArrayList<>());
    }

    public void openGui(String guiId, Player player, List<String> args) {
        cancelRefreshTask(player, guiId);

        GuiConfig guiConfig = guis.get(guiId);
        if (guiConfig == null) {
            player.sendMessage(NotionMenus.NotionString(String.format("<red>GUI not found! (%s)", guiId)));
            return;
        }
        if (!guiConfig.canPlayerOpen(player)) {
            return;
        }

        Map<String, String> parsedArgs = new LinkedHashMap<>();
        List<String> argNames = new ArrayList<>(guiConfig.getArgs().keySet());
        for (int i = 0; i < argNames.size(); i++) {
            String argName = argNames.get(i);
            String defaultValue = guiConfig.getArgs().get(argName);
            if (i < args.size()) {
                parsedArgs.put(argName, GuiConfig.replacePlaceholders(player, args.get(i), parsedArgs));
            } else if (defaultValue != null) {
                parsedArgs.put(argName, GuiConfig.replacePlaceholders(player, defaultValue, parsedArgs));
            } else {
                player.sendMessage(NotionMenus.NotionString("<red>Missing required argument: " + argName));
                return;
            }
        }

        String title = GuiConfig.replacePlaceholders(player,
                GuiConfig.parsePlaceholders(player, guiConfig.getTitle()), parsedArgs);
        Inventory gui = Bukkit.createInventory(new CustomInventoryHolder(guiId, parsedArgs),
                guiConfig.getSize(), MiniMessage.miniMessage().deserialize(title));
        updateGuiItems(gui, guiConfig, player, parsedArgs);
        player.openInventory(gui);

        if (ConditionUtil.checkConditions(guiConfig.getOpenActions().conditions(), player)) {
            executeActions(guiConfig.getOpenActions(), player, true);
        } else {
            executeActions(guiConfig.getOpenActions(), player, false);
        }

        if (guiConfig.getRefreshRate() > 0) {
            startAutoRefresh(player, guiId, guiConfig);
        }
    }

    private void updateGuiItems(Inventory gui, GuiConfig guiConfig, Player player, Map<String, String> args) {
        for (int slot = 0; slot < gui.getSize(); slot++) {
            if (gui.getItem(slot) == null || guiConfig.shouldUpdateOnRefresh(slot)) {
                ItemStack item = guiConfig.createItemFromConfig(slot, player, args);
                gui.setItem(slot, item != null ? item : new ItemStack(Material.AIR));
            }
        }
    }

    private void startAutoRefresh(Player player, String guiId, GuiConfig guiConfig) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(NotionMenus.getInstance(), () -> {
            InventoryView view = player.getOpenInventory();
            if (!player.isOnline() || view == null) {
                cancelRefreshTask(player, guiId);
                return;
            }

            Inventory topInventory = view.getTopInventory();
            if (topInventory == null || !(topInventory.getHolder() instanceof CustomInventoryHolder holder)) {
                cancelRefreshTask(player, guiId);
                return;
            }

            if (holder.getGuiId().equals(guiId)) {
                refreshGui(player, guiConfig, false);
            } else {
                cancelRefreshTask(player, guiId);
            }
        }, guiConfig.getRefreshRate(), guiConfig.getRefreshRate());

        refreshTasks.computeIfAbsent(player, k -> new HashMap<>()).put(guiId, task);
    }

    private void cancelRefreshTask(Player player, String guiId) {
        if (refreshTasks.containsKey(player)) {
            BukkitTask task = refreshTasks.get(player).remove(guiId);
            if (task != null) {
                task.cancel();
            }
            if (refreshTasks.get(player).isEmpty()) {
                refreshTasks.remove(player);
            }
        }
    }

    public void refreshPlayerGui(Player player) {
        InventoryView view = player.getOpenInventory();
        if (view.getTopInventory().getHolder() instanceof CustomInventoryHolder holder) {
            GuiConfig guiConfig = guis.get(holder.getGuiId());
            if (guiConfig != null) {
                refreshGui(player, guiConfig, true);
            }
        }
    }

    private void refreshGui(Player player, GuiConfig guiConfig, boolean manualRefresh) {
        InventoryView openInventory = player.getOpenInventory();
        Inventory topInventory = openInventory.getTopInventory();

        for (int slot = 0; slot < topInventory.getSize(); slot++) {
            if (manualRefresh || guiConfig.shouldUpdateOnRefresh(slot)) {
                ItemStack item = guiConfig.createItemFromConfig(slot, player,
                        ((CustomInventoryHolder) topInventory.getHolder()).getArgs());
                topInventory.setItem(slot, item != null ? item : new ItemStack(Material.AIR));
            }
        }

        if (manualRefresh) {
            player.updateInventory();
        }
    }

    public void cleanupPlayer(Player player) {
        if (refreshTasks.containsKey(player)) {
            refreshTasks.get(player).values().forEach(BukkitTask::cancel);
            refreshTasks.remove(player);
        }
    }

    private void executeActions(GuiConfig.ClickAction clickAction, Player player, boolean shouldExecuteActions) {
        if (clickAction == null) return;
        List<String> actions = shouldExecuteActions ? clickAction.actions() : clickAction.denyActions();
        for (String action : actions) {
            ActionUtil.executeAction(action, player);
        }
    }

    public void reloadGuis() {
        reloadGuis(false);
    }

    public void reloadGuis(boolean quiet) {
        guis.clear();
        commandToGuiIdMap.clear();
        loadGuis(quiet);
    }
}