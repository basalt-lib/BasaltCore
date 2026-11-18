package tlm.dev.basalt.nms.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import tlm.dev.basalt.core.interaction.BlockRequest;
import tlm.dev.basalt.core.interaction.Hand;
import tlm.dev.basalt.core.interaction.InteractionKind;
import tlm.dev.basalt.core.position.BlockFace;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.Point;

import java.util.Optional;

public final class MojangRequests {

    private MojangRequests() {
    }

    public static Optional<BlockRequest> block(Object message) {
        return switch (message) {
            case ServerboundPlayerActionPacket action -> breaking(action);
            case ServerboundUseItemOnPacket use -> using(use);
            default -> Optional.empty();
        };
    }

    private static Optional<BlockRequest> breaking(ServerboundPlayerActionPacket packet) {
        InteractionKind kind = switch (packet.getAction()) {
            case START_DESTROY_BLOCK -> InteractionKind.START_BREAK;
            case ABORT_DESTROY_BLOCK -> InteractionKind.ABORT_BREAK;
            case STOP_DESTROY_BLOCK -> InteractionKind.STOP_BREAK;
            default -> null;
        };
        if (kind == null) {
            return Optional.empty();
        }
        return position(packet.getPos())
                .map(position -> BlockRequest.breaking(kind, position, face(packet.getDirection()), packet.getSequence()));
    }

    private static Optional<BlockRequest> using(ServerboundUseItemOnPacket packet) {
        BlockHitResult hit = packet.getHitResult();
        Vec3 location = hit.getLocation();
        Hand hand = packet.getHand() == InteractionHand.MAIN_HAND ? Hand.MAIN : Hand.OFF;
        return position(hit.getBlockPos()).map(position -> BlockRequest.using(
                position, face(hit.getDirection()), new Point(location.x, location.y, location.z), hand, packet.getSequence()));
    }

    private static Optional<BlockPosition> position(BlockPos pos) {
        if (Math.abs(pos.getX()) > BlockPosition.MAX_HORIZONTAL || Math.abs(pos.getZ()) > BlockPosition.MAX_HORIZONTAL
                || Math.abs(pos.getY()) > BlockPosition.MAX_VERTICAL) {
            return Optional.empty();
        }
        return Optional.of(new BlockPosition(pos.getX(), pos.getY(), pos.getZ()));
    }

    private static BlockFace face(Direction direction) {
        return switch (direction) {
            case DOWN -> BlockFace.DOWN;
            case UP -> BlockFace.UP;
            case NORTH -> BlockFace.NORTH;
            case SOUTH -> BlockFace.SOUTH;
            case WEST -> BlockFace.WEST;
            case EAST -> BlockFace.EAST;
        };
    }
}
