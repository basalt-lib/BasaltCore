package tlm.dev.basalt.core.interaction;

import tlm.dev.basalt.core.position.BlockPosition;

import java.util.UUID;

public interface InteractionReplies {

    void block(UUID player, BlockPosition position, int state);

    void resyncInventory(UUID player);

    void acknowledge(UUID player, int sequence);
}
