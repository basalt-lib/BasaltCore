package tlm.dev.basalt.core.visibility;

public final class InboundFilter {

    private InboundFilter() {
    }

    public static InboundVerdict decide(InboundFacts facts, ViewSnapshot snapshot) {
        if (!snapshot.isVirtual()) {
            return InboundVerdict.PASS;
        }
        return switch (facts.kind()) {
            case OTHER -> InboundVerdict.PASS;
            case ENTITY -> snapshot.holdsEntity(facts.entity()) ? InboundVerdict.CONSUME : InboundVerdict.PASS;
            case BLOCK_ACTION -> snapshot.hides(facts.clicked().chunk()) ? InboundVerdict.CONSUME : InboundVerdict.PASS;
            case USE_ON -> snapshot.hides(facts.clicked().chunk()) || snapshot.hides(facts.clicked().relative(facts.face()).chunk())
                    ? InboundVerdict.CONSUME : InboundVerdict.PASS;
        };
    }
}
