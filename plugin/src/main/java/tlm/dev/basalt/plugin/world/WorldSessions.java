package tlm.dev.basalt.plugin.world;

import tlm.dev.basalt.core.world.VirtualWorlds;
import tlm.dev.basalt.plugin.lifecycle.Lifecycle;

import java.util.logging.Logger;

public final class WorldSessions implements Lifecycle {

    private final VirtualWorlds<?> worlds;
    private final Logger logger;

    public WorldSessions(VirtualWorlds<?> worlds, Logger logger) {
        this.worlds = worlds;
        this.logger = logger;
    }

    @Override
    public void onDisable() {
        int closed = worlds.closeAll();
        logger.info("Closed " + closed + " virtual world" + (closed == 1 ? "" : "s"));
    }
}
