package tlm.dev.basalt.nms.platform;

import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import tlm.dev.basalt.core.platform.GameVersion;
import tlm.dev.basalt.core.platform.GameVersionSource;

public final class MojangGameVersionSource implements GameVersionSource {

    @Override
    public GameVersion current() {
        WorldVersion version = SharedConstants.getCurrentVersion();
        return new GameVersion(version.name(), version.protocolVersion());
    }
}
