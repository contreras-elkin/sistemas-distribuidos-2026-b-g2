package co.corhuila.marketagri.workflow.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Reads or creates the trace context, keeps it for this request and returns it in the response. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceContextFilter extends OncePerRequestFilter {

    public static final String TRACEPARENT = "traceparent";
    public static final String CORRELATION_ID = "X-Correlation-Id";

    private final StructuredLogger log = new StructuredLogger();

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        TraceContext ctx = TraceContext.fromIncoming(req.getHeader(TRACEPARENT), req.getHeader(CORRELATION_ID));
        TraceContextHolder.set(ctx);
        res.setHeader(TRACEPARENT, ctx.toTraceparent());
        res.setHeader(CORRELATION_ID, ctx.correlationId());
        long start = System.nanoTime();
        try {
            chain.doFilter(req, res);
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000;
            log.info("http.request.completed", req.getMethod() + " " + req.getRequestURI() + " " + res.getStatus(),
                    ms, java.util.Map.of("http_status", res.getStatus()));
            TraceContextHolder.clear();
        }
    }
}
