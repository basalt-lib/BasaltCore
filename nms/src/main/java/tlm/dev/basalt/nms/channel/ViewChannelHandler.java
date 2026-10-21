package tlm.dev.basalt.nms.channel;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import tlm.dev.basalt.nms.port.OutboundFilter;

final class ViewChannelHandler extends ChannelOutboundHandlerAdapter {

    private final OutboundFilter filter;

    ViewChannelHandler(OutboundFilter filter) {
        this.filter = filter;
    }

    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) {
        Object filtered = filter.filter(message);
        if (filtered == null) {
            promise.trySuccess();
            return;
        }
        context.write(filtered, promise);
    }
}
