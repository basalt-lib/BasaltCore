package tlm.dev.basalt.nms.port;

import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import tlm.dev.basalt.core.chunk.VirtualChunk;

public record VirtualCopy(VirtualChunk chunk, ClientboundLevelChunkWithLightPacket packet) {

    public VirtualCopy {
        if (chunk == null || packet == null) {
            throw new IllegalArgumentException("a virtual copy needs its chunk and its packet");
        }
    }
}
