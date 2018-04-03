package com.yanimetaxas.doubleentry.model;

import com.yanimetaxas.doubleentry.AbstractAccountingConcept;
import com.yanimetaxas.doubleentry.InfrastructureException;

/**
 * @author yanimetaxas
 * @since 11-Feb-18
 */
public class LedgerMock extends AbstractAccountingConcept {

  public LedgerMock() throws InfrastructureException {
    super("some.fake.package.BankFactoryImpl");
  }
}
