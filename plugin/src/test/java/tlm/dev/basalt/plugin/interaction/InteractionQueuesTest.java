package tlm.dev.basalt.plugin.interaction;

import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.interaction.BlockRequest;
import tlm.dev.basalt.core.interaction.EntityRequest;
import tlm.dev.basalt.core.interaction.InteractionKind;
import tlm.dev.basalt.core.interaction.InteractionRequest;
import tlm.dev.basalt.core.position.BlockFace;
import tlm.dev.basalt.core.position.BlockPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractionQueuesTest {

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private final InteractionQueues queues = new InteractionQueues(2);
    private final List<String> handled = new ArrayList<>();

    @Test
    void dropsWhatGoesOverItsCapacity() {
        assertTrue(queues.offer(A, request(1)));
        assertTrue(queues.offer(A, request(2)));
        assertFalse(queues.offer(A, request(3)));
        assertTrue(queues.offer(B, request(1)));

        queues.drainEach((player, request) -> handled.add(player.toString().substring(35) + " " + label(request)));

        assertEquals(List.of("a 1", "a 2", "b 1"), handled.stream().sorted().toList());
    }

    @Test
    void handsEachRequestOnceInOrder() {
        queues.offer(A, request(1));
        queues.offer(A, request(2));
        queues.drainEach((player, request) -> handled.add("first " + label(request)));
        queues.offer(A, request(3));
        queues.drainEach((player, request) -> handled.add("second " + label(request)));

        assertEquals(List.of("first 1", "first 2", "second 3"), handled);
    }

    @Test
    void forgetsALeavingPlayer() {
        queues.offer(A, request(1));

        queues.forget(A);
        queues.drainEach((player, request) -> handled.add("handled"));

        assertEquals(List.of(), handled);
        assertThrows(IllegalArgumentException.class, () -> new InteractionQueues(0));
    }

    @Test
    void keepsBlockAndEntityRequestsInOneQueue() {
        queues.offer(A, request(1));
        queues.offer(A, EntityRequest.attack(1001));

        queues.drainEach((player, request) -> handled.add(label(request)));

        assertEquals(List.of("1", "attack 1001"), handled);
    }

    private static String label(InteractionRequest request) {
        return switch (request) {
            case BlockRequest block -> String.valueOf(block.sequence());
            case EntityRequest entity -> "attack " + entity.entity();
        };
    }

    private static InteractionRequest request(int sequence) {
        return BlockRequest.breaking(InteractionKind.START_BREAK, new BlockPosition(1029, -58, 1029), BlockFace.UP, sequence);
    }
}
