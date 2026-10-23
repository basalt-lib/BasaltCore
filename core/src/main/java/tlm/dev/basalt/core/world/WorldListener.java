package tlm.dev.basalt.core.world;

import java.util.UUID;

public interface WorldListener {

    void emptied(UUID world);

    void closed(UUID world);
}
