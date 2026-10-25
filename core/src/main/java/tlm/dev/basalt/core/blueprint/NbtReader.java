package tlm.dev.basalt.core.blueprint;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

final class NbtReader {

    static final int END = 0;
    static final int BYTE = 1;
    static final int SHORT = 2;
    static final int INT = 3;
    static final int LONG = 4;
    static final int FLOAT = 5;
    static final int DOUBLE = 6;
    static final int BYTE_ARRAY = 7;
    static final int STRING = 8;
    static final int LIST = 9;
    static final int COMPOUND = 10;
    static final int INT_ARRAY = 11;
    static final int LONG_ARRAY = 12;
    static final int MAX_DEPTH = 512;

    private final byte[] data;
    private int position;
    private int depth;

    NbtReader(byte[] data) {
        this.data = data;
    }

    void enterRoot() {
        int type = readUnsignedByte();
        if (type != COMPOUND) {
            throw new PlanException("the NBT root is not a compound");
        }
        skipString();
        enterCompound();
    }

    void enterCompound() {
        if (++depth > MAX_DEPTH) {
            throw new PlanException("the NBT data nests deeper than " + MAX_DEPTH + " levels");
        }
    }

    int nextEntry() {
        int type = readUnsignedByte();
        if (type == END) {
            depth--;
            return END;
        }
        checkType(type);
        return type;
    }

    String readName() {
        return readString();
    }

    byte readByte() {
        require(1);
        return data[position++];
    }

    short readShort() {
        require(2);
        short value = (short) ((data[position] & 0xFF) << 8 | data[position + 1] & 0xFF);
        position += 2;
        return value;
    }

    int readInt() {
        require(4);
        int value = (data[position] & 0xFF) << 24 | (data[position + 1] & 0xFF) << 16 | (data[position + 2] & 0xFF) << 8 | data[position + 3] & 0xFF;
        position += 4;
        return value;
    }

    String readString() {
        require(2);
        int length = (data[position] & 0xFF) << 8 | data[position + 1] & 0xFF;
        require(2L + length);
        try {
            String value = new DataInputStream(new ByteArrayInputStream(data, position, 2 + length)).readUTF();
            position += 2 + length;
            return value;
        } catch (IOException malformed) {
            throw new PlanException("an NBT string is not valid modified UTF-8", malformed);
        }
    }

    byte[] readByteArray() {
        int length = readLength(1);
        byte[] value = Arrays.copyOfRange(data, position, position + length);
        position += length;
        return value;
    }

    ListHeader readListHeader() {
        int type = readUnsignedByte();
        checkType(type);
        int length = readInt();
        if (length < 0) {
            throw new PlanException("an NBT list has a negative length " + length);
        }
        if (type == END && length > 0) {
            throw new PlanException("an NBT list of " + length + " entries has no type");
        }
        require((long) length * minimumSize(type));
        return new ListHeader(type, length);
    }

    void skip(int type) {
        switch (type) {
            case BYTE -> skipBytes(1);
            case SHORT -> skipBytes(2);
            case INT, FLOAT -> skipBytes(4);
            case LONG, DOUBLE -> skipBytes(8);
            case BYTE_ARRAY -> skipBytes(readLength(1));
            case INT_ARRAY -> skipBytes(readLength(4) * 4L);
            case LONG_ARRAY -> skipBytes(readLength(8) * 8L);
            case STRING -> skipString();
            case LIST -> skipList();
            case COMPOUND -> skipCompound();
            default -> throw new PlanException("unknown NBT tag type " + type);
        }
    }

    boolean exhausted() {
        return position == data.length;
    }

    record ListHeader(int type, int length) {
    }

    private void skipList() {
        ListHeader header = readListHeader();
        if (++depth > MAX_DEPTH) {
            throw new PlanException("the NBT data nests deeper than " + MAX_DEPTH + " levels");
        }
        for (int index = 0; index < header.length(); index++) {
            skip(header.type());
        }
        depth--;
    }

    private void skipCompound() {
        enterCompound();
        int type;
        while ((type = nextEntry()) != END) {
            skipString();
            skip(type);
        }
    }

    private void skipString() {
        require(2);
        int length = (data[position] & 0xFF) << 8 | data[position + 1] & 0xFF;
        skipBytes(2L + length);
    }

    private int readLength(int elementSize) {
        int length = readInt();
        if (length < 0) {
            throw new PlanException("an NBT array has a negative length " + length);
        }
        require((long) length * elementSize);
        return length;
    }

    private void skipBytes(long count) {
        require(count);
        position += (int) count;
    }

    private int readUnsignedByte() {
        require(1);
        return data[position++] & 0xFF;
    }

    private void require(long count) {
        if (count > data.length - position) {
            throw new PlanException("the NBT data is truncated");
        }
    }

    private static void checkType(int type) {
        if (type > LONG_ARRAY) {
            throw new PlanException("unknown NBT tag type " + type);
        }
    }

    private static int minimumSize(int type) {
        return switch (type) {
            case END -> 0;
            case BYTE, COMPOUND -> 1;
            case SHORT, STRING -> 2;
            case INT, FLOAT, BYTE_ARRAY, INT_ARRAY, LONG_ARRAY -> 4;
            case LIST -> 5;
            default -> 8;
        };
    }
}
