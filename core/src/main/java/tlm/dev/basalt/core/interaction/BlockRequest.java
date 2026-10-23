package tlm.dev.basalt.core.interaction;

import tlm.dev.basalt.core.position.BlockFace;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.Point;

import java.util.Optional;

public record BlockRequest(InteractionKind kind, BlockPosition position, BlockFace face, Optional<Point> hit, Hand hand, int sequence) implements InteractionRequest {

    public BlockRequest {
        if (kind == null || position == null || face == null || hit == null || hand == null) {
            throw new IllegalArgumentException("a request needs a kind, a block, a face, a hand and its hit point or none");
        }
        if ((kind == InteractionKind.USE_ON) != hit.isPresent()) {
            throw new IllegalArgumentException(kind == InteractionKind.USE_ON ? "a use needs its hit point" : "a break has no hit point");
        }
    }

    public static BlockRequest breaking(InteractionKind kind, BlockPosition position, BlockFace face, int sequence) {
        return new BlockRequest(kind, position, face, Optional.empty(), Hand.MAIN, sequence);
    }

    public static BlockRequest using(BlockPosition position, BlockFace face, Point hit, Hand hand, int sequence) {
        return new BlockRequest(InteractionKind.USE_ON, position, face, Optional.of(hit), hand, sequence);
    }
}
