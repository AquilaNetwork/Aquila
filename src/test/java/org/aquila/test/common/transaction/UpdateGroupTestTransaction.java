package org.aquila.test.common.transaction;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.data.transaction.TransactionData;
import org.aquila.data.transaction.UpdateGroupTransactionData;
import org.aquila.group.Group.ApprovalThreshold;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;

public class UpdateGroupTestTransaction extends TestTransaction {

	public static TransactionData randomTransaction(Repository repository, PrivateKeyAccount account, boolean wantValid) throws DataException {
		final int groupId = 1;
		String newOwner = account.getAddress();
		String newDescription = "updated random test group";
		final boolean newIsOpen = false;
		ApprovalThreshold newApprovalThreshold = ApprovalThreshold.PCT20;
		final int newMinimumBlockDelay = 10;
		final int newMaximumBlockDelay = 60;

		return new UpdateGroupTransactionData(generateBase(account), groupId, newOwner, newDescription, newIsOpen, newApprovalThreshold, newMinimumBlockDelay, newMaximumBlockDelay);
	}

}
