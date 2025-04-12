package org.notionsmp.notionMenus.listeners.hooks;

import com.nexomc.nexo.api.events.NexoItemsLoadedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.notionsmp.notionMenus.NotionMenus;

public class NexoHookListener implements Listener {
    @EventHandler
    public void on(NexoItemsLoadedEvent event) {
        NotionMenus.getInstance().getGuiManager().reloadGuis();
    }
}