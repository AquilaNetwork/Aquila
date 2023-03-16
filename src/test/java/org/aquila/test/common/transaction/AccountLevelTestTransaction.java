package org.aquila.test.common.transaction;

import java.util.Random;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.data.transaction.AccountLevelTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;

public class AccountLevelTestTransaction extends TestTransaction {

	public static TransactionData randomTransaction(Repository repository, PrivateKeyAccount account, boolean wantValid) throws DataException {
		Random random = new Random();
		final int level = random.nextInt(10);

		return new AccountLevelTransactionData(generateBase(account), account.getAddress(), level);
	}

}
