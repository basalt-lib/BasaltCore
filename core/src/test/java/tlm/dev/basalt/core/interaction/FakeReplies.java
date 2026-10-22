package tlm.dev.basalt.core.interaction;

import tlm.dev.basalt.core.position.BlockPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class FakeReplies implements InteractionReplies {

    final List<String> sent = new ArrayList<>();

    @Override
    public void block(UUID player, BlockPosition position, int state) {
        sent.add("block " + position.x() + " " + position.y() + " " + position.z() + " = " + state);
    }

    @Override
    public void resyncInventory(UUID player) {
        sent.add("inventory");
    }

    @Override
    public void acknowledge(UUID player, int sequence) {
        sent.add("ack " + sequence);
    }
}
