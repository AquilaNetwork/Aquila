package org.aquila.test.common.transaction;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.data.transaction.CancelSellNameTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;

public class CancelSellNameTestTransaction extends TestTransaction {

	public static TransactionData randomTransaction(Repository repository, PrivateKeyAccount account, boolean wantValid) throws DataException {
		String name = "name-for-sale";

		return new CancelSellNameTransactionData(generateBase(account), name);
	}

}
