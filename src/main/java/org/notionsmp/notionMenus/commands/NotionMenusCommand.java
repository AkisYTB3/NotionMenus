package org.notionsmp.notionMenus.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.notionsmp.notionMenus.NotionMenus;

@CommandAlias("notionmenus|nm")
@CommandPermission("notionmenus.use")
public class NotionMenusCommand extends BaseCommand {

    @Subcommand("gui")
    @CommandCompletion("@guis @players")
    @Syntax("<gui_id> [player]")
    @Description("Opens a GUI for yourself or another player")
    public void openGui(CommandSender sender, String guiId, @Optional Player targetPlayer) {
        if (targetPlayer == null && !(sender instanceof Player)) {
            sender.sendMessage(NotionMenus.NotionString("<red>You must specify a player when using this command from console!"));
            return;
        }

        if (targetPlayer == null) {
            targetPlayer = (Player) sender;
        }

        NotionMenus.getInstance().getGuiManager().openGui(guiId, targetPlayer);
    }

    @Subcommand("reload|rl")
    @CommandPermission("notionmenus.reload")
    @Description("Reloads all GUIs from config")
    public void reload(CommandSender sender) {
        //NotionMenus.getInstance().getGuiManager().reloadGuis();
        NotionMenus.getInstance().reload();
        sender.sendMessage(NotionMenus.NotionString("<green>GUIs reloaded successfully!"));
    }

    @Default
    @HelpCommand
    public void help(CommandSender sender) {
        sender.sendMessage(NotionMenus.NotionString("<red>Usage: /notionmenus gui <gui_id> [player]"));
        sender.sendMessage(NotionMenus.NotionString("<red>Usage: /notionmenus reload"));
    }
}