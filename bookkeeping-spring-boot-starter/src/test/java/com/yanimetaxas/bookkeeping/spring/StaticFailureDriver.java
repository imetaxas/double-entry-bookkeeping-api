package com.yanimetaxas.bookkeeping.spring;

/** Driver whose static initialization fails, so JDBC setup surfaces an {@link Error}. */
public final class StaticFailureDriver {

  static {
    if (Boolean.parseBoolean("true")) {
      throw new ExceptionInInitializerError("boom");
    }
  }

  private StaticFailureDriver() {}
}
