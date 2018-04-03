package com.project.springjta.doubleentry;

import com.project.springjta.doubleentry.model.ConnectionOptions;

/**
 * @author yanimetaxas
 * @since 03-Feb-18
 */
@FunctionalInterface
public interface Initializable {

  void init(String className, ConnectionOptions options) throws Exception;
}
