package com.project.springjta.doubleentry;

/**
 * Business exception thrown if a referenced account does not exist.
 *
 * @author yanimetaxas
 * @since 14-Nov-14
 */
public class AccountNotFoundException extends BusinessException {
    private static final long serialVersionUID = 1L;

    public AccountNotFoundException(String accountRef) {
        super("No account found for reference '" + accountRef + "'");
    }
}
