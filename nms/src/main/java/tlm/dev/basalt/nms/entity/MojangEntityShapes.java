package tlm.dev.basalt.nms.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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
        return entityType(type).map(found -> new EntityShape(found.getWidth(), found.getHeight()));
    }

    @Override
    public boolean creatable(String type) {
        return entityType(type)
                .filter(EntityType::canSummon)
                .filter(found -> found.isEnabled(((CraftServer) server).getServer().overworld().enabledFeatures()))
                .isPresent();
    }

    private static Optional<EntityType<?>> entityType(String type) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.tryParse(type));
    }
}
