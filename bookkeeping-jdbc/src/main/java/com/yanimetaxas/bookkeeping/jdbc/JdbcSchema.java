package com.yanimetaxas.bookkeeping.jdbc;

import com.yanimetaxas.bookkeeping.exception.InfrastructureException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Set;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;

/** Creates and drops the bookkeeping tables for one connection configuration. */
@Slf4j
final class JdbcSchema {

  private final DataSource dataSource;
  private final String schemaScript;

  JdbcSchema(DataSource dataSource, String schemaScript) {
    this.dataSource = dataSource;
    this.schemaScript = schemaScript;
  }

  void ensureSchema() {
    executeScript("db/" + schemaScript);
  }

  void dropAll() {
    String dropScript = schemaScript.replace("-schema.sql", "-drop.sql");
    log.debug("Dropping database using script: {}", dropScript);
    executeScript("db/" + dropScript, true);
  }

  void insertClientIfAbsent(String clientRef, Instant createdAt) {
    if (clientExists(clientRef)) {
      return;
    }
    String sql = "INSERT INTO client (ref, creation_date) VALUES (?, ?)";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, clientRef);
      ps.setTimestamp(2, java.sql.Timestamp.from(createdAt));
      ps.executeUpdate();
    } catch (SQLException e) {
      if (!clientExists(clientRef)) {
        throw SqlExceptionTranslator.infrastructure(e);
      }
    }
  }

  private boolean clientExists(String clientRef) {
    String sql = "SELECT ref FROM client WHERE ref = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, clientRef);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw SqlExceptionTranslator.infrastructure(e);
    }
  }

  private void executeScript(String resourcePath) {
    executeScript(resourcePath, false);
  }

  private void executeScript(String resourcePath, boolean ignoreMissingTable) {
    String sql = loadResource(resourcePath);
    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement()) {
      for (String command : sql.split(";")) {
        String trimmed = command.trim();
        if (trimmed.isEmpty()) {
          continue;
        }
        try {
          stmt.execute(trimmed);
        } catch (SQLException exception) {
          if (ignoreMissingTable && isMissingTable(exception)) {
            continue;
          }
          throw SqlExceptionTranslator.infrastructure(exception);
        }
      }
    } catch (SQLException exception) {
      throw SqlExceptionTranslator.infrastructure(exception);
    }
  }

  private static final Set<String> MISSING_TABLE = Set.of("42Y07", "42Y55", "42X05");

  private static boolean isMissingTable(SQLException exception) {
    return MISSING_TABLE.contains(exception.getSQLState());
  }

  private static String loadResource(String path) {
    InputStream input = JdbcSchema.class.getClassLoader().getResourceAsStream(path);
    if (input == null) {
      throw new InfrastructureException("Resource not found: " + path);
    }
    return readScript(input);
  }

  static String readScript(InputStream input) {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
      StringBuilder script = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        if (!script.isEmpty()) {
          script.append('\n');
        }
        script.append(line);
      }
      return script.toString();
    } catch (IOException exception) {
      throw new InfrastructureException(exception);
    }
  }
}
