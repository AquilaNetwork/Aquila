package org.aquila.test.common.transaction;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.asset.Asset;
import org.aquila.data.transaction.MessageTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.utils.Amounts;

public class MessageTestTransaction extends TestTransaction {

	public static TransactionData randomTransaction(Repository repository, PrivateKeyAccount account, boolean wantValid) throws DataException {
		final int version = 4;
		final int nonce = 0;
		String recipient = account.getAddress();
		final long assetId = Asset.UNCIA;
		long amount = 123L * Amounts.MULTIPLIER;
		byte[] data = "message contents".getBytes();
		final boolean isText = true;
		final boolean isEncrypted = false;

		return new MessageTransactionData(generateBase(account), version, nonce, recipient, amount, assetId, data, isText, isEncrypted);
	}

}
