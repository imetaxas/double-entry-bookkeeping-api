package com.project.springjta.doubleentry.model;

import com.project.springjta.doubleentry.Money;
import java.math.BigDecimal;
import java.util.Currency;

/**
 * Immutable value object representing an account
 *
 * @author yanimetaxas
 * @since 14-Nov-14
*/
public class Account {
	
	private final String accountRef;
	private final Money balance;
	private static final Account NULL_ACCOUNT = new Account("", new Money(new BigDecimal("0.00"), Currency.getInstance("XXX")));

	public Account(String accountRef, Money balance) {
		if (accountRef == null) {
			throw new NullPointerException("Argument accountRef is NULL");
		}
		if (balance == null) {
			throw new NullPointerException("Argument balance is NULL");
		}
		this.accountRef = accountRef;
		this.balance = balance;
	}

	public static Account nullAccount() {
		return NULL_ACCOUNT;
	}
	
	public String getAccountRef() {
		return accountRef;
	}
	
	public Money getBalance() {
		return balance;
	}
	
	public Currency getCurrency() {
		return balance.getCurrency();
	}
	
	public boolean isOverdrawn() {
		return balance.getAmount().doubleValue() < 0.0;
	}
	
	public boolean isNullAccount() {
		return this.equals(NULL_ACCOUNT);
	}
	
	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof Account)) {
			return false;
		}
		Account account = (Account) other;
		if (!accountRef.equals(account.accountRef)) {
			return false;
		}
		if (!balance.equals(account.balance)) {
			return false;
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		int result = accountRef.hashCode();
		result = 31 * result + balance.hashCode();
		return result;
	}
	
	@Override
	public String toString() {
		return "Account{" +
				"accountRef='" + accountRef + '\'' +
				", balance=" + balance +
				'}';
	}
}