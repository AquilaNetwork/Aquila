package org.aquila.test.common.transaction;

import java.util.ArrayList;
import java.util.List;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.arbitrary.misc.Service;
import org.aquila.asset.Asset;
import org.aquila.data.PaymentData;
import org.aquila.data.transaction.ArbitraryTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.data.transaction.ArbitraryTransactionData.DataType;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.utils.Amounts;

public class ArbitraryTestTransaction extends TestTransaction {

	public static TransactionData randomTransaction(Repository repository, PrivateKeyAccount account, boolean wantValid) throws DataException {
		final int version = 5;
		final Service service = Service.ARBITRARY_DATA;
		final int nonce = 0;
		final int size = 4 * 1024 * 1024;
		final String name = "TEST";
		final String identifier = "aquila_avatar";
		final ArbitraryTransactionData.Method method = ArbitraryTransactionData.Method.PUT;

		final byte[] secret = new byte[32];
		random.nextBytes(secret);

        final ArbitraryTransactionData.Compression compression = ArbitraryTransactionData.Compression.ZIP;

		final byte[] metadataHash = new byte[32];
		random.nextBytes(metadataHash);

		byte[] data = new byte[1024];
		random.nextBytes(data);

		DataType dataType = DataType.RAW_DATA;

		String recipient = account.getAddress();
		final long assetId = Asset.UNCIA;
		long amount = 123L * Amounts.MULTIPLIER;

		List<PaymentData> payments = new ArrayList<>();
		payments.add(new PaymentData(recipient, assetId, amount));

		return new ArbitraryTransactionData(generateBase(account), version, service, nonce, size,name, identifier,
				method, secret, compression, data, dataType, metadataHash, payments);
	}

}
