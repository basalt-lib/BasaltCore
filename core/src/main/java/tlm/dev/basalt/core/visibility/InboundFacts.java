package tlm.dev.basalt.core.visibility;

import tlm.dev.basalt.core.position.BlockFace;
import tlm.dev.basalt.core.position.BlockPosition;

public record InboundFacts(InboundKind kind, BlockPosition clicked, BlockFace face, int entity) {

    private static final InboundFacts UNRELATED = new InboundFacts(InboundKind.OTHER, null, null, 0);

    public InboundFacts {
        if (kind == null) {
            throw new IllegalArgumentException("inbound kind is missing");
        }
        boolean aboutBlock = kind == InboundKind.BLOCK_ACTION || kind == InboundKind.USE_ON;
        if (aboutBlock != (clicked != null) || aboutBlock != (face != null)) {
            throw new IllegalArgumentException(kind + (aboutBlock ? " needs a block and a face" : " carries no block"));
        }
        if (kind != InboundKind.ENTITY && entity != 0) {
            throw new IllegalArgumentException(kind + " carries no entity");
        }
    }

    public static InboundFacts about(InboundKind kind, BlockPosition clicked, BlockFace face) {
        return new InboundFacts(kind, clicked, face, 0);
    }

    public static InboundFacts entity(int id) {
        return new InboundFacts(InboundKind.ENTITY, null, null, id);
    }

    public static InboundFacts unrelated() {
        return UNRELATED;
    }
}
