package org.aquila.test.common.transaction;

import java.util.ArrayList;
import java.util.List;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.asset.Asset;
import org.aquila.data.PaymentData;
import org.aquila.data.transaction.MultiPaymentTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.utils.Amounts;

public class MultiPaymentTestTransaction extends TestTransaction {

	public static TransactionData randomTransaction(Repository repository, PrivateKeyAccount account, boolean wantValid) throws DataException {
		String recipient = account.getAddress();
		final long assetId = Asset.UNCIA;
		long amount = 123L * Amounts.MULTIPLIER;

		List<PaymentData> payments = new ArrayList<>();
		payments.add(new PaymentData(recipient, assetId, amount));

		return new MultiPaymentTransactionData(generateBase(account), payments);
	}

}
