package com.project.springjta.doubleentry;

/**
 * @author yanimetaxas
 * @since 03-Feb-18
 */
@FunctionalInterface
public interface Initializable {

  void init(String className) throws Exception;
}
