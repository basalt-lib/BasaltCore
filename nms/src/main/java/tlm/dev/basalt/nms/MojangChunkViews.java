package tlm.dev.basalt.nms;

import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import org.bukkit.Server;
import tlm.dev.basalt.core.visibility.ChunkViewService;
import tlm.dev.basalt.core.visibility.ChunkViews;
import tlm.dev.basalt.nms.channel.NettyPlayerChannels;
import tlm.dev.basalt.nms.packet.MojangChunkPackets;
import tlm.dev.basalt.nms.packet.ViewPacketFilter;
import tlm.dev.basalt.nms.view.MojangViewDisplay;
import tlm.dev.basalt.nms.view.ViewTools;

public final class MojangChunkViews {

    private MojangChunkViews() {
    }

    public static ChunkViews create(Server server) {
        ViewTools tools = new ViewTools(new NettyPlayerChannels(), new MojangChunkPackets(), ViewPacketFilter::new);
        return new ChunkViewService<ClientboundLevelChunkWithLightPacket>(new MojangViewDisplay(server, tools));
    }
}
