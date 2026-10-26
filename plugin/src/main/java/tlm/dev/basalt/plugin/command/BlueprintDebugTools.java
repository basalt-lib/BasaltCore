package tlm.dev.basalt.plugin.command;

import org.bukkit.World;
import tlm.dev.basalt.api.Blueprint;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.ToLongFunction;

public record BlueprintDebugTools(Path folder, RealPlacer real, ToLongFunction<Blueprint> bytes) {

    public BlueprintDebugTools {
        if (folder == null || real == null || bytes == null) {
            throw new IllegalArgumentException("the blueprint tools need the folder, the real placer and the byte count");
        }
    }

    @FunctionalInterface
    public interface RealPlacer {

        boolean place(World world, Path file, int x, int y, int z) throws IOException;
    }
}
