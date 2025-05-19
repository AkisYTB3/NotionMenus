package org.notionsmp.notionMenus.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.notionsmp.notionMenus.NotionMenus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CommandAlias("notionmenus|nm")
@CommandPermission("notionmenus.use")
public class NotionMenusCommand extends BaseCommand {

    @Subcommand("gui")
    @CommandCompletion("@guis @players")
    @Syntax("<gui_id> <player> [args...]")
    @Description("Opens a GUI for another player")
    public void openGui(CommandSender sender, String guiId, String playerName, @Optional String[] args) {
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            sender.sendMessage(NotionMenus.NotionString("<red>Player not found!"));
            return;
        }

        List<String> argsList = args != null ? Arrays.asList(args) : new ArrayList<>();
        NotionMenus.getInstance().getGuiManager().openGui(guiId, targetPlayer, argsList);
    }

    @Subcommand("reload|rl")
    @CommandPermission("notionmenus.reload")
    @Description("Reloads all GUIs from config")
    public void reload(CommandSender sender) {
        NotionMenus.getInstance().reload();
        sender.sendMessage(NotionMenus.NotionString("<green>GUIs reloaded successfully!"));
    }

    @Default
    @HelpCommand
    public void help(CommandSender sender) {
        sender.sendMessage(NotionMenus.NotionString("<red>Usage: /notionmenus gui <gui_id> <player> [args...]"));
        sender.sendMessage(NotionMenus.NotionString("<red>Usage: /notionmenus reload"));
    }
}