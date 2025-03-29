package org.notionsmp.notionMenus.hooks;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface ItemHook {
    ItemStack getItem(String id, Player player);
}