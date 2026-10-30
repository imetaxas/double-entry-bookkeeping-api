/**
 * Double-entry bookkeeping engine for Java.
 *
 * <p>This package provides a complete, embeddable double-entry bookkeeping system. Start with
 * {@link com.yanimetaxas.bookkeeping.ChartOfAccounts} to define accounts, then use
 * {@link com.yanimetaxas.bookkeeping.Ledger.LedgerBuilder} to create a ledger.
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link com.yanimetaxas.bookkeeping.Ledger} — the central ledger</li>
 *   <li>{@link com.yanimetaxas.bookkeeping.ChartOfAccounts} — define accounts before building</li>
 *   <li>{@link com.yanimetaxas.bookkeeping.StorageProvider} — SPI for pluggable persistence</li>
 *   <li>{@link com.yanimetaxas.bookkeeping.StorageFactory} — factory interface for storage</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * Ledger ledger = new LedgerBuilder(new ChartOfAccountsBuilder()
 *     .create("Cash", "1000.00", "USD").create("Revenue", "0.00", "USD").build()).build().init();
 * ledger.commit(ledger.createTransferRequest().reference("T1").type("sale")
 *     .account("Cash").debit("50.00", "USD").account("Revenue").credit("50.00", "USD").build());
 * }</pre>
 *
 * @see com.yanimetaxas.bookkeeping.Ledger
 */
package com.yanimetaxas.bookkeeping;
