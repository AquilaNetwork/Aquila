package org.aquila.transaction;

import java.util.Collections;
import java.util.List;

import org.aquila.account.Account;
import org.aquila.asset.Asset;
import org.aquila.data.transaction.SetGroupTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.group.Group;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;

public class SetGroupTransaction extends Transaction {

	// Properties
	private SetGroupTransactionData setGroupTransactionData;

	// Constructors

	public SetGroupTransaction(Repository repository, TransactionData transactionData) {
		super(repository, transactionData);

		this.setGroupTransactionData = (SetGroupTransactionData) this.transactionData;
	}

	// More information

	@Override
	public List<String> getRecipientAddresses() throws DataException {
		return Collections.emptyList();
	}

	// Navigation

	// Processing

	@Override
	public ValidationResult isValid() throws DataException {
		int defaultGroupId = this.setGroupTransactionData.getDefaultGroupId();

		// Check group exists
		if (!this.repository.getGroupRepository().groupExists(defaultGroupId))
			return ValidationResult.GROUP_DOES_NOT_EXIST;

		Account creator = getCreator();

		// Must be member of group
		if (!this.repository.getGroupRepository().memberExists(defaultGroupId, creator.getAddress()))
			return ValidationResult.NOT_GROUP_MEMBER;

		// Check creator has enough funds
		if (creator.getConfirmedBalance(Asset.UNCIA) < this.setGroupTransactionData.getFee())
			return ValidationResult.NO_BALANCE;

		return ValidationResult.OK;
	}

	@Override
	public void preProcess() throws DataException {
		// Nothing to do
	}

	@Override
	public void process() throws DataException {
		Account creator = getCreator();

		Integer previousDefaultGroupId = this.repository.getAccountRepository().getDefaultGroupId(creator.getAddress());
		if (previousDefaultGroupId == null)
			previousDefaultGroupId = Group.NO_GROUP;

		this.setGroupTransactionData.setPreviousDefaultGroupId(previousDefaultGroupId);

		// Save this transaction with account's previous defaultGroupId value
		this.repository.getTransactionRepository().save(this.setGroupTransactionData);

		// Set account's new default groupID
		creator.setDefaultGroupId(this.setGroupTransactionData.getDefaultGroupId());
	}

	@Override
	public void orphan() throws DataException {
		// Revert
		Account creator = getCreator();

		Integer previousDefaultGroupId = this.setGroupTransactionData.getPreviousDefaultGroupId();
		if (previousDefaultGroupId == null)
			previousDefaultGroupId = Group.NO_GROUP;

		creator.setDefaultGroupId(previousDefaultGroupId);

		// Save this transaction with removed previous defaultGroupId value
		this.setGroupTransactionData.setPreviousDefaultGroupId(null);
		this.repository.getTransactionRepository().save(this.setGroupTransactionData);
	}

}
