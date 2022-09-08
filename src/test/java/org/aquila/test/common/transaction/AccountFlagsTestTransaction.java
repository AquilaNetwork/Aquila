package org.aquila.test.common.transaction;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.data.transaction.AccountFlagsTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;

public class AccountFlagsTestTransaction extends TestTransaction {

	public static TransactionData randomTransaction(Repository repository, PrivateKeyAccount account, boolean wantValid) throws DataException {
		final int andMask = -1;
		final int orMask = 0;
		final int xorMask = 0;

		return new AccountFlagsTransactionData(generateBase(account), account.getAddress(), andMask, orMask, xorMask);
	}

}
