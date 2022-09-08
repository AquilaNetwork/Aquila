package org.aquila.test.common.transaction;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.data.transaction.RegisterNameTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;

public class RegisterNameTestTransaction extends TestTransaction {

	public static TransactionData randomTransaction(Repository repository, PrivateKeyAccount account, boolean wantValid) throws DataException {
		String name = "test name";
		if (!wantValid)
			name += " " + random.nextInt(1_000_000);

		String data = "{ \"key\": \"value\" }";

		return new RegisterNameTransactionData(generateBase(account), name, data);
	}

}
