package org.notionsmp.notionMenus.hooks;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.notionsmp.notionMenus.NotionMenus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class HeadHook implements ItemHook {

    private final Map<String, PlayerProfile> cache = new ConcurrentHashMap<>();

    public void getHeadAsync(String id, Consumer<ItemStack> callback) {
        if (cache.containsKey(id)) {
            PlayerProfile cachedProfile = cache.get(id);
            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (meta != null) {
                meta.setPlayerProfile(cachedProfile);
                item.setItemMeta(meta);
            }
            callback.accept(item);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(NotionMenus.getInstance(), () -> {
            PlayerProfile profile = Bukkit.createProfile(id);
            profile.complete();
            cache.put(id, profile);

            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (meta != null) {
                meta.setPlayerProfile(profile);
                item.setItemMeta(meta);
            }

            Bukkit.getScheduler().runTask(NotionMenus.getInstance(), () -> callback.accept(item));
        });
    }

    @Override
    public ItemStack getItem(String id, Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            PlayerProfile profile = cache.getOrDefault(id, Bukkit.createProfile(id));
            meta.setPlayerProfile(profile);
            item.setItemMeta(meta);
        }
        return item;
    }
}
