package org.aquila.test.common.transaction;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.asset.Asset;
import org.aquila.data.transaction.TransactionData;
import org.aquila.data.transaction.TransferAssetTransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.utils.Amounts;

public class TransferAssetTestTransaction extends TestTransaction {

	public static TransactionData randomTransaction(Repository repository, PrivateKeyAccount account, boolean wantValid) throws DataException {
		String recipient = account.getAddress();
		final long assetId = Asset.UNCIA;
		long amount = 123L * Amounts.MULTIPLIER;

		return new TransferAssetTransactionData(generateBase(account), recipient, amount, assetId);
	}

}
