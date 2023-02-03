package org.aquila.test.common;

import static org.junit.Assert.assertEquals;
import static org.aquila.crypto.Aquila25519Extras.signForAggregation;

import java.security.SecureRandom;
import java.util.*;

import com.google.common.primitives.Longs;
import org.aquila.account.PrivateKeyAccount;
import org.aquila.block.BlockChain;
import org.aquila.crypto.Crypto;
import org.aquila.crypto.Aquila25519Extras;
import org.aquila.data.network.OnlineAccountData;
import org.aquila.data.transaction.BaseTransactionData;
import org.aquila.data.transaction.PaymentTransactionData;
import org.aquila.data.transaction.RewardShareTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.group.Group;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.transform.Transformer;
import org.aquila.utils.Amounts;

public class AccountUtils {

	public static final int txGroupId = Group.NO_GROUP;
	public static final long fee = 1L * Amounts.MULTIPLIER;

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	public static void pay(Repository repository, String testSenderName, String testRecipientName, long amount) throws DataException {
		PrivateKeyAccount sendingAccount = Common.getTestAccount(repository, testSenderName);
		PrivateKeyAccount recipientAccount = Common.getTestAccount(repository, testRecipientName);

		pay(repository, sendingAccount, recipientAccount.getAddress(), amount);
	}

	public static void pay(Repository repository, PrivateKeyAccount sendingAccount, String recipientAddress, long amount) throws DataException {
		byte[] reference = sendingAccount.getLastReference();
		long timestamp = repository.getTransactionRepository().fromSignature(reference).getTimestamp() + 1;

		BaseTransactionData baseTransactionData = new BaseTransactionData(timestamp, txGroupId, reference, sendingAccount.getPublicKey(), fee, null);
		TransactionData transactionData = new PaymentTransactionData(baseTransactionData, recipientAddress, amount);

		TransactionUtils.signAndMint(repository, transactionData, sendingAccount);
	}

	public static TransactionData createRewardShare(Repository repository, String minter, String recipient, int sharePercent) throws DataException {
		PrivateKeyAccount mintingAccount = Common.getTestAccount(repository, minter);
		PrivateKeyAccount recipientAccount = Common.getTestAccount(repository, recipient);
		return createRewardShare(repository, mintingAccount, recipientAccount, sharePercent);
	}

	public static TransactionData createRewardShare(Repository repository, PrivateKeyAccount mintingAccount, PrivateKeyAccount recipientAccount, int sharePercent) throws DataException {
		byte[] reference = mintingAccount.getLastReference();
		long timestamp = repository.getTransactionRepository().fromSignature(reference).getTimestamp() + 1;

		byte[] rewardSharePrivateKey = mintingAccount.getRewardSharePrivateKey(recipientAccount.getPublicKey());
		byte[] rewardSharePublicKey = Crypto.toPublicKey(rewardSharePrivateKey);

		BaseTransactionData baseTransactionData = new BaseTransactionData(timestamp, txGroupId, reference, mintingAccount.getPublicKey(), fee, null);
		TransactionData transactionData = new RewardShareTransactionData(baseTransactionData, recipientAccount.getAddress(), rewardSharePublicKey, sharePercent);

		return transactionData;
	}

	public static byte[] rewardShare(Repository repository, String minter, String recipient, int sharePercent) throws DataException {
		TransactionData transactionData = createRewardShare(repository, minter, recipient, sharePercent);

		PrivateKeyAccount rewardShareAccount = Common.getTestAccount(repository, minter);
		TransactionUtils.signAndMint(repository, transactionData, rewardShareAccount);

		PrivateKeyAccount recipientAccount = Common.getTestAccount(repository, recipient);
		byte[] rewardSharePrivateKey = rewardShareAccount.getRewardSharePrivateKey(recipientAccount.getPublicKey());

		return rewardSharePrivateKey;
	}

	public static byte[] rewardShare(Repository repository, PrivateKeyAccount minterAccount, PrivateKeyAccount recipientAccount, int sharePercent) throws DataException {
		TransactionData transactionData = createRewardShare(repository, minterAccount, recipientAccount, sharePercent);

		TransactionUtils.signAndMint(repository, transactionData, minterAccount);
		byte[] rewardSharePrivateKey = minterAccount.getRewardSharePrivateKey(recipientAccount.getPublicKey());

		return rewardSharePrivateKey;
	}

	public static Map<String, Map<Long, Long>> getBalances(Repository repository, long... assetIds) throws DataException {
		Map<String, Map<Long, Long>> balances = new HashMap<>();

		for (TestAccount account : Common.getTestAccounts(repository))
			for (Long assetId : assetIds) {
				long balance = account.getConfirmedBalance(assetId);

				balances.compute(account.accountName, (key, value) -> {
					if (value == null)
						value = new HashMap<Long, Long>();

					value.put(assetId, balance);

					return value;
				});
			}

		return balances;
	}

	public static long getBalance(Repository repository, String accountName, long assetId) throws DataException {
		return Common.getTestAccount(repository, accountName).getConfirmedBalance(assetId);
	}

	public static void assertBalance(Repository repository, String accountName, long assetId, long expectedBalance) throws DataException {
		long actualBalance = getBalance(repository, accountName, assetId);
		String assetName = repository.getAssetRepository().fromAssetId(assetId).getName();

		assertEquals(String.format("%s's %s [%d] balance incorrect", accountName, assetName, assetId), expectedBalance, actualBalance);
	}


	public static List<OnlineAccountData> generateOnlineAccounts(int numAccounts) {
		List<OnlineAccountData> onlineAccounts = new ArrayList<>();

		long timestamp = System.currentTimeMillis();
		byte[] timestampBytes = Longs.toByteArray(timestamp);

		final boolean mempowActive = timestamp >= BlockChain.getInstance().getOnlineAccountsMemoryPoWTimestamp();

		for (int a = 0; a < numAccounts; ++a) {
			byte[] privateKey = new byte[Transformer.PUBLIC_KEY_LENGTH];
			SECURE_RANDOM.nextBytes(privateKey);

			byte[] publicKey = new byte[Transformer.PUBLIC_KEY_LENGTH];
			Qortal25519Extras.generatePublicKey(privateKey, 0, publicKey, 0);

			byte[] signature = signForAggregation(privateKey, timestampBytes);

			Integer nonce = mempowActive ? new Random().nextInt(500000) : null;

			onlineAccounts.add(new OnlineAccountData(timestamp, signature, publicKey, nonce));
		}

		return onlineAccounts;
	}
}
