package org.aquila.test.common.transaction;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.data.transaction.TransactionData;
import org.aquila.data.transaction.TransferPrivsTransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;

public class TransferPrivsTestTransaction extends TestTransaction {

	public static TransactionData randomTransaction(Repository repository, PrivateKeyAccount account, boolean wantValid) throws DataException {
		String recipient = account.getAddress();

		return new TransferPrivsTransactionData(generateBase(account), recipient);
	}

}
