package org.notionsmp.notionMenus.gui;

import lombok.Getter;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

@Getter
public class CustomInventoryHolder implements InventoryHolder {
    private final String guiId;

    public CustomInventoryHolder(String guiId) {
        this.guiId = guiId;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}