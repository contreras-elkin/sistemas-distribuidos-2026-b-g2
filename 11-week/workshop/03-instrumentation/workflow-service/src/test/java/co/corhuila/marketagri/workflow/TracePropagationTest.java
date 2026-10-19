package co.corhuila.marketagri.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

@SpringBootTest(properties = "catalog.base-url=http://catalog-service:8080")
@AutoConfigureMockMvc
@AutoConfigureObservability
class TracePropagationTest {

    static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    static final String INCOMING = "00-" + TRACE_ID + "-00f067aa0ba902b7-01";

    @TestConfiguration
    static class MockCatalog {
        static final RestClient.Builder BUILDER = RestClient.builder();
        static final MockRestServiceServer SERVER = MockRestServiceServer.bindTo(BUILDER).build();

        @Bean
        RestClient.Builder restClientBuilder() { return BUILDER; }
    }

    @Autowired MockMvc mvc;

    @Test
    void traceIdReceivedIsTheTraceIdSentToCatalog() throws Exception {
        AtomicReference<String> sent = new AtomicReference<>();
        MockCatalog.SERVER.expect(requestTo(
                        "http://catalog-service:8080/internal/v1/catalog/products/p-1/reservations"))
                .andExpect(req -> sent.set(req.getHeaders().getFirst("traceparent")))
                .andRespond(withSuccess("{\"remainingQuantity\":3}", MediaType.APPLICATION_JSON));

        mvc.perform(post("/api/v1/workflow/checkouts")
                        .header("traceparent", INCOMING)
                        .header("X-Correlation-Id", "5f0c2b1e-8d4a-4c3b-9e2f-1a7b6c5d4e3f")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"p-1\",\"quantity\":2}"))
                .andExpect(status().isCreated());

        MockCatalog.SERVER.verify();
        assertThat(sent.get()).startsWith("00-" + TRACE_ID + "-");
        assertThat(sent.get()).isNotEqualTo(INCOMING); // new child span_id

        mvc.perform(get("/actuator/prometheus"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "workflow_checkout_requests_total{outcome=\"success\"")));
    }
}
