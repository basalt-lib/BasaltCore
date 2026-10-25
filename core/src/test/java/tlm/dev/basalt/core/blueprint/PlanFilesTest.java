package tlm.dev.basalt.core.blueprint;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanFilesTest {

    private static final String[] NAMES = {"minecraft:air", "minecraft:stone", "minecraft:oak_stairs[facing=east,half=bottom]",
            "minecraft:structure_void", "minecraft:gold_block"};
    private static final int[] STATES = {FakeBlockNames.AIR, FakeBlockNames.STONE, FakeBlockNames.STAIRS, BlockPlan.VOID, FakeBlockNames.GOLD};
    private static final int[] PATTERN = {1, 2, 0, 4, 1, 3, 2, 0, 1, 4, 3, 1};
    private static final PlanSettings SETTINGS = new PlanSettings(1_000_000, 33_554_432);

    private final PlanService service = new PlanService(new FakeBlockNames(), (world, box) -> new int[0], SETTINGS);

    @Test
    void readsAStructureWithItsVoidsAndPropertiesInAnyEntryOrder() throws IOException {
        BlockPlan plan = read(structure(PATTERN));

        assertSample(plan);
        assertEquals(new PlanSize(3, 2, 2), plan.size());
        assertEquals(8, plan.blockCount());
    }

    @Test
    void readsTheSamePlanFromBothSchematicVersions() throws IOException {
        assertSample(read(schematic2(3, 2, 2, NAMES, PATTERN)));
        assertSample(read(schematic3(3, 2, 2, NAMES, PATTERN)));
    }

    @Test
    void readsTheSchematicOrderXThenZThenYWithAWidthOfOne() throws IOException {
        int[] pattern = {1, 2, 4, 1, 0, 2};
        BlockPlan plan = read(schematic2(1, 3, 2, NAMES, pattern));

        for (int y = 0; y < 3; y++) {
            for (int z = 0; z < 2; z++) {
                assertEquals(STATES[pattern[z + y * 2]], plan.stateAt(0, y, z), "at y " + y + " z " + z);
            }
        }
    }

    @Test
    void readsIndicesOfSeveralBytesFromAPaletteOfMoreThan127Entries() throws IOException {
        String[] names = new String[20000];
        for (int index = 0; index < names.length; index++) {
            names[index] = "minecraft:numbered_" + index;
        }
        int[] pattern = new int[4 * 3 * 5];
        for (int index = 0; index < pattern.length; index++) {
            pattern[index] = (index * 331) % names.length;
        }

        BlockPlan plan = read(schematic3(4, 3, 5, names, pattern));

        for (int y = 0; y < 3; y++) {
            for (int z = 0; z < 5; z++) {
                for (int x = 0; x < 4; x++) {
                    assertEquals(FakeBlockNames.NUMBERED + pattern[x + z * 4 + y * 20], plan.stateAt(x, y, z));
                }
            }
        }
        assertTrue(TestNbt.varints(pattern).length > pattern.length);
    }

    @Test
    void refusesAnEmptySize() {
        assertRefused("is empty", schematic2(0, 1, 1, NAMES, new int[0]));
        assertRefused("is empty or negative", TestNbt.root().putIntList("size", 0, 1, 1).list("palette", 10, 0).list("blocks", 10, 0).end().gzip());
    }

    @Test
    void refusesAPlanLargerThanTheLimit() {
        PlanService small = new PlanService(new FakeBlockNames(), (world, box) -> new int[0], new PlanSettings(11, 33_554_432));

        PlanException refused = assertThrows(PlanException.class, () -> small.read(new ByteArrayInputStream(structure(PATTERN))));
        assertTrue(refused.getMessage().contains("more than the limit of 11"), refused.getMessage());
    }

    @Test
    void namesTheUnknownBlockAndTheDataVersion() {
        String[] names = NAMES.clone();
        names[4] = "minecraft:old_grass";

        assertRefused("unknown block state minecraft:old_grass in a file of DataVersion 1343", schematic2(3, 2, 2, names, PATTERN));
    }

    @Test
    void refusesATruncatedFile() {
        byte[] file = structure(PATTERN);

        assertRefused("truncated", Arrays.copyOf(file, file.length / 2));
        byte[] plain = TestNbt.root().putIntList("size", 1, 1, 1).bytes();
        assertRefused("truncated", TestNbt.gzip(plain));
    }

    @Test
    void refusesAGzipBomb() {
        PlanService small = new PlanService(new FakeBlockNames(), (world, box) -> new int[0], new PlanSettings(1000, 4096));
        byte[] bomb = TestNbt.gzip(new byte[10_000_000]);

        PlanException refused = assertThrows(PlanException.class, () -> small.read(new ByteArrayInputStream(bomb)));
        assertTrue(refused.getMessage().contains("larger than 4096 bytes"), refused.getMessage());
        assertTrue(bomb.length < 20_000);
    }

    @Test
    void refusesNegativeOrHugeLengthsBeforeAllocating() {
        assertRefused("negative length", TestNbt.root().list("blocks", 10, -1).end().gzip());
        assertRefused("truncated", TestNbt.root().list("blocks", 10, Integer.MAX_VALUE).end().gzip());
        assertRefused("negative length", TestNbt.root().raw(out -> {
            out.writeByte(7);
            out.writeUTF("Data");
            out.writeInt(-5);
        }).end().gzip());
        assertRefused("truncated", TestNbt.root().raw(out -> {
            out.writeByte(11);
            out.writeUTF("Numbers");
            out.writeInt(1 << 29);
        }).end().gzip());
    }

    @Test
    void refusesAnUnknownTagTypeAndADeepNesting() {
        assertRefused("unknown NBT tag type 13", TestNbt.root().raw(out -> {
            out.writeByte(13);
            out.writeUTF("Strange");
        }).end().gzip());
        TestNbt deep = TestNbt.root();
        deep.list("Deep", 9, 1);
        for (int level = 0; level < 600; level++) {
            deep.listHeader(9, 1);
        }
        assertRefused("deeper than 512", deep.listHeader(1, 0).end().gzip());
    }

    @Test
    void refusesTheOldFormatAndOtherVersionsAndUnknownFiles() {
        assertRefused("old .schematic format", TestNbt.root().putShort("Width", 1).putString("Materials", "Alpha")
                .putByteArray("Blocks", new byte[1]).end().gzip());
        assertRefused("version 1 is not supported", TestNbt.root().putInt("Version", 1).putShort("Width", 1)
                .putByteArray("BlockData", new byte[1]).end().gzip());
        assertRefused("neither a structure nor a Sponge schematic", TestNbt.root().putInt("Something", 1).end().gzip());
        assertRefused("not gzip compressed", TestNbt.root().end().bytes());
    }

    @Test
    void refusesBadSchematicData() {
        assertRefused("fewer than 12 entries", schematic2Data(TestNbt.varints(1, 2, 0)));
        int[] extra = Arrays.copyOf(PATTERN, PATTERN.length + 1);
        assertRefused("more than 12 entries", schematic2Data(TestNbt.varints(extra)));
        int[] absent = PATTERN.clone();
        absent[3] = 99;
        assertRefused("index 99, absent from the palette", schematic2Data(TestNbt.varints(absent)));
        byte[] tooLong = Arrays.copyOf(TestNbt.varints(PATTERN), 20);
        Arrays.fill(tooLong, 0, 6, (byte) 0x80);
        assertRefused("longer than 5 bytes", schematic2Data(tooLong));
    }

    @Test
    void refusesAStructureBlockOutsideItsSizeOrPalette() {
        assertRefused("outside the size", TestNbt.root().putIntList("size", 1, 1, 1).list("palette", 10, 1).putString("Name", "minecraft:stone").end()
                .list("blocks", 10, 1).putIntList("pos", 1, 0, 0).putInt("state", 0).end().end().gzip());
        assertRefused("outside a palette of 1", TestNbt.root().putIntList("size", 1, 1, 1).list("palette", 10, 1).putString("Name", "minecraft:stone").end()
                .list("blocks", 10, 1).putIntList("pos", 0, 0, 0).putInt("state", 3).end().end().gzip());
    }

    private void assertRefused(String reason, byte[] file) {
        PlanException refused = assertThrows(PlanException.class, () -> service.read(new ByteArrayInputStream(file)));
        assertTrue(refused.getMessage().contains(reason), refused.getMessage());
    }

    private BlockPlan read(byte[] file) throws IOException {
        return service.read(new ByteArrayInputStream(file));
    }

    private static void assertSample(BlockPlan plan) {
        for (int y = 0; y < 2; y++) {
            for (int z = 0; z < 2; z++) {
                for (int x = 0; x < 3; x++) {
                    assertEquals(STATES[PATTERN[x + z * 3 + y * 6]], plan.stateAt(x, y, z), "at " + x + " " + y + " " + z);
                }
            }
        }
    }

    private static byte[] structure(int[] pattern) {
        int listed = (int) Arrays.stream(pattern).filter(index -> index != 3).count();
        TestNbt nbt = TestNbt.root().putInt("DataVersion", 4671).list("blocks", 10, listed);
        for (int index = 0; index < pattern.length; index++) {
            if (pattern[index] == 3) {
                continue;
            }
            nbt.putIntList("pos", index % 3, index / 6, index / 3 % 2).putInt("state", pattern[index]);
            if (index == 0) {
                nbt.compound("nbt").putString("id", "minecraft:chest").end();
            }
            nbt.end();
        }
        nbt.list("entities", 10, 0).putIntList("size", 3, 2, 2).list("palette", 10, NAMES.length);
        nbt.putString("Name", "minecraft:air").end();
        nbt.putString("Name", "minecraft:stone").end();
        nbt.putString("Name", "minecraft:oak_stairs").compound("Properties").putString("half", "bottom").putString("facing", "east").end().end();
        nbt.putString("Name", "minecraft:structure_void").end();
        nbt.putString("Name", "minecraft:gold_block").end();
        return nbt.end().gzip();
    }

    private static byte[] schematic2(int width, int height, int length, String[] names, int[] pattern) {
        TestNbt nbt = TestNbt.root().putInt("Version", 2).putInt("DataVersion", 1343)
                .putShort("Width", width).putShort("Height", height).putShort("Length", length).putInt("PaletteMax", names.length);
        palette(nbt.compound("Palette"), names);
        return nbt.putByteArray("BlockData", TestNbt.varints(pattern)).end().gzip();
    }

    private static byte[] schematic2Data(byte[] data) {
        TestNbt nbt = TestNbt.root().putInt("Version", 2).putShort("Width", 3).putShort("Height", 2).putShort("Length", 2);
        palette(nbt.compound("Palette"), NAMES);
        return nbt.putByteArray("BlockData", data).end().gzip();
    }

    private static byte[] schematic3(int width, int height, int length, String[] names, int[] pattern) {
        TestNbt nbt = TestNbt.root().compound("Schematic").putInt("Version", 3).putInt("DataVersion", 4671)
                .putShort("Width", width).putShort("Height", height).putShort("Length", length).compound("Blocks");
        palette(nbt.compound("Palette"), names);
        return nbt.putByteArray("Data", TestNbt.varints(pattern)).list("BlockEntities", 10, 0).end().end().end().gzip();
    }

    private static void palette(TestNbt nbt, String[] names) {
        for (int index = 0; index < names.length; index++) {
            nbt.putInt(names[index], index);
        }
        nbt.end();
    }
}
