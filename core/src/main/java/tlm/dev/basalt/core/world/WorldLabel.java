package tlm.dev.basalt.core.world;

import java.util.UUID;

public record WorldLabel(UUID id, String owner, boolean openToEveryone) {

    public WorldLabel {
        if (id == null || owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("a world label needs the world id and its owner");
        }
    }

    WorldLabel withOpenToEveryone(boolean open) {
        return new WorldLabel(id, owner, open);
    }
}
