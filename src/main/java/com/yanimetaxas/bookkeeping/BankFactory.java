package com.yanimetaxas.bookkeeping;

import com.yanimetaxas.bookkeeping.service.AccountService;
import com.yanimetaxas.bookkeeping.service.TransferService;

/**
 * Factory interface for creating instances of central business services.
 *
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public interface BankFactory {

  /**
   * Configure the data source properties
   */
  void configureDataSource(ConnectionOptions options) throws Exception;

  /**
   * @return an instance of the AccountService providing account management
   */
  AccountService getAccountService();

  /**
   * @return an instance of the TransferService providing account transfers
   */
  TransferService getTransferService();

  /**
   * Support method for setting up the initial state in a persistent store. Targeted
   * for testing only.
   */
  void setupInitialData(ConnectionOptions options);
}
