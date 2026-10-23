package tlm.dev.basalt.core.entity;

public record EntityTools(VirtualEntityIds ids, EntityDisplay display, EntityViews views) {

    public EntityTools {
        if (ids == null || display == null || views == null) {
            throw new IllegalArgumentException("entity tools need the ids, the display and the views");
        }
    }
}
