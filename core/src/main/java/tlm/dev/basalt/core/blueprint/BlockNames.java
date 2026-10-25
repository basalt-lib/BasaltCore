package tlm.dev.basalt.core.blueprint;

import java.util.OptionalInt;

public interface BlockNames {

    OptionalInt stateOf(String blockState);

    boolean isAir(int state);
}
