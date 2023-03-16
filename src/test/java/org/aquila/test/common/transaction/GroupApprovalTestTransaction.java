package org.aquila.test.common.transaction;

import java.util.Random;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.data.transaction.GroupApprovalTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;

public class GroupApprovalTestTransaction extends TestTransaction {

	public static TransactionData randomTransaction(Repository repository, PrivateKeyAccount account, boolean wantValid) throws DataException {
		Random random = new Random();

		byte[] pendingSignature = new byte[64];
		random.nextBytes(pendingSignature);
		final boolean approval = true;

		return new GroupApprovalTransactionData(generateBase(account), pendingSignature, approval);
	}

}
