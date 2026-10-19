package co.corhuila.marketagri.workflow.observability;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.regex.Pattern;

/** W3C trace context: traceparent = 00-{trace_id 32 hex}-{span_id 16 hex}-{flags}. */
public record TraceContext(String traceId, String spanId, String correlationId) {

    private static final Pattern TRACEPARENT =
            Pattern.compile("^00-([0-9a-f]{32})-([0-9a-f]{16})-[0-9a-f]{2}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Continues the caller's trace with a new span of this service, or starts a new trace. */
    public static TraceContext fromIncoming(String traceparent, String correlationId) {
        String corr = correlationId != null ? correlationId : java.util.UUID.randomUUID().toString();
        if (traceparent != null) {
            var m = TRACEPARENT.matcher(traceparent.trim());
            if (m.matches()) {
                return new TraceContext(m.group(1), randomHex(8), corr);
            }
        }
        return new TraceContext(randomHex(16), randomHex(8), corr);
    }

    /** Child span for an outgoing call: same trace_id, new span_id. */
    public TraceContext child() {
        return new TraceContext(traceId, randomHex(8), correlationId);
    }

    public String toTraceparent() {
        return "00-" + traceId + "-" + spanId + "-01";
    }

    private static String randomHex(int bytes) {
        byte[] b = new byte[bytes];
        RANDOM.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }
}
