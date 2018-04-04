package com.yanimetaxas.doubleentry;

import com.yanimetaxas.doubleentry.dao.AccountDao;
import com.yanimetaxas.doubleentry.dao.ClientDao;
import com.yanimetaxas.doubleentry.dao.TransactionDao;
import com.yanimetaxas.doubleentry.model.ConnectionOptions;
import com.yanimetaxas.doubleentry.util.BankContextUtil;
import java.sql.Driver;
import java.util.Date;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
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
  public void configureDataSource(ConnectionOptions options) throws Exception {
    SimpleDriverDataSource dataSource = BankContextUtil.getBean("dataSource");
    dataSource.setDriver((Driver) Class.forName(options.getDriver().getDriverClassName()).newInstance());
    dataSource.setUrl(options.getUrl());
    dataSource.setUsername(options.getUsername());
    dataSource.setPassword(options.getPassword());

    DatabasePopulatorUtils.execute(createDatabasePopulator(), dataSource);
  }

  private DatabasePopulator createDatabasePopulator() {
    ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
    databasePopulator.setContinueOnError(true);
    databasePopulator.addScript(new ClassPathResource("db/schema.sql"));
    return databasePopulator;
  }

  @Override
  public void setupInitialData() {
    String clientRef = "Client_" + System.currentTimeMillis();

    ClientDao clientDao = BankContextUtil.getBean("clientDao");
    clientDao.createClient(clientRef, new Date());

    AccountDao accountDao = BankContextUtil.getBean("accountDao");
    accountDao.setClientRef(clientRef);

    TransactionDao transactionDao = BankContextUtil.getBean("transactionDao");
    transactionDao.setClientRef(clientRef);

    try {
      clientDao.truncateTables();
      accountDao.truncateTables();
      transactionDao.truncateTables();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}