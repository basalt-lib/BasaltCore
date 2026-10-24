package tlm.dev.basalt.core.entity;

public record EntityTools(VirtualEntityIds ids, EntityDisplay display, EntityViews views, EntityEvents events) {

    public EntityTools {
        if (ids == null || display == null || views == null || events == null) {
            throw new IllegalArgumentException("entity tools need the ids, the display, the views and the events");
        }
    }
}
