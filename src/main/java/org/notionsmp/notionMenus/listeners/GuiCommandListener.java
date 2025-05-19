package org.notionsmp.notionMenus.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.entity.Player;
import org.notionsmp.notionMenus.NotionMenus;

import java.util.ArrayList;
import java.util.List;

public class GuiCommandListener implements Listener {
    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String[] parts = event.getMessage().substring(1).split(" ");
        String command = parts[0].toLowerCase();

        String guiId = NotionMenus.getInstance().getGuiManager().getGuiIdByCommand(command);
        if (guiId != null) {
            event.setCancelled(true);
            List<String> args = new ArrayList<>();
            for (int i = 1; i < parts.length; i++) {
                args.add(parts[i]);
            }
            NotionMenus.getInstance().getGuiManager().openGui(guiId, player, args);
        }
    }
}