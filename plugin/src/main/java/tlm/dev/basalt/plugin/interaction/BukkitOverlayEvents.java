package tlm.dev.basalt.plugin.interaction;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import tlm.dev.basalt.api.Basalt;
import tlm.dev.basalt.api.Overlay;
import tlm.dev.basalt.api.OverlayEntity;
import tlm.dev.basalt.api.event.OverlayBlockBreakEvent;
import tlm.dev.basalt.api.event.OverlayBlockUseEvent;
import tlm.dev.basalt.api.event.OverlayEntityUseEvent;
import tlm.dev.basalt.core.interaction.Hand;
import tlm.dev.basalt.core.interaction.InteractionEvents;
import tlm.dev.basalt.core.interaction.PlaceDecision;
import tlm.dev.basalt.core.position.BlockFace;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.nms.platform.MojangBlockStates;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

public final class BukkitOverlayEvents implements InteractionEvents {

    private final Basalt basalt;
    private final Server server;
    private final MojangBlockStates blocks;

    public BukkitOverlayEvents(Basalt basalt, Server server, MojangBlockStates blocks) {
        this.basalt = basalt;
        this.server = server;
        this.blocks = blocks;
    }

    @Override
    public boolean breaks(UUID player, BlockPosition position, int state) {
        return viewer(player).map(viewer -> new OverlayBlockBreakEvent(viewer.overlay(), viewer.player(),
                location(viewer.overlay(), position), blocks.dataOf(state)).callEvent()).orElse(false);
    }

    @Override
    public PlaceDecision places(UUID player, BlockPosition position, int state, Hand hand) {
        return PlaceDecision.REFUSED;
    }

    @Override
    public boolean uses(UUID player, BlockPosition position, int state, BlockFace face, Hand hand) {
        Optional<Viewer> viewer = viewer(player);
        viewer.ifPresent(found -> new OverlayBlockUseEvent(found.overlay(), found.player(), location(found.overlay(), position),
                blocks.dataOf(state), org.bukkit.block.BlockFace.valueOf(face.name()), slot(hand)).callEvent());
        return viewer.isPresent();
    }

    @Override
    public OptionalDouble damages(UUID player, int entity, double damage) {
        return OptionalDouble.empty();
    }

    @Override
    public boolean usesEntity(UUID player, int entity, Hand hand) {
        Optional<Viewer> viewer = viewer(player);
        Optional<OverlayEntity> target = viewer.flatMap(found -> found.overlay().entities().stream()
                .filter(candidate -> candidate.id() == entity).findFirst());
        target.ifPresent(found -> new OverlayEntityUseEvent(viewer.get().player(), found, slot(hand)).callEvent());
        return target.isPresent();
    }

    private Optional<Viewer> viewer(UUID player) {
        Player online = server.getPlayer(player);
        Overlay overlay = online == null ? null : basalt.overlayOf(online);
        return overlay == null ? Optional.empty() : Optional.of(new Viewer(online, overlay));
    }

    private static Location location(Overlay overlay, BlockPosition position) {
        return new Location(overlay.area().world(), position.x(), position.y(), position.z());
    }

    private static EquipmentSlot slot(Hand hand) {
        return hand == Hand.MAIN ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND;
    }

    private record Viewer(Player player, Overlay overlay) {
    }
}
