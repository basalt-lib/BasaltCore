package tlm.dev.basalt.core.chunk;

import java.util.Arrays;

public final class ByteReader {

    private static final int VAR_INT_MAX_BYTES = 5;

    private final byte[] bytes;
    private int position;

    public ByteReader(byte[] bytes) {
        this.bytes = bytes.clone();
    }

    public int readUnsignedByte() {
        require(1);
        return bytes[position++] & 0xFF;
    }

    public int readShort() {
        require(2);
        int high = bytes[position++] & 0xFF;
        int low = bytes[position++] & 0xFF;
        return (short) (high << 8 | low);
    }

    public int readInt() {
        require(4);
        int value = 0;
        for (int index = 0; index < 4; index++) {
            value = value << 8 | bytes[position++] & 0xFF;
        }
        return value;
    }

    public long readLong() {
        require(8);
        long value = 0;
        for (int index = 0; index < 8; index++) {
            value = value << 8 | bytes[position++] & 0xFFL;
        }
        return value;
    }

    public int readVarInt() {
        int value = 0;
        for (int index = 0; index < VAR_INT_MAX_BYTES; index++) {
            int current = readUnsignedByte();
            value |= (current & 0x7F) << 7 * index;
            if ((current & 0x80) == 0) {
                return value;
            }
        }
        throw new MalformedChunkDataException("VarInt longer than " + VAR_INT_MAX_BYTES + " bytes");
    }

    public byte[] readBytes(int count) {
        if (count < 0) {
            throw new MalformedChunkDataException("negative byte count " + count);
        }
        require(count);
        byte[] read = Arrays.copyOfRange(bytes, position, position + count);
        position += count;
        return read;
    }

    public int remaining() {
        return bytes.length - position;
    }

    private void require(int count) {
        if (remaining() < count) {
            throw new MalformedChunkDataException("needs " + count + " more bytes, " + remaining() + " left");
        }
    }
}
