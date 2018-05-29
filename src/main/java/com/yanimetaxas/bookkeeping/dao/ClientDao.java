package com.yanimetaxas.bookkeeping.dao;

import java.util.Date;

/**
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public interface ClientDao {

  void createClient(String clientRef, Date creationDate);

  void truncateTables();
}
