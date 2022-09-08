package org.aquila.test.common;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.repository.Repository;
import org.aquila.utils.Base58;

public class TestAccount extends PrivateKeyAccount {

	public final String accountName;
	public final boolean isRewardShare;

	public TestAccount(Repository repository, String accountName, String privateKey, boolean isRewardShare) {
		super(repository, Base58.decode(privateKey));

		this.accountName = accountName;
		this.isRewardShare = isRewardShare;
	}

	public TestAccount(Repository repository, TestAccount testAccount) {
		this(repository, testAccount.accountName, Base58.encode(testAccount.getPrivateKey()), testAccount.isRewardShare);
	}

}
