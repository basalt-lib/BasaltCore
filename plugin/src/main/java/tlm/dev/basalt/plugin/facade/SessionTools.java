package tlm.dev.basalt.plugin.facade;

import tlm.dev.basalt.core.entity.VirtualEntityService;
import tlm.dev.basalt.core.overlay.Overlays;
import tlm.dev.basalt.core.world.VirtualWorlds;
import tlm.dev.basalt.nms.platform.MojangLook;

public record SessionTools(VirtualWorlds<?> worlds, VirtualEntityService entities, Overlays<MojangLook> overlays) {

    public SessionTools {
        if (worlds == null || entities == null || overlays == null) {
            throw new IllegalArgumentException("the sessions need the worlds, their entities and the overlays");
        }
    }
}
