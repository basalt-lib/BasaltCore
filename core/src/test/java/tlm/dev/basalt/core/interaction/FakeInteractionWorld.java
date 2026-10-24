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
    static final float AIRBORNE_SLOWDOWN = 5.0F;

    final Map<UUID, InteractionContext> contexts = new HashMap<>();
    final Map<Integer, Float> progress = new HashMap<>();
    final Set<BlockPosition> occupied = new HashSet<>();

    void stand(UUID player, GameMode mode, long tick, OptionalInt heldBlock) {
        PlayerStance stance = new PlayerStance(new Point(1029.5, -58.38, 1027.5), new Point(1029.5, -60.0, 1027.5), true, new Reach(4.5, 3.0));
        contexts.put(player, new InteractionContext(tick, mode, stance, new HeldItem(heldBlock, HeldItem.MIN_DAMAGE)));
    }

    void eyes(UUID player, Point eyes) {
        InteractionContext current = contexts.get(player);
        PlayerStance stance = current.stance();
        put(player, current, new PlayerStance(eyes, stance.feet(), stance.onGround(), stance.reach()));
    }

    void airborne(UUID player, Point feet) {
        InteractionContext current = contexts.get(player);
        put(player, current, new PlayerStance(current.stance().eyes(), feet, false, current.stance().reach()));
    }

    void holding(UUID player, double attackDamage) {
        InteractionContext current = contexts.get(player);
        contexts.put(player, new InteractionContext(current.tick(), current.mode(), current.stance(), new HeldItem(current.held().block(), attackDamage)));
    }

    void at(UUID player, long tick) {
        InteractionContext current = contexts.get(player);
        contexts.put(player, new InteractionContext(tick, current.mode(), current.stance(), current.held()));
    }

    private void put(UUID player, InteractionContext current, PlayerStance stance) {
        contexts.put(player, new InteractionContext(current.tick(), current.mode(), stance, current.held()));
    }

    @Override
    public Optional<InteractionContext> context(UUID player, Hand hand) {
        return Optional.ofNullable(contexts.get(player));
    }

    @Override
    public float destroyProgress(UUID player, BlockPosition position, int state, boolean standing) {
        float perTick = progress.getOrDefault(state, 0.0F);
        return standing ? perTick : perTick / AIRBORNE_SLOWDOWN;
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
    public boolean supports(int state) {
        return state != AIR && state != SHORT_GRASS;
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
