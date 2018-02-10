package com.project.springjta.doubleentry;

/**
 * @author yanimetaxas
 * @since 03-Feb-18
 */
public abstract class AbstractAccountingConcept implements Initializable {

  public BankFactory bankFactory;
  private TransferService transferService;
  private AccountService accountService;

  public AbstractAccountingConcept(String className) throws InfrastructureException {
    init(className);
  }

  @Override
  public void init(String className) throws InfrastructureException {
    try {
      bankFactory = (BankFactory) Class.forName(className).newInstance();
      transferService = bankFactory.getTransferService();
      accountService = bankFactory.getAccountService();
    } catch (Exception e) {
      throw new InfrastructureException(e.getCause());
    }
    bankFactory.setupInitialData();
  }

  public TransferService getTransferService() {
    return transferService;
  }

  public AccountService getAccountService() {
    return accountService;
  }
}
