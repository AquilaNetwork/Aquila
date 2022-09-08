package org.aquila.test.common.transaction;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.data.transaction.SetGroupTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;

public class SetGroupTestTransaction extends TestTransaction {

	public static TransactionData randomTransaction(Repository repository, PrivateKeyAccount account, boolean wantValid) throws DataException {
		final int defaultGroupId = 1;

		return new SetGroupTransactionData(generateBase(account), defaultGroupId);
	}

}
