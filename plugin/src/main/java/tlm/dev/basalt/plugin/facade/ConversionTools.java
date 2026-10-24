package tlm.dev.basalt.plugin.facade;

import tlm.dev.basalt.core.entity.EntityShapes;
import tlm.dev.basalt.nms.platform.MojangBlockStates;

public record ConversionTools(MojangBlockStates blocks, EntityShapes entities) {

    public ConversionTools {
        if (blocks == null || entities == null) {
            throw new IllegalArgumentException("the conversions need the block states and the entity shapes");
        }
    }
}
