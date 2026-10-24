package tlm.dev.basalt.nms;

import org.bukkit.Server;
import org.bukkit.craftbukkit.CraftServer;
import tlm.dev.basalt.core.interaction.InteractionInbox;
import tlm.dev.basalt.core.visibility.ChunkRequests;
import tlm.dev.basalt.core.visibility.ChunkViewService;
import tlm.dev.basalt.core.visibility.ChunkViews;
import tlm.dev.basalt.core.visibility.ViewChanges;
import tlm.dev.basalt.nms.channel.NettyPlayerChannels;
import tlm.dev.basalt.nms.packet.InteractionPacketReader;
import tlm.dev.basalt.nms.packet.MojangChunkPackets;
import tlm.dev.basalt.nms.packet.ViewPacketFilter;
import tlm.dev.basalt.nms.port.ChannelFilters;
import tlm.dev.basalt.nms.port.ChunkPackets;
import tlm.dev.basalt.nms.port.WorldSource;
import tlm.dev.basalt.nms.view.ChunkPacketCache;
import tlm.dev.basalt.nms.view.MojangViewDisplay;
import tlm.dev.basalt.nms.view.ViewTools;

public final class MojangChunkViews {

    private MojangChunkViews() {
    }

    public static ChunkViews create(Server server, InteractionInbox inbox, ViewChanges changes, WorldSource source) {
        ChunkRequests requests = new ChunkRequests();
        ChunkPackets packets = new MojangChunkPackets(((CraftServer) server).getServer().registryAccess());
        ViewTools tools = new ViewTools(new NettyPlayerChannels(), packets,
                (player, view) -> new ChannelFilters(new ViewPacketFilter(player, view, requests), new InteractionPacketReader(player, view, inbox)),
                new ChunkPacketCache(source, packets));
        return new ChunkViewService(new MojangViewDisplay(server, tools), changes, requests);
    }
}
