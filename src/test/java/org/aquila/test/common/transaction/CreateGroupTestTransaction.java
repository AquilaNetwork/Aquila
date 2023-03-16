package org.aquila.test.common.transaction;

import java.util.Random;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.data.transaction.CreateGroupTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.group.Group.ApprovalThreshold;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;

public class CreateGroupTestTransaction extends TestTransaction {

	public static TransactionData randomTransaction(Repository repository, PrivateKeyAccount account, boolean wantValid) throws DataException {
		Random random = new Random();

		String groupName = "test group " + random.nextInt(1_000_000);
		String description = "random test group";
		final boolean isOpen = false;
		ApprovalThreshold approvalThreshold = ApprovalThreshold.PCT40;
		final int minimumBlockDelay = 5;
		final int maximumBlockDelay = 20;

		return new CreateGroupTransactionData(generateBase(account), groupName, description, isOpen, approvalThreshold, minimumBlockDelay, maximumBlockDelay);
	}

}
