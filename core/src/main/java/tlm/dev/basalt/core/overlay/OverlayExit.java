package tlm.dev.basalt.core.overlay;

public enum OverlayExit {
    REQUESTED(true),
    REAL_WORLD_CHANGE(false),
    DISCONNECT(false),
    CLOSED(true),
    SHUTDOWN(true);

    private final boolean restoresView;

    OverlayExit(boolean restoresView) {
        this.restoresView = restoresView;
    }

    public boolean restoresView() {
        return restoresView;
    }
}
