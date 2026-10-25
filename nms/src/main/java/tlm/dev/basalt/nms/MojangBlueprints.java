package tlm.dev.basalt.nms;

import org.bukkit.Server;
import org.bukkit.World;
import tlm.dev.basalt.core.blueprint.BlockNames;
import tlm.dev.basalt.core.blueprint.RealBlocks;
import tlm.dev.basalt.nms.blueprint.MojangBlockNames;
import tlm.dev.basalt.nms.blueprint.MojangRealBlocks;
import tlm.dev.basalt.nms.blueprint.MojangStructures;

import java.io.IOException;
import java.nio.file.Path;

public final class MojangBlueprints {

    private MojangBlueprints() {
    }

    public static BlockNames names() {
        return new MojangBlockNames();
    }

    public static RealBlocks realBlocks(Server server) {
        return new MojangRealBlocks(server);
    }

    public static boolean placeReal(World world, Path file, long maxBytes, int x, int y, int z) throws IOException {
        return MojangStructures.placeReal(world, file, maxBytes, x, y, z);
    }
}
