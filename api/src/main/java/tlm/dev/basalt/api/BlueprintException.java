package tlm.dev.basalt.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Thrown by {@link Basalt#readBlueprint(Path)} when the content of a file is not a blueprint Basalt can read: not gzip
 * compressed NBT, truncated, larger than the limits of the BasaltCore config, in an unsupported format, or naming a
 * block state unknown to this server. The message names the cause.
 */
@ApiStatus.Experimental
public final class BlueprintException extends RuntimeException {

    /**
     * Creates the exception. BasaltCore throws it, plugins do not.
     *
     * @param message the message naming the cause
     * @param cause the cause, or null
     */
    @ApiStatus.Internal
    public BlueprintException(@NotNull String message, Throwable cause) {
        super(message, cause);
    }
}
