package com.yanimetaxas.bookkeeping;

/**
 * Factory for creating {@link StorageProvider} instances from configuration objects.
 *
 * <p>This interface allows JDBC configuration objects (like {@code ConnectionOptions}) to be passed
 * directly to {@link Ledger.LedgerBuilder#options(StorageFactory)}, hiding the concrete provider
 * class from the user.
 *
 * @author yanimetaxas
 * @since 1.0.0
 */
@FunctionalInterface
public interface StorageFactory {

  StorageProvider createStorage();
}
