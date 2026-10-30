# Contributing

Contributions are welcome! Here's how to get started:

## Getting Started

1. **Fork** this repository
2. **Clone** your fork locally
3. **Create a branch** for your change (`git checkout -b my-feature`)
4. **Make your changes** and add tests
5. **Run the test suite** to make sure everything passes:
   ```bash
   mvn -T 1C verify
   ```
6. **Push** your branch and open a **Pull Request**

## Development Requirements

- Java 17+
- Maven 3.8+
- Docker (for integration tests only)

## Code Style

- Follow existing code conventions
- All public API classes and methods should have Javadoc
- New features should include tests

## Tests

- **Assertions** use [RealityCheck](https://github.com/imetaxas/realitycheck) 1.1.0 (`io.github.imetaxas:realitycheck-core`). Import `io.github.imetaxas.realitycheck.RealityAssertions`
- **Unit tests** run with JUnit Jupiter via `mvn test` (no external dependencies). RealityCheck does not replace the test runner
- **Validation without Docker** is `mvn -T 1C verify -DskipITs`
- **Integration tests** (`*IT.java`) run with `mvn -T 1C verify` and require Docker (Testcontainers 2.0.5). MySQL and PostgreSQL start together. Those tests assert with RealityCheck
- All new code should have corresponding test coverage

## Reporting Issues

Use [GitHub Issues](https://github.com/imetaxas/double-entry-bookkeeping-api/issues) to report bugs or request features.

Thank you for contributing!
