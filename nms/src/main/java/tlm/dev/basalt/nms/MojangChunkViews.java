package tlm.dev.basalt.nms;

import org.bukkit.Server;
import tlm.dev.basalt.core.interaction.InteractionInbox;
import tlm.dev.basalt.core.visibility.ChunkViewService;
import tlm.dev.basalt.core.visibility.ChunkViews;
import tlm.dev.basalt.core.visibility.ViewChanges;
import tlm.dev.basalt.nms.channel.NettyPlayerChannels;
import tlm.dev.basalt.nms.packet.InteractionPacketReader;
import tlm.dev.basalt.nms.packet.MojangChunkPackets;
import tlm.dev.basalt.nms.packet.ViewPacketFilter;
import tlm.dev.basalt.nms.port.ChannelFilters;
import tlm.dev.basalt.nms.port.VirtualCopy;
import tlm.dev.basalt.nms.view.MojangViewDisplay;
import tlm.dev.basalt.nms.view.ViewTools;

public final class MojangChunkViews {

    private MojangChunkViews() {
    }

    public static ChunkViews create(Server server, InteractionInbox inbox, ViewChanges changes) {
        ViewTools tools = new ViewTools(new NettyPlayerChannels(), new MojangChunkPackets(),
                (player, view) -> new ChannelFilters(new ViewPacketFilter(view), new InteractionPacketReader(player, view, inbox)));
        return new ChunkViewService<VirtualCopy>(new MojangViewDisplay(server, tools), changes);
    }
}
