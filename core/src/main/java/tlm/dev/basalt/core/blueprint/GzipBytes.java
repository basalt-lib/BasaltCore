package tlm.dev.basalt.core.blueprint;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

final class GzipBytes {

    private static final int FIRST_MAGIC = 0x1f;
    private static final int SECOND_MAGIC = 0x8b;
    private static final int BUFFER = 8192;

    private GzipBytes() {
    }

    static byte[] read(InputStream in, int maxBytes) throws IOException {
        BufferedInputStream buffered = new BufferedInputStream(new KeptOpen(in));
        buffered.mark(2);
        int first = buffered.read();
        int second = buffered.read();
        buffered.reset();
        if (first != FIRST_MAGIC || second != SECOND_MAGIC) {
            throw new PlanException("the file is not gzip compressed NBT");
        }
        byte[] out = new byte[BUFFER];
        int length = 0;
        try (GZIPInputStream gzip = new GZIPInputStream(buffered, BUFFER)) {
            while (true) {
                if (length == out.length) {
                    if (length > maxBytes) {
                        throw tooLarge(maxBytes);
                    }
                    out = Arrays.copyOf(out, (int) Math.min((long) maxBytes + 1, (long) out.length * 2));
                }
                int read = gzip.read(out, length, out.length - length);
                if (read < 0) {
                    break;
                }
                length += read;
                if (length > maxBytes) {
                    throw tooLarge(maxBytes);
                }
            }
        } catch (EOFException | ZipException corrupt) {
            throw new PlanException("the gzip data is truncated or corrupt", corrupt);
        }
        return Arrays.copyOf(out, length);
    }

    private static PlanException tooLarge(int maxBytes) {
        return new PlanException("the file is larger than " + maxBytes + " bytes once decompressed");
    }

    private static final class KeptOpen extends FilterInputStream {

        private KeptOpen(InputStream in) {
            super(in);
        }

        @Override
        public void close() {
        }
    }
}
