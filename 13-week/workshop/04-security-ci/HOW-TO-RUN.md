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

## Console output (CP8)

Run on my machine on 2026-10-30 (Windows, JDK 21, Maven):

```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running co.corhuila.marketagri.transactions.AdminTransactionsAuthzTest
19:32:08.715 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [co.corhuila.marketagri.transactions.AdminTransactionsAuthzTest]: AdminTransactionsAuthzTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
19:32:08.908 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration co.corhuila.marketagri.transactions.TransactionsAuthzApplication for test class co.corhuila.marketagri.transactions.AdminTransactionsAuthzTest

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.3.5)

2026-10-30T19:32:09.487-05:00  INFO 15672 --- [           main] c.c.m.t.AdminTransactionsAuthzTest       : Starting AdminTransactionsAuthzTest using Java 21.0.7 with PID 15672 (started by ELKIN in D:\ELKIN\Universidad\Materias2026-b\distribuidos\repositorios\sistemas-distribuidos-2026-b-g2\13-week\workshop\04-security-ci\transactions-authz)
2026-10-30T19:32:09.489-05:00  INFO 15672 --- [           main] c.c.m.t.AdminTransactionsAuthzTest       : No active profile set, falling back to 1 default profile: "default"
WARNING: A Java agent has been loaded dynamically (C:\Users\ELKIN\.m2\repository\net\bytebuddy\byte-buddy-agent\1.14.19\byte-buddy-agent-1.14.19.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
Java HotSpot(TM) 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
2026-10-30T19:32:13.368-05:00  INFO 15672 --- [           main] o.s.b.t.m.w.SpringBootMockServletContext : Initializing Spring TestDispatcherServlet ''
2026-10-30T19:32:13.369-05:00  INFO 15672 --- [           main] o.s.t.web.servlet.TestDispatcherServlet  : Initializing Servlet ''
2026-10-30T19:32:13.370-05:00  INFO 15672 --- [           main] o.s.t.web.servlet.TestDispatcherServlet  : Completed initialization in 1 ms
2026-10-30T19:32:13.412-05:00  INFO 15672 --- [           main] c.c.m.t.AdminTransactionsAuthzTest       : Started AdminTransactionsAuthzTest in 4.299 seconds (process running for 6.115)
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.327 s -- in co.corhuila.marketagri.transactions.AdminTransactionsAuthzTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  12.087 s
[INFO] Finished at: 2026-10-30T19:32:13-05:00
[INFO] ------------------------------------------------------------------------
```

Result: Tests run: 4, Failures: 0, Errors: 0, Skipped: 0

## Workflow validation (CP8)

`security.yml` checked with [actionlint](https://github.com/rhysd/actionlint) 1.7.7:

```text
$ actionlint 13-week/workshop/04-security-ci/security.yml
$ echo $?
0
```

No findings: the three jobs, the `pull_request` trigger, the `needs` dependency and the
`${{ env.MODULE }}` expressions are valid. The workflow lives in this folder as a proposal; to see
it run on GitHub it must be copied to `.github/workflows/security.yml`.
