package tlm.dev.basalt.api;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Thrown by {@link Basalt#createWorld(Plugin, ChunkArea)} when a real chunk of the area holds a block other than air.
 * A virtual world stands where the real world is air: a real block there would push or hurt its viewers.
 * The message names the world, the chunk and the first block found.
 */
@ApiStatus.Experimental
public final class HostAreaException extends RuntimeException {

    /**
     * The name of the real world of the area.
     */
    private final String worldName;

    /**
     * The x of the first real chunk found holding a block.
     */
    private final int chunkX;

    /**
     * The z of the first real chunk found holding a block.
     */
    private final int chunkZ;

    /**
     * Creates the exception. BasaltCore throws it, plugins do not.
     *
     * @param message the message naming the world, the chunk and the first block found
     * @param worldName the name of the real world
     * @param chunkX the x of the chunk that holds a block
     * @param chunkZ the z of the chunk that holds a block
     */
    @ApiStatus.Internal
    public HostAreaException(@NotNull String message, @NotNull String worldName, int chunkX, int chunkZ) {
        super(message);
        this.worldName = worldName;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    /**
     * Returns the name of the real world of the area.
     *
     * @return the world name
     */
    public @NotNull String worldName() {
        return worldName;
    }

    /**
     * Returns the x of the first real chunk found holding a block.
     *
     * @return the chunk x
     */
    public int chunkX() {
        return chunkX;
    }

    /**
     * Returns the z of the first real chunk found holding a block.
     *
     * @return the chunk z
     */
    public int chunkZ() {
        return chunkZ;
    }
}
