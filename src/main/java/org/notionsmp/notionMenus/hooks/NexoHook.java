package org.notionsmp.notionMenus.hooks;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class NexoHook implements ItemHook {
    @Override
    public ItemStack getItem(String id, Player player) {
        ItemBuilder itemBuilder = NexoItems.itemFromId(id);
        if (itemBuilder != null) {
            return itemBuilder.build();
        }
        return null;
    }
}