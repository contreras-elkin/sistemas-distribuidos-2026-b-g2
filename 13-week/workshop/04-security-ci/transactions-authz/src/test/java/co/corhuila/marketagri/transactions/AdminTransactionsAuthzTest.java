package co.corhuila.marketagri.transactions;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Negative authorization test for the issue
 * "[security] Only an ADMIN can read the platform transactions and ledger" (STRIDE E, risk 6).
 * The JwtDecoder is mocked so the test does not need auth's RS256 key; the "role" claim mapping is real.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminTransactionsAuthzTest {

  private static final String E44 = "/api/v1/transactions/admin/transactions";

  @Autowired MockMvc mvc;
  @MockBean JwtDecoder jwtDecoder;

  @BeforeEach
  void tokens() {
    given(jwtDecoder.decode("buyer-token")).willReturn(tokenWithRole("BUYER"));
    given(jwtDecoder.decode("producer-token")).willReturn(tokenWithRole("PRODUCER"));
    given(jwtDecoder.decode("admin-token")).willReturn(tokenWithRole("ADMIN"));
  }

  @Test
  void buyerTokenOnE44Returns403() throws Exception {
    mvc.perform(get(E44).header("Authorization", "Bearer buyer-token")).andExpect(status().isForbidden());
  }

  @Test
  void producerTokenOnE44Returns403() throws Exception {
    mvc.perform(get(E44).header("Authorization", "Bearer producer-token")).andExpect(status().isForbidden());
  }

  @Test
  void noTokenOnE44Returns401() throws Exception {
    mvc.perform(get(E44)).andExpect(status().isUnauthorized());
  }

  @Test
  void adminTokenOnE44Returns200() throws Exception {
    mvc.perform(get(E44).header("Authorization", "Bearer admin-token")).andExpect(status().isOk());
  }

  private static Jwt tokenWithRole(String role) {
    return Jwt.withTokenValue(role.toLowerCase() + "-token")
        .header("alg", "RS256")
        .subject("3f1c2a9e-0000-4000-8000-000000000001")
        .claim("role", role)
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();
  }
}
