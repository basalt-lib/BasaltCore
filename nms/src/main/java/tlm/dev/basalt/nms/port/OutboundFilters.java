package tlm.dev.basalt.nms.port;

import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import tlm.dev.basalt.core.visibility.PlayerView;

public interface OutboundFilters {

    OutboundFilter forView(PlayerView<ClientboundLevelChunkWithLightPacket> view);
}
