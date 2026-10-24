package tlm.dev.basalt.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;

/**
 * The Basalt service, registered in the Bukkit services manager while BasaltCore is enabled.
 * Get it with {@link BasaltProvider#get()}, never implement it.
 * Every method but {@link #version()} runs on the main server thread only and throws {@link IllegalStateException}
 * on another thread.
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface Basalt {

    /**
     * Returns the version of the running BasaltCore plugin.
     *
     * @return the version, for example {@code 0.1.0-SNAPSHOT}
     */
    String version();

    /**
     * Creates an empty virtual world on an area of a real world, with no viewer.
     * Loads the real chunks of the area and checks they hold only air.
     *
     * @param area the area, at most {@code world.max-chunks} chunks of the BasaltCore config
     * @return the new world
     * @throws HostAreaException if a real chunk of the area holds a block other than air
     * @throws IllegalArgumentException if the area holds more chunks than {@code world.max-chunks}
     * @throws IllegalStateException if the real world is not loaded, if {@code world.max-worlds} worlds are already open,
     *                               or if called outside the main thread
     */
    @NotNull VirtualWorld createWorld(@NotNull ChunkArea area);

    /**
     * Returns the open virtual worlds.
     *
     * @return a copy of the open worlds, in no particular order
     */
    @NotNull @Unmodifiable Collection<VirtualWorld> worlds();

    /**
     * Returns the virtual world a player views.
     *
     * @param player the player
     * @return the world the player views, or null if the player sees the real world
     */
    @Nullable VirtualWorld worldOf(@NotNull Player player);
}
