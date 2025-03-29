package org.notionsmp.notionMenus.hooks;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.notionsmp.notionMenus.utils.SkullUtil;

public class BaseheadHook implements ItemHook {
    @Override
    public ItemStack getItem(String id, Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            SkullUtil.applyBase64Texture(meta, id);
            item.setItemMeta(meta);
        }
        return item;
    }
}