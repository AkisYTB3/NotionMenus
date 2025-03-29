package org.notionsmp.notionMenus.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;
import org.notionsmp.notionMenus.NotionMenus;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;

public class SkullUtil {

    public static void applyBase64Texture(SkullMeta meta, String base64) {
        PlayerProfile profile = getProfileBase64(base64);
        if (profile != null) {
            meta.setPlayerProfile(profile);
        }
    }

    private static PlayerProfile getProfileBase64(String base64) {
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        PlayerTextures textures = profile.getTextures();
        URL urlObject;
        try {
            urlObject = getUrlFromBase64(base64);
        } catch (MalformedURLException exception) {
            NotionMenus.getInstance().getLogger().warning("Invalid base64 texture: " + base64);
            return null;
        }
        textures.setSkin(urlObject);
        profile.setTextures(textures);
        return profile;
    }

    private static URL getUrlFromBase64(String base64) throws MalformedURLException {
        try {
            String decoded = new String(Base64.getDecoder().decode(base64));
            JsonObject json = JsonParser.parseString(decoded).getAsJsonObject();
            String url = json.getAsJsonObject("textures")
                    .getAsJsonObject("SKIN")
                    .get("url")
                    .getAsString();
            return URI.create(url).toURL();
        } catch (Throwable t) {
            throw new MalformedURLException("Invalid base64 string: " + base64);
        }
    }
}