package co.corhuila.marketagri.workflow.observability;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/** Writes one JSON line per call to stdout, with the fields of the CP2 log contract. */
public class StructuredLogger {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String SERVICE = "workflow-service";
    private static final String VERSION = System.getenv().getOrDefault("APP_VERSION", "2.0.0");
    private static final String ENVIRONMENT = System.getenv().getOrDefault("APP_ENV", "dev");

    public void info(String event, String message, long durationMs, Map<String, ?> domain) {
        write("INFO", event, message, durationMs, domain);
    }

    public void warn(String event, String message, long durationMs, Map<String, ?> domain) {
        write("WARN", event, message, durationMs, domain);
    }

    public void error(String event, String message, long durationMs, Map<String, ?> domain) {
        write("ERROR", event, message, durationMs, domain);
    }

    private void write(String level, String event, String message, long durationMs, Map<String, ?> domain) {
        TraceContext ctx = TraceContextHolder.get();
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("timestamp", Instant.now().truncatedTo(ChronoUnit.MILLIS).toString());
        line.put("level", level);
        line.put("service", SERVICE);
        line.put("version", VERSION);
        line.put("environment", ENVIRONMENT);
        line.put("trace_id", ctx != null ? ctx.traceId() : null);
        line.put("span_id", ctx != null ? ctx.spanId() : null);
        line.put("correlation_id", ctx != null ? ctx.correlationId() : null);
        line.put("event", event);
        line.put("message", message);
        line.put("duration_ms", durationMs);
        line.putAll(domain);
        try {
            System.out.println(JSON.writeValueAsString(line));
        } catch (JsonProcessingException e) {
            System.out.println("{\"level\":\"ERROR\",\"event\":\"log.serialization.failed\"}");
        }
    }
}
