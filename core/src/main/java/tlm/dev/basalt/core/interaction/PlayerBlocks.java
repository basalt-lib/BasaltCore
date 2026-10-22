package tlm.dev.basalt.core.interaction;

import tlm.dev.basalt.core.position.BlockPosition;

import java.util.OptionalInt;
import java.util.UUID;

public interface PlayerBlocks {

    OptionalInt stateAt(UUID player, BlockPosition position);

    boolean set(UUID player, BlockPosition position, int state);
}
