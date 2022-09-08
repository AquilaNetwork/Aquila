package org.aquila.test.common.transaction;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.data.transaction.RewardShareTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;

public class RewardShareTestTransaction extends TestTransaction {

	public static TransactionData randomTransaction(Repository repository, PrivateKeyAccount account, boolean wantValid) throws DataException {
		String recipient = account.getAddress();
		byte[] rewardSharePublicKey = account.getRewardSharePrivateKey(account.getPublicKey());
		int sharePercent = 50_00;

		return new RewardShareTransactionData(generateBase(account), recipient, rewardSharePublicKey, sharePercent);
	}

}
