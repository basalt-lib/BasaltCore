package tlm.dev.basalt.core.blueprint;

import tlm.dev.basalt.core.position.BlockBox;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public final class PlanService {

    private final BlockNames names;
    private final RealBlocks real;
    private final PlanSettings settings;

    public PlanService(BlockNames names, RealBlocks real, PlanSettings settings) {
        this.names = names;
        this.real = real;
        this.settings = settings;
    }

    public BlockPlan read(InputStream in) throws IOException {
        return PlanFiles.read(GzipBytes.read(in, settings.maxFileBytes()), settings, names);
    }

    public BlockPlan capture(UUID world, BlockBox box) {
        if (box.volume() > settings.maxBlocks()) {
            throw new IllegalArgumentException("the box holds " + box.volume() + " blocks, more than the limit of " + settings.maxBlocks());
        }
        PlanSize size = new PlanSize(box.max().x() - box.min().x() + 1, box.max().y() - box.min().y() + 1, box.max().z() - box.min().z() + 1);
        int[] states = real.states(world, box);
        if (states.length != size.volume()) {
            throw new IllegalStateException("the real world gave " + states.length + " blocks for a box of " + size);
        }
        return BlockPlan.ofStates(size, states, names::isAir);
    }

    public PlanSettings settings() {
        return settings;
    }
}
