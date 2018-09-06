package com.yanimetaxas.bookkeeping;

import com.yanimetaxas.bookkeeping.exception.InfrastructureException;
import org.junit.Test;

/**
 * @author yanimetaxas
 * @since 07-Apr-18
 */
public class BankFactoryImplTest {

  @Test(expected = InfrastructureException.class)
  public void testConfigureDataSourceWhenDriverIsIllegal() throws Exception {
    BankFactory bankFactory = new BankFactoryImpl();

    ConnectionOptions options = new ConnectionOptions("myDriver")
        .url("myUrl")
        .username("myUsername")
        .password("myPassword")
        .schema("mySchema");

    bankFactory.configureDataSource(options);
  }

}