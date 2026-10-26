package tlm.dev.basalt.api;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;

/**
 * The Basalt service, registered in the Bukkit services manager while BasaltCore is enabled.
 * Get it with {@link BasaltProvider#get()}, never implement it.
 * Every method but {@link #version()} and the two {@code readBlueprint} methods runs on the main server thread only and
 * throws {@link IllegalStateException} on another thread.
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
     * The world belongs to its owner: Basalt closes it when the owner is disabled, before the owner's own
     * {@code onDisable} runs. A world never closes because it has no viewer: it lives until {@link VirtualWorld#close()},
     * the disabling of its owner or the stop of BasaltCore.
     *
     * @param owner the enabled plugin the world belongs to
     * @param area the area, at most {@code world.max-chunks} chunks of the BasaltCore config
     * @return the new world
     * @throws HostAreaException if a real chunk of the area holds a block other than air
     * @throws IllegalArgumentException if the area holds more chunks than {@code world.max-chunks}
     * @throws IllegalStateException if the owner is not enabled, if the real world is not loaded,
     *                               if {@code world.max-worlds} worlds are already open, or if called outside the main thread
     */
    @NotNull VirtualWorld createWorld(@NotNull Plugin owner, @NotNull ChunkArea area);

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

    /**
     * Captures the real blocks of a box into a blueprint, block states only. Loads the real chunks it needs.
     * The box covers the blocks from the floor of its minimum corner to the block before the ceiling of its maximum corner,
     * and at least one block on each axis, like {@link VirtualWorld#fill(BoundingBox, org.bukkit.block.data.BlockData)}.
     * Later changes of the real world do not change the blueprint.
     *
     * @param world the real world
     * @param box the box of blocks, in block coordinates, inside the height of the world
     * @return the captured blueprint, without voids
     * @throws IllegalArgumentException if the box leaves the height of the world or holds more blocks than
     *                                  {@code blueprint.max-blocks} of the BasaltCore config
     * @throws IllegalStateException if the world is not loaded or if called outside the main thread
     */
    @NotNull Blueprint captureBlueprint(@NotNull World world, @NotNull BoundingBox box);

    /**
     * Reads a blueprint from a structure file: a vanilla structure ({@code .nbt}) or a Sponge schematic of version 2 or 3
     * ({@code .schem}), both gzip compressed NBT, recognised by their content. Block states are read by their names on this
     * server's version, without any data upgrade: a name unknown to this server is refused. Block entity data and
     * entities of the file are ignored, as are the offset and the biomes of a schematic. Runs on any thread.
     *
     * @param file the file to read
     * @return the blueprint
     * @throws IOException if the file cannot be read
     * @throws BlueprintException if the content is not a blueprint Basalt can read, larger than {@code blueprint.max-file-bytes}
     *                            once decompressed or than {@code blueprint.max-blocks}, or names an unknown block state
     */
    @NotNull Blueprint readBlueprint(@NotNull Path file) throws IOException;

    /**
     * Reads a blueprint from a stream, as {@link #readBlueprint(Path)} does from a file. The stream is read to the end of
     * the compressed data and left open. Runs on any thread.
     *
     * @param in the stream to read
     * @return the blueprint
     * @throws IOException if the stream cannot be read
     * @throws BlueprintException if the content is not a blueprint Basalt can read, larger than {@code blueprint.max-file-bytes}
     *                            once decompressed or than {@code blueprint.max-blocks}, or names an unknown block state
     */
    @NotNull Blueprint readBlueprint(@NotNull InputStream in) throws IOException;
}
