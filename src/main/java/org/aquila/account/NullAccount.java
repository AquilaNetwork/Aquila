package org.aquila.account;

import org.aquila.crypto.Crypto;
import org.aquila.repository.Repository;

public final class NullAccount extends PublicKeyAccount {

	public static final byte[] PUBLIC_KEY = new byte[32];
	public static final String ADDRESS = Crypto.toAddress(PUBLIC_KEY);

	public NullAccount(Repository repository) {
		super(repository, PUBLIC_KEY, ADDRESS);
	}

	protected NullAccount() {
	}

	@Override
	public boolean verify(byte[] signature, byte[] message) {
		// Can't sign, hence can't verify.
		return false;
	}

}
