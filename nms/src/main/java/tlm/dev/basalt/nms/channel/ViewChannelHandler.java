package tlm.dev.basalt.nms.channel;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import tlm.dev.basalt.nms.port.ChannelFilters;
import tlm.dev.basalt.nms.port.PacketRun;

import java.util.List;

final class ViewChannelHandler extends ChannelDuplexHandler {

    private final ChannelFilters filters;

    ViewChannelHandler(ChannelFilters filters) {
        this.filters = filters;
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) {
        if (filters.inbound().consumes(message)) {
            return;
        }
        context.fireChannelRead(message);
    }

    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) {
        Object filtered = filters.outbound().filter(message);
        if (filtered == null) {
            promise.trySuccess();
            return;
        }
        if (filtered instanceof PacketRun run) {
            List<Object> packets = run.packets();
            int last = packets.size() - 1;
            for (int index = 0; index < last; index++) {
                context.write(packets.get(index), context.voidPromise());
            }
            context.write(packets.get(last), promise);
            return;
        }
        context.write(filtered, promise);
    }
}
