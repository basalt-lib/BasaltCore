package tlm.dev.basalt.nms.view;

import tlm.dev.basalt.nms.port.ChunkPackets;
import tlm.dev.basalt.nms.port.PlayerChannels;
import tlm.dev.basalt.nms.port.ViewFilters;

public record ViewTools(PlayerChannels channels, ChunkPackets packets, ViewFilters filters) {
}
