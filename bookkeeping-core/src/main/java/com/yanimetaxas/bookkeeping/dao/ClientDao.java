package com.yanimetaxas.bookkeeping.dao;

import java.time.Instant;

/**
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public interface ClientDao {

  void createClient(String clientRef, Instant creationDate);
}
