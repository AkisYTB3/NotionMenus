package org.notionsmp.notionMenus.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.entity.Player;
import org.notionsmp.notionMenus.NotionMenus;

public class GuiCommandListener implements Listener {
    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase().substring(1);

        String guiId = NotionMenus.getInstance().getGuiManager().getGuiIdByCommand(command);
        if (guiId != null) {
            event.setCancelled(true);
            NotionMenus.getInstance().getGuiManager().openGui(guiId, player);
        }
    }
}