package com.yanimetaxas.bookkeeping;

import com.yanimetaxas.bookkeeping.service.AccountService;
import com.yanimetaxas.bookkeeping.service.TransferService;

/**
 * Account and transfer services bound to one ledger name. The services keep that name on every
 * thread.
 */
public record StorageSession(AccountService accounts, TransferService transfers) {}
