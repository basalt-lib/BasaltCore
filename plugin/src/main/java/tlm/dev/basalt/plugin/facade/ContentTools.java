package tlm.dev.basalt.plugin.facade;

import tlm.dev.basalt.core.blueprint.PlanService;
import tlm.dev.basalt.core.entity.EntityShapes;
import tlm.dev.basalt.nms.platform.MojangBlockStates;
import tlm.dev.basalt.nms.platform.MojangLooks;

public record ContentTools(MojangBlockStates blocks, EntityShapes entities, PlanService plans, MojangLooks looks) {

    public ContentTools {
        if (blocks == null || entities == null || plans == null || looks == null) {
            throw new IllegalArgumentException("the content tools need the block states, the entity shapes, the plans and the entity looks");
        }
    }
}
