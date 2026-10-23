package tlm.dev.basalt.core.world;

import java.util.Optional;
import java.util.UUID;

public sealed interface Creation {

    record Created(UUID world) implements Creation {

        public Created {
            if (world == null) {
                throw new IllegalArgumentException("a created world has an id");
            }
        }
    }

    record Refused(CreationRefusal reason, Optional<RealBlock> cause) implements Creation {

        public Refused {
            if (reason == null || cause == null || cause.isPresent() != (reason == CreationRefusal.REAL_BLOCKS)) {
                throw new IllegalArgumentException("only a refusal for real blocks names the block found");
            }
        }
    }
}
