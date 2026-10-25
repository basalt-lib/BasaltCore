package tlm.dev.basalt.core.visibility;

public record PacketMemory(int packets, long bytes) {

    public static final PacketMemory NONE = new PacketMemory(0, 0);

    public PacketMemory {
        if (packets < 0 || bytes < 0) {
            throw new IllegalArgumentException("a packet memory counts from 0: " + packets + " packets, " + bytes + " bytes");
        }
    }
}
