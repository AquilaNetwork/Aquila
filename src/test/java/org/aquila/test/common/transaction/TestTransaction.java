package org.aquila.test.common.transaction;

import java.util.Random;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.block.BlockChain;
import org.aquila.data.transaction.BaseTransactionData;
import org.aquila.group.Group;
import org.aquila.repository.DataException;

public abstract class TestTransaction {

	protected static final Random random = new Random();

	public static BaseTransactionData generateBase(PrivateKeyAccount account, int txGroupId) throws DataException {
		return new BaseTransactionData(System.currentTimeMillis(), txGroupId, account.getLastReference(), account.getPublicKey(), BlockChain.getInstance().getUnitFee(), null);
	}

	public static BaseTransactionData generateBase(PrivateKeyAccount account) throws DataException {
		return generateBase(account, Group.NO_GROUP);
	}

}
