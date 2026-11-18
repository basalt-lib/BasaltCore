package tlm.dev.basalt.nms.port;

import org.bukkit.Server;
import tlm.dev.basalt.core.chunk.SectionHeader;
import tlm.dev.basalt.core.interaction.EntityRequest;

import java.util.Optional;
import java.util.function.IntSupplier;

public interface MojangVersion {

    SectionHeader sectionHeader();

    IntSupplier entityIds(Server server);

    Optional<EntityRequest> entityRequest(Object message);
}
