package tlm.dev.basalt.nms.view;

import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.world.RealBlock;
import tlm.dev.basalt.core.world.RealChunks;

import java.util.Optional;
import java.util.UUID;

public final class MojangRealChunks implements RealChunks {

    private final Server server;

    public MojangRealChunks(Server server) {
        this.server = server;
    }

    @Override
    public Optional<RealBlock> firstBlock(UUID world, ChunkKey chunk) {
        World bukkitWorld = server.getWorld(world);
        if (bukkitWorld == null) {
            throw new IllegalStateException("world " + world + " is not loaded");
        }
        ServerLevel level = ((CraftWorld) bukkitWorld).getHandle();
        LevelChunk levelChunk = level.getChunk(chunk.x(), chunk.z());
        LevelChunkSection[] sections = levelChunk.getSections();
        for (int index = sections.length - 1; index >= 0; index--) {
            if (!sections[index].hasOnlyAir()) {
                Optional<RealBlock> found = highestIn(sections[index], chunk, SectionPos.sectionToBlockCoord(levelChunk.getMinSectionY() + index));
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<RealBlock> highestIn(LevelChunkSection section, ChunkKey chunk, int bottomY) {
        for (int y = 15; y >= 0; y--) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState state = section.getBlockState(x, y, z);
                    if (!state.isAir()) {
                        BlockPosition position = new BlockPosition((chunk.x() << 4) + x, bottomY + y, (chunk.z() << 4) + z);
                        return Optional.of(new RealBlock(position, BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()));
                    }
                }
            }
        }
        return Optional.empty();
    }
}
