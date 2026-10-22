package tlm.dev.basalt.core.interaction;

import tlm.dev.basalt.core.position.BlockPosition;

import java.util.Optional;
import java.util.UUID;

public interface InteractionWorld {

    Optional<InteractionContext> context(UUID player, Hand hand);

    float destroyProgress(UUID player, BlockPosition position, int state);

    int air();

    boolean isAir(int state);

    boolean replaceable(int state);

    boolean unobstructed(UUID player, BlockPosition position, int state);
}
