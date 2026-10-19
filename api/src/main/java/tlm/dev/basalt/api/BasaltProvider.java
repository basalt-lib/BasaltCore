package tlm.dev.basalt.api;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.ApiStatus;

/**
 * Static entry point to the {@link Basalt} service.
 */
@ApiStatus.Experimental
public final class BasaltProvider {

    private BasaltProvider() {
    }

    /**
     * Returns the Basalt service from the Bukkit services manager.
     *
     * @return the running Basalt service
     * @throws IllegalStateException if BasaltCore is not enabled
     */
    public static Basalt get() {
        Basalt basalt = Bukkit.getServicesManager().load(Basalt.class);
        if (basalt == null) {
            throw new IllegalStateException("BasaltCore is not enabled");
        }
        return basalt;
    }
}
