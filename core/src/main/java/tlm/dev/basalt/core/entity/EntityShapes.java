package tlm.dev.basalt.core.entity;

import java.util.Optional;

public interface EntityShapes {

    Optional<EntityShape> shapeOf(String type);

    boolean creatable(String type);
}
