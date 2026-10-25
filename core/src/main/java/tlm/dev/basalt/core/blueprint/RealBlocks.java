package tlm.dev.basalt.core.blueprint;

import tlm.dev.basalt.core.position.BlockBox;

import java.util.UUID;

public interface RealBlocks {

    int[] states(UUID world, BlockBox box);
}
