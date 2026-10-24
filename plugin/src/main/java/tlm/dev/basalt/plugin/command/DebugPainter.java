package tlm.dev.basalt.plugin.command;

import org.bukkit.Server;
import org.bukkit.util.BoundingBox;
import tlm.dev.basalt.api.VirtualWorld;
import tlm.dev.basalt.core.chunk.DebugChunkPattern;
import tlm.dev.basalt.core.chunk.LocalBox;
import tlm.dev.basalt.core.chunk.PatternBox;
import tlm.dev.basalt.core.debug.DebugPattern;

final class DebugPainter {

    private DebugPainter() {
    }

    static void paint(VirtualWorld world, Server server, DebugPattern pattern) {
        int bottom = world.area().world().getMinHeight();
        for (int chunkZ = world.area().minChunkZ(); chunkZ <= world.area().maxChunkZ(); chunkZ++) {
            for (int chunkX = world.area().minChunkX(); chunkX <= world.area().maxChunkX(); chunkX++) {
                for (PatternBox painted : DebugChunkPattern.boxes(pattern, bottom)) {
                    LocalBox box = painted.box();
                    int x = chunkX << 4;
                    int z = chunkZ << 4;
                    world.fill(new BoundingBox(x + box.minX(), box.minY(), z + box.minZ(), x + box.maxX() + 1, box.maxY() + 1, z + box.maxZ() + 1),
                            server.createBlockData(painted.block()));
                }
            }
        }
    }
}
