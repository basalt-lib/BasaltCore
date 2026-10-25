package tlm.dev.basalt.core.blueprint;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.zip.GZIPOutputStream;

final class TestNbt {

    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final DataOutputStream out = new DataOutputStream(bytes);

    static TestNbt root() {
        TestNbt nbt = new TestNbt();
        nbt.header(10, "");
        return nbt;
    }

    TestNbt compound(String name) {
        header(10, name);
        return this;
    }

    TestNbt end() {
        return raw(out -> out.writeByte(0));
    }

    TestNbt putInt(String name, int value) {
        header(3, name);
        return raw(out -> out.writeInt(value));
    }

    TestNbt putShort(String name, int value) {
        header(2, name);
        return raw(out -> out.writeShort(value));
    }

    TestNbt putString(String name, String value) {
        header(8, name);
        return raw(out -> out.writeUTF(value));
    }

    TestNbt putByteArray(String name, byte[] value) {
        header(7, name);
        return raw(out -> {
            out.writeInt(value.length);
            out.write(value);
        });
    }

    TestNbt putIntList(String name, int... values) {
        list(name, 3, values.length);
        for (int value : values) {
            raw(stream -> stream.writeInt(value));
        }
        return this;
    }

    TestNbt list(String name, int type, int length) {
        header(9, name);
        return listHeader(type, length);
    }

    TestNbt listHeader(int type, int length) {
        return raw(out -> {
            out.writeByte(type);
            out.writeInt(length);
        });
    }

    TestNbt raw(Write write) {
        try {
            write.run(out);
        } catch (IOException impossible) {
            throw new UncheckedIOException(impossible);
        }
        return this;
    }

    byte[] bytes() {
        return bytes.toByteArray();
    }

    byte[] gzip() {
        return gzip(bytes());
    }

    static byte[] gzip(byte[] plain) {
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
            gzip.write(plain);
        } catch (IOException impossible) {
            throw new UncheckedIOException(impossible);
        }
        return compressed.toByteArray();
    }

    static byte[] varints(int... values) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int value : values) {
            int rest = value;
            while ((rest & ~0x7F) != 0) {
                out.write(rest & 0x7F | 0x80);
                rest >>>= 7;
            }
            out.write(rest);
        }
        return out.toByteArray();
    }

    private void header(int type, String name) {
        raw(out -> {
            out.writeByte(type);
            out.writeUTF(name);
        });
    }

    @FunctionalInterface
    interface Write {

        void run(DataOutputStream out) throws IOException;
    }
}
