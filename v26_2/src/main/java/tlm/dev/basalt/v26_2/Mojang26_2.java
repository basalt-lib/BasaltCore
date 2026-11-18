package tlm.dev.basalt.v26_2;

import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import org.bukkit.Server;
import org.bukkit.craftbukkit.CraftServer;
import tlm.dev.basalt.core.chunk.SectionHeader;
import tlm.dev.basalt.core.interaction.EntityRequest;
import tlm.dev.basalt.core.interaction.Hand;
import tlm.dev.basalt.nms.port.MojangVersion;

import java.util.Optional;
import java.util.function.IntSupplier;

public final class Mojang26_2 implements MojangVersion {

    @Override
    public SectionHeader sectionHeader() {
        return SectionHeader.BLOCK_AND_FLUID_COUNTS;
    }

    @Override
    public IntSupplier entityIds(Server server) {
        ServerLevel overworld = ((CraftServer) server).getServer().overworld();
        return overworld::getNextEntityId;
    }

    @Override
    public Optional<EntityRequest> entityRequest(Object message) {
        return switch (message) {
            case ServerboundAttackPacket attack -> Optional.of(EntityRequest.attack(attack.entityId()));
            case ServerboundInteractPacket interact -> Optional.of(EntityRequest.interact(interact.entityId(), hand(interact.hand())));
            default -> Optional.empty();
        };
    }

    private static Hand hand(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? Hand.MAIN : Hand.OFF;
    }
}
