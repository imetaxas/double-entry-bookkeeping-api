package com.yanimetaxas.bookkeeping;

/**
 * @deprecated Since 1.0.0 — use {@code com.yanimetaxas.bookkeeping.jdbc.ConnectionOptions} from
 *     the {@code bookkeeping-jdbc} module instead. This wrapper does not provide the v0.1.0
 *     constants or constructors; those require the JDBC module so that core stays free of database
 *     dependencies. It will be removed in a future release.
 */
@Deprecated(since = "1.0.0", forRemoval = true)
public class ConnectionOptions implements StorageFactory {

  private final StorageFactory delegate;

  /**
   * Wraps a {@link StorageFactory} as a backward-compatible {@code ConnectionOptions} reference.
   *
   * @param delegate the actual storage factory (typically from {@code bookkeeping-jdbc})
   */
  public ConnectionOptions(StorageFactory delegate) {
    this.delegate = delegate;
  }

  @Override
  public StorageProvider createStorage() {
    return delegate.createStorage();
  }
}
