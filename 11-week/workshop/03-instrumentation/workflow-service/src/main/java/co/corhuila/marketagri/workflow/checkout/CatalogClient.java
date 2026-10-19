package co.corhuila.marketagri.workflow.checkout;

import co.corhuila.marketagri.workflow.observability.StructuredLogger;
import co.corhuila.marketagri.workflow.observability.TraceContext;
import co.corhuila.marketagri.workflow.observability.TraceContextFilter;
import co.corhuila.marketagri.workflow.observability.TraceContextHolder;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Saga step RESERVE_STOCK: calls catalog I-03 carrying the trace context downstream. */
@Component
public class CatalogClient {

    private final RestClient http;
    private final StructuredLogger log = new StructuredLogger();

    public CatalogClient(RestClient.Builder builder, @Value("${catalog.base-url}") String baseUrl) {
        this.http = builder.baseUrl(baseUrl)
                .requestInterceptor((request, body, execution) -> {
                    TraceContext current = TraceContextHolder.get();
                    if (current != null) {
                        TraceContext span = current.child();
                        request.getHeaders().set(TraceContextFilter.TRACEPARENT, span.toTraceparent());
                        request.getHeaders().set(TraceContextFilter.CORRELATION_ID, span.correlationId());
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    public Map<?, ?> reserve(String productId, String transactionId, double quantity) {
        long start = System.nanoTime();
        Map<?, ?> view = http.post()
                .uri("/internal/v1/catalog/products/{id}/reservations", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("transactionId", transactionId, "quantity", quantity))
                .retrieve()
                .body(Map.class);
        log.info("catalog.reservation.created", "stock reserved in catalog",
                (System.nanoTime() - start) / 1_000_000,
                Map.of("transaction_id", transactionId, "product_id", productId, "saga_step", "RESERVE_STOCK"));
        return view;
    }
}
