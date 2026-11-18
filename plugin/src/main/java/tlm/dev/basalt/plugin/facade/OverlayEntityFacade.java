package tlm.dev.basalt.plugin.facade;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import tlm.dev.basalt.api.Overlay;
import tlm.dev.basalt.api.OverlayEntity;
import tlm.dev.basalt.core.position.Point;
import tlm.dev.basalt.nms.platform.MojangLook;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class OverlayEntityFacade implements OverlayEntity {

    private final OverlayFacade overlay;
    private final int id;
    private final UUID uuid;
    private final String type;
    private final FacadeTools tools;

    OverlayEntityFacade(OverlayFacade overlay, tlm.dev.basalt.core.overlay.OverlayEntity<MojangLook> entity, FacadeTools tools) {
        this.overlay = overlay;
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
    public Overlay overlay() {
        tools.checkThread();
        return overlay;
    }

    @Override
    public Location location() {
        tlm.dev.basalt.core.overlay.OverlayEntity<MojangLook> entity = current();
        return new Location(overlay.area().world(), entity.position().x(), entity.position().y(), entity.position().z(),
                entity.yaw(), entity.pitch());
    }

    @Override
    public void teleport(Location location) {
        current();
        Point to = overlay.pointOf(location);
        changed(tools.overlays().moveEntity(overlay.id(), id, to, location.getYaw(), location.getPitch()), to.x() + " " + to.y() + " " + to.z());
    }

    @Override
    public ItemStack item() {
        return current().look().item().orElse(null);
    }

    @Override
    public void setItem(ItemStack item) {
        MojangLook look = current().look();
        if (!tools.content().looks().showsItem(type)) {
            throw new IllegalStateException("a " + type + " shows no item, only an item or an item display does");
        }
        relook(Optional.ofNullable(item), look.name());
    }

    @Override
    public Component customName() {
        return current().look().name().orElse(null);
    }

    @Override
    public void setCustomName(Component name) {
        MojangLook look = current().look();
        relook(look.item(), Optional.ofNullable(name));
    }

    @Override
    public void remove() {
        tools.checkThread();
        tools.overlays().removeEntity(overlay.id(), id);
    }

    @Override
    public boolean isRemoved() {
        tools.checkThread();
        return tools.overlays().entity(overlay.id(), id).isEmpty();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof OverlayEntityFacade entity && entity.id == id && entity.overlay.equals(overlay);
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id) * 31 + overlay.hashCode();
    }

    private void relook(Optional<ItemStack> item, Optional<Component> name) {
        MojangLook look = tools.content().looks().look(overlay.area().world(), type, item, name)
                .orElseThrow(() -> new IllegalStateException(type + " cannot be summoned any more"));
        changed(tools.overlays().changeLook(overlay.id(), id, look), "");
    }

    private void changed(tlm.dev.basalt.core.overlay.OverlayEdit result, String where) {
        switch (result) {
            case CHANGED, UNCHANGED -> {
            }
            case OUTSIDE -> throw overlay.outside(where);
            case NOT_FOUND -> throw removed();
            case TOO_MANY, CLOSED -> throw overlay.closed();
        }
    }

    private tlm.dev.basalt.core.overlay.OverlayEntity<MojangLook> current() {
        tools.checkThread();
        return tools.overlays().entity(overlay.id(), id).orElseThrow(this::removed);
    }

    private IllegalStateException removed() {
        return new IllegalStateException("the overlay entity " + id + " is removed");
    }
}
