package tlm.dev.basalt.plugin.facade;

import org.bukkit.Server;
import tlm.dev.basalt.core.entity.VirtualEntityService;
import tlm.dev.basalt.core.world.VirtualWorlds;

public record FacadeTools(Server server, VirtualWorlds<?> worlds, VirtualEntityService entities, ContentTools content) {

    public FacadeTools {
        if (server == null || worlds == null || entities == null || content == null) {
            throw new IllegalArgumentException("the facade needs the server, the worlds, the entities and the content tools");
        }
    }

    void checkThread() {
        if (!server.isPrimaryThread()) {
            throw new IllegalStateException("Basalt runs on the main server thread only");
        }
    }
}
