package tlm.dev.basalt.nms.blueprint;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import tlm.dev.basalt.core.blueprint.RealBlocks;
import tlm.dev.basalt.core.position.BlockBox;

import java.util.UUID;

public final class MojangRealBlocks implements RealBlocks {

    private static final int AIR = Block.getId(Blocks.AIR.defaultBlockState());

    private final Server server;

    public MojangRealBlocks(Server server) {
        this.server = server;
    }

    @Override
    public int[] states(UUID world, BlockBox box) {
        World bukkitWorld = server.getWorld(world);
        if (bukkitWorld == null) {
            throw new IllegalStateException("world " + world + " is not loaded");
        }
        ServerLevel level = ((CraftWorld) bukkitWorld).getHandle();
        int sizeX = box.max().x() - box.min().x() + 1;
        int sizeY = box.max().y() - box.min().y() + 1;
        int sizeZ = box.max().z() - box.min().z() + 1;
        int[] states = new int[sizeX * sizeY * sizeZ];
        for (int chunkX = box.min().x() >> 4; chunkX <= box.max().x() >> 4; chunkX++) {
            for (int chunkZ = box.min().z() >> 4; chunkZ <= box.max().z() >> 4; chunkZ++) {
                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                int fromX = Math.max(box.min().x(), chunkX << 4);
                int toX = Math.min(box.max().x(), (chunkX << 4) + 15);
                int fromZ = Math.max(box.min().z(), chunkZ << 4);
                int toZ = Math.min(box.max().z(), (chunkZ << 4) + 15);
                for (int y = box.min().y(); y <= box.max().y(); y++) {
                    LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
                    boolean empty = section.hasOnlyAir();
                    int planY = y - box.min().y();
                    for (int z = fromZ; z <= toZ; z++) {
                        int row = (z - box.min().z()) * sizeX + planY * sizeX * sizeZ - box.min().x();
                        for (int x = fromX; x <= toX; x++) {
                            states[row + x] = empty ? AIR : Block.getId(section.getBlockState(x & 15, y & 15, z & 15));
                        }
                    }
                }
            }
        }
        return states;
    }
}
