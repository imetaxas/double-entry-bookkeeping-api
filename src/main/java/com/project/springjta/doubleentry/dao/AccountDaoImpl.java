package com.project.springjta.doubleentry.dao;

import com.project.springjta.doubleentry.Money;
import com.project.springjta.doubleentry.TransactionLeg;
import com.project.springjta.doubleentry.model.Account;
import com.project.springjta.doubleentry.util.DbUtil;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Currency;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

/**
 * The Data Object Access support to abstract and encapsulate all access to the account table of the data source.
 *
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public class AccountDaoImpl extends JdbcDaoSupport implements AccountDao {

    private DbUtil dbUtil;
    private String clientRef;
    
	@Override
    public boolean accountExists(String accountRef) {
        String sql = "SELECT id FROM account WHERE account_ref = ?";
    	List<Long> accountIds = getJdbcTemplate().query(sql, dbUtil.longRowMapper(), accountRef);
        return !accountIds.isEmpty();
    }

    @Override
    public void createAccount(String clientRef, String accountRef, Money initialAmount) {
        String sql = "INSERT INTO account (client_ref, account_ref, currency, amount) VALUES (?, ?, ?, ?)";
        getJdbcTemplate().update(sql, clientRef, accountRef, initialAmount.getCurrency().getCurrencyCode(), initialAmount.getAmount().doubleValue());
    }

    @Override
    public Account getAccount(String accountRef) {
        String sql = "SELECT account_ref, amount, currency FROM account WHERE account_ref = ?";
        return getJdbcTemplate().query(sql, new AccountExtractor(), accountRef);
    }

    @Override
    public void truncateTables() {
        getJdbcTemplate().execute("TRUNCATE TABLE account");
    }

    @Override
    public void updateBalance(TransactionLeg leg) {
        String sql = "UPDATE account SET amount = amount + ? WHERE account_ref = ?";
        getJdbcTemplate().update(sql, leg.getAmount().getAmount().doubleValue(), leg.getAccountRef());
    }

    public void setDbUtil(DbUtil dbUtil) {
        this.dbUtil = dbUtil;
    }
    
    @Override
	public void setClientRef(String clientRef) {
		this.clientRef = clientRef;
	}
    
    @Override
    public String getClientRef() {
		return clientRef;
	}

	private static class AccountExtractor implements ResultSetExtractor<Account> {
        @Override
        public Account extractData(ResultSet rs) throws SQLException, DataAccessException {
            if (rs.next()) {
                String accountRef = rs.getString("account_ref");
                BigDecimal amount = new BigDecimal(rs.getString("amount"));
                Currency currency = Currency.getInstance(rs.getString("currency"));
                Money balance = new Money(amount, currency);
                return new Account(accountRef, balance);
            }
            return Account.nullAccount();
        }
    }

}