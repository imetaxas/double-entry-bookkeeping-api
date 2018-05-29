package com.yanimetaxas.bookkeeping;

import com.yanimetaxas.bookkeeping.model.ConnectionOptions;
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
        "myPassword", "myschema");
    bankFactory.configureDataSource(options);
  }

}