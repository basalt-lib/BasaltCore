package tlm.dev.basalt.api;

import org.jetbrains.annotations.ApiStatus;

/**
 * The Basalt service, registered in the Bukkit services manager while BasaltCore is enabled.
 * Get it with {@link BasaltProvider#get()}.
 */
@ApiStatus.Experimental
public interface Basalt {

    /**
     * Returns the version of the running BasaltCore plugin.
     *
     * @return the version, for example {@code 0.1.0-SNAPSHOT}
     */
    String version();
}
