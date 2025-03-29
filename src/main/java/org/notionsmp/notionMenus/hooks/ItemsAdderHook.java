package org.notionsmp.notionMenus.hooks;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemsAdderHook implements ItemHook {

    @Override
    public ItemStack getItem(String id, Player player) {
        CustomStack customStack = CustomStack.getInstance(id);
        ItemStack item = customStack.getItemStack().clone();
        if (customStack != null) {
            return item.clone();
        }
        return null;
    }
}