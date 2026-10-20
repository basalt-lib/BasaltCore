package tlm.dev.basalt.core.visibility;

import tlm.dev.basalt.core.position.ChunkKey;

public record ViewSnapshot<T>(ChunkKey virtualChunk, T virtualPayload) {

    public ViewSnapshot {
        if ((virtualChunk == null) != (virtualPayload == null)) {
            throw new IllegalArgumentException("a virtual chunk and its payload go together");
        }
    }

    public static <T> ViewSnapshot<T> real() {
        return new ViewSnapshot<>(null, null);
    }

    public static <T> ViewSnapshot<T> virtual(ChunkKey chunk, T payload) {
        if (chunk == null || payload == null) {
            throw new IllegalArgumentException("a virtual view needs a chunk and a payload");
        }
        return new ViewSnapshot<>(chunk, payload);
    }

    public boolean isVirtual() {
        return virtualChunk != null;
    }

    public boolean hides(ChunkKey chunk) {
        return virtualChunk != null && virtualChunk.equals(chunk);
    }
}
