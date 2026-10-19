package co.corhuila.marketagri.workflow.checkout;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

/** Reduced E-40: only the RESERVE_STOCK step, enough to show the trace and the two metrics. */
@RestController
public class CheckoutController {

    public record CheckoutRequest(String productId, double quantity) { }

    private final CatalogClient catalog;
    private final MeterRegistry registry;
    private final Timer latency;

    public CheckoutController(CatalogClient catalog, MeterRegistry registry) {
        this.catalog = catalog;
        this.registry = registry;
        this.latency = Timer.builder("workflow.checkout.request.duration")
                .description("Latency of POST /api/v1/workflow/checkouts")
                .tag("route", "/api/v1/workflow/checkouts")
                .serviceLevelObjectives(Duration.ofMillis(100), Duration.ofMillis(250), Duration.ofMillis(500),
                        Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3), Duration.ofSeconds(5))
                .register(registry);
    }

    @PostMapping("/api/v1/workflow/checkouts")
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest req) {
        return latency.record(() -> {
            String transactionId = UUID.randomUUID().toString();
            try {
                Map<?, ?> reservation = catalog.reserve(req.productId(), transactionId, req.quantity());
                count("success");
                return ResponseEntity.status(201).body(Map.of("transactionId", transactionId, "reservation", reservation));
            } catch (RestClientException e) {
                count("error");
                return ResponseEntity.status(503).body(Map.of("error", "CATALOG_UNAVAILABLE",
                        "message", "El catálogo no está disponible. Intente de nuevo."));
            }
        });
    }

    private void count(String outcome) {
        Counter.builder("workflow.checkout.requests")
                .description("Checkout requests by outcome")
                .tag("route", "/api/v1/workflow/checkouts")
                .tag("outcome", outcome)
                .register(registry)
                .increment();
    }
}
