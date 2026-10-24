package tlm.dev.basalt.plugin.world;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import tlm.dev.basalt.core.world.VirtualWorlds;

public final class OwnerListener implements Listener {

    private final Plugin basalt;
    private final VirtualWorlds<?> worlds;

    public OwnerListener(Plugin basalt, VirtualWorlds<?> worlds) {
        this.basalt = basalt;
        this.worlds = worlds;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDisable(PluginDisableEvent event) {
        Plugin owner = event.getPlugin();
        if (owner.equals(basalt)) {
            return;
        }
        int closed = worlds.closeOwnedBy(owner.getName());
        if (closed > 0) {
            basalt.getLogger().info("Closed " + closed + " virtual world" + (closed == 1 ? "" : "s") + " of " + owner.getName());
        }
    }
}
