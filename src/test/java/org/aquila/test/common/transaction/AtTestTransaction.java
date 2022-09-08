package org.aquila.test.common.transaction;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.asset.Asset;
import org.aquila.crypto.Crypto;
import org.aquila.data.transaction.ATTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.utils.Amounts;

public class AtTestTransaction extends TestTransaction {

	public static TransactionData randomTransaction(Repository repository, PrivateKeyAccount account, boolean wantValid) throws DataException {
		return AtTestTransaction.paymentType(repository, account, wantValid);
	}

	public static TransactionData paymentType(Repository repository, PrivateKeyAccount account, boolean wantValid) throws DataException {
		byte[] signature = new byte[64];
		random.nextBytes(signature);
		String atAddress = Crypto.toATAddress(signature);
		String recipient = account.getAddress();

		// Use PAYMENT-type
		long amount = 123L * Amounts.MULTIPLIER;
		final long assetId = Asset.UNCIA;

		return new ATTransactionData(generateBase(account), atAddress, recipient, amount, assetId);
	}

	public static TransactionData messageType(Repository repository, PrivateKeyAccount account, boolean wantValid) throws DataException {
		byte[] signature = new byte[64];
		random.nextBytes(signature);
		String atAddress = Crypto.toATAddress(signature);
		String recipient = account.getAddress();

		// Use MESSAGE-type
		byte[] message = new byte[32];
		random.nextBytes(message);

		return new ATTransactionData(generateBase(account), atAddress, recipient, message);
	}

}
