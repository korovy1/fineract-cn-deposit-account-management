/*
 * Copyright 2017 The Mifos Initiative.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mifos.deposit.service.internal.service.helper;

import com.google.common.collect.Lists;
import io.mifos.accounting.api.v1.client.AccountNotFoundException;
import io.mifos.accounting.api.v1.client.LedgerManager;
import io.mifos.accounting.api.v1.client.LedgerNotFoundException;
import io.mifos.accounting.api.v1.domain.Account;
import io.mifos.accounting.api.v1.domain.AccountEntry;
import io.mifos.accounting.api.v1.domain.AccountPage;
import io.mifos.accounting.api.v1.domain.JournalEntry;
import io.mifos.accounting.api.v1.domain.Ledger;
import io.mifos.core.lang.ServiceException;
import io.mifos.deposit.service.ServiceConstants;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
public class AccountingService {

  private final Logger logger;
  private final LedgerManager ledgerManager;

  @Autowired
  public AccountingService(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger,
                           final LedgerManager ledgerManager) {
    super();
    this.logger = logger;
    this.ledgerManager = ledgerManager;
  }

  public void createAccount(final String equityLedger,
                            final String productName,
                            final String customer,
                            final String accountNumber,
                            final String alternativeAccountNumber) {
    try {
      final Ledger ledger = this.ledgerManager.findLedger(equityLedger);
      final Account account = new Account();
      account.setIdentifier(accountNumber);
      account.setType(ledger.getType());
      account.setLedger(equityLedger);
      account.setName(productName);
      account.setHolders(new HashSet<>(Lists.newArrayList(customer)));
      account.setBalance(0.00D);
      if (alternativeAccountNumber != null && !alternativeAccountNumber.equals(accountNumber)) {
        account.setAlternativeAccountNumber(alternativeAccountNumber);
      }

      this.ledgerManager.createAccount(account);
    } catch (final LedgerNotFoundException lnfex) {
      throw ServiceException.notFound("Ledger {0} not found.", equityLedger);
    }
  }

  public Account findAccount(final String identifier) {
    try {
      return this.ledgerManager.findAccount(identifier);
    } catch (final AccountNotFoundException anfex) {
      final AccountPage accountPage = this.ledgerManager.fetchAccounts(true, identifier, null, true,
          0, 10, null, null);

      return accountPage.getAccounts()
          .stream()
          .filter(account -> account.getAlternativeAccountNumber().equals(identifier))
          .findFirst()
          .orElseThrow(() -> ServiceException.notFound("Account {0} not found.", identifier));
    }
  }

  public void updateAccount(final Account account) {
    this.ledgerManager.modifyAccount(account.getIdentifier(), account);
  }

  public List<AccountEntry> fetchEntries(final String identifier, final String dateRange, final String direction) {
    return this.ledgerManager
        .fetchAccountEntries(identifier, dateRange, null, 0, 1, "transactionDate", direction)
        .getAccountEntries();
  }

  public void post(final JournalEntry journalEntry) {
    this.ledgerManager.createJournalEntry(journalEntry);
  }
}
