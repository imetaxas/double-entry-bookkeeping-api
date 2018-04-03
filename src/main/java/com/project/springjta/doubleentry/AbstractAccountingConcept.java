package com.project.springjta.doubleentry;

import static com.project.springjta.doubleentry.model.ConnectionOptions.NO_CONNECTION;

import com.project.springjta.doubleentry.model.ConnectionOptions;

/**
 * @author yanimetaxas
 * @since 03-Feb-18
 */
public abstract class AbstractAccountingConcept implements Initializable {
  private TransferService transferService;
  private AccountService accountService;

  public AbstractAccountingConcept(String className) throws InfrastructureException {
    init(className, NO_CONNECTION);
  }

  public AbstractAccountingConcept(String className, ConnectionOptions options) throws InfrastructureException {
    init(className, options);
  }

  @Override
  public void init(String className, ConnectionOptions options) throws InfrastructureException {
    BankFactory bankFactory;
    try {
      bankFactory = (BankFactory) Class.forName(className).newInstance();
      bankFactory.configureDataSource(options);
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
