package tlm.dev.basalt.core.interaction;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public record InteractionOutcome(List<BlockChange> changes, Optional<Refusal> refusal, List<BlockChange> updates, boolean resyncInventory, OptionalInt ack) {

    public InteractionOutcome {
        if (refusal == null || ack == null) {
            throw new IllegalArgumentException("an outcome needs its refusal or none and its ack or none");
        }
        changes = List.copyOf(changes);
        updates = List.copyOf(updates);
        if (refusal.isPresent() && !changes.isEmpty()) {
            throw new IllegalArgumentException("a refused interaction changes no block");
        }
    }

    public static InteractionOutcome ignored(Refusal refusal) {
        return new InteractionOutcome(List.of(), Optional.of(refusal), List.of(), false, OptionalInt.empty());
    }

    public static InteractionOutcome accepted(List<BlockChange> changes, List<BlockChange> updates, boolean resyncInventory, int sequence) {
        return new InteractionOutcome(changes, Optional.empty(), updates, resyncInventory, OptionalInt.of(sequence));
    }

    public static InteractionOutcome done() {
        return new InteractionOutcome(List.of(), Optional.empty(), List.of(), false, OptionalInt.empty());
    }

    public static InteractionOutcome refused(Refusal refusal, List<BlockChange> updates, boolean resyncInventory, int sequence) {
        return new InteractionOutcome(List.of(), Optional.of(refusal), updates, resyncInventory, OptionalInt.of(sequence));
    }
}
