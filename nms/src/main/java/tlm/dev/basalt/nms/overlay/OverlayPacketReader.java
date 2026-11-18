package tlm.dev.basalt.nms.overlay;

import tlm.dev.basalt.core.interaction.BlockRequest;
import tlm.dev.basalt.core.interaction.EntityRequest;
import tlm.dev.basalt.core.interaction.InteractionInbox;
import tlm.dev.basalt.core.interaction.InteractionKind;
import tlm.dev.basalt.core.overlay.OverlaySnapshot;
import tlm.dev.basalt.core.overlay.OverlayView;
import tlm.dev.basalt.nms.platform.MojangLook;
import tlm.dev.basalt.nms.platform.MojangRequests;
import tlm.dev.basalt.nms.port.InboundGate;
import tlm.dev.basalt.nms.port.MojangVersion;

import java.util.Optional;
import java.util.UUID;

public final class OverlayPacketReader implements InboundGate {

    private final UUID player;
    private final OverlayView<MojangLook> view;
    private final InteractionInbox inbox;
    private final MojangVersion version;

    public OverlayPacketReader(UUID player, OverlayView<MojangLook> view, InteractionInbox inbox, MojangVersion version) {
        this.player = player;
        this.view = view;
        this.inbox = inbox;
        this.version = version;
    }

    @Override
    public boolean consumes(Object message) {
        OverlaySnapshot<MojangLook> snapshot = view.current();
        if (snapshot.isNone()) {
            return false;
        }
        Optional<EntityRequest> entity = version.entityRequest(message);
        if (entity.isPresent()) {
            if (!snapshot.holdsEntity(entity.get().entity())) {
                return false;
            }
            inbox.offer(player, entity.get());
            return true;
        }
        Optional<BlockRequest> request = MojangRequests.block(message);
        if (request.isEmpty()) {
            return false;
        }
        BlockRequest read = request.get();
        boolean claimed = read.kind() == InteractionKind.USE_ON
                ? snapshot.claimsUse(read.position(), read.face())
                : snapshot.claimsBlock(read.position());
        if (!claimed) {
            return false;
        }
        inbox.offer(player, read);
        return true;
    }
}
