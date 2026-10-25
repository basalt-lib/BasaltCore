package tlm.dev.basalt.plugin.interaction;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import tlm.dev.basalt.api.Basalt;
import tlm.dev.basalt.api.VirtualEntity;
import tlm.dev.basalt.api.VirtualWorld;
import tlm.dev.basalt.api.event.VirtualBlockBreakEvent;
import tlm.dev.basalt.api.event.VirtualBlockPlaceEvent;
import tlm.dev.basalt.api.event.VirtualBlockUseEvent;
import tlm.dev.basalt.api.event.VirtualEntityDamageEvent;
import tlm.dev.basalt.api.event.VirtualEntityUseEvent;
import tlm.dev.basalt.core.interaction.Hand;
import tlm.dev.basalt.core.interaction.InteractionEvents;
import tlm.dev.basalt.core.interaction.PlaceDecision;
import tlm.dev.basalt.core.position.BlockFace;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.nms.platform.MojangBlockStates;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

public final class BukkitInteractionEvents implements InteractionEvents {

    private final Basalt basalt;
    private final Server server;
    private final MojangBlockStates blocks;

    public BukkitInteractionEvents(Basalt basalt, Server server, MojangBlockStates blocks) {
        this.basalt = basalt;
        this.server = server;
        this.blocks = blocks;
    }

    @Override
    public boolean breaks(UUID player, BlockPosition position, int state) {
        return viewer(player).map(viewer -> new VirtualBlockBreakEvent(viewer.world(), viewer.player(),
                location(viewer.world(), position), blocks.dataOf(state)).callEvent()).orElse(false);
    }

    @Override
    public PlaceDecision places(UUID player, BlockPosition position, int state, Hand hand) {
        return viewer(player).map(viewer -> {
            VirtualBlockPlaceEvent event = new VirtualBlockPlaceEvent(viewer.world(), viewer.player(),
                    location(viewer.world(), position), blocks.dataOf(state), slot(hand));
            return event.callEvent() ? new PlaceDecision(true, event.consumeItem()) : PlaceDecision.REFUSED;
        }).orElse(PlaceDecision.REFUSED);
    }

    @Override
    public boolean uses(UUID player, BlockPosition position, int state, BlockFace face, Hand hand) {
        return viewer(player).map(viewer -> new VirtualBlockUseEvent(viewer.world(), viewer.player(),
                location(viewer.world(), position), blocks.dataOf(state), org.bukkit.block.BlockFace.valueOf(face.name()), slot(hand))
                .callEvent()).orElse(false);
    }

    @Override
    public OptionalDouble damages(UUID player, int entity, double damage) {
        Optional<Viewer> viewer = viewer(player);
        Optional<VirtualEntity> target = viewer.flatMap(found -> entity(found.world(), entity));
        if (target.isEmpty()) {
            return OptionalDouble.empty();
        }
        VirtualEntityDamageEvent event = new VirtualEntityDamageEvent(viewer.get().player(), target.get(), damage);
        return event.callEvent() ? OptionalDouble.of(event.damage()) : OptionalDouble.empty();
    }

    @Override
    public boolean usesEntity(UUID player, int entity, Hand hand) {
        Optional<Viewer> viewer = viewer(player);
        return viewer.flatMap(found -> entity(found.world(), entity))
                .map(target -> new VirtualEntityUseEvent(viewer.get().player(), target, slot(hand)).callEvent())
                .orElse(false);
    }

    private Optional<Viewer> viewer(UUID player) {
        Player online = server.getPlayer(player);
        VirtualWorld world = online == null ? null : basalt.worldOf(online);
        return world == null ? Optional.empty() : Optional.of(new Viewer(online, world));
    }

    private static Optional<VirtualEntity> entity(VirtualWorld world, int id) {
        return world.entities().stream().filter(entity -> entity.id() == id).findFirst();
    }

    private static Location location(VirtualWorld world, BlockPosition position) {
        return new Location(world.area().world(), position.x(), position.y(), position.z());
    }

    private static EquipmentSlot slot(Hand hand) {
        return hand == Hand.MAIN ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND;
    }

    private record Viewer(Player player, VirtualWorld world) {
    }
}
