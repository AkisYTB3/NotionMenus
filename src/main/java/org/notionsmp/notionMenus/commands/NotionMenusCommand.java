package org.notionsmp.notionMenus.commands;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.notionsmp.notionMenus.NotionMenus;
import org.notionsmp.notionMenus.gui.GuiManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class NotionMenusCommand implements CommandExecutor, TabCompleter {

    private final GuiManager guiManager;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(NotionMenus.NotionString("<red>Usage: /notionmenus gui <gui_id> [player]"));
            sender.sendMessage(NotionMenus.NotionString("<red>Usage: /notionmenus reload"));
            return false;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("gui")) {
            if (args.length < 2) {
                sender.sendMessage(NotionMenus.NotionString("<red>Usage: /notionmenus gui <gui_id> [player]"));
                return false;
            }

            String guiId = args[1];
            Player targetPlayer;

            if (args.length > 2) {
                targetPlayer = Bukkit.getPlayer(args[2]);
                if (targetPlayer == null) {
                    sender.sendMessage(NotionMenus.NotionString("<red>Player not found!"));
                    return false;
                }
            } else {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(NotionMenus.NotionString("<red>Only players can use this command without specifying a player!"));
                    return false;
                }
                targetPlayer = (Player) sender;
            }

            guiManager.openGui(guiId, targetPlayer);
            return true;
        } else if (subCommand.equals("reload")) {
            if (!sender.hasPermission("notionmenus.reload")) {
                sender.sendMessage(NotionMenus.NotionString("<red>You do not have permission to reload the GUIs!"));
                return false;
            }

            guiManager.reloadGuis();
            sender.sendMessage(NotionMenus.NotionString("<green>GUIs reloaded successfully!"));
            return true;
        } else {
            sender.sendMessage(NotionMenus.NotionString("<red>Unknown subcommand!"));
            sender.sendMessage(NotionMenus.NotionString("<red>Usage: /notionmenus gui <gui_id> [player]"));
            sender.sendMessage(NotionMenus.NotionString("<red>Usage: /notionmenus reload"));
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("gui");
            completions.add("reload");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("gui")) {
            completions.addAll(guiManager.getGuis().keySet());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("gui")) {
            completions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList()));
        }

        return completions;
    }
}