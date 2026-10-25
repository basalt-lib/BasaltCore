package tlm.dev.basalt.core.blueprint;

import java.util.HashMap;
import java.util.Map;

final class PlanFiles {

    private static final int NO_VERSION = Integer.MIN_VALUE;

    private PlanFiles() {
    }

    static BlockPlan read(byte[] nbt, PlanSettings settings, BlockNames names) {
        Map<String, Integer> rootTypes = new HashMap<>();
        int version = scan(nbt, rootTypes);
        NbtReader reader = new NbtReader(nbt);
        reader.enterRoot();
        if (rootTypes.getOrDefault("Schematic", NbtReader.END) == NbtReader.COMPOUND) {
            return SchematicReader.readVersion3(reader, settings, names);
        }
        if (rootTypes.containsKey("BlockData") || rootTypes.containsKey("Width") && rootTypes.containsKey("Version")) {
            if (version != 2) {
                throw new PlanException("the Sponge schematic version " + (version == NO_VERSION ? "unknown" : version)
                        + " is not supported, only versions 2 and 3");
            }
            return SchematicReader.readVersion2(reader, settings, names);
        }
        if (rootTypes.containsKey("Materials") || rootTypes.getOrDefault("Blocks", NbtReader.END) == NbtReader.BYTE_ARRAY) {
            throw new PlanException("the old .schematic format with numeric block ids is not supported");
        }
        if (rootTypes.containsKey("size") && rootTypes.containsKey("blocks")) {
            return StructureReader.read(reader, settings, names);
        }
        throw new PlanException("the file is neither a structure nor a Sponge schematic");
    }

    private static int scan(byte[] nbt, Map<String, Integer> rootTypes) {
        NbtReader reader = new NbtReader(nbt);
        reader.enterRoot();
        int version = NO_VERSION;
        int type;
        while ((type = reader.nextEntry()) != NbtReader.END) {
            String name = reader.readName();
            rootTypes.put(name, type);
            if (name.equals("Version") && type == NbtReader.INT) {
                version = reader.readInt();
            } else {
                reader.skip(type);
            }
        }
        return version;
    }
}
