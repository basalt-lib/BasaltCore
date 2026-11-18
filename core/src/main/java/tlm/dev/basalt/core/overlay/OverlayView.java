package tlm.dev.basalt.core.overlay;

public final class OverlayView<L> {

    private volatile OverlaySnapshot<L> snapshot = OverlaySnapshot.none();

    public OverlaySnapshot<L> current() {
        return snapshot;
    }

    void publish(OverlaySnapshot<L> published) {
        snapshot = published;
    }
}
