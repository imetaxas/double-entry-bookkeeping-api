package com.yanimetaxas.bookkeeping;

import com.yanimetaxas.bookkeeping.dao.AccountDao;
import com.yanimetaxas.bookkeeping.dao.ClientDao;
import com.yanimetaxas.bookkeeping.dao.TransactionDao;
import com.yanimetaxas.bookkeeping.model.ConnectionOptions;
import com.yanimetaxas.bookkeeping.util.BankContextUtil;
import java.util.Date;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

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
  public void configureDataSource(ConnectionOptions options) {
    try {
      DriverManagerDataSource dataSource = BankContextUtil.getBean("dataSource");
      dataSource.setDriverClassName(options.getDriverClassName());
      dataSource.setUrl(options.getUrl());
      dataSource.setUsername(options.getUsername());
      dataSource.setPassword(options.getPassword());

      ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
      databasePopulator.setContinueOnError(true);
      databasePopulator.addScript(new ClassPathResource("db/" + options.getSchema()));

      DatabasePopulatorUtils.execute(databasePopulator, dataSource);
    } catch (Exception e) {
      throw new InfrastructureException(e);
    }
  }

  @Override
  public void setupInitialData(ConnectionOptions options) {
    configureDataSource(options);

    String clientRef = "Client_" + System.currentTimeMillis();

    ClientDao clientDao = BankContextUtil.getBean("clientDao");
    clientDao.truncateTables();
    clientDao.createClient(clientRef, new Date());

    AccountDao accountDao = BankContextUtil.getBean("accountDao");
    accountDao.truncateTables();
    accountDao.setClientRef(clientRef);

    TransactionDao transactionDao = BankContextUtil.getBean("transactionDao");
    transactionDao.truncateTables();
    transactionDao.setClientRef(clientRef);
  }

}