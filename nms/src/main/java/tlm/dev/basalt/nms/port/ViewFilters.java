package tlm.dev.basalt.nms.port;

import tlm.dev.basalt.core.visibility.PlayerView;

import java.util.UUID;

public interface ViewFilters {

    ChannelFilters forView(UUID player, PlayerView view);
}
