package tlm.dev.basalt.nms.port;

import tlm.dev.basalt.core.chunk.VirtualChunk;
import tlm.dev.basalt.core.world.VirtualWorlds;
import tlm.dev.basalt.core.world.WorldChunks;

import java.util.concurrent.Executor;

public record WorldSource(VirtualWorlds<VirtualChunk> worlds, WorldChunks<VirtualChunk> chunks, Executor encoders) {

    public WorldSource {
        if (worlds == null || chunks == null || encoders == null) {
            throw new IllegalArgumentException("a world source needs the worlds, their chunks and the encoders");
        }
    }
}
