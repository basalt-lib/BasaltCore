package tlm.dev.basalt.core.chunk;

public interface BlockStateTraits {

    boolean isAir(int state);

    boolean countsIn(HeightmapType type, int state);
}
