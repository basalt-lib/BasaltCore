package tlm.dev.basalt.nms;

import org.bukkit.Server;
import org.bukkit.craftbukkit.CraftServer;
import tlm.dev.basalt.core.chunk.VirtualChunk;
import tlm.dev.basalt.core.world.RealChunks;
import tlm.dev.basalt.core.world.WorldChunks;
import tlm.dev.basalt.nms.packet.MojangWorldChunks;
import tlm.dev.basalt.nms.port.MojangVersion;
import tlm.dev.basalt.nms.view.MojangRealChunks;

public final class MojangWorlds {

    private MojangWorlds() {
    }

    public static WorldChunks<VirtualChunk> chunks(Server server, MojangVersion version) {
        return new MojangWorldChunks(((CraftServer) server).getServer().registryAccess(), version.sectionHeader());
    }

    public static RealChunks realChunks(Server server) {
        return new MojangRealChunks(server);
    }
}
