package com.project.springjta.doubleentry;

/**
 * Defines the business logic for managing monetary accounts.
 *
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public interface AccountService {
    /**
     * Create a new account with an initial balance.
     *
     * @param accountRef a client defined account reference
     * @param amount the initial account balance
     */
    void createAccount(String accountRef, Money amount);

    /**
     * Get the current balance for a given account.
     *
     * @param accountRef the client defined account reference
     * @return the account balance
     * @throws AccountNotFoundException if the referenced account does not exist
     */
    Money getAccountBalance(String accountRef) throws AccountNotFoundException;
}
