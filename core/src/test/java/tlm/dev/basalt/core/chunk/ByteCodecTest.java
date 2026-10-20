package tlm.dev.basalt.core.chunk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ByteCodecTest {

    @Test
    void writesNumbersBigEndian() {
        ByteWriter writer = new ByteWriter(0);
        writer.writeShort(0x0102);
        writer.writeInt(0x03040506);
        writer.writeLong(0x0708090A0B0C0D0EL);

        assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14}, writer.toByteArray());
    }

    @Test
    void writesVarIntsLikeTheProtocol() {
        assertArrayEquals(new byte[] {0}, varInt(0));
        assertArrayEquals(new byte[] {127}, varInt(127));
        assertArrayEquals(new byte[] {(byte) 0x80, 1}, varInt(128));
        assertArrayEquals(new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x0F}, varInt(-1));
    }

    @Test
    void readsBackWhatItWrote() {
        ByteWriter writer = new ByteWriter(4);
        writer.writeByte(200);
        writer.writeShort(-2);
        writer.writeInt(Integer.MIN_VALUE);
        writer.writeLong(Long.MAX_VALUE);
        writer.writeVarInt(Integer.MAX_VALUE);
        writer.writeVarInt(-5);
        writer.writeBytes(new byte[] {9, 8});

        ByteReader reader = new ByteReader(writer.toByteArray());

        assertEquals(200, reader.readUnsignedByte());
        assertEquals(-2, reader.readShort());
        assertEquals(Integer.MIN_VALUE, reader.readInt());
        assertEquals(Long.MAX_VALUE, reader.readLong());
        assertEquals(Integer.MAX_VALUE, reader.readVarInt());
        assertEquals(-5, reader.readVarInt());
        assertArrayEquals(new byte[] {9, 8}, reader.readBytes(2));
        assertEquals(0, reader.remaining());
    }

    @Test
    void growsPastItsInitialCapacity() {
        ByteWriter writer = new ByteWriter(1);
        for (int index = 0; index < 1000; index++) {
            writer.writeLong(index);
        }

        assertEquals(8000, writer.size());
    }

    @Test
    void rejectsReadsPastTheEnd() {
        assertThrows(MalformedChunkDataException.class, () -> new ByteReader(new byte[3]).readInt());
        assertThrows(MalformedChunkDataException.class, () -> new ByteReader(new byte[0]).readUnsignedByte());
        assertThrows(MalformedChunkDataException.class, () -> new ByteReader(new byte[2]).readBytes(3));
    }

    @Test
    void rejectsNegativeByteCounts() {
        assertThrows(MalformedChunkDataException.class, () -> new ByteReader(new byte[2]).readBytes(-1));
    }

    @Test
    void rejectsVarIntsLongerThanFiveBytes() {
        byte[] sixBytes = {(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, 1};

        assertThrows(MalformedChunkDataException.class, () -> new ByteReader(sixBytes).readVarInt());
    }

    @Test
    void rejectsATruncatedVarInt() {
        assertThrows(MalformedChunkDataException.class, () -> new ByteReader(new byte[] {(byte) 0x80}).readVarInt());
    }

    private static byte[] varInt(int value) {
        ByteWriter writer = new ByteWriter(5);
        writer.writeVarInt(value);
        return writer.toByteArray();
    }
}
