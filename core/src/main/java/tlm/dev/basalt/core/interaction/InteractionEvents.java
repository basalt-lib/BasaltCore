package tlm.dev.basalt.core.interaction;

import tlm.dev.basalt.core.position.BlockFace;
import tlm.dev.basalt.core.position.BlockPosition;

import java.util.OptionalDouble;
import java.util.UUID;

public interface InteractionEvents {

    boolean breaks(UUID player, BlockPosition position, int state);

    PlaceDecision places(UUID player, BlockPosition position, int state, Hand hand);

    boolean uses(UUID player, BlockPosition position, int state, BlockFace face, Hand hand);

    OptionalDouble damages(UUID player, int entity, double damage);

    boolean usesEntity(UUID player, int entity, Hand hand);
}
