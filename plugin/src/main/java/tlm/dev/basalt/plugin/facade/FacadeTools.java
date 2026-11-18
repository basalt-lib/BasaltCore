package tlm.dev.basalt.plugin.facade;

import org.bukkit.Server;
import tlm.dev.basalt.core.entity.VirtualEntityService;
import tlm.dev.basalt.core.overlay.Overlays;
import tlm.dev.basalt.core.world.VirtualWorlds;
import tlm.dev.basalt.nms.platform.MojangLook;

public record FacadeTools(Server server, SessionTools sessions, ContentTools content) {

    public FacadeTools {
        if (server == null || sessions == null || content == null) {
            throw new IllegalArgumentException("the facade needs the server, the sessions and the content tools");
        }
    }

    VirtualWorlds<?> worlds() {
        return sessions.worlds();
    }

    VirtualEntityService entities() {
        return sessions.entities();
    }

    Overlays<MojangLook> overlays() {
        return sessions.overlays();
    }

    void checkThread() {
        if (!server.isPrimaryThread()) {
            throw new IllegalStateException("Basalt runs on the main server thread only");
        }
    }
}
