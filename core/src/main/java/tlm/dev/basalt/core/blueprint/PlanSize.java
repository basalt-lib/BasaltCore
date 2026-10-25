package tlm.dev.basalt.core.blueprint;

public record PlanSize(int sizeX, int sizeY, int sizeZ) {

    public PlanSize {
        if (sizeX < 1 || sizeY < 1 || sizeZ < 1) {
            throw new IllegalArgumentException("a plan is at least one block on each side: " + sizeX + " x " + sizeY + " x " + sizeZ);
        }
    }

    public long volume() {
        return (long) sizeX * sizeY * sizeZ;
    }

    public int index(int x, int y, int z) {
        return x + z * sizeX + y * sizeX * sizeZ;
    }

    public boolean contains(int x, int y, int z) {
        return x >= 0 && x < sizeX && y >= 0 && y < sizeY && z >= 0 && z < sizeZ;
    }

    @Override
    public String toString() {
        return sizeX + " x " + sizeY + " x " + sizeZ;
    }
}
