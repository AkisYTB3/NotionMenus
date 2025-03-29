package org.notionsmp.notionMenus.commands;

import co.aikar.commands.BukkitCommandCompletionContext;
import co.aikar.commands.CommandCompletions;
import org.notionsmp.notionMenus.NotionMenus;

import java.util.Collection;

public class GUICompletions implements CommandCompletions.CommandCompletionHandler<BukkitCommandCompletionContext> {

    @Override
    public Collection<String> getCompletions(BukkitCommandCompletionContext context) {
        return NotionMenus.getInstance().getGuiManager().getGuis().keySet();
    }
}