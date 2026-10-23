package tlm.dev.basalt.core.entity;

import java.util.Optional;

public record EntityStatus(int entities, Optional<VirtualEntity> last, int hits, int uses) {

    public EntityStatus {
        if (entities < 0 || last == null || hits < 0 || uses < 0) {
            throw new IllegalArgumentException("a status counts from 0 and holds the last entity or none");
        }
    }
}
