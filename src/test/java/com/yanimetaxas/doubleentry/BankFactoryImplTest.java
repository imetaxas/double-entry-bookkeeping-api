package com.yanimetaxas.doubleentry;

import com.yanimetaxas.doubleentry.model.ConnectionOptions;
import org.junit.Test;

/**
 * @author yanimetaxas
 * @since 07-Apr-18
 */
public class BankFactoryImplTest {

  @Test(expected = InfrastructureException.class)
  public void testConfigureDataSourceWhenDriverIsIllegal() throws Exception {
    BankFactory bankFactory = new BankFactoryImpl();

    ConnectionOptions options = new ConnectionOptions("myDriver", "myUrl", "myUsername",
        "myPassword");
    bankFactory.configureDataSource(options);
  }

}