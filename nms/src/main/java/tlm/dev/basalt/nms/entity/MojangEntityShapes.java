package tlm.dev.basalt.nms.entity;

import net.minecraft.world.entity.EntityType;
import tlm.dev.basalt.core.entity.EntityShape;
import tlm.dev.basalt.core.entity.EntityShapes;

import java.util.Optional;

public final class MojangEntityShapes implements EntityShapes {

    @Override
    public Optional<EntityShape> shapeOf(String type) {
        return EntityType.byString(type).map(found -> new EntityShape(found.getWidth(), found.getHeight()));
    }
}
