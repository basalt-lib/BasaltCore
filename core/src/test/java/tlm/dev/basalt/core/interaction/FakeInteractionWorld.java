package tlm.dev.basalt.core.interaction;

import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.Point;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

final class FakeInteractionWorld implements InteractionWorld {

    static final int AIR = 0;
    static final int SHORT_GRASS = 1;
    static final int BASALT = 2;
    static final int STONE = 3;

    final Map<UUID, InteractionContext> contexts = new HashMap<>();
    final Map<Integer, Float> progress = new HashMap<>();
    final Set<BlockPosition> occupied = new HashSet<>();

    void stand(UUID player, GameMode mode, long tick, OptionalInt heldBlock) {
        contexts.put(player, new InteractionContext(tick, mode, new Point(1029.5, -58.38, 1027.5), 4.5, heldBlock));
    }

    void eyes(UUID player, Point eyes) {
        InteractionContext current = contexts.get(player);
        contexts.put(player, new InteractionContext(current.tick(), current.mode(), eyes, current.reach(), current.heldBlock()));
    }

    void at(UUID player, long tick) {
        InteractionContext current = contexts.get(player);
        contexts.put(player, new InteractionContext(tick, current.mode(), current.eyes(), current.reach(), current.heldBlock()));
    }

    @Override
    public Optional<InteractionContext> context(UUID player, Hand hand) {
        return Optional.ofNullable(contexts.get(player));
    }

    @Override
    public float destroyProgress(UUID player, BlockPosition position, int state) {
        return progress.getOrDefault(state, 0.0F);
    }

    @Override
    public int air() {
        return AIR;
    }

    @Override
    public boolean isAir(int state) {
        return state == AIR;
    }

    @Override
    public boolean replaceable(int state) {
        return state == AIR || state == SHORT_GRASS;
    }

    @Override
    public boolean unobstructed(UUID player, BlockPosition position, int state) {
        return !occupied.contains(position);
    }
}
