package org.aquila.test.common.transaction;

import com.google.common.primitives.Longs;
import org.aquila.account.PrivateKeyAccount;
import org.aquila.data.transaction.PresenceTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.transaction.PresenceTransaction.PresenceType;
import org.aquila.utils.NTP;

public class PresenceTestTransaction extends TestTransaction {

	public static TransactionData randomTransaction(Repository repository, PrivateKeyAccount account, boolean wantValid) throws DataException {
		final int nonce = 0;

		byte[] tradePrivateKey = new byte[32];
		PrivateKeyAccount tradeNativeAccount = new PrivateKeyAccount(repository, tradePrivateKey);
		long timestamp = NTP.getTime();
		byte[] timestampSignature = tradeNativeAccount.sign(Longs.toByteArray(timestamp));

		return new PresenceTransactionData(generateBase(account), nonce, PresenceType.TRADE_BOT, timestampSignature);
	}

}
