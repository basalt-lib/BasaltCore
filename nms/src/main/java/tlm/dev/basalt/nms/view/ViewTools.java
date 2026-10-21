package tlm.dev.basalt.nms.view;

import tlm.dev.basalt.nms.port.ChunkPackets;
import tlm.dev.basalt.nms.port.OutboundFilters;
import tlm.dev.basalt.nms.port.PlayerChannels;

public record ViewTools(PlayerChannels channels, ChunkPackets packets, OutboundFilters filters) {
}
