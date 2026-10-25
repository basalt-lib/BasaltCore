package tlm.dev.basalt.core.blueprint;

import java.util.List;
import java.util.OptionalInt;

final class PaletteStates {

    private PaletteStates() {
    }

    static int[] resolve(List<String> entries, BlockNames names, String dataVersion) {
        int[] states = new int[entries.size()];
        for (int index = 0; index < states.length; index++) {
            String entry = entries.get(index);
            if (BlockStateText.isStructureVoid(entry)) {
                states[index] = BlockPlan.VOID;
                continue;
            }
            OptionalInt state = names.stateOf(entry);
            if (state.isEmpty()) {
                throw new PlanException("unknown block state " + entry + " in a file of DataVersion " + dataVersion);
            }
            states[index] = state.getAsInt();
        }
        return states;
    }
}
