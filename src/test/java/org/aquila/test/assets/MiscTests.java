package org.aquila.test.assets;

import static org.junit.Assert.*;

import org.aquila.data.transaction.IssueAssetTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.repository.RepositoryManager;
import org.aquila.transaction.Transaction;
import org.aquila.transaction.Transaction.ValidationResult;
import org.aquila.utils.Amounts;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.aquila.test.common.Common;
import org.aquila.test.common.TestAccount;
import org.aquila.test.common.TransactionUtils;
import org.aquila.test.common.transaction.TestTransaction;

public class MiscTests extends Common {

	@Before
	public void beforeTest() throws DataException {
		Common.useDefaultSettings();
	}

	@After
	public void afterTest() throws DataException {
		Common.orphanCheck();
	}

	@Test
	public void testCreateAssetWithExistingName() throws DataException {
		try (Repository repository = RepositoryManager.getRepository()) {
			TestAccount alice = Common.getTestAccount(repository, "alice");

			String assetName = "test-asset";
			String description = "description";
			long quantity = 12345678L;
			boolean isDivisible = true;
			String data = "{}";
			boolean isUnspendable = false;

			TransactionData transactionData = new IssueAssetTransactionData(TestTransaction.generateBase(alice), assetName, description, quantity, isDivisible, data, isUnspendable);
			TransactionUtils.signAndMint(repository, transactionData, alice);

			String duplicateAssetName = "TEST-Ásset";
			transactionData = new IssueAssetTransactionData(TestTransaction.generateBase(alice), duplicateAssetName, description, quantity, isDivisible, data, isUnspendable);

			Transaction transaction = Transaction.fromData(repository, transactionData);
			transaction.sign(alice);

			ValidationResult result = transaction.importAsUnconfirmed();
			assertTrue("Transaction should be invalid", ValidationResult.OK != result);
		}
	}

	@Test
	public void testCalcCommitmentWithRoundUp() throws DataException {
		long amount = 1234_87654321L;
		long price = 1_35615263L;

		// 1234.87654321 * 1.35615263 = 1674.6810717995501423
		// rounded up to 8dp gives: 1674.68107180
		long expectedCommitment = 1674_68107180L;

		long actualCommitment = Amounts.roundUpScaledMultiply(amount, price);
		assertEquals(expectedCommitment, actualCommitment);
	}

	@Test
	public void testCalcCommitmentWithoutRoundUp() throws DataException {
		long amount = 1234_87650000L;
		long price = 1_35610000L;

		// 1234.87650000 * 1.35610000 = 1674.6160216500000000
		// rounded up to 8dp gives: 1674.61602165
		long expectedCommitment = 1674_61602165L;

		long actualCommitment = Amounts.roundUpScaledMultiply(amount, price);
		assertEquals(expectedCommitment, actualCommitment);
	}

}
