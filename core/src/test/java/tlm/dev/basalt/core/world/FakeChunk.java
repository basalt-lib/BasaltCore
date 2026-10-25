package tlm.dev.basalt.core.world;

import tlm.dev.basalt.core.edit.SectionWrite;
import tlm.dev.basalt.core.position.BlockBox;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.ChunkKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

record FakeChunk(ChunkKey key, Map<BlockPosition, Integer> blocks) implements WorldChunks<FakeChunk> {

    static final int AIR = 0;
    static final int MIN_Y = -64;
    static final FakeChunk CHUNKS = new FakeChunk(new ChunkKey(0, 0), Map.of());

    FakeChunk {
        blocks = Map.copyOf(blocks);
    }

    @Override
    public FakeChunk empty(ChunkKey chunk, HostArea host) {
        return new FakeChunk(chunk, Map.of());
    }

    @Override
    public FakeChunk fill(FakeChunk chunk, BlockBox box, int state) {
        Map<BlockPosition, Integer> changed = new HashMap<>(chunk.blocks());
        boolean any = false;
        for (int y = box.min().y(); y <= box.max().y(); y++) {
            for (int z = box.min().z(); z <= box.max().z(); z++) {
                for (int x = box.min().x(); x <= box.max().x(); x++) {
                    BlockPosition position = new BlockPosition(x, y, z);
                    if (!position.chunk().equals(chunk.key())) {
                        throw new IllegalArgumentException(position + " is outside chunk " + chunk.key());
                    }
                    int previous = changed.getOrDefault(position, AIR);
                    if (previous != state) {
                        any = true;
                        if (state == AIR) {
                            changed.remove(position);
                        } else {
                            changed.put(position, state);
                        }
                    }
                }
            }
        }
        return any ? new FakeChunk(chunk.key(), changed) : chunk;
    }

    @Override
    public FakeChunk written(FakeChunk chunk, List<SectionWrite> sections) {
        Map<BlockPosition, Integer> changed = new HashMap<>(chunk.blocks());
        boolean any = false;
        for (SectionWrite section : sections) {
            for (int index = 0; index < SectionWrite.BLOCKS; index++) {
                int state = section.stateAt(index);
                if (state == SectionWrite.KEEP) {
                    continue;
                }
                BlockPosition position = new BlockPosition(chunk.key().x() * 16 + (index & 15), MIN_Y + section.sectionIndex() * 16 + (index >> 8),
                        chunk.key().z() * 16 + (index >> 4 & 15));
                int previous = changed.getOrDefault(position, AIR);
                if (previous != state) {
                    any = true;
                    if (state == AIR) {
                        changed.remove(position);
                    } else {
                        changed.put(position, state);
                    }
                }
            }
        }
        return any ? new FakeChunk(chunk.key(), changed) : chunk;
    }

    @Override
    public int stateAt(FakeChunk chunk, BlockPosition position) {
        return chunk.blocks().getOrDefault(position, AIR);
    }

    @Override
    public int air() {
        return AIR;
    }
}
