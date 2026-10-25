package tlm.dev.basalt.core.blueprint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class StructureReader {

    private static final int NO_DATA_VERSION = Integer.MIN_VALUE;

    private final NbtReader nbt;
    private final PlanSettings settings;
    private int dataVersion = NO_DATA_VERSION;
    private PlanSize size;
    private List<String> palette;
    private List<String> firstOfPalettes;
    private int[] blocks;
    private int blockTotal;

    private StructureReader(NbtReader nbt, PlanSettings settings) {
        this.nbt = nbt;
        this.settings = settings;
    }

    static BlockPlan read(NbtReader nbt, PlanSettings settings, BlockNames names) {
        return new StructureReader(nbt, settings).read(names);
    }

    private BlockPlan read(BlockNames names) {
        int type;
        while ((type = nbt.nextEntry()) != NbtReader.END) {
            String name = nbt.readName();
            switch (name) {
                case "DataVersion" -> dataVersion = type == NbtReader.INT ? nbt.readInt() : skipped(type);
                case "size" -> size = readSize(type);
                case "palette" -> palette = readPalette(type);
                case "palettes" -> firstOfPalettes = readPalettes(type);
                case "blocks" -> readBlocks(type);
                default -> nbt.skip(type);
            }
        }
        List<String> entries = firstOfPalettes != null ? firstOfPalettes : palette;
        if (size == null || entries == null || blocks == null) {
            throw new PlanException("the structure file has no size, palette or blocks");
        }
        int[] states = PaletteStates.resolve(entries, names, dataVersion == NO_DATA_VERSION ? "unknown" : Integer.toString(dataVersion));
        int[] plan = Arrays.copyOf(states, states.length + 1);
        plan[states.length] = BlockPlan.VOID;
        int[] indices = new int[(int) size.volume()];
        Arrays.fill(indices, states.length);
        for (int block = 0; block < blockTotal; block++) {
            int x = blocks[block * 4];
            int y = blocks[block * 4 + 1];
            int z = blocks[block * 4 + 2];
            int state = blocks[block * 4 + 3];
            if (!size.contains(x, y, z)) {
                throw new PlanException("a block at " + x + " " + y + " " + z + " lies outside the size " + size);
            }
            if (state < 0 || state >= states.length) {
                throw new PlanException("a block uses the state " + state + " outside a palette of " + states.length);
            }
            indices[size.index(x, y, z)] = state;
        }
        return BlockPlan.of(size, plan, indices, names::isAir);
    }

    private PlanSize readSize(int type) {
        if (type != NbtReader.LIST) {
            throw new PlanException("the structure size is not a list of three integers");
        }
        NbtReader.ListHeader header = nbt.readListHeader();
        if (header.type() != NbtReader.INT || header.length() != 3) {
            throw new PlanException("the structure size is not a list of three integers");
        }
        int x = nbt.readInt();
        int y = nbt.readInt();
        int z = nbt.readInt();
        if (x < 1 || y < 1 || z < 1) {
            throw new PlanException("the structure size " + x + " x " + y + " x " + z + " is empty or negative");
        }
        long volume = (long) x * y * z;
        if (volume > settings.maxBlocks()) {
            throw new PlanException("the structure holds " + volume + " blocks, more than the limit of " + settings.maxBlocks());
        }
        return new PlanSize(x, y, z);
    }

    private List<String> readPalettes(int type) {
        if (type != NbtReader.LIST) {
            throw new PlanException("the structure palettes are not a list of palettes");
        }
        NbtReader.ListHeader header = nbt.readListHeader();
        if (header.length() == 0) {
            return null;
        }
        if (header.type() != NbtReader.LIST) {
            throw new PlanException("the structure palettes are not a list of palettes");
        }
        List<String> first = readPalette(NbtReader.LIST);
        for (int index = 1; index < header.length(); index++) {
            nbt.skip(NbtReader.LIST);
        }
        return first;
    }

    private List<String> readPalette(int type) {
        if (type != NbtReader.LIST) {
            throw new PlanException("the structure palette is not a list of block states");
        }
        NbtReader.ListHeader header = nbt.readListHeader();
        if (header.length() > 0 && header.type() != NbtReader.COMPOUND) {
            throw new PlanException("the structure palette is not a list of block states");
        }
        if (header.length() > settings.maxBlocks()) {
            throw new PlanException("the structure palette holds " + header.length() + " states, more than the limit of " + settings.maxBlocks());
        }
        List<String> entries = new ArrayList<>(Math.min(header.length(), 4096));
        for (int index = 0; index < header.length(); index++) {
            entries.add(readBlockState());
        }
        return entries;
    }

    private String readBlockState() {
        nbt.enterCompound();
        String name = null;
        Map<String, String> properties = new TreeMap<>();
        int type;
        while ((type = nbt.nextEntry()) != NbtReader.END) {
            String key = nbt.readName();
            if (key.equals("Name") && type == NbtReader.STRING) {
                name = nbt.readString();
            } else if (key.equals("Properties") && type == NbtReader.COMPOUND) {
                nbt.enterCompound();
                int propertyType;
                while ((propertyType = nbt.nextEntry()) != NbtReader.END) {
                    String property = nbt.readName();
                    if (propertyType != NbtReader.STRING) {
                        throw new PlanException("the property " + property + " of a palette entry is not a string");
                    }
                    properties.put(property, nbt.readString());
                }
            } else {
                nbt.skip(type);
            }
        }
        if (name == null) {
            throw new PlanException("a palette entry has no Name");
        }
        return BlockStateText.of(name, properties);
    }

    private void readBlocks(int type) {
        if (type != NbtReader.LIST) {
            throw new PlanException("the structure blocks are not a list");
        }
        NbtReader.ListHeader header = nbt.readListHeader();
        if (header.length() > 0 && header.type() != NbtReader.COMPOUND) {
            throw new PlanException("the structure blocks are not a list of compounds");
        }
        if (header.length() > settings.maxBlocks()) {
            throw new PlanException("the structure lists " + header.length() + " blocks, more than the limit of " + settings.maxBlocks());
        }
        blocks = new int[header.length() * 4];
        blockTotal = header.length();
        for (int block = 0; block < header.length(); block++) {
            readBlock(block);
        }
    }

    private void readBlock(int block) {
        nbt.enterCompound();
        boolean hasPos = false;
        boolean hasState = false;
        int entryType;
        while ((entryType = nbt.nextEntry()) != NbtReader.END) {
            String key = nbt.readName();
            if (key.equals("pos") && entryType == NbtReader.LIST) {
                NbtReader.ListHeader pos = nbt.readListHeader();
                if (pos.type() != NbtReader.INT || pos.length() != 3) {
                    throw new PlanException("a block pos is not a list of three integers");
                }
                blocks[block * 4] = nbt.readInt();
                blocks[block * 4 + 1] = nbt.readInt();
                blocks[block * 4 + 2] = nbt.readInt();
                hasPos = true;
            } else if (key.equals("state") && entryType == NbtReader.INT) {
                blocks[block * 4 + 3] = nbt.readInt();
                hasState = true;
            } else {
                nbt.skip(entryType);
            }
        }
        if (!hasPos || !hasState) {
            throw new PlanException("a block of the structure has no pos or state");
        }
    }

    private int skipped(int type) {
        nbt.skip(type);
        return NO_DATA_VERSION;
    }
}
