package tlm.dev.basalt.nms.port;

public interface InboundGate {

    boolean consumes(Object message);
}
