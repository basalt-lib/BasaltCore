package tlm.dev.basalt.core.chunk;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SectionCodecTest {

    private static final ChunkFormats FORMATS = ChunkFormats.of(30000, 70, SectionHeader.BLOCK_COUNT);
    private static final SectionCodec CODEC = new SectionCodec(FORMATS);
    private static final SectionCodec FLUID_CODEC = new SectionCodec(ChunkFormats.of(30000, 70, SectionHeader.BLOCK_AND_FLUID_COUNTS));
    private static final int AIR = 0;
    private static final int PLAINS = 3;

    @Test
    void writesAnEmptySectionAsTwoSingleValues() {
        byte[] bytes = encode(section(filled(AIR), 0));

        assertArrayEquals(new byte[] {0, 0, 0, AIR, 0, PLAINS}, bytes);
    }

    @Test
    void readsBackAnEmptySection() {
        ChunkSection read = roundTrip(section(filled(AIR), 0));

        assertEquals(0, read.nonAirCount());
        assertEquals(AIR, read.stateAt(15, 15, 15));
        assertEquals(PLAINS, read.biomes().get(63));
    }

    @Test
    void readsBackASingleNonAirValue() {
        ChunkSection read = roundTrip(section(filled(85), 4096));

        assertEquals(4096, read.nonAirCount());
        assertEquals(0, read.states().bits());
        assertEquals(85, read.stateAt(3, 4, 5));
    }

    @Test
    void readsBackAPaletteOfSeveralEntries() {
        int[] states = filled(AIR);
        states[ChunkSection.blockIndex(0, 0, 0)] = 79;
        states[ChunkSection.blockIndex(15, 0, 15)] = 2104;
        states[ChunkSection.blockIndex(8, 2, 8)] = 4775;

        ChunkSection read = roundTrip(section(states, 3));

        assertArrayEquals(new int[] {79, AIR, 2104, 4775}, read.states().palette());
        assertEquals(4, read.states().bits());
        assertArrayEquals(states, read.states().toArray());
    }

    @Test
    void readsBackAFullBitArray() {
        int[] states = new int[4096];
        for (int index = 0; index < states.length; index++) {
            states[index] = index * 7 + 1;
        }

        ChunkSection read = roundTrip(section(states, 4096));

        assertEquals(15, read.states().bits());
        assertArrayEquals(states, read.states().toArray());
    }

    @Test
    void readsBackBiomePalettes() {
        int[] biomes = new int[64];
        for (int index = 0; index < biomes.length; index++) {
            biomes[index] = index % 5;
        }
        ChunkSection section = new ChunkSection(
                PalettedValues.of(filled(AIR), FORMATS.blockStates()), PalettedValues.of(biomes, FORMATS.biomes()), 0, 0);

        ChunkSection read = roundTrip(section);

        assertEquals(3, read.biomes().bits());
        assertArrayEquals(biomes, read.biomes().toArray());
    }

    @Test
    void writesTheFluidCountAfterTheBlockCountWhenTheVersionHasIt() {
        ChunkSection water = new ChunkSection(
                PalettedValues.of(filled(86), FORMATS.blockStates()), PalettedValues.of(filledBiomes(), FORMATS.biomes()), 4096, 4096);
        ByteWriter out = new ByteWriter(16);
        FLUID_CODEC.write(water, out);

        assertArrayEquals(new byte[] {16, 0, 16, 0, 0, 86, 0, PLAINS}, out.toByteArray());
        assertArrayEquals(new byte[] {16, 0, 0, 86, 0, PLAINS}, encode(water));
    }

    @Test
    void readsBackTheFluidCount() {
        int[] states = filled(AIR);
        states[ChunkSection.blockIndex(1, 2, 3)] = 86;
        states[ChunkSection.blockIndex(4, 5, 6)] = 1;
        ChunkSection section = new ChunkSection(
                PalettedValues.of(states, FORMATS.blockStates()), PalettedValues.of(filledBiomes(), FORMATS.biomes()), 2, 1);
        ByteWriter out = new ByteWriter(4096);
        FLUID_CODEC.write(section, out);
        ByteReader in = new ByteReader(out.toByteArray());

        ChunkSection read = FLUID_CODEC.read(in);

        assertEquals(0, in.remaining());
        assertEquals(2, read.nonAirCount());
        assertEquals(1, read.fluidCount());
        assertArrayEquals(states, read.states().toArray());
    }

    @Test
    void rejectsMoreFluidsThanNonAirBlocks() {
        ByteWriter out = new ByteWriter(8);
        out.writeShort(1);
        out.writeShort(2);

        assertThrows(MalformedChunkDataException.class, () -> FLUID_CODEC.read(new ByteReader(out.toByteArray())));
        assertThrows(IllegalArgumentException.class, () -> new ChunkSection(
                PalettedValues.of(filled(AIR), FORMATS.blockStates()), PalettedValues.of(filledBiomes(), FORMATS.biomes()), 0, 1));
    }

    @Test
    void rejectsATruncatedSection() {
        int[] states = filled(AIR);
        states[0] = 1;
        byte[] bytes = encode(section(states, 1));

        assertThrows(MalformedChunkDataException.class, () -> CODEC.read(new ByteReader(Arrays.copyOf(bytes, bytes.length - 1))));
    }

    @Test
    void rejectsANegativeNonAirCount() {
        ByteWriter out = new ByteWriter(8);
        out.writeShort(-1);

        assertThrows(MalformedChunkDataException.class, () -> CODEC.read(new ByteReader(out.toByteArray())));
    }

    @Test
    void rejectsAnEmptyPalette() {
        ByteWriter out = new ByteWriter(8);
        out.writeShort(0);
        out.writeByte(4);
        out.writeVarInt(0);

        assertThrows(MalformedChunkDataException.class, () -> CODEC.read(new ByteReader(out.toByteArray())));
    }

    @Test
    void rejectsAPaletteLargerThanItsBits() {
        ByteWriter out = new ByteWriter(64);
        out.writeShort(0);
        out.writeByte(4);
        out.writeVarInt(17);
        for (int index = 0; index < 17; index++) {
            out.writeVarInt(index);
        }

        assertThrows(MalformedChunkDataException.class, () -> CODEC.read(new ByteReader(out.toByteArray())));
    }

    @Test
    void rejectsAnIndexOutsideThePalette() {
        ByteWriter out = new ByteWriter(4096);
        out.writeShort(1);
        out.writeByte(4);
        out.writeVarInt(2);
        out.writeVarInt(AIR);
        out.writeVarInt(1);
        out.writeLong(5L);
        for (int index = 1; index < 256; index++) {
            out.writeLong(0L);
        }
        out.writeByte(0);
        out.writeVarInt(PLAINS);

        assertThrows(MalformedChunkDataException.class, () -> CODEC.read(new ByteReader(out.toByteArray())));
    }

    @Test
    void rejectsANegativeRegistryId() {
        ByteWriter out = new ByteWriter(8);
        out.writeShort(0);
        out.writeByte(0);
        out.writeVarInt(-1);

        assertThrows(MalformedChunkDataException.class, () -> CODEC.read(new ByteReader(out.toByteArray())));
    }

    private static ChunkSection section(int[] states, int nonAirCount) {
        return new ChunkSection(
                PalettedValues.of(states, FORMATS.blockStates()), PalettedValues.of(filledBiomes(), FORMATS.biomes()), nonAirCount, 0);
    }

    private static int[] filled(int state) {
        int[] states = new int[4096];
        Arrays.fill(states, state);
        return states;
    }

    private static int[] filledBiomes() {
        int[] biomes = new int[64];
        Arrays.fill(biomes, PLAINS);
        return biomes;
    }

    private static byte[] encode(ChunkSection section) {
        ByteWriter out = new ByteWriter(64);
        CODEC.write(section, out);
        return out.toByteArray();
    }

    private static ChunkSection roundTrip(ChunkSection section) {
        ByteReader in = new ByteReader(encode(section));
        ChunkSection read = CODEC.read(in);
        assertEquals(0, in.remaining());
        return read;
    }
}
