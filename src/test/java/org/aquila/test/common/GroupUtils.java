package org.aquila.test.common;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.data.transaction.BaseTransactionData;
import org.aquila.data.transaction.CreateGroupTransactionData;
import org.aquila.data.transaction.GroupApprovalTransactionData;
import org.aquila.data.transaction.JoinGroupTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.group.Group;
import org.aquila.group.Group.ApprovalThreshold;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.transaction.Transaction.ApprovalStatus;
import org.aquila.utils.Amounts;

public class GroupUtils {

	public static final int txGroupId = Group.NO_GROUP;
	public static final long fee = 1L * Amounts.MULTIPLIER;

	public static int createGroup(Repository repository, String creatorAccountName, String groupName, boolean isOpen, ApprovalThreshold approvalThreshold,
				int minimumBlockDelay, int maximumBlockDelay) throws DataException {
		PrivateKeyAccount account = Common.getTestAccount(repository, creatorAccountName);

		byte[] reference = account.getLastReference();
		long timestamp = repository.getTransactionRepository().fromSignature(reference).getTimestamp() + 1;
		String groupDescription = groupName + " (test group)";

		BaseTransactionData baseTransactionData = new BaseTransactionData(timestamp, Group.NO_GROUP, reference, account.getPublicKey(), GroupUtils.fee, null);
		TransactionData transactionData = new CreateGroupTransactionData(baseTransactionData, groupName, groupDescription, isOpen, approvalThreshold, minimumBlockDelay, maximumBlockDelay);

		TransactionUtils.signAndMint(repository, transactionData, account);

		return repository.getGroupRepository().fromGroupName(groupName).getGroupId();
	}

	public static void joinGroup(Repository repository, String joinerAccountName, int groupId) throws DataException {
		PrivateKeyAccount account = Common.getTestAccount(repository, joinerAccountName);

		byte[] reference = account.getLastReference();
		long timestamp = repository.getTransactionRepository().fromSignature(reference).getTimestamp() + 1;

		BaseTransactionData baseTransactionData = new BaseTransactionData(timestamp, Group.NO_GROUP, reference, account.getPublicKey(), GroupUtils.fee, null);
		TransactionData transactionData = new JoinGroupTransactionData(baseTransactionData, groupId);

		TransactionUtils.signAndMint(repository, transactionData, account);
	}

	public static void approveTransaction(Repository repository, String accountName, byte[] pendingSignature, boolean decision) throws DataException {
		PrivateKeyAccount account = Common.getTestAccount(repository, accountName);

		byte[] reference = account.getLastReference();
		long timestamp = repository.getTransactionRepository().fromSignature(reference).getTimestamp() + 1;

		BaseTransactionData baseTransactionData = new BaseTransactionData(timestamp, Group.NO_GROUP, reference, account.getPublicKey(), GroupUtils.fee, null);
		TransactionData transactionData = new GroupApprovalTransactionData(baseTransactionData, pendingSignature, decision);

		TransactionUtils.signAndMint(repository, transactionData, account);
	}

	public static ApprovalStatus getApprovalStatus(Repository repository, byte[] signature) throws DataException {
		return repository.getTransactionRepository().fromSignature(signature).getApprovalStatus();
	}

	public static Integer getApprovalHeight(Repository repository, byte[] signature) throws DataException {
		return repository.getTransactionRepository().fromSignature(signature).getApprovalHeight();
	}

}
