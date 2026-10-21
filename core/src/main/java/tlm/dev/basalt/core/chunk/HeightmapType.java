package tlm.dev.basalt.core.chunk;

import java.util.Optional;

public enum HeightmapType {
    WORLD_SURFACE(1),
    MOTION_BLOCKING(4),
    MOTION_BLOCKING_NO_LEAVES(5);

    private final int networkId;

    HeightmapType(int networkId) {
        this.networkId = networkId;
    }

    public int networkId() {
        return networkId;
    }

    public static Optional<HeightmapType> byNetworkId(int networkId) {
        for (HeightmapType type : values()) {
            if (type.networkId == networkId) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
