package tlm.dev.basalt.core.visibility;

import tlm.dev.basalt.core.position.ChunkKey;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;

public final class ChunkRequests {

    private final Queue<Request> queue = new ConcurrentLinkedQueue<>();

    public void offer(UUID player, ChunkKey chunk) {
        queue.offer(new Request(player, chunk));
    }

    void drain(BiConsumer<UUID, ChunkKey> handler) {
        Request request;
        while ((request = queue.poll()) != null) {
            handler.accept(request.player(), request.chunk());
        }
    }

    private record Request(UUID player, ChunkKey chunk) {
    }
}
