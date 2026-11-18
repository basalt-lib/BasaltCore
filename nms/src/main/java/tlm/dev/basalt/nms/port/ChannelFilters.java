package tlm.dev.basalt.nms.port;

import java.util.ArrayList;
import java.util.List;

public record ChannelFilters(OutboundFilter outbound, InboundGate inbound) {

    public ChannelFilters {
        if (outbound == null || inbound == null) {
            throw new IllegalArgumentException("a channel needs its outbound filter and its inbound gate");
        }
    }

    public static ChannelFilters both(ChannelFilters first, ChannelFilters second) {
        OutboundFilter outbound = message -> {
            Object kept = first.outbound().filter(message);
            if (kept == null) {
                return null;
            }
            return kept instanceof PacketRun run ? throughEach(second.outbound(), run) : second.outbound().filter(kept);
        };
        InboundGate inbound = message -> first.inbound().consumes(message) || second.inbound().consumes(message);
        return new ChannelFilters(outbound, inbound);
    }

    private static Object throughEach(OutboundFilter filter, PacketRun run) {
        List<Object> kept = new ArrayList<>();
        for (Object packet : run.packets()) {
            Object filtered = filter.filter(packet);
            if (filtered instanceof PacketRun inner) {
                kept.addAll(inner.packets());
            } else if (filtered != null) {
                kept.add(filtered);
            }
        }
        return kept.isEmpty() ? null : new PacketRun(kept);
    }
}
