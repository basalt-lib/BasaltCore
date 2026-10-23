package tlm.dev.basalt.core.visibility;

import tlm.dev.basalt.core.position.ChunkKey;

import java.util.Set;

public record ViewSnapshot<T>(ChunkKey virtualChunk, T virtualPayload, Set<Integer> virtualEntities) {

    public ViewSnapshot {
        if ((virtualChunk == null) != (virtualPayload == null)) {
            throw new IllegalArgumentException("a virtual chunk and its payload go together");
        }
        virtualEntities = Set.copyOf(virtualEntities);
        if (virtualChunk == null && !virtualEntities.isEmpty()) {
            throw new IllegalArgumentException("virtual entities live in a virtual view");
        }
    }

    public static <T> ViewSnapshot<T> real() {
        return new ViewSnapshot<>(null, null, Set.of());
    }

    public static <T> ViewSnapshot<T> virtual(ChunkKey chunk, T payload) {
        if (chunk == null || payload == null) {
            throw new IllegalArgumentException("a virtual view needs a chunk and a payload");
        }
        return new ViewSnapshot<>(chunk, payload, Set.of());
    }

    public ViewSnapshot<T> withPayload(T payload) {
        return new ViewSnapshot<>(virtualChunk, payload, virtualEntities);
    }

    public ViewSnapshot<T> withEntities(Set<Integer> ids) {
        return new ViewSnapshot<>(virtualChunk, virtualPayload, ids);
    }

    public boolean isVirtual() {
        return virtualChunk != null;
    }

    public boolean hides(ChunkKey chunk) {
        return virtualChunk != null && virtualChunk.equals(chunk);
    }

    public boolean holdsEntity(int id) {
        return virtualEntities.contains(id);
    }
}
