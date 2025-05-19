package org.notionsmp.notionMenus.gui;

import lombok.Getter;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

@Getter
public class CustomInventoryHolder implements InventoryHolder {
    private final String guiId;
    private final Map<String, String> args;

    public CustomInventoryHolder(String guiId, Map<String, String> args) {
        this.guiId = guiId;
        this.args = new HashMap<>(args);
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}