package org.aquila.test.common.transaction;

import java.util.Random;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.data.transaction.TransactionData;
import org.aquila.data.transaction.VoteOnPollTransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;

public class VoteOnPollTestTransaction extends TestTransaction {

	public static TransactionData randomTransaction(Repository repository, PrivateKeyAccount account, boolean wantValid) throws DataException {
		Random random = new Random();

		String pollName = "test poll " + random.nextInt(1_000_000);
		final int optionIndex = random.nextInt(3);

		return new VoteOnPollTransactionData(generateBase(account), pollName, optionIndex);
	}

}
