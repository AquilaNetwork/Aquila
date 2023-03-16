package org.aquila.test.group;

import static org.junit.Assert.*;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.data.transaction.AddGroupAdminTransactionData;
import org.aquila.data.transaction.CreateGroupTransactionData;
import org.aquila.data.transaction.JoinGroupTransactionData;
import org.aquila.data.transaction.RemoveGroupAdminTransactionData;
import org.aquila.group.Group.ApprovalThreshold;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.repository.RepositoryManager;
import org.aquila.transaction.Transaction.ValidationResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.aquila.test.common.BlockUtils;
import org.aquila.test.common.Common;
import org.aquila.test.common.TransactionUtils;
import org.aquila.test.common.transaction.TestTransaction;

public class OwnerTests extends Common {

	@Before
	public void beforeTest() throws DataException {
		Common.useDefaultSettings();
	}

	@After
	public void afterTest() throws DataException {
		Common.orphanCheck();
	}

	@Test
	public void testAddAdmin() throws DataException {
		try (final Repository repository = RepositoryManager.getRepository()) {
			PrivateKeyAccount alice = Common.getTestAccount(repository, "alice");
			PrivateKeyAccount bob = Common.getTestAccount(repository, "bob");

			// Create group
			int groupId = createGroup(repository, alice, "open-group", true);

			// Attempt to promote non-member
			ValidationResult result = addGroupAdmin(repository, alice, groupId, bob.getAddress());
			// Should NOT be OK
			assertNotSame(ValidationResult.OK, result);

			// Bob to join
			joinGroup(repository, bob, groupId);

			// Promote Bob to admin
			addGroupAdmin(repository, alice, groupId, bob.getAddress());

			// Confirm Bob is now admin
			assertTrue(isAdmin(repository, bob.getAddress(), groupId));

			// Attempt to re-promote admin
			result = addGroupAdmin(repository, alice, groupId, bob.getAddress());
			// Should NOT be OK
			assertNotSame(ValidationResult.OK, result);

			// Orphan last block
			BlockUtils.orphanLastBlock(repository);

			// Confirm Bob no longer an admin
			assertFalse(isAdmin(repository, bob.getAddress(), groupId));

			// Confirm Bob is still a member
			assertTrue(isMember(repository, bob.getAddress(), groupId));

			// Have Alice try to promote herself
			result = addGroupAdmin(repository, alice, groupId, alice.getAddress());
			// Should NOT be OK
			assertNotSame(ValidationResult.OK, result);
		}
	}

	@Test
	public void testRemoveAdmin() throws DataException {
		try (final Repository repository = RepositoryManager.getRepository()) {
			PrivateKeyAccount alice = Common.getTestAccount(repository, "alice");
			PrivateKeyAccount bob = Common.getTestAccount(repository, "bob");

			// Create group
			int groupId = createGroup(repository, alice, "open-group", true);

			// Attempt to demote non-member
			ValidationResult result = removeGroupAdmin(repository, alice, groupId, bob.getAddress());
			// Should NOT be OK
			assertNotSame(ValidationResult.OK, result);

			// Bob to join
			joinGroup(repository, bob, groupId);

			// Attempt to demote non-admin member
			result = removeGroupAdmin(repository, alice, groupId, bob.getAddress());
			// Should NOT be OK
			assertNotSame(ValidationResult.OK, result);

			// Promote Bob to admin
			addGroupAdmin(repository, alice, groupId, bob.getAddress());

			// Confirm Bob is now admin
			assertTrue(isAdmin(repository, bob.getAddress(), groupId));

			// Attempt to demote admin
			result = removeGroupAdmin(repository, alice, groupId, bob.getAddress());
			// Should be OK
			assertEquals(ValidationResult.OK, result);

			// Confirm Bob no longer an admin
			assertFalse(isAdmin(repository, bob.getAddress(), groupId));

			// Confirm Bob is still a member
			assertTrue(isMember(repository, bob.getAddress(), groupId));

			// Orphan last block
			BlockUtils.orphanLastBlock(repository);

			// Confirm Bob is now admin
			assertTrue(isAdmin(repository, bob.getAddress(), groupId));

			// Have Alice (owner) try to demote herself
			result = removeGroupAdmin(repository, alice, groupId, alice.getAddress());
			// Should NOT be OK
			assertNotSame(ValidationResult.OK, result);

			// Have Bob try to demote Alice (owner)
			result = removeGroupAdmin(repository, bob, groupId, alice.getAddress());
			// Should NOT be OK
			assertNotSame(ValidationResult.OK, result);
		}
	}

	private Integer createGroup(Repository repository, PrivateKeyAccount owner, String groupName, boolean isOpen) throws DataException {
		String description = groupName + " (description)";

		ApprovalThreshold approvalThreshold = ApprovalThreshold.ONE;
		int minimumBlockDelay = 10;
		int maximumBlockDelay = 1440;

		CreateGroupTransactionData transactionData = new CreateGroupTransactionData(TestTransaction.generateBase(owner), groupName, description, isOpen, approvalThreshold, minimumBlockDelay, maximumBlockDelay);
		TransactionUtils.signAndMint(repository, transactionData, owner);

		return repository.getGroupRepository().fromGroupName(groupName).getGroupId();
	}

	private ValidationResult joinGroup(Repository repository, PrivateKeyAccount joiner, int groupId) throws DataException {
		JoinGroupTransactionData transactionData = new JoinGroupTransactionData(TestTransaction.generateBase(joiner), groupId);
		ValidationResult result = TransactionUtils.signAndImport(repository, transactionData, joiner);

		if (result == ValidationResult.OK)
			BlockUtils.mintBlock(repository);

		return result;
	}

	private ValidationResult addGroupAdmin(Repository repository, PrivateKeyAccount owner, int groupId, String member) throws DataException {
		AddGroupAdminTransactionData transactionData = new AddGroupAdminTransactionData(TestTransaction.generateBase(owner), groupId, member);
		ValidationResult result = TransactionUtils.signAndImport(repository, transactionData, owner);

		if (result == ValidationResult.OK)
			BlockUtils.mintBlock(repository);

		return result;
	}

	private ValidationResult removeGroupAdmin(Repository repository, PrivateKeyAccount owner, int groupId, String member) throws DataException {
		RemoveGroupAdminTransactionData transactionData = new RemoveGroupAdminTransactionData(TestTransaction.generateBase(owner), groupId, member);
		ValidationResult result = TransactionUtils.signAndImport(repository, transactionData, owner);

		if (result == ValidationResult.OK)
			BlockUtils.mintBlock(repository);

		return result;
	}

	private boolean isMember(Repository repository, String address, int groupId) throws DataException {
		return repository.getGroupRepository().memberExists(groupId, address);
	}

	private boolean isAdmin(Repository repository, String address, int groupId) throws DataException {
		return repository.getGroupRepository().adminExists(groupId, address);
	}

}
