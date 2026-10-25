package tlm.dev.basalt.core.blueprint;

import java.util.Map;
import java.util.StringJoiner;

final class BlockStateText {

    private static final String NAMESPACE = "minecraft:";
    private static final String STRUCTURE_VOID = "minecraft:structure_void";

    private BlockStateText() {
    }

    static String of(String name, Map<String, String> sortedProperties) {
        if (sortedProperties.isEmpty()) {
            return name;
        }
        StringJoiner properties = new StringJoiner(",", "[", "]");
        sortedProperties.forEach((key, value) -> properties.add(key + "=" + value));
        return name + properties;
    }

    static boolean isStructureVoid(String blockState) {
        int bracket = blockState.indexOf('[');
        String name = (bracket < 0 ? blockState : blockState.substring(0, bracket)).trim();
        return (name.indexOf(':') < 0 ? NAMESPACE + name : name).equals(STRUCTURE_VOID);
    }
}
