# How to run - workflow-service instrumentation

Module: `workflow-service/` (Java 21, Spring Boot 3.3, Maven). It is a reduced version of the
checkout saga of `market-agri-workflow`: only the `RESERVE_STOCK` step (I-03 to catalog).

## Requirements

- JDK 21 (`java -version`)
- Maven 3.9+ (`mvn -v`)

## Install and test

```bash
cd 11-week/workshop/03-instrumentation/workflow-service
mvn test
```

The test `TracePropagationTest` sends `POST /api/v1/workflow/checkouts` with a fixed
`traceparent`, mocks catalog with `MockRestServiceServer` and checks:

1. the `traceparent` sent to `/internal/v1/catalog/products/{id}/reservations` has the **same
   `trace_id`** as the one received and a **new `span_id`**;
2. `/actuator/prometheus` exposes `workflow_checkout_requests_total{outcome="success",...}`.

## Run

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--catalog.base-url=http://localhost:9090
```

```bash
curl -i -X POST http://localhost:8080/api/v1/workflow/checkouts \
  -H "Content-Type: application/json" \
  -H "traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01" \
  -d '{"productId":"p-1","quantity":2}'
curl -s http://localhost:8080/actuator/prometheus | grep workflow_checkout
```

Without a catalog on port 9090 the answer is `503 CATALOG_UNAVAILABLE` and the counter
`outcome="error"` goes up: that is the error path of the metric.

## Real console output (my machine)

`mvn test`:

```
[INFO] Scanning for projects...
[INFO] 
[INFO] -------< co.corhuila.marketagri:workflow-service-observability >--------
[INFO] Building workflow-service-observability 0.1.0
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ workflow-service-observability ---
[INFO] Copying 1 resource from src\main\resources to target\classes
[INFO] Copying 0 resource from src\main\resources to target\classes
[INFO] 
[INFO] --- compiler:3.13.0:compile (default-compile) @ workflow-service-observability ---
[INFO] Recompiling the module because of added or removed source files.
[INFO] Compiling 7 source files with javac [debug parameters release 21] to target\classes
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ workflow-service-observability ---
[INFO] skip non existing resourceDirectory D:\ELKIN\Universidad\Materias2026-b\distribuidos\repositorios\sistemas-distribuidos-2026-b-g2\11-week\workshop\03-instrumentation\workflow-service\src\test\resources
[INFO] 
[INFO] --- compiler:3.13.0:testCompile (default-testCompile) @ workflow-service-observability ---
[INFO] Recompiling the module because of changed dependency.
[INFO] Compiling 1 source file with javac [debug parameters release 21] to target\test-classes
[INFO] 
[INFO] --- surefire:3.2.5:test (default-test) @ workflow-service-observability ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
Downloading from central: https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-launcher/1.10.5/junit-platform-launcher-1.10.5.pom
Downloaded from central: https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-launcher/1.10.5/junit-platform-launcher-1.10.5.pom (3.0 kB at 4.9 kB/s)
Downloading from central: https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-launcher/1.10.5/junit-platform-launcher-1.10.5.jar
Downloaded from central: https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-launcher/1.10.5/junit-platform-launcher-1.10.5.jar (184 kB at 761 kB/s)
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running co.corhuila.marketagri.workflow.TracePropagationTest
09:45:13.524 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration co.corhuila.marketagri.workflow.WorkflowApplication for test class co.corhuila.marketagri.workflow.TracePropagationTest

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.3.5)

WARNING: A Java agent has been loaded dynamically (C:\Users\ELKIN\.m2\repository\net\bytebuddy\byte-buddy-agent\1.14.19\byte-buddy-agent-1.14.19.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
Java HotSpot(TM) 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
{"timestamp":"2026-10-19T14:45:16.534Z","level":"INFO","service":"workflow-service","version":"2.0.0","environment":"dev","trace_id":"4bf92f3577b34da6a3ce929d0e0e4736","span_id":"ffb19a1e6675c706","correlation_id":"5f0c2b1e-8d4a-4c3b-9e2f-1a7b6c5d4e3f","event":"catalog.reservation.created","message":"stock reserved in catalog","duration_ms":37,"saga_step":"RESERVE_STOCK","product_id":"p-1","transaction_id":"201804ad-0218-4c8f-a940-d1b3a0d20aff"}
{"timestamp":"2026-10-19T14:45:16.547Z","level":"INFO","service":"workflow-service","version":"2.0.0","environment":"dev","trace_id":"4bf92f3577b34da6a3ce929d0e0e4736","span_id":"ffb19a1e6675c706","correlation_id":"5f0c2b1e-8d4a-4c3b-9e2f-1a7b6c5d4e3f","event":"http.request.completed","message":"POST /api/v1/workflow/checkouts 201","duration_ms":148,"http_status":201}
{"timestamp":"2026-10-19T14:45:17.488Z","level":"INFO","service":"workflow-service","version":"2.0.0","environment":"dev","trace_id":"02a543c8ed30211bad5a49babed9314a","span_id":"1f3f0c15e5cbf69a","correlation_id":"2647577f-f7b6-493b-b6fb-79e814cf341e","event":"http.request.completed","message":"GET /actuator/prometheus 200","duration_ms":888,"http_status":200}
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.276 s -- in co.corhuila.marketagri.workflow.TracePropagationTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  9.828 s
[INFO] Finished at: 2026-10-19T09:45:17-05:00
[INFO] ------------------------------------------------------------------------
```

Log lines printed by the test (the `trace_id` is the one sent in `traceparent`, so the CP2 contract holds):

```
{"timestamp":"2026-10-19T14:45:16.534Z","level":"INFO","service":"workflow-service","version":"2.0.0","environment":"dev","trace_id":"4bf92f3577b34da6a3ce929d0e0e4736","span_id":"ffb19a1e6675c706","correlation_id":"5f0c2b1e-8d4a-4c3b-9e2f-1a7b6c5d4e3f","event":"catalog.reservation.created","message":"stock reserved in catalog","duration_ms":37,"saga_step":"RESERVE_STOCK","product_id":"p-1","transaction_id":"201804ad-0218-4c8f-a940-d1b3a0d20aff"}
{"timestamp":"2026-10-19T14:45:16.547Z","level":"INFO","service":"workflow-service","version":"2.0.0","environment":"dev","trace_id":"4bf92f3577b34da6a3ce929d0e0e4736","span_id":"ffb19a1e6675c706","correlation_id":"5f0c2b1e-8d4a-4c3b-9e2f-1a7b6c5d4e3f","event":"http.request.completed","message":"POST /api/v1/workflow/checkouts 201","duration_ms":148,"http_status":201}
```
