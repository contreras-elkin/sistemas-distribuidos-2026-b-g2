package co.corhuila.marketagri.transactions;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** E-44 stub: only the route matters for the authorization test. */
@RestController
class AdminTransactionsController {

  @GetMapping("/api/v1/transactions/admin/transactions")
  Map<String, Object> list() {
    return Map.of("data", List.of(), "meta", Map.of("page", 1, "limit", 20, "total", 0));
  }
}
