package tlm.dev.basalt.plugin.facade;

import tlm.dev.basalt.api.Blueprint;
import tlm.dev.basalt.core.blueprint.BlockPlan;

final class BlueprintFacade implements Blueprint {

    private final BlockPlan plan;

    BlueprintFacade(BlockPlan plan) {
        this.plan = plan;
    }

    @Override
    public int sizeX() {
        return plan.size().sizeX();
    }

    @Override
    public int sizeY() {
        return plan.size().sizeY();
    }

    @Override
    public int sizeZ() {
        return plan.size().sizeZ();
    }

    @Override
    public int blockCount() {
        return plan.blockCount();
    }

    BlockPlan plan() {
        return plan;
    }

    @Override
    public String toString() {
        return "Blueprint " + plan.size() + " with " + plan.blockCount() + " blocks";
    }
}
