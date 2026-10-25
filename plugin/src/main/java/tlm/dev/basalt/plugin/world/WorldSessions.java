package tlm.dev.basalt.plugin.world;

import tlm.dev.basalt.core.world.VirtualWorlds;
import tlm.dev.basalt.plugin.lifecycle.Lifecycle;

import java.util.function.IntSupplier;
import java.util.logging.Logger;

public final class WorldSessions implements Lifecycle {

    private final VirtualWorlds<?> worlds;
    private final IntSupplier heldEntities;
    private final Logger logger;

    public WorldSessions(VirtualWorlds<?> worlds, IntSupplier heldEntities, Logger logger) {
        this.worlds = worlds;
        this.heldEntities = heldEntities;
        this.logger = logger;
    }

    @Override
    public void onDisable() {
        int closed = worlds.closeAll();
        logger.info("Closed " + closed + " virtual world" + (closed == 1 ? "" : "s"));
        logger.info("Virtual entities still held: " + heldEntities.getAsInt());
    }
}
