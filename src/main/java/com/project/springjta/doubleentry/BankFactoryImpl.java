package com.project.springjta.doubleentry;

import com.project.springjta.doubleentry.dao.AccountDao;
import com.project.springjta.doubleentry.dao.ClientDao;
import com.project.springjta.doubleentry.dao.TransactionDao;
import com.project.springjta.doubleentry.model.ConnectionOptions;
import com.project.springjta.doubleentry.util.BankContextUtil;
import java.sql.Driver;
import java.util.Date;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

/**
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public class BankFactoryImpl implements BankFactory {

  @Override
  public AccountService getAccountService() {
    return BankContextUtil.getBean("accountService");
  }

  @Override
  public TransferService getTransferService() {
    return BankContextUtil.getBean("transferService");
  }

  @Override
  public void configureDataSource(ConnectionOptions options) throws Exception {
    if (options == ConnectionOptions.NO_CONNECTION) {
      return;
    }
    SimpleDriverDataSource dataSource = BankContextUtil.getBean("dataSource");
    dataSource.setDriver(
        (Driver) Class.forName(options.getDriver().getDriverClassName()).newInstance());
    dataSource.setUrl(options.getUrl());
    dataSource.setUsername(options.getUsername());
    dataSource.setPassword(options.getPassword());
  }

  @Override
  public void setupInitialData() {

    ClientDao clientDao = BankContextUtil.getBean("clientDao");
    clientDao.truncateTables();
    String clientRef = "Client_" + System.currentTimeMillis();
    clientDao.createClient(clientRef, new Date());

    AccountDao accountDao = BankContextUtil.getBean("accountDao");
    accountDao.truncateTables();
    accountDao.setClientRef(clientRef);

    TransactionDao transactionDao = BankContextUtil.getBean("transactionDao");
    transactionDao.truncateTables();
    transactionDao.setClientRef(clientRef);
  }

}