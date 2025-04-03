package org.notionsmp.notionMenus.utils;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.notionsmp.notionMenus.NotionMenus;
import org.notionsmp.notionMenus.gui.GuiConfig;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActionUtil {
    private static final Random random = new Random();
    private static final Pattern DELAY_PATTERN = Pattern.compile("<delay=(\\d+)>");
    private static final Pattern CHANCE_PATTERN = Pattern.compile("<chance=(\\d+)>");
    private static final GsonComponentSerializer GSON_SERIALIZER = GsonComponentSerializer.gson();

    public static void executeAction(String action, Player player) {
        executeAction(action, player, null);
    }

    public static void executeAction(String action, Player player, InventoryClickEvent event) {
        if (action == null || action.isEmpty()) return;
        try {
            ParsedAction parsed = parseActionTags(action);
            if (random.nextInt(100) >= parsed.chance) return;
            if (parsed.delay > 0) {
                scheduleDelayedAction(parsed.cleanAction, player, event, parsed.delay);
            } else {
                executeImmediateAction(parsed.cleanAction, player, event);
            }
        } catch (Exception e) {
            NotionMenus.getInstance().getLogger().warning("Failed to execute action: " + action);
            NotionMenus.getInstance().getLogger().warning(e.getMessage());
        }
    }

    private static ParsedAction parseActionTags(String action) {
        int chance = 100;
        int delay = 0;
        String cleanAction = action;
        Matcher chanceMatcher = CHANCE_PATTERN.matcher(action);
        if (chanceMatcher.find()) {
            chance = Math.min(100, Math.max(0, Integer.parseInt(chanceMatcher.group(1))));
            cleanAction = cleanAction.replace(chanceMatcher.group(0), "");
        }
        Matcher delayMatcher = DELAY_PATTERN.matcher(action);
        if (delayMatcher.find()) {
            delay = Math.max(0, Integer.parseInt(delayMatcher.group(1)));
            cleanAction = cleanAction.replace(delayMatcher.group(0), "");
        }
        return new ParsedAction(cleanAction.trim(), chance, delay);
    }

    private static void scheduleDelayedAction(String action, Player player, InventoryClickEvent event, int delay) {
        new BukkitRunnable() {
            @Override
            public void run() {
                executeImmediateAction(action, player, event);
            }
        }.runTaskLater(NotionMenus.getInstance(), delay);
    }

    private static void executeImmediateAction(String action, Player player, InventoryClickEvent event) {
        String actionType = action.split(" ")[0].toLowerCase();
        String content = action.substring(actionType.length()).trim();
        String processedContent;

        switch (actionType) {
            case "[close]":
                if (event != null) player.closeInventory();
                break;
            case "[console]":
                processedContent = processContent(content, player, event);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedContent);
                break;
            case "[player]":
                processedContent = processContent(content, player, event);
                Bukkit.dispatchCommand(player, processedContent);
                break;
            case "[message]":
                processedContent = processContent(content, player, event);
                player.sendMessage(MiniMessage.miniMessage().deserialize(processedContent));
                break;
            case "[actionbar]":
                processedContent = processContent(content, player, event);
                player.sendActionBar(MiniMessage.miniMessage().deserialize(processedContent));
                break;
            case "[sound]":
                handleSoundAction(content, player, event, false);
                break;
            case "[broadcast]":
                processedContent = processContent(content, player, event);
                Bukkit.broadcast(MiniMessage.miniMessage().deserialize(processedContent));
                break;
            case "[opengui]":
                processedContent = processContent(content, player, event);
                NotionMenus.getInstance().getGuiManager().openGui(processedContent, player);
                break;
            case "[soundall]":
                handleSoundAction(content, player, event, true);
                break;
            case "[takemoney]":
                if (NotionMenus.getInstance().getEconomy() != null) {
                    processedContent = processContent(content, player, event);
                    NotionMenus.getInstance().getEconomy().withdrawPlayer(player, Double.parseDouble(processedContent));
                }
                break;
            case "[givemoney]":
                if (NotionMenus.getInstance().getEconomy() != null) {
                    processedContent = processContent(content, player, event);
                    NotionMenus.getInstance().getEconomy().depositPlayer(player, Double.parseDouble(processedContent));
                }
                break;
            case "[takexp]":
                handleXpAction(content, player, event, false);
                break;
            case "[givexp]":
                handleXpAction(content, player, event, true);
                break;
            case "[giveperm]":
                if (NotionMenus.getInstance().getPermissions() != null) {
                    processedContent = processContent(content, player, event);
                    NotionMenus.getInstance().getPermissions().playerAdd(player, processedContent);
                }
                break;
            case "[takeperm]":
                if (NotionMenus.getInstance().getPermissions() != null) {
                    processedContent = processContent(content, player, event);
                    NotionMenus.getInstance().getPermissions().playerRemove(player, processedContent);
                }
                break;
            case "[chat]":
                processedContent = processContent(content, player, event);
                player.chat(processedContent);
                break;
            case "[placeholder]":
                processedContent = processContent(content, player, event);
                GuiConfig.parsePlaceholders(player, processedContent);
                break;
            case "[json]":
                handleJsonAction(content, player, event, false);
                break;
            case "[jsonbroadcast]":
                handleJsonAction(content, player, event, true);
                break;
            case "[meta]":
                processedContent = processContent(content, player, event);
                handleMetaAction(processedContent, player);
                break;
            case "[server]":
                processedContent = processContent(content, player, event);
                connectToServer(player, processedContent);
                break;
            case "[refresh]":
                NotionMenus.getInstance().getGuiManager().refreshPlayerGui(player);
                break;
        }
    }

    private static String processContent(String content, Player player, InventoryClickEvent event) {
        content = replacePlaceholders(content, player, event);
        return GuiConfig.parsePlaceholders(player, content);
    }

    private static String replacePlaceholders(String text, Player player, InventoryClickEvent event) {
        if (text == null) return "";
        Pattern randomPattern = Pattern.compile("<random:(-?\\d+),(-?\\d+)>");
        Matcher randomMatcher = randomPattern.matcher(text);
        while (randomMatcher.find()) {
            int min = Integer.parseInt(randomMatcher.group(1));
            int max = Integer.parseInt(randomMatcher.group(2));
            int randomNum = random.nextInt(max - min + 1) + min;
            text = text.replace(randomMatcher.group(0), String.valueOf(randomNum));
        }
        String result = text.replace("<player>", player.getName())
                .replace("<location>", player.getLocation().toString())
                .replace("<playerX>", String.valueOf(player.getLocation().getX()))
                .replace("<playerY>", String.valueOf(player.getLocation().getY()))
                .replace("<playerZ>", String.valueOf(player.getLocation().getZ()))
                .replace("<player_health>", String.valueOf(player.getHealth()))
                .replace("<player_food>", String.valueOf(player.getFoodLevel()));
        if (event != null) {
            result = result.replace("<slot_clicked>", String.valueOf(event.getSlot()))
                    .replace("<raw_slot_clicked>", String.valueOf(event.getRawSlot()));
        }
        return result;
    }

    private static void handleSoundAction(String content, Player player, InventoryClickEvent event, boolean allPlayers) {
        String processed = processContent(content, player, event).trim();
        String[] parts = processed.split("\\s+");
        if (parts.length == 0) return;
        String sound = parts[0];
        float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
        float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
        boolean allWorlds = parts.length <= 3 || Boolean.parseBoolean(parts[3]);
        if (allPlayers) {
            if (allWorlds) {
                Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), sound, volume, pitch));
            } else {
                player.getWorld().getPlayers().forEach(p -> p.playSound(p.getLocation(), sound, volume, pitch));
            }
        } else {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    private static void handleXpAction(String content, Player player, InventoryClickEvent event, boolean give) {
        String xp = processContent(content, player, event);
        if (xp.endsWith("l")) {
            int levels = Integer.parseInt(xp.substring(0, xp.length() - 1));
            player.setLevel(player.getLevel() + (give ? levels : -levels));
        } else {
            int points = Integer.parseInt(xp);
            player.giveExp(give ? points : -points);
        }
    }

    private static void handleJsonAction(String content, Player player, InventoryClickEvent event, boolean broadcast) {
        String json = processContent(content, player, event);
        try {
            Component component = GSON_SERIALIZER.deserialize(json);
            if (broadcast) {
                Bukkit.getServer().sendMessage(component);
            } else {
                player.sendMessage(component);
            }
        } catch (Exception e) {
            String error = broadcast ? "Invalid JSON broadcast" : "<red>Invalid JSON";
            if (broadcast) NotionMenus.getInstance().getLogger().warning(error);
            else player.sendMessage(MiniMessage.miniMessage().deserialize(error));
            NotionMenus.getInstance().getLogger().warning("JSON Error: " + e.getMessage());
        }
    }

    private static void handleMetaAction(String metaAction, Player player) {
        String[] parts = metaAction.split(" ");
        if (parts.length < 3) return;
        String operation = parts[0].toLowerCase();
        String key = parts[1];
        String type = parts[2].toLowerCase();
        String value = parts.length > 3 ? parts[3] : null;
        PersistentDataContainer container = player.getPersistentDataContainer();
        NamespacedKey namespacedKey = new NamespacedKey(NotionMenus.getInstance(), key);
        try {
            switch (operation) {
                case "set":
                    if (value == null) return;
                    switch (type) {
                        case "string":
                            container.set(namespacedKey, PersistentDataType.STRING, value);
                            break;
                        case "int":
                            container.set(namespacedKey, PersistentDataType.INTEGER, Integer.parseInt(value));
                            break;
                        case "double":
                            container.set(namespacedKey, PersistentDataType.DOUBLE, Double.parseDouble(value));
                            break;
                        case "float":
                            container.set(namespacedKey, PersistentDataType.FLOAT, Float.parseFloat(value));
                            break;
                        case "long":
                            container.set(namespacedKey, PersistentDataType.LONG, Long.parseLong(value));
                            break;
                        case "boolean":
                            container.set(namespacedKey, PersistentDataType.BYTE, (byte) (Boolean.parseBoolean(value) ? 1 : 0));
                            break;
                    }
                    break;
                case "remove":
                    container.remove(namespacedKey);
                    break;
                case "add":
                    if (value == null) return;
                    switch (type) {
                        case "int":
                            int currentInt = container.getOrDefault(namespacedKey, PersistentDataType.INTEGER, 0);
                            container.set(namespacedKey, PersistentDataType.INTEGER, currentInt + Integer.parseInt(value));
                            break;
                        case "double":
                            double currentDouble = container.getOrDefault(namespacedKey, PersistentDataType.DOUBLE, 0.0);
                            container.set(namespacedKey, PersistentDataType.DOUBLE, currentDouble + Double.parseDouble(value));
                            break;
                        case "float":
                            float currentFloat = container.getOrDefault(namespacedKey, PersistentDataType.FLOAT, 0.0f);
                            container.set(namespacedKey, PersistentDataType.FLOAT, currentFloat + Float.parseFloat(value));
                            break;
                        case "long":
                            long currentLong = container.getOrDefault(namespacedKey, PersistentDataType.LONG, 0L);
                            container.set(namespacedKey, PersistentDataType.LONG, currentLong + Long.parseLong(value));
                            break;
                    }
                    break;
                case "subtract":
                    if (value == null) return;
                    switch (type) {
                        case "int":
                            int currentInt = container.getOrDefault(namespacedKey, PersistentDataType.INTEGER, 0);
                            container.set(namespacedKey, PersistentDataType.INTEGER, currentInt - Integer.parseInt(value));
                            break;
                        case "double":
                            double currentDouble = container.getOrDefault(namespacedKey, PersistentDataType.DOUBLE, 0.0);
                            container.set(namespacedKey, PersistentDataType.DOUBLE, currentDouble - Double.parseDouble(value));
                            break;
                        case "float":
                            float currentFloat = container.getOrDefault(namespacedKey, PersistentDataType.FLOAT, 0.0f);
                            container.set(namespacedKey, PersistentDataType.FLOAT, currentFloat - Float.parseFloat(value));
                            break;
                        case "long":
                            long currentLong = container.getOrDefault(namespacedKey, PersistentDataType.LONG, 0L);
                            container.set(namespacedKey, PersistentDataType.LONG, currentLong - Long.parseLong(value));
                            break;
                    }
                    break;
                case "switch":
                    if (type.equals("boolean")) {
                        byte current = container.getOrDefault(namespacedKey, PersistentDataType.BYTE, (byte) 0);
                        container.set(namespacedKey, PersistentDataType.BYTE, (byte) (current == 1 ? 0 : 1));
                    }
                    break;
            }
        } catch (Exception e) {
            NotionMenus.getInstance().getLogger().warning("Failed meta action: " + metaAction);
            NotionMenus.getInstance().getLogger().warning(e.getMessage());
        }
    }

    public static void connectToServer(Player p, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        try {
            out.writeUTF("Connect");
            out.writeUTF(server);
            p.sendPluginMessage(NotionMenus.getInstance(), "BungeeCord", out.toByteArray());
        } catch (Exception e) {
            NotionMenus.getInstance().getLogger().warning("Failed to send " + p.getName() + " to " + server);
            NotionMenus.getInstance().getLogger().warning(e.getMessage());
        }
    }

    private record ParsedAction(String cleanAction, int chance, int delay) {}
}