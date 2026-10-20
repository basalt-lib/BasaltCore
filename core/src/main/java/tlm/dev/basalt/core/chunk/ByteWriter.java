package tlm.dev.basalt.core.chunk;

import java.util.Arrays;

public final class ByteWriter {

    private byte[] buffer;
    private int size;

    public ByteWriter(int initialCapacity) {
        buffer = new byte[Math.max(16, initialCapacity)];
    }

    public void writeByte(int value) {
        ensure(1);
        buffer[size++] = (byte) value;
    }

    public void writeShort(int value) {
        ensure(2);
        buffer[size++] = (byte) (value >>> 8);
        buffer[size++] = (byte) value;
    }

    public void writeInt(int value) {
        ensure(4);
        for (int shift = 24; shift >= 0; shift -= 8) {
            buffer[size++] = (byte) (value >>> shift);
        }
    }

    public void writeLong(long value) {
        ensure(8);
        for (int shift = 56; shift >= 0; shift -= 8) {
            buffer[size++] = (byte) (value >>> shift);
        }
    }

    public void writeVarInt(int value) {
        int remaining = value;
        while ((remaining & ~0x7F) != 0) {
            writeByte((remaining & 0x7F) | 0x80);
            remaining >>>= 7;
        }
        writeByte(remaining);
    }

    public void writeBytes(byte[] bytes) {
        ensure(bytes.length);
        System.arraycopy(bytes, 0, buffer, size, bytes.length);
        size += bytes.length;
    }

    public int size() {
        return size;
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(buffer, size);
    }

    private void ensure(int extra) {
        if (size + extra > buffer.length) {
            buffer = Arrays.copyOf(buffer, Math.max(buffer.length * 2, size + extra));
        }
    }
}
