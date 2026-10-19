package co.corhuila.marketagri.workflow.observability;

/** The trace context of the request being handled by this thread. */
public final class TraceContextHolder {
    private static final ThreadLocal<TraceContext> CURRENT = new ThreadLocal<>();

    private TraceContextHolder() { }

    public static void set(TraceContext ctx) { CURRENT.set(ctx); }
    public static TraceContext get() { return CURRENT.get(); }
    public static void clear() { CURRENT.remove(); }
}
