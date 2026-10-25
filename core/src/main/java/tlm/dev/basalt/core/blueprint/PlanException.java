package tlm.dev.basalt.core.blueprint;

public final class PlanException extends RuntimeException {

    public PlanException(String message) {
        super(message);
    }

    public PlanException(String message, Throwable cause) {
        super(message, cause);
    }
}
