package tlm.dev.basalt.plugin.interaction;

import tlm.dev.basalt.core.interaction.InteractionInbox;
import tlm.dev.basalt.core.interaction.InteractionRequest;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class InteractionQueues implements InteractionInbox {

    private final int capacity;
    private final Map<UUID, BlockingQueue<InteractionRequest>> queues = new ConcurrentHashMap<>();

    public InteractionQueues(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("a queue holds at least one request");
        }
        this.capacity = capacity;
    }

    @Override
    public boolean offer(UUID player, InteractionRequest request) {
        return queues.computeIfAbsent(player, id -> new ArrayBlockingQueue<>(capacity)).offer(request);
    }

    public void drainEach(BiConsumer<UUID, InteractionRequest> handler) {
        for (Map.Entry<UUID, BlockingQueue<InteractionRequest>> entry : queues.entrySet()) {
            for (int taken = 0; taken < capacity; taken++) {
                InteractionRequest request = entry.getValue().poll();
                if (request == null) {
                    break;
                }
                handler.accept(entry.getKey(), request);
            }
        }
    }

    public void forget(UUID player) {
        queues.remove(player);
    }
}
