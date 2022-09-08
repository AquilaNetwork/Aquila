package org.aquila.test.common.transaction;

import java.util.Random;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.data.transaction.CancelAssetOrderTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;

public class CancelAssetOrderTestTransaction extends TestTransaction {

	public static TransactionData randomTransaction(Repository repository, PrivateKeyAccount account, boolean wantValid) throws DataException {
		Random random = new Random();
		byte[] orderId = new byte[64];
		random.nextBytes(orderId);

		return new CancelAssetOrderTransactionData(generateBase(account), orderId);
	}

}
