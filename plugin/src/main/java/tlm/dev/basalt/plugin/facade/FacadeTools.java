package tlm.dev.basalt.plugin.facade;

import org.bukkit.Server;
import tlm.dev.basalt.core.world.VirtualWorlds;
import tlm.dev.basalt.core.world.WorldSettings;
import tlm.dev.basalt.nms.platform.MojangBlockStates;

public record FacadeTools(Server server, VirtualWorlds<?> worlds, MojangBlockStates blocks, WorldSettings settings) {

    public FacadeTools {
        if (server == null || worlds == null || blocks == null || settings == null) {
            throw new IllegalArgumentException("the facade needs the server, the worlds, the block states and the world settings");
        }
    }

    void checkThread() {
        if (!server.isPrimaryThread()) {
            throw new IllegalStateException("Basalt runs on the main server thread only");
        }
    }
}
