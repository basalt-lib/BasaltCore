package tlm.dev.basalt.core.chunk;

public record LocalBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    public LocalBox {
        if (minX < 0 || maxX > 15 || minZ < 0 || maxZ > 15) {
            throw new IndexOutOfBoundsException("x and z of a chunk go from 0 to 15: " + minX + ".." + maxX + ", " + minZ + ".." + maxZ);
        }
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("box " + minX + " " + minY + " " + minZ + " to " + maxX + " " + maxY + " " + maxZ + " is upside down");
        }
    }
}
