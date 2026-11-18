package tlm.dev.basalt.nms.packet;

import tlm.dev.basalt.core.interaction.BlockRequest;
import tlm.dev.basalt.core.interaction.EntityRequest;
import tlm.dev.basalt.core.interaction.InteractionInbox;
import tlm.dev.basalt.core.interaction.InteractionKind;
import tlm.dev.basalt.core.visibility.InboundFacts;
import tlm.dev.basalt.core.visibility.InboundFilter;
import tlm.dev.basalt.core.visibility.InboundKind;
import tlm.dev.basalt.core.visibility.InboundVerdict;
import tlm.dev.basalt.core.visibility.PlayerView;
import tlm.dev.basalt.nms.platform.MojangRequests;
import tlm.dev.basalt.nms.port.InboundGate;
import tlm.dev.basalt.nms.port.MojangVersion;

import java.util.Optional;
import java.util.UUID;

public final class InteractionPacketReader implements InboundGate {

    private final UUID player;
    private final PlayerView view;
    private final InteractionInbox inbox;
    private final MojangVersion version;

    public InteractionPacketReader(UUID player, PlayerView view, InteractionInbox inbox, MojangVersion version) {
        this.player = player;
        this.view = view;
        this.inbox = inbox;
        this.version = version;
    }

    @Override
    public boolean consumes(Object message) {
        Optional<EntityRequest> entity = version.entityRequest(message);
        if (entity.isPresent()) {
            return consumesEntity(entity.get());
        }
        Optional<BlockRequest> request = MojangRequests.block(message);
        if (request.isEmpty()) {
            return false;
        }
        BlockRequest read = request.get();
        InboundKind kind = read.kind() == InteractionKind.USE_ON ? InboundKind.USE_ON : InboundKind.BLOCK_ACTION;
        if (InboundFilter.decide(InboundFacts.about(kind, read.position(), read.face()), view.snapshot()) == InboundVerdict.PASS) {
            return false;
        }
        inbox.offer(player, read);
        return true;
    }

    private boolean consumesEntity(EntityRequest request) {
        if (InboundFilter.decide(InboundFacts.entity(request.entity()), view.snapshot()) == InboundVerdict.PASS) {
            return false;
        }
        inbox.offer(player, request);
        return true;
    }
}
