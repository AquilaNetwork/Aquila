package org.aquila.test.common.transaction;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.data.transaction.SellNameTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.utils.Amounts;

public class SellNameTestTransaction extends TestTransaction {

	public static TransactionData randomTransaction(Repository repository, PrivateKeyAccount account, boolean wantValid) throws DataException {
		String name = "test name";
		if (!wantValid)
			name += " " + random.nextInt(1_000_000);

		long amount = 123L * Amounts.MULTIPLIER;

		return new SellNameTransactionData(generateBase(account), name, amount);
	}

}
