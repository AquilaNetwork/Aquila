package org.aquila.test.common.transaction;

import java.util.Random;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.data.transaction.IssueAssetTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.test.common.AssetUtils;

public class IssueAssetTestTransaction extends TestTransaction {

	public static TransactionData randomTransaction(Repository repository, PrivateKeyAccount account, boolean wantValid) throws DataException {
		Random random = new Random();

		String assetName = "test-asset-" + random.nextInt(1_000_000);
		String description = "random test asset";
		final long quantity = 1_000_000L;
		final boolean isDivisible = true;
		String data = AssetUtils.randomData();
		final boolean isUnspendable = false;

		return new IssueAssetTransactionData(generateBase(account), assetName, description, quantity, isDivisible, data, isUnspendable);
	}

}
