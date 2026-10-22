package tlm.dev.basalt.core.interaction;

import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.ChunkKey;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

final class FakePlayerBlocks implements PlayerBlocks {

    static final ChunkKey VIRTUAL = new ChunkKey(64, 64);

    private final Map<UUID, Map<BlockPosition, Integer>> copies = new HashMap<>();

    void put(UUID player, BlockPosition position, int state) {
        copies.computeIfAbsent(player, id -> new HashMap<>()).put(position, state);
    }

    @Override
    public OptionalInt stateAt(UUID player, BlockPosition position) {
        if (!position.chunk().equals(VIRTUAL) || position.y() < -64 || position.y() >= 320) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(copies.getOrDefault(player, Map.of()).getOrDefault(position, FakeInteractionWorld.AIR));
    }

    @Override
    public boolean set(UUID player, BlockPosition position, int state) {
        if (stateAt(player, position).isEmpty()) {
            return false;
        }
        put(player, position, state);
        return true;
    }
}
