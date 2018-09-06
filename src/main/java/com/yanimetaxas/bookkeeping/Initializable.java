package com.yanimetaxas.bookkeeping;

/**
 * @author yanimetaxas
 * @since 03-Feb-18
 */
@FunctionalInterface
public interface Initializable {

  void init(String className, ConnectionOptions options) throws Exception;
}
