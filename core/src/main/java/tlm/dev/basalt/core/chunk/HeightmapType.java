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

    public boolean counts(int state, BlockStateTraits traits) {
        return switch (this) {
            case WORLD_SURFACE -> !traits.isAir(state);
            case MOTION_BLOCKING -> traits.isMotionBlocking(state);
            case MOTION_BLOCKING_NO_LEAVES -> traits.isMotionBlocking(state) && !traits.isLeaves(state);
        };
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
