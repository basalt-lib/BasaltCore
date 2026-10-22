package tlm.dev.basalt.nms.port;

public record ChannelFilters(OutboundFilter outbound, InboundGate inbound) {

    public ChannelFilters {
        if (outbound == null || inbound == null) {
            throw new IllegalArgumentException("a channel needs its outbound filter and its inbound gate");
        }
    }
}
