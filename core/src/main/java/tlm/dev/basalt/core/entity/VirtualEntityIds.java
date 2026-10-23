package tlm.dev.basalt.core.entity;

import java.util.function.IntSupplier;

public final class VirtualEntityIds {

    private final IntSupplier source;
    private int last;

    public VirtualEntityIds(IntSupplier source) {
        this.source = source;
    }

    public int next() {
        int id = source.getAsInt();
        if (id <= last) {
            throw new IllegalStateException("the id source went back from " + last + " to " + id);
        }
        last = id;
        return id;
    }
}
