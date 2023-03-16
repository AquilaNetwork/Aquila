package org.aquila.test.common.transaction;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.data.transaction.TransactionData;
import org.aquila.data.transaction.UpdateNameTransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;

public class UpdateNameTestTransaction extends TestTransaction {

	public static TransactionData randomTransaction(Repository repository, PrivateKeyAccount account, boolean wantValid) throws DataException {
		String newOwner = account.getAddress();
		String name = "test name";
		if (!wantValid)
			name += " " + random.nextInt(1_000_000);

		String newData = "{ \"key\": \"updated value\" }";

		return new UpdateNameTransactionData(generateBase(account), newOwner, name, newData);
	}

}
