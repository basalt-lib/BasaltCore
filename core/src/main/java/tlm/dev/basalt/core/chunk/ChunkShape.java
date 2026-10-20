package tlm.dev.basalt.core.chunk;

public record ChunkShape(int minY, int sectionCount) {

    public static final int LOWEST_Y = -2032;
    public static final int HIGHEST_Y = 2032;

    public ChunkShape {
        if (minY % 16 != 0 || minY < LOWEST_Y) {
            throw new IllegalArgumentException("min y must be a multiple of 16 from " + LOWEST_Y + ": " + minY);
        }
        if (sectionCount < 1 || minY + sectionCount * 16 > HIGHEST_Y) {
            throw new IllegalArgumentException("section count out of range: " + sectionCount + " from y " + minY);
        }
    }

    public int height() {
        return sectionCount * 16;
    }

    public boolean containsY(int y) {
        return y >= minY && y < minY + height();
    }

    public int sectionIndex(int y) {
        if (!containsY(y)) {
            throw new IndexOutOfBoundsException("y " + y + " outside " + minY + ".." + (minY + height() - 1));
        }
        return (y - minY) >> 4;
    }

    public int heightmapBits() {
        return PackedLongs.ceilLog2(height() + 1);
    }

    public int lightSectionCount() {
        return sectionCount + 2;
    }
}
