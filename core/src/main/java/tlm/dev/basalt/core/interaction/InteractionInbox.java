package tlm.dev.basalt.core.interaction;

import java.util.UUID;

public interface InteractionInbox {

    boolean offer(UUID player, InteractionRequest request);
}
