package tlm.dev.basalt.plugin.facade;

import tlm.dev.basalt.api.Basalt;

public final class BasaltFacade implements Basalt {

    private final String version;

    public BasaltFacade(String version) {
        this.version = version;
    }

    @Override
    public String version() {
        return version;
    }
}
