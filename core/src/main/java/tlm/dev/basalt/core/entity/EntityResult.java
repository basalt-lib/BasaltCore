package tlm.dev.basalt.core.entity;

import java.util.Optional;

public record EntityResult(Optional<VirtualEntity> entity, Optional<EntityRefusal> refusal) {

    public EntityResult {
        if (entity == null || refusal == null || entity.isPresent() == refusal.isPresent()) {
            throw new IllegalArgumentException("a result holds an entity or a refusal");
        }
    }

    public static EntityResult done(VirtualEntity entity) {
        return new EntityResult(Optional.of(entity), Optional.empty());
    }

    public static EntityResult refused(EntityRefusal refusal) {
        return new EntityResult(Optional.empty(), Optional.of(refusal));
    }
}
