package tlm.dev.basalt.core.visibility;

import tlm.dev.basalt.core.position.ChunkKey;

public record PacketFacts(PacketKind kind, ChunkKey chunk) {

    private static final PacketFacts UNRELATED = new PacketFacts(PacketKind.OTHER, null);

    public PacketFacts {
        if (kind == null) {
            throw new IllegalArgumentException("packet kind is missing");
        }
        if ((kind == PacketKind.OTHER) != (chunk == null)) {
            throw new IllegalArgumentException(kind + " needs " + (chunk == null ? "a chunk" : "no chunk"));
        }
    }

    public static PacketFacts about(PacketKind kind, ChunkKey chunk) {
        return new PacketFacts(kind, chunk);
    }

    public static PacketFacts unrelated() {
        return UNRELATED;
    }
}
