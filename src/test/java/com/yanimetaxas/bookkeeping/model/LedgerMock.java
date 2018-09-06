package com.yanimetaxas.bookkeeping.model;

import com.yanimetaxas.bookkeeping.AbstractAccountingConcept;
import com.yanimetaxas.bookkeeping.exception.InfrastructureException;

/**
 * @author yanimetaxas
 * @since 11-Feb-18
 */
public class LedgerMock extends AbstractAccountingConcept {

  public LedgerMock() throws InfrastructureException {
    super("some.fake.package.BankFactoryImpl");
  }
}
