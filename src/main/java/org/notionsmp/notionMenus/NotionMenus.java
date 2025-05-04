package org.notionsmp.notionMenus;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.notionsmp.notionMenus.commands.GUICompletions;
import org.notionsmp.notionMenus.commands.NotionMenusCommand;
import org.notionsmp.notionMenus.gui.GuiManager;
import org.notionsmp.notionMenus.listeners.GuiClickListener;
import org.notionsmp.notionMenus.listeners.GuiCommandListener;
import org.notionsmp.notionMenus.listeners.hooks.NexoHookListener;
import org.notionsmp.notionMenus.utils.DeluxeMenusConverter;
import org.notionsmp.notionMenus.utils.Metrics;

@Getter
public class NotionMenus extends JavaPlugin {

    @Getter
    private static NotionMenus instance;
    private GuiManager guiManager;
    private PaperCommandManager commandManager;
    private boolean isPAPILoaded;
    private Economy economy;
    private Permission permissions;
    private boolean isVaultEnabled;
    private boolean isVaultUnlocked = false;

    private String prefix;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        prefix = getConfig().getString("prefix", "<gradient:#663399:#7069ff>Notion</gradient> <dark_gray>»</dark_gray> ");

        guiManager = new GuiManager();

        DeluxeMenusConverter converter = new DeluxeMenusConverter();
        converter.convertIfNeeded();

        this.isPAPILoaded = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;

        registerListeners();
        registerCommands();
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        checkForVaultWithRetries(0);
        initMetrics();
    }

    private void initMetrics() {
        Metrics metrics = new Metrics(this, 25732);
    }

    public static Component NotionString(String... strings) {
        StringBuilder messageBuilder = new StringBuilder();
        for (String str : strings) {
            messageBuilder.append(str);
        }
        return MiniMessage.miniMessage().deserialize(instance.prefix + messageBuilder);
    }

    private void checkForVaultWithRetries(int attempt) {
        final int maxAttempts = 3;
        final long delayBetweenAttempts = 20L;

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
                if (attempt < maxAttempts - 1) {
                    getLogger().info("Vault not found, retrying (attempt " + (attempt + 1) + ")...");
                    checkForVaultWithRetries(attempt + 1);
                } else {
                    getLogger().warning("No Economy plugin found, some features will be disabled");
                }
                return;
            }

            try {
                logAvailableServices();
            } catch (NoClassDefFoundError e) {
                getLogger().warning("Vault found but economy classes not available - economy features disabled");
                return;
            }

            this.isVaultEnabled = setupEconomy();

            if (this.isVaultEnabled) {
                setupPermissions();
                getLogger().info("Successfully hooked into " + (isVaultUnlocked ? "VaultUnlocked" : "Vault") +
                        " economy via " + economy.getName());
            } else if (attempt < maxAttempts - 1) {
                getLogger().info("Retrying Vault connection (attempt " + (attempt + 1) + ")...");
                checkForVaultWithRetries(attempt + 1);
            } else {
                getLogger().warning("No Economy plugin found, some features will be disabled");
            }
        }, delayBetweenAttempts);
    }

    private boolean setupEconomy() {
        try {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
                if (economy != null) {
                    isVaultUnlocked = false;
                    return true;
                }
            }

            try {
                Class<?> vaultUnlockedClass = Class.forName("net.milkbowl.vault2.economy.Economy");
                RegisteredServiceProvider<?> rspV2 = getServer().getServicesManager().getRegistration(vaultUnlockedClass);
                if (rspV2 != null) {
                    economy = (Economy) rspV2.getProvider();
                    isVaultUnlocked = true;
                    return true;
                }
            } catch (ClassNotFoundException e) {

            }

            return false;
        } catch (Exception e) {
            getLogger().warning("Error setting up economy: " + e.getMessage());
            return false;
        }
    }

    private void setupPermissions() {
        try {
            RegisteredServiceProvider<Permission> rsp;

            if (isVaultUnlocked) {
                try {
                    Class<?> v2PermissionClass = Class.forName("net.milkbowl.vault2.permission.Permission");
                    RegisteredServiceProvider<?> v2Rsp = getServer().getServicesManager().getRegistration(v2PermissionClass);
                    if (v2Rsp != null) {
                        permissions = (Permission) v2Rsp.getProvider();
                        getLogger().info("Hooked into VaultUnlocked permissions");
                        return;
                    }
                } catch (ClassNotFoundException e) {

                }
            }

            rsp = getServer().getServicesManager().getRegistration(Permission.class);
            if (rsp != null) {
                permissions = rsp.getProvider();
                getLogger().info("Hooked into Vault permissions");
            } else {
                getLogger().warning("No permission service registered - permission features disabled");
            }
        } catch (Exception e) {
            getLogger().warning("Error setting up permissions: " + e.getMessage());
        }
    }

    private void logAvailableServices() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return;
        }

        getLogger().info("Available services:");

        try {
            getLogger().info("Economy services:");
            for (RegisteredServiceProvider<Economy> provider :
                    getServer().getServicesManager().getRegistrations(Economy.class)) {
                getLogger().info("- V1: " + provider.getProvider().getName() +
                        " (priority: " + provider.getPriority() + ")");
            }

            Class<?> v2Class = Class.forName("net.milkbowl.vault2.economy.Economy");
            for (RegisteredServiceProvider<?> provider :
                    getServer().getServicesManager().getRegistrations(v2Class)) {
                getLogger().info("- V2: " + provider.getProvider().getClass().getName() +
                        " (priority: " + provider.getPriority() + ")");
            }
        } catch (ClassNotFoundException e) {
            getLogger().info("- No V2 economy services available");
        } catch (NoClassDefFoundError e) {
            getLogger().info("- Economy services not available");
        }

        try {
            getLogger().info("Permission services:");
            for (RegisteredServiceProvider<Permission> provider :
                    getServer().getServicesManager().getRegistrations(Permission.class)) {
                getLogger().info("- V1: " + provider.getProvider().getClass().getName() +
                        " (priority: " + provider.getPriority() + ")");
            }

            Class<?> v2PermissionClass = Class.forName("net.milkbowl.vault2.permission.Permission");
            for (RegisteredServiceProvider<?> provider :
                    getServer().getServicesManager().getRegistrations(v2PermissionClass)) {
                getLogger().info("- V2: " + provider.getProvider().getClass().getName() +
                        " (priority: " + provider.getPriority() + ")");
            }
        } catch (ClassNotFoundException e) {
            getLogger().info("- No V2 permission services available");
        } catch (NoClassDefFoundError e) {
            getLogger().info("- Permission services not available");
        }
    }

    private void registerListeners() {
        registerListener(new GuiClickListener());
        registerListener(new GuiCommandListener());
        try {
            registerListener(new NexoHookListener());
        } catch (NoClassDefFoundError ignored) {}
    }

    private void registerListener(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }

    private void registerCommands() {
        commandManager = new PaperCommandManager(this);
        commandManager.registerCommand(new NotionMenusCommand());
        commandManager.getCommandCompletions().registerCompletion("guis", new GUICompletions());
    }

    public void reload() {
        reloadConfig();
        guiManager.reloadGuis();
        checkForVaultWithRetries(0);
    }
}