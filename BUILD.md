# Build

## Build from Source

```bash
mvn -T 1C verify
```

Assertions use RealityCheck 1.1.0. JUnit Jupiter runs the tests. The full build uses one Maven thread per core and starts the MySQL and PostgreSQL integration tests together.

## Run Unit Tests Only

```bash
mvn test
```

## Validate without Docker

```bash
mvn -T 1C verify -DskipITs
```

## Run Integration Tests (requires Docker)

```bash
mvn -T 1C verify
```

## Run a documented example

```bash
mvn -pl bookkeeping-examples -am compile -Prun-example \
  -Dexec.mainClass=com.yanimetaxas.bookkeeping.examples.ECommerceLedgerExample
```

The same command runs the other classes in `bookkeeping-examples`. `SupportedDatabasesExample` posts to embedded H2, HSQLDB, and Derby on every run. Add `-Dbookkeeping.example.mysql.url` or `-Dbookkeeping.example.postgres.url` to post to those servers as well.

## Package without Tests

```bash
mvn package -DskipTests
```

## Release to Maven Central

See [RELEASE.md](RELEASE.md) for the full Maven Central publishing workflow.
