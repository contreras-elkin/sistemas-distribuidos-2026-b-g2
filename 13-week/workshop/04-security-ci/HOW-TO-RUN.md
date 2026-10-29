# CP4 - Security gates and negative authorization test

## What is here

| File | Purpose |
|---|---|
| `security.yml` | GitHub Actions workflow on `pull_request`: gitleaks, OWASP dependency-check, authorization tests |
| `transactions-authz/` | Minimal Spring Boot module of `transactions-api` with the E-44 route and its security rule |
| `transactions-authz/src/test/.../AdminTransactionsAuthzTest.java` | The negative test |

The real `market-agri-transactions-api` (branch `develop`) only exposes `/health` today, so the
rule and the test live in this minimal module first. They are a proposal for that repository.

## Story and acceptance criterion covered

- Issue: `[security] Only an ADMIN can read the platform transactions and ledger` (https://github.com/code-corhuila/market-agri-docs/issues/88)
- STRIDE **E**, risk 6, label `P1`. Boundary TB2: api-gateway -> transactions-api.
- Criterion 1: `GET /api/v1/transactions/admin/transactions` (E-44) with a `BUYER` or `PRODUCER`
  token returns `403`. Criterion 3: an automated negative test covers it and runs in CI.

| Test | Request | Expected |
|---|---|---|
| `buyerTokenOnE44Returns403` | E-44 with `role = BUYER` | `403` |
| `producerTokenOnE44Returns403` | E-44 with `role = PRODUCER` | `403` |
| `noTokenOnE44Returns401` | E-44 without `Authorization` | `401` |
| `adminTokenOnE44Returns200` | E-44 with `role = ADMIN` (control case) | `200` |

## How to run it locally

Requirements: JDK 21 and Maven 3.9+.

```bash
cd 13-week/workshop/04-security-ci/transactions-authz
mvn test -Dtest=AdminTransactionsAuthzTest
```

The `JwtDecoder` is mocked, so no auth key is needed; the mapping of our `role` claim to
`ROLE_<role>` in `SecurityConfig` is the real code under test.

## Console output

Pending (CP8).
