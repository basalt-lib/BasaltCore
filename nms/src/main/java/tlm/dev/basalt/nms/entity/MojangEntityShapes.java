package tlm.dev.basalt.nms.entity;

import net.minecraft.world.entity.EntityType;
import org.bukkit.Server;
import org.bukkit.craftbukkit.CraftServer;
import tlm.dev.basalt.core.entity.EntityShape;
import tlm.dev.basalt.core.entity.EntityShapes;

import java.util.Optional;

public final class MojangEntityShapes implements EntityShapes {

    private final Server server;

    public MojangEntityShapes(Server server) {
        this.server = server;
    }

    @Override
    public Optional<EntityShape> shapeOf(String type) {
        return EntityType.byString(type).map(found -> new EntityShape(found.getWidth(), found.getHeight()));
    }

    @Override
    public boolean creatable(String type) {
        return EntityType.byString(type)
                .filter(EntityType::canSummon)
                .filter(found -> found.isEnabled(((CraftServer) server).getServer().overworld().enabledFeatures()))
                .isPresent();
    }
}
