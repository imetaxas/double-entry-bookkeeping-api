package com.yanimetaxas.bookkeeping.dao;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.yanimetaxas.bookkeeping.model.Money;
import com.yanimetaxas.bookkeeping.model.Transaction;
import com.yanimetaxas.bookkeeping.model.TransactionLeg;
import com.yanimetaxas.bookkeeping.util.DbUtil;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

/**
 * The Data Object Access support to abstract and encapsulate all access to the transaction_history
 * and transaction_leg tables of the data source.
 *
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public class TransactionDaoImpl extends JdbcDaoSupport implements TransactionDao {

  private DbUtil dbUtil;
  private String clientRef;

  @Override
  public void storeTransaction(Transaction transaction) {
    StringBuilder sql = new StringBuilder()
        .append("INSERT INTO transaction_history ")
        .append("(client_ref, transaction_ref, transaction_type, transaction_date)")
        .append(" VALUES (?, ?, ?, ?)");

    getJdbcTemplate().update(sql.toString(),
        getClientRef(),
        transaction.getTransactionRef(),
        transaction.getTransactionType(),
        transaction.getTransactionDate());
    storeTransactionLegs(transaction.getLegs(), getClientRef(), transaction.getTransactionRef());
  }

  @Override
  public Set<String> getTransactionRefsForAccount(String accountRef) {
    String sql = "SELECT transaction_ref FROM transaction_leg WHERE account_ref = ?";
    List<String> transactionRefs = getJdbcTemplate()
        .query(sql, dbUtil.stringRowMapper(), accountRef);
    return Sets.newHashSet(transactionRefs);
  }

  @Override
  public List<Transaction> getTransactions(List<String> transactionRefs) {
    StringBuilder sql = new StringBuilder()
        .append("SELECT transaction_ref, transaction_type, transaction_date ")
        .append("FROM transaction_history WHERE transaction_ref IN (")
        .append(dbUtil.commaSeparatedQuestionMarks(transactionRefs.size()))
        .append(")");
    return getJdbcTemplate()
        .query(sql.toString(), new TransactionMapper(), transactionRefs.toArray());
  }

  @Override
  public Transaction getTransactionByRef(String transactionRef) {
    List<String> transactionRefList = Lists.newArrayList();
    transactionRefList.add(transactionRef);
    List<Transaction> transactionsList = getTransactions(transactionRefList);
    if (transactionsList.isEmpty()) {
      return null;
    }
    return transactionsList.get(0);
  }

  private void storeTransactionLegs(List<TransactionLeg> legs, String clientRef,
      String transactionRef) {
    StringBuilder sql = new StringBuilder()
        .append("INSERT INTO transaction_leg ")
        .append("(client_ref, transaction_ref, account_ref, amount, currency) ")
        .append("VALUES (?, ?, ?, ?, ?)");
    getJdbcTemplate().batchUpdate(sql.toString(),
        new TransactionLegBatchUpdater(legs, clientRef, transactionRef));
  }

  private List<TransactionLeg> getLegsForTransaction(String transactionRef) {
    StringBuilder sql = new StringBuilder()
        .append("SELECT account_ref, amount, currency ")
        .append("FROM transaction_leg WHERE transaction_ref = ?");
    return getJdbcTemplate().query(sql.toString(), new TransactionLegMapper(), transactionRef);
  }

  @Override
  public void setClientRef(String clientRef) {
    this.clientRef = clientRef;
  }

  @Override
  public String getClientRef() {
    return this.clientRef;
  }

  public void setDbUtil(DbUtil dbUtil) {
    this.dbUtil = dbUtil;
  }

  private class TransactionMapper implements RowMapper<Transaction> {

    @Override
    public Transaction mapRow(@Nonnull ResultSet rs, int rowNum) throws SQLException {
      String transactionRef = rs.getString("transaction_ref");
      String transactionType = rs.getString("transaction_type");
      Date transactionDate = new Date(rs.getDate("transaction_date").getTime());
      List<TransactionLeg> legs = getLegsForTransaction(transactionRef);
      return new Transaction(transactionRef, transactionType, transactionDate, legs);
    }
  }

  private static class TransactionLegMapper implements RowMapper<TransactionLeg> {

    @Override
    public TransactionLeg mapRow(@Nonnull ResultSet rs, int rowNum) throws SQLException {
      String accountRef = rs.getString("account_ref");
      BigDecimal amount = new BigDecimal(rs.getString("amount"));
      Currency currency = Currency.getInstance(rs.getString("currency"));
      Money transferredFunds = new Money(amount, currency);
      return new TransactionLeg(accountRef, transferredFunds);
    }
  }

  private static class TransactionLegBatchUpdater implements BatchPreparedStatementSetter {

    private final List<TransactionLeg> legs;
    private final String clientRef;
    private final String transactionRef;

    private TransactionLegBatchUpdater(List<TransactionLeg> legs, String clientRef,
        String transactionRef) {
      this.legs = Collections.unmodifiableList(legs);
      this.clientRef = clientRef;
      this.transactionRef = transactionRef;
    }

    @Override
    public void setValues(@Nonnull PreparedStatement ps, int i) throws SQLException {
      TransactionLeg leg = legs.get(i);
      ps.setString(1, clientRef);
      ps.setString(2, transactionRef);
      ps.setString(3, leg.getAccountRef());
      ps.setBigDecimal(4, leg.getAmount().getAmount());
      ps.setString(5, leg.getAmount().getCurrency().getCurrencyCode());
    }

    @Override
    public int getBatchSize() {
      return legs.size();
    }
  }
}
