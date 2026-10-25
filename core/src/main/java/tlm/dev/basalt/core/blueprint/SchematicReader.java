package tlm.dev.basalt.core.blueprint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class SchematicReader {

    private static final int NO_VALUE = Integer.MIN_VALUE;
    private static final int VARINT_MAX_BYTES = 5;

    private final NbtReader nbt;
    private final PlanSettings settings;
    private final int version;
    private int dataVersion = NO_VALUE;
    private int width = NO_VALUE;
    private int height = NO_VALUE;
    private int length = NO_VALUE;
    private TreeMap<Integer, String> palette;
    private byte[] data;

    private SchematicReader(NbtReader nbt, PlanSettings settings, int version) {
        this.nbt = nbt;
        this.settings = settings;
        this.version = version;
    }

    static BlockPlan readVersion2(NbtReader nbt, PlanSettings settings, BlockNames names) {
        SchematicReader reader = new SchematicReader(nbt, settings, 2);
        reader.readFields();
        return reader.plan(names);
    }

    static BlockPlan readVersion3(NbtReader nbt, PlanSettings settings, BlockNames names) {
        SchematicReader reader = new SchematicReader(nbt, settings, 3);
        int type;
        boolean found = false;
        while ((type = nbt.nextEntry()) != NbtReader.END) {
            String name = nbt.readName();
            if (name.equals("Schematic") && type == NbtReader.COMPOUND && !found) {
                nbt.enterCompound();
                reader.readFields();
                found = true;
            } else {
                nbt.skip(type);
            }
        }
        if (!found) {
            throw new PlanException("the version 3 schematic has no Schematic compound");
        }
        return reader.plan(names);
    }

    private void readFields() {
        int type;
        while ((type = nbt.nextEntry()) != NbtReader.END) {
            String name = nbt.readName();
            switch (name) {
                case "Version" -> checkVersion(type);
                case "DataVersion" -> dataVersion = type == NbtReader.INT ? nbt.readInt() : skipped(type);
                case "Width" -> width = readSide(type, name);
                case "Height" -> height = readSide(type, name);
                case "Length" -> length = readSide(type, name);
                case "Palette" -> palette = version == 2 ? readPalette(type) : skippedPalette(type);
                case "BlockData" -> data = version == 2 ? readData(type) : skippedData(type);
                case "Blocks" -> readBlocks(type);
                default -> nbt.skip(type);
            }
        }
    }

    private void checkVersion(int type) {
        if (type != NbtReader.INT) {
            throw new PlanException("the schematic Version is not an integer");
        }
        int found = nbt.readInt();
        if (found != version) {
            throw new PlanException("the Sponge schematic version " + found + " is not supported, only versions 2 and 3");
        }
    }

    private int readSide(int type, String name) {
        if (type != NbtReader.SHORT) {
            throw new PlanException("the schematic " + name + " is not a short");
        }
        return nbt.readShort() & 0xFFFF;
    }

    private void readBlocks(int type) {
        if (version != 3 || type != NbtReader.COMPOUND) {
            nbt.skip(type);
            return;
        }
        nbt.enterCompound();
        int entryType;
        while ((entryType = nbt.nextEntry()) != NbtReader.END) {
            String name = nbt.readName();
            switch (name) {
                case "Palette" -> palette = readPalette(entryType);
                case "Data" -> data = readData(entryType);
                default -> nbt.skip(entryType);
            }
        }
    }

    private TreeMap<Integer, String> readPalette(int type) {
        if (type != NbtReader.COMPOUND) {
            throw new PlanException("the schematic Palette is not a compound");
        }
        nbt.enterCompound();
        TreeMap<Integer, String> entries = new TreeMap<>();
        int entryType;
        while ((entryType = nbt.nextEntry()) != NbtReader.END) {
            String state = nbt.readName();
            if (entryType != NbtReader.INT) {
                throw new PlanException("the palette entry " + state + " is not an integer");
            }
            int index = nbt.readInt();
            if (index < 0) {
                throw new PlanException("the palette entry " + state + " has a negative index " + index);
            }
            if (entries.put(index, state) != null) {
                throw new PlanException("the palette index " + index + " is used twice");
            }
            if (entries.size() > settings.maxBlocks()) {
                throw new PlanException("the schematic palette holds more than " + settings.maxBlocks() + " states");
            }
        }
        return entries;
    }

    private byte[] readData(int type) {
        if (type != NbtReader.BYTE_ARRAY) {
            throw new PlanException("the schematic block data is not a byte array");
        }
        return nbt.readByteArray();
    }

    private BlockPlan plan(BlockNames names) {
        if (width == NO_VALUE || height == NO_VALUE || length == NO_VALUE) {
            throw new PlanException("the schematic has no Width, Height or Length");
        }
        if (palette == null) {
            throw new PlanException("the schematic has no block palette, numeric block ids are not supported");
        }
        if (data == null) {
            throw new PlanException("the schematic has no block data");
        }
        if (width == 0 || height == 0 || length == 0) {
            throw new PlanException("the schematic size " + width + " x " + height + " x " + length + " is empty");
        }
        long volume = (long) width * height * length;
        if (volume > settings.maxBlocks()) {
            throw new PlanException("the schematic holds " + volume + " blocks, more than the limit of " + settings.maxBlocks());
        }
        List<String> states = new ArrayList<>(palette.values());
        Map<Integer, Integer> slots = new HashMap<>();
        for (Integer index : palette.keySet()) {
            slots.put(index, slots.size());
        }
        int[] resolved = PaletteStates.resolve(states, names, dataVersion == NO_VALUE ? "unknown" : Integer.toString(dataVersion));
        int[] indices = decode((int) volume, slots);
        return BlockPlan.of(new PlanSize(width, height, length), resolved.length == 0 ? new int[] {BlockPlan.VOID} : resolved,
                indices, names::isAir);
    }

    private int[] decode(int volume, Map<Integer, Integer> slots) {
        int[] indices = new int[volume];
        int position = 0;
        int lastIndex = NO_VALUE;
        int lastSlot = 0;
        for (int block = 0; block < volume; block++) {
            int value = 0;
            int shift = 0;
            while (true) {
                if (position >= data.length) {
                    throw new PlanException("the schematic block data holds fewer than " + volume + " entries");
                }
                int current = data[position++];
                value |= (current & 0x7F) << shift;
                if ((current & 0x80) == 0) {
                    break;
                }
                shift += 7;
                if (shift >= 7 * VARINT_MAX_BYTES) {
                    throw new PlanException("a varint of the schematic block data is longer than " + VARINT_MAX_BYTES + " bytes");
                }
            }
            if (value < 0) {
                throw new PlanException("the schematic block data holds a negative index " + value);
            }
            if (value != lastIndex) {
                Integer slot = slots.get(value);
                if (slot == null) {
                    throw new PlanException("the schematic block data uses the index " + value + ", absent from the palette");
                }
                lastIndex = value;
                lastSlot = slot;
            }
            indices[block] = lastSlot;
        }
        if (position != data.length) {
            throw new PlanException("the schematic block data holds more than " + volume + " entries");
        }
        return indices;
    }

    private TreeMap<Integer, String> skippedPalette(int type) {
        nbt.skip(type);
        return palette;
    }

    private byte[] skippedData(int type) {
        nbt.skip(type);
        return data;
    }

    private int skipped(int type) {
        nbt.skip(type);
        return NO_VALUE;
    }
}
