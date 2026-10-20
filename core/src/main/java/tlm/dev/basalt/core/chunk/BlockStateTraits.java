package tlm.dev.basalt.core.chunk;

public interface BlockStateTraits {

    boolean isAir(int state);

    boolean isMotionBlocking(int state);

    boolean isLeaves(int state);
}
