package tlm.dev.basalt.core.interaction;

import tlm.dev.basalt.core.position.BlockPosition;

import java.util.Optional;
import java.util.UUID;

public interface InteractionWorld {

    Optional<InteractionContext> context(UUID player, Hand hand);

    float destroyProgress(UUID player, BlockPosition position, int state, boolean standing);

    int air();

    boolean isAir(int state);

    boolean supports(int state);

    boolean replaceable(int state);

    boolean unobstructed(UUID player, BlockPosition position, int state);

    void consume(UUID player, Hand hand);
}
