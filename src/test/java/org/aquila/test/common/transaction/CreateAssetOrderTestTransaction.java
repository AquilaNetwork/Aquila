package org.aquila.test.common.transaction;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.asset.Asset;
import org.aquila.data.transaction.CreateAssetOrderTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.utils.Amounts;

public class CreateAssetOrderTestTransaction extends TestTransaction {

	public static TransactionData randomTransaction(Repository repository, PrivateKeyAccount account, boolean wantValid) throws DataException {
		final long haveAssetId = Asset.UNCIA;
		final long wantAssetId = 1;
		long amount = 123L * Amounts.MULTIPLIER;
		long price = 123L * Amounts.MULTIPLIER;

		return new CreateAssetOrderTransactionData(generateBase(account), haveAssetId, wantAssetId, amount, price);
	}

}
