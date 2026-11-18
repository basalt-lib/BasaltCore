package tlm.dev.basalt.nms.port;

import java.util.List;

public record PacketRun(List<Object> packets) {

    public PacketRun {
        packets = List.copyOf(packets);
        if (packets.isEmpty()) {
            throw new IllegalArgumentException("a run of packets holds at least one packet");
        }
    }
}
