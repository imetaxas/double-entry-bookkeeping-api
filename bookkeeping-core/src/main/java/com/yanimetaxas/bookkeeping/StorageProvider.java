package com.yanimetaxas.bookkeeping;

import com.yanimetaxas.bookkeeping.service.AccountService;
import com.yanimetaxas.bookkeeping.service.TransferService;
import java.time.Clock;

/**
 * SPI for pluggable storage backends. The core module provides an in-memory implementation;
 * the JDBC module provides a database-backed implementation.
 *
 * @author yanimetaxas
 * @since 1.0.0
 */
public interface StorageProvider {

  /**
   * Creates any missing schema, binds {@code clientRef}, and returns services for that client.
   * Concurrent calls for different names each receive their own services. {@link #accountService()}
   * and {@link #transferService()} then return the most recently opened session on every thread.
   *
   * @param clientRef stable storage identity, normally the ledger name
   * @return services that stay bound to {@code clientRef}
   */
  StorageSession open(String clientRef);

  /**
   * Opens {@code clientRef} and stamps new transactions with {@code clock}. The clock applies only
   * to the returned session. A later {@link #open(String)} uses the clock from {@link
   * #setClock(Clock)}.
   *
   * @param clientRef stable storage identity, normally the ledger name
   * @param clock clock captured by the returned transfer service
   * @return services that stay bound to {@code clientRef}
   */
  StorageSession open(String clientRef, Clock clock);

  /**
   * Returns the account service from the most recently opened session.
   */
  AccountService accountService();

  /**
   * Returns the transfer service from the most recently opened session.
   */
  TransferService transferService();

  /**
   * Creates any missing schema and binds this provider to {@code clientRef}.
   *
   * <p>Calling this again with the same reference preserves existing accounts and transactions.
   *
   * @param clientRef stable storage identity, normally the ledger name
   */
  default void initialize(String clientRef) {
    open(clientRef);
  }

  /**
   * Sets the clock used by a later {@link #open(String)} on every thread. It does not change a
   * session that is already open, and it is not kept for the calling thread alone.
   *
   * @param clock the clock (must not be null)
   */
  void setClock(Clock clock);

  /** Destroy all data (tables, in-memory state). */
  void dropAll();
}
