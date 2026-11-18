package tlm.dev.basalt.nms.port;

import java.util.UUID;

public interface OverlayChannel {

    ChannelFilters filtersFor(UUID player);
}
