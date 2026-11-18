package tlm.dev.basalt.nms.overlay;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import tlm.dev.basalt.core.overlay.OverlayEntity;
import tlm.dev.basalt.nms.platform.MojangLook;

import java.util.Set;

final class OverlayPackets {

    private OverlayPackets() {
    }

    static ClientboundBlockUpdatePacket block(int x, int y, int z, int state) {
        return new ClientboundBlockUpdatePacket(new BlockPos(x, y, z), Block.stateById(state));
    }

    static ClientboundAddEntityPacket add(OverlayEntity<MojangLook> entity) {
        return new ClientboundAddEntityPacket(entity.id(), entity.uuid(), entity.position().x(), entity.position().y(), entity.position().z(),
                entity.pitch(), entity.yaw(), entity.look().type(), 0, Vec3.ZERO, entity.yaw());
    }

    static ClientboundSetEntityDataPacket data(OverlayEntity<MojangLook> entity) {
        return new ClientboundSetEntityDataPacket(entity.id(), entity.look().data());
    }

    static ClientboundTeleportEntityPacket move(OverlayEntity<MojangLook> entity) {
        Vec3 at = new Vec3(entity.position().x(), entity.position().y(), entity.position().z());
        return ClientboundTeleportEntityPacket.teleport(entity.id(),
                new PositionMoveRotation(at, Vec3.ZERO, entity.yaw(), entity.pitch()), Set.of(), false);
    }
}
