package org.aquila.test.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Base64;
import java.util.Base64.Encoder;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.data.asset.OrderData;
import org.aquila.data.transaction.BaseTransactionData;
import org.aquila.data.transaction.CancelAssetOrderTransactionData;
import org.aquila.data.transaction.CreateAssetOrderTransactionData;
import org.aquila.data.transaction.IssueAssetTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.data.transaction.TransferAssetTransactionData;
import org.aquila.group.Group;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.repository.RepositoryManager;
import org.aquila.transaction.Transaction;
import org.aquila.utils.Amounts;

import java.util.Map;
import java.util.Random;

public class AssetUtils {

	public static final int txGroupId = Group.NO_GROUP;
	public static final long fee = 1L * Amounts.MULTIPLIER;

	// UNCIA: 0, LEGACY_QORA: 1, UNCIA_FROM_QORA: 2
	public static final long testAssetId = 3L; // Owned by Alice
	public static final long otherAssetId = 4L; // Owned by Bob
	public static final long goldAssetId = 5L; // Owned by Alice

	public static long issueAsset(Repository repository, String issuerAccountName, String assetName, long quantity, boolean isDivisible) throws DataException {
		PrivateKeyAccount account = Common.getTestAccount(repository, issuerAccountName);

		byte[] reference = account.getLastReference();
		long timestamp = repository.getTransactionRepository().fromSignature(reference).getTimestamp() + 1;

		BaseTransactionData baseTransactionData = new BaseTransactionData(timestamp, AssetUtils.txGroupId, reference, account.getPublicKey(), AssetUtils.fee, null);
		TransactionData transactionData = new IssueAssetTransactionData(baseTransactionData, assetName, "desc", quantity, isDivisible, "{}", false);

		TransactionUtils.signAndMint(repository, transactionData, account);

		return repository.getAssetRepository().fromAssetName(assetName).getAssetId();
	}

	public static void transferAsset(Repository repository, String fromAccountName, String toAccountName, long assetId, long amount) throws DataException {
		PrivateKeyAccount fromAccount = Common.getTestAccount(repository, fromAccountName);
		PrivateKeyAccount toAccount = Common.getTestAccount(repository, toAccountName);

		byte[] reference = fromAccount.getLastReference();
		long timestamp = repository.getTransactionRepository().fromSignature(reference).getTimestamp() + 1;

		BaseTransactionData baseTransactionData = new BaseTransactionData(timestamp, AssetUtils.txGroupId, reference, fromAccount.getPublicKey(), AssetUtils.fee, null);
		TransactionData transactionData = new TransferAssetTransactionData(baseTransactionData, toAccount.getAddress(), amount, assetId);

		TransactionUtils.signAndMint(repository, transactionData, fromAccount);
	}

	public static byte[] createOrder(Repository repository, String accountName, long haveAssetId, long wantAssetId, long amount, long price) throws DataException {
		PrivateKeyAccount account = Common.getTestAccount(repository, accountName);

		byte[] reference = account.getLastReference();
		long timestamp = repository.getTransactionRepository().fromSignature(reference).getTimestamp() + 1;

		BaseTransactionData baseTransactionData = new BaseTransactionData(timestamp, AssetUtils.txGroupId, reference, account.getPublicKey(), AssetUtils.fee, null);
		TransactionData transactionData = new CreateAssetOrderTransactionData(baseTransactionData, haveAssetId, wantAssetId, amount, price);

		TransactionUtils.signAndMint(repository, transactionData, account);

		return repository.getAssetRepository().getAccountsOrders(account.getPublicKey(), null, null, null, null, true).get(0).getOrderId();
	}

	public static Transaction buildCancelOrder(Repository repository, String accountName, byte[] orderId) throws DataException {
		PrivateKeyAccount account = Common.getTestAccount(repository, accountName);

		byte[] reference = account.getLastReference();
		long timestamp = repository.getTransactionRepository().fromSignature(reference).getTimestamp() + 1;

		BaseTransactionData baseTransactionData = new BaseTransactionData(timestamp, AssetUtils.txGroupId, reference, account.getPublicKey(), AssetUtils.fee, null);
		TransactionData transactionData = new CancelAssetOrderTransactionData(baseTransactionData, orderId);

		return Transaction.fromData(repository, transactionData);
	}

	public static void cancelOrder(Repository repository, String accountName, byte[] orderId) throws DataException {
		PrivateKeyAccount account = Common.getTestAccount(repository, accountName);
		Transaction transaction = buildCancelOrder(repository, accountName, orderId);

		TransactionUtils.signAndMint(repository, transaction.getTransactionData(), account);
	}

	public static void genericTradeTest(long haveAssetId, long wantAssetId,
			long aliceAmount, long alicePrice,
			long bobAmount, long bobPrice,
			long aliceCommitment, long bobCommitment,
			long aliceReturn, long bobReturn, long bobSaving) throws DataException {
		try (Repository repository = RepositoryManager.getRepository()) {
			Map<String, Map<Long, Long>> initialBalances = AccountUtils.getBalances(repository, haveAssetId, wantAssetId);

			// Create target order
			byte[] targetOrderId = createOrder(repository, "alice", haveAssetId, wantAssetId, aliceAmount, alicePrice);

			// Create initiating order
			byte[] initiatingOrderId = createOrder(repository, "bob", wantAssetId, haveAssetId, bobAmount, bobPrice);

			// Check balances to check expected outcome
			long expectedBalance;
			OrderData targetOrderData = repository.getAssetRepository().fromOrderId(targetOrderId);
			OrderData initiatingOrderData = repository.getAssetRepository().fromOrderId(initiatingOrderId);

			// Alice selling have asset
			expectedBalance = initialBalances.get("alice").get(haveAssetId) - aliceCommitment;
			AccountUtils.assertBalance(repository, "alice", haveAssetId, expectedBalance);

			// Alice buying want asset
			expectedBalance = initialBalances.get("alice").get(wantAssetId) + aliceReturn;
			AccountUtils.assertBalance(repository, "alice", wantAssetId, expectedBalance);

			// Bob selling want asset
			expectedBalance = initialBalances.get("bob").get(wantAssetId) - bobCommitment + bobSaving;
			AccountUtils.assertBalance(repository, "bob", wantAssetId, expectedBalance);

			// Bob buying have asset
			expectedBalance = initialBalances.get("bob").get(haveAssetId) + bobReturn;
			AccountUtils.assertBalance(repository, "bob", haveAssetId, expectedBalance);

			// Check orders
			long expectedFulfilled = (initiatingOrderData.getHaveAssetId() < initiatingOrderData.getWantAssetId()) ? bobReturn : aliceReturn;

			// Check matching order
			assertNotNull("matching order missing", initiatingOrderData);
			assertEquals(String.format("Bob's order \"fulfilled\" incorrect"), expectedFulfilled, initiatingOrderData.getFulfilled());

			// Check initial order
			assertNotNull("initial order missing", targetOrderData);
			assertEquals(String.format("Alice's order \"fulfilled\" incorrect"), expectedFulfilled, targetOrderData.getFulfilled());
		}
	}

	public static String randomData() {
		Random random = new Random();
		byte[] rawData = new byte[1024];
		random.nextBytes(rawData);

		Encoder base64Encoder = Base64.getEncoder();
		return "{ \"logo\": \"data:image/png;base64," + base64Encoder.encodeToString(rawData) + "\" }";
	}

}
