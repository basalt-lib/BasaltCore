package tlm.dev.basalt.api;

import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;

/**
 * A plan of blocks: an immutable box of block states that {@link VirtualWorld#place(Blueprint, int, int, int)} sets in a
 * virtual world, as many times and in as many worlds as wanted. It holds block states only, no block entity data and
 * no entity. A position marked {@code minecraft:structure_void} in a file, or left out of a structure file, is a void:
 * placing the blueprint keeps what the world already holds there.
 * <p>
 * A blueprint cannot change and is safe to share between threads. Get one from
 * {@link Basalt#captureBlueprint(World, BoundingBox)} or {@link Basalt#readBlueprint(Path)}, never implement this interface.
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface Blueprint {

    /**
     * Returns the size of this blueprint along the x axis.
     *
     * @return the width in blocks, at least 1
     */
    int sizeX();

    /**
     * Returns the size of this blueprint along the y axis.
     *
     * @return the height in blocks, at least 1
     */
    int sizeY();

    /**
     * Returns the size of this blueprint along the z axis.
     *
     * @return the length in blocks, at least 1
     */
    int sizeZ();

    /**
     * Returns the number of blocks of this blueprint that are neither air nor void.
     *
     * @return the number of solid, liquid or other non air blocks
     */
    int blockCount();
}
