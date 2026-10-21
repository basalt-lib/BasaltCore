package tlm.dev.basalt.core.visibility;

import tlm.dev.basalt.core.position.BlockFace;
import tlm.dev.basalt.core.position.BlockPosition;

public record InboundFacts(InboundKind kind, BlockPosition clicked, BlockFace face) {

    private static final InboundFacts UNRELATED = new InboundFacts(InboundKind.OTHER, null, null);

    public InboundFacts {
        if (kind == null) {
            throw new IllegalArgumentException("inbound kind is missing");
        }
        if ((kind == InboundKind.OTHER) != (clicked == null) || (kind == InboundKind.OTHER) != (face == null)) {
            throw new IllegalArgumentException(kind + (kind == InboundKind.OTHER ? " carries no block" : " needs a block and a face"));
        }
    }

    public static InboundFacts about(InboundKind kind, BlockPosition clicked, BlockFace face) {
        return new InboundFacts(kind, clicked, face);
    }

    public static InboundFacts unrelated() {
        return UNRELATED;
    }
}
