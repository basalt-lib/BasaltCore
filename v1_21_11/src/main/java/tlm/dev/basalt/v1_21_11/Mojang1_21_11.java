package tlm.dev.basalt.v1_21_11;

import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Server;
import tlm.dev.basalt.core.chunk.SectionHeader;
import tlm.dev.basalt.core.interaction.EntityRequest;
import tlm.dev.basalt.core.interaction.Hand;
import tlm.dev.basalt.core.position.Point;
import tlm.dev.basalt.nms.port.MojangVersion;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntSupplier;

public final class Mojang1_21_11 implements MojangVersion {

    @Override
    public SectionHeader sectionHeader() {
        return SectionHeader.BLOCK_COUNT;
    }

    @Override
    public IntSupplier entityIds(Server server) {
        return Entity::nextEntityId;
    }

    @Override
    public Optional<EntityRequest> entityRequest(Object message) {
        if (!(message instanceof ServerboundInteractPacket packet)) {
            return Optional.empty();
        }
        int id = packet.getEntityId();
        AtomicReference<EntityRequest> read = new AtomicReference<>(EntityRequest.attack(id));
        packet.dispatch(new ServerboundInteractPacket.Handler() {
            @Override
            public void onInteraction(InteractionHand hand) {
                read.set(EntityRequest.interact(id, hand(hand)));
            }

            @Override
            public void onInteraction(InteractionHand hand, Vec3 location) {
                read.set(EntityRequest.interactAt(id, hand(hand), new Point(location.x, location.y, location.z)));
            }

            @Override
            public void onAttack() {
                read.set(EntityRequest.attack(id));
            }
        });
        return Optional.of(read.get());
    }

    private static Hand hand(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? Hand.MAIN : Hand.OFF;
    }
}
