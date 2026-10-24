package tlm.dev.basalt.plugin.facade;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.EntityType;
import tlm.dev.basalt.api.VirtualEntity;
import tlm.dev.basalt.api.VirtualWorld;
import tlm.dev.basalt.core.entity.EntityResult;
import tlm.dev.basalt.core.entity.Rotation;
import tlm.dev.basalt.core.position.Point;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class EntityFacade implements VirtualEntity {

    private final WorldFacade world;
    private final int id;
    private final UUID uuid;
    private final String type;
    private final FacadeTools tools;

    EntityFacade(WorldFacade world, tlm.dev.basalt.core.entity.VirtualEntity entity, FacadeTools tools) {
        this.world = world;
        this.id = entity.id();
        this.uuid = entity.uuid();
        this.type = entity.type();
        this.tools = tools;
    }

    @Override
    public int id() {
        tools.checkThread();
        return id;
    }

    @Override
    public UUID uuid() {
        tools.checkThread();
        return uuid;
    }

    @Override
    public EntityType type() {
        tools.checkThread();
        return Registry.ENTITY_TYPE.getOrThrow(Objects.requireNonNull(NamespacedKey.fromString(type)));
    }

    @Override
    public VirtualWorld world() {
        tools.checkThread();
        return world;
    }

    @Override
    public Location location() {
        tlm.dev.basalt.core.entity.VirtualEntity entity = current();
        return new Location(world.area().world(), entity.position().x(), entity.position().y(), entity.position().z(),
                entity.rotation().yaw(), entity.rotation().pitch());
    }

    @Override
    public void teleport(Location location) {
        tools.checkThread();
        Point to = world.pointOf(location);
        changed(tools.entities().teleport(world.id(), id, to, new Rotation(location.getYaw(), location.getPitch())));
    }

    @Override
    public Component customName() {
        return current().looks().name().map(GsonComponentSerializer.gson()::deserialize).orElse(null);
    }

    @Override
    public void setCustomName(Component name) {
        tools.checkThread();
        Optional<String> json = Optional.ofNullable(name).map(GsonComponentSerializer.gson()::serialize);
        changed(tools.entities().rename(world.id(), id, json));
    }

    @Override
    public double health() {
        return current().health();
    }

    @Override
    public void setHealth(double health) {
        tools.checkThread();
        changed(tools.entities().heal(world.id(), id, health));
    }

    @Override
    public double maxHealth() {
        return current().life().maxHealth();
    }

    @Override
    public void setMaxHealth(double maxHealth) {
        tools.checkThread();
        changed(tools.entities().limitHealth(world.id(), id, maxHealth));
    }

    @Override
    public boolean isSilent() {
        return current().looks().silent();
    }

    @Override
    public void setSilent(boolean silent) {
        tools.checkThread();
        changed(tools.entities().silence(world.id(), id, silent));
    }

    @Override
    public void remove() {
        tools.checkThread();
        tools.entities().remove(world.id(), id);
    }

    @Override
    public boolean isRemoved() {
        tools.checkThread();
        return tools.entities().entity(world.id(), id).isEmpty();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof EntityFacade entity && entity.id == id && entity.world.equals(world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, id);
    }

    @Override
    public String toString() {
        return "VirtualEntity #" + id + " of " + world;
    }

    private tlm.dev.basalt.core.entity.VirtualEntity current() {
        tools.checkThread();
        return tools.entities().entity(world.id(), id).orElseThrow(this::removed);
    }

    private void changed(EntityResult result) {
        result.refusal().ifPresent(refusal -> {
            throw switch (refusal) {
                case GONE, NO_WORLD -> removed();
                case DYING -> new IllegalStateException("the virtual entity #" + id + " is dying");
                case OUTSIDE_AREA -> new IllegalArgumentException("the location is outside the area of the virtual world " + world.id());
                case TOO_MANY -> new IllegalStateException("the virtual world " + world.id() + " holds too many entities");
            };
        });
    }

    private IllegalStateException removed() {
        return new IllegalStateException("the virtual entity #" + id + " is removed");
    }
}
