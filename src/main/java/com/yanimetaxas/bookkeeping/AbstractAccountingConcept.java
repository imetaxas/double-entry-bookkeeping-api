package com.yanimetaxas.bookkeeping;

import static com.yanimetaxas.bookkeeping.ConnectionOptions.EMBEDDED_H2_CONNECTION;

import com.yanimetaxas.bookkeeping.exception.InfrastructureException;
import com.yanimetaxas.bookkeeping.service.AccountService;
import com.yanimetaxas.bookkeeping.service.TransferService;

/**
 * @author yanimetaxas
 * @since 03-Feb-18
 */
public abstract class AbstractAccountingConcept implements Initializable {

  private TransferService transferService;
  private AccountService accountService;

  public AbstractAccountingConcept(String className) throws InfrastructureException {
    init(className, EMBEDDED_H2_CONNECTION);
  }

  public AbstractAccountingConcept(String className, ConnectionOptions options)
      throws InfrastructureException {
    init(className, options);
  }

  @Override
  public void init(String className, ConnectionOptions options) throws InfrastructureException {
    BankFactory bankFactory;
    try {
      bankFactory = (BankFactory) Class.forName(className).newInstance();
      transferService = bankFactory.getTransferService();
      accountService = bankFactory.getAccountService();
    } catch (Exception e) {
      throw new InfrastructureException(e.getCause());
    }
    bankFactory.setupInitialData(options);
  }

  public TransferService getTransferService() {
    return transferService;
  }

  public AccountService getAccountService() {
    return accountService;
  }
}
