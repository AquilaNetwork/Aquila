package org.aquila.test.common.transaction;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.data.transaction.TransactionData;
import org.aquila.data.transaction.UpdateAssetTransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.test.common.AssetUtils;

public class UpdateAssetTestTransaction extends TestTransaction {

	public static TransactionData randomTransaction(Repository repository, PrivateKeyAccount account, boolean wantValid) throws DataException {
		final long assetId = 1;
		String newOwner = account.getAddress();
		String newDescription = "updated random test asset";
		String newData = AssetUtils.randomData();

		return new UpdateAssetTransactionData(generateBase(account), assetId, newOwner, newDescription, newData);
	}

}
