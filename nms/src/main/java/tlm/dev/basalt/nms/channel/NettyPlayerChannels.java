package tlm.dev.basalt.nms.channel;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.protocol.Packet;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import tlm.dev.basalt.nms.port.OutboundFilter;
import tlm.dev.basalt.nms.port.PlayerChannels;

import java.util.NoSuchElementException;

public final class NettyPlayerChannels implements PlayerChannels {

    static final String HANDLER_NAME = "basalt_view";
    private static final String UNBUNDLER = "unbundler";
    private static final String ENCODER = "encoder";

    @Override
    public void attach(Player player, OutboundFilter filter) {
        ChannelPipeline pipeline = pipeline(player);
        removeHandler(pipeline);
        ViewChannelHandler handler = new ViewChannelHandler(filter);
        if (pipeline.get(UNBUNDLER) != null) {
            pipeline.addBefore(UNBUNDLER, HANDLER_NAME, handler);
        } else {
            pipeline.addAfter(ENCODER, HANDLER_NAME, handler);
        }
    }

    @Override
    public void detach(Player player) {
        removeHandler(pipeline(player));
    }

    @Override
    public void writeDirect(Player player, Packet<?> packet) {
        ChannelHandlerContext context = pipeline(player).context(HANDLER_NAME);
        if (context != null) {
            context.writeAndFlush(packet);
        }
    }

    private static ChannelPipeline pipeline(Player player) {
        return ((CraftPlayer) player).getHandle().connection.connection.channel.pipeline();
    }

    private static void removeHandler(ChannelPipeline pipeline) {
        try {
            if (pipeline.get(HANDLER_NAME) != null) {
                pipeline.remove(HANDLER_NAME);
            }
        } catch (NoSuchElementException alreadyRemovedWithTheChannel) {
        }
    }
}
