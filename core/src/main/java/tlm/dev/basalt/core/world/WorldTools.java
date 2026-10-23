package tlm.dev.basalt.core.world;

public record WorldTools<C>(WorldChunks<C> chunks, RealChunks real, WorldViewers viewers, WorldListener listener) {

    public WorldTools {
        if (chunks == null || real == null || viewers == null || listener == null) {
            throw new IllegalArgumentException("world tools need the chunks, the real chunks, the viewers and the listener");
        }
    }
}
