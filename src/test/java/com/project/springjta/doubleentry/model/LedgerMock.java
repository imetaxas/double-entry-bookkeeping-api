package com.project.springjta.doubleentry.model;

import com.project.springjta.doubleentry.AbstractAccountingConcept;
import com.project.springjta.doubleentry.InfrastructureException;

/**
 * @author yanimetaxas
 * @since 11-Feb-18
 */
public class LedgerMock extends AbstractAccountingConcept {

  public LedgerMock() throws InfrastructureException {
    super("some.fake.package.BankFactoryImpl");
  }
}
