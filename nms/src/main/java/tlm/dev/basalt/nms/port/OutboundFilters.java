package tlm.dev.basalt.nms.port;

import tlm.dev.basalt.core.visibility.PlayerView;

public interface OutboundFilters {

    OutboundFilter forView(PlayerView<VirtualCopy> view);
}
