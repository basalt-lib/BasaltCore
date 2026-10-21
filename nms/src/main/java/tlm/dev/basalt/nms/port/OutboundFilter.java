package tlm.dev.basalt.nms.port;

public interface OutboundFilter {

    Object filter(Object message);
}
