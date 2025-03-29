package org.notionsmp.notionMenus;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.notionsmp.notionMenus.commands.NotionMenusCommand;
import org.notionsmp.notionMenus.gui.GuiManager;
import org.notionsmp.notionMenus.listeners.GuiClickListener;
import org.notionsmp.notionMenus.listeners.GuiCommandListener;
import org.notionsmp.notionMenus.utils.DeluxeMenusConverter;

import java.util.Objects;

@Getter
public class NotionMenus extends JavaPlugin {

    @Getter
    private static NotionMenus instance;
    private GuiManager guiManager;
    private boolean isPAPILoaded;
    private Economy economy;
    private Permission permissions;
    private boolean isVaultEnabled;
    private boolean isVaultUnlocked = false;

    private static final String PREFIX = "<gradient:#663399:#7069ff>Notion</gradient> <dark_gray>»</dark_gray> ";

    public static Component NotionString(String... strings) {
        StringBuilder messageBuilder = new StringBuilder();
        for (String str : strings) {
            messageBuilder.append(str);
        }
        return MiniMessage.miniMessage().deserialize(PREFIX + messageBuilder);
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        guiManager = new GuiManager();

        DeluxeMenusConverter converter = new DeluxeMenusConverter();
        converter.convertIfNeeded();

        this.isPAPILoaded = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;

        registerListeners();
        registerCommands();
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        checkForVaultWithRetries(0);
    }

    private void checkForVaultWithRetries(int attempt) {
        final int maxAttempts = 5;
        final long delayBetweenAttempts = 20L;

        Bukkit.getScheduler().runTaskLater(this, () -> {
            logAvailableServices();

            this.isVaultEnabled = setupEconomy();

            if (this.isVaultEnabled) {
                setupPermissions();
            }

            if (isVaultEnabled) {
                getLogger().info("Successfully hooked into " + (isVaultUnlocked ? "VaultUnlocked" : "Vault") +
                        " economy via " + economy.getName());
            } else if (attempt < maxAttempts - 1) {
                getLogger().info("Retrying Vault connection (attempt " + (attempt + 1) + ")...");
                checkForVaultWithRetries(attempt + 1);
            } else {
                getLogger().warning("Failed to hook into economy after " + maxAttempts + " attempts. Economy features disabled.");
            }
        }, delayBetweenAttempts);
    }

    private boolean setupEconomy() {
        try {

            if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
                getLogger().warning("Vault plugin not found!");
                return false;
            }

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

            getLogger().warning("Found Vault plugin but no economy service registered!");
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
        getLogger().info("Available services:");

        getLogger().info("Economy services:");
        for (RegisteredServiceProvider<Economy> provider :
                getServer().getServicesManager().getRegistrations(Economy.class)) {
            getLogger().info("- V1: " + provider.getProvider().getName() +
                    " (priority: " + provider.getPriority() + ")");
        }
        try {
            Class<?> v2Class = Class.forName("net.milkbowl.vault2.economy.Economy");
            for (RegisteredServiceProvider<?> provider :
                    getServer().getServicesManager().getRegistrations(v2Class)) {
                getLogger().info("- V2: " + provider.getProvider().getClass().getName() +
                        " (priority: " + provider.getPriority() + ")");
            }
        } catch (ClassNotFoundException e) {
            getLogger().info("- No V2 economy services available");
        }

        getLogger().info("Permission services:");
        for (RegisteredServiceProvider<Permission> provider :
                getServer().getServicesManager().getRegistrations(Permission.class)) {
            getLogger().info("- V1: " + provider.getProvider().getClass().getName() +
                    " (priority: " + provider.getPriority() + ")");
        }
        try {
            Class<?> v2PermissionClass = Class.forName("net.milkbowl.vault2.permission.Permission");
            for (RegisteredServiceProvider<?> provider :
                    getServer().getServicesManager().getRegistrations(v2PermissionClass)) {
                getLogger().info("- V2: " + provider.getProvider().getClass().getName() +
                        " (priority: " + provider.getPriority() + ")");
            }
        } catch (ClassNotFoundException e) {
            getLogger().info("- No V2 permission services available");
        }
    }

    private void registerListeners() {
        registerListener(new GuiClickListener());
        registerListener(new GuiCommandListener());
    }

    private void registerListener(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }

    private void registerCommands() {
        registerCommand("notionmenus", new NotionMenusCommand(getGuiManager()));
        registerCompleter("notionmenus", new NotionMenusCommand(getGuiManager()));
    }

    private void registerCommand(String name, CommandExecutor commandExecutor) {
        Objects.requireNonNull(getCommand(name)).setExecutor(commandExecutor);
    }

    private void registerCompleter(String name, TabCompleter tabCompleter) {
        Objects.requireNonNull(getCommand(name)).setTabCompleter(tabCompleter);
    }

    public void reload() {
        reloadConfig();
        guiManager.reloadGuis();
        checkForVaultWithRetries(0);
    }
}