package org.aquila.test.crosschain.litecoinv1;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.controller.Controller;
import org.aquila.crosschain.LitecoinACCTv1;
import org.aquila.crypto.Crypto;
import org.aquila.group.Group;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.repository.RepositoryFactory;
import org.aquila.repository.RepositoryManager;
import org.aquila.repository.hsqldb.HSQLDBRepositoryFactory;
import org.aquila.transaction.MessageTransaction;
import org.aquila.transform.TransformationException;
import org.aquila.transform.transaction.TransactionTransformer;
import org.aquila.utils.Base58;
import org.aquila.test.crosschain.apps.Common;

public class SendCancelMessage {

	private static void usage(String error) {
		if (error != null)
			System.err.println(error);

		System.err.println(String.format("usage: SendCancelMessage <your Aquila PRIVATE key> <AT address>"));
		System.err.println(String.format("example: SendCancelMessage "
				+ "7Eztjz2TsxwbrWUYEaSdLbASKQGTfK2rR7ViFc5gaiZw \\\n"
				+ "\tAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
		System.exit(1);
	}

	public static void main(String[] args) {
		if (args.length != 2)
			usage(null);

		Common.init();

		byte[] aquilaPrivateKey = null;
		String atAddress = null;

		int argIndex = 0;
		try {
			aquilaPrivateKey = Base58.decode(args[argIndex++]);
			if (aquilaPrivateKey.length != 32)
				usage("Refund private key must be 32 bytes");

			atAddress = args[argIndex++];
			if (!Crypto.isValidAtAddress(atAddress))
				usage("Invalid AT address");
		} catch (IllegalArgumentException e) {
			usage(String.format("Invalid argument %d: %s", argIndex, e.getMessage()));
		}

		try {
			RepositoryFactory repositoryFactory = new HSQLDBRepositoryFactory(Controller.getRepositoryUrl());
			RepositoryManager.setRepositoryFactory(repositoryFactory);
		} catch (DataException e) {
			System.err.println(String.format("Repository start-up issue: %s", e.getMessage()));
			System.exit(2);
		}

		try (final Repository repository = RepositoryManager.getRepository()) {
			PrivateKeyAccount aquilaAccount = new PrivateKeyAccount(repository, aquilaPrivateKey);

			String creatorAquilaAddress = aquilaAccount.getAddress();
			System.out.println(String.format("Aquila address: %s", creatorAquilaAddress));

			byte[] messageData = LitecoinACCTv1.getInstance().buildCancelMessage(creatorAquilaAddress);
			MessageTransaction messageTransaction = MessageTransaction.build(repository, aquilaAccount, Group.NO_GROUP, atAddress, messageData, false, false);

			System.out.println("Computing nonce...");
			messageTransaction.computeNonce();
			messageTransaction.sign(aquilaAccount);

			byte[] signedBytes = null;
			try {
				signedBytes = TransactionTransformer.toBytes(messageTransaction.getTransactionData());
			} catch (TransformationException e) {
				System.err.println(String.format("Unable to convert transaction to bytes: %s", e.getMessage()));
				System.exit(2);
			}

			System.out.println(String.format("%nSigned transaction in base58, ready for POST /transactions/process:%n%s", Base58.encode(signedBytes)));
		} catch (DataException e) {
			System.err.println(String.format("Repository issue: %s", e.getMessage()));
			System.exit(2);
		}
	}

}
