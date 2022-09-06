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
import org.aquila.test.crosschain.apps.Common;
import org.aquila.transaction.MessageTransaction;
import org.aquila.transform.TransformationException;
import org.aquila.transform.transaction.TransactionTransformer;
import org.aquila.utils.Base58;
import org.aquila.utils.NTP;

import com.google.common.hash.HashCode;

public class SendTradeMessage {

	private static void usage(String error) {
		if (error != null)
			System.err.println(error);

		System.err.println(String.format("usage: SendTradeMessage <trade PRIVATE key> <AT address> <partner trade Aquila address> <partner tradeLitecoin PKH/P2PKH> <hash-of-secret> <locktime>"));
		System.err.println(String.format("example: SendTradeMessage "
				+ "ed77aa2c62d785a9428725fc7f95b907be8a1cc43213239876a62cf70fdb6ecb \\\n"
				+ "\tTttttttttttttttttttttttttttttttttt \\\n"
				+ "\tAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\\\n"
				+ "\tffffffffffffffffffffffffffffffffffffffff \\\n"
				+ "\tdaf59884b4d1aec8c1b17102530909ee43c0151a \\\n"
				+ "\t1600184800"));
		System.exit(1);
	}

	public static void main(String[] args) {
		if (args.length != 6)
			usage(null);

		Common.init();

		byte[] tradePrivateKey = null;
		String atAddress = null;
		String partnerTradeAddress = null;
		byte[] partnerTradePublicKeyHash = null;
		byte[] hashOfSecret = null;
		int lockTime = 0;

		int argIndex = 0;
		try {
			tradePrivateKey = HashCode.fromString(args[argIndex++]).asBytes();
			if (tradePrivateKey.length != 32)
				usage("Refund private key must be 32 bytes");

			atAddress = args[argIndex++];
			if (!Crypto.isValidAtAddress(atAddress))
				usage("Invalid AT address");

			partnerTradeAddress = args[argIndex++];
			if (!Crypto.isValidAddress(partnerTradeAddress))
				usage("Invalid partner trade Aquila address");

			partnerTradePublicKeyHash = HashCode.fromString(args[argIndex++]).asBytes();
			if (partnerTradePublicKeyHash.length != 20)
				usage("Partner trade PKH must be 20 bytes");

			hashOfSecret = HashCode.fromString(args[argIndex++]).asBytes();
			if (hashOfSecret.length != 20)
				usage("HASH160 of secret must be 20 bytes");

			lockTime = Integer.parseInt(args[argIndex++]);
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
			PrivateKeyAccount tradeAccount = new PrivateKeyAccount(repository, tradePrivateKey);

			int refundTimeout = LitecoinACCTv1.calcRefundTimeout(NTP.getTime(), lockTime);
			if (refundTimeout < 1) {
				System.err.println("Refund timeout too small. Is locktime in the past?");
				System.exit(2);
			}

			byte[] messageData = LitecoinACCTv1.buildTradeMessage(partnerTradeAddress, partnerTradePublicKeyHash, hashOfSecret, lockTime, refundTimeout);
			MessageTransaction messageTransaction = MessageTransaction.build(repository, tradeAccount, Group.NO_GROUP, atAddress, messageData, false, false);

			System.out.println("Computing nonce...");
			messageTransaction.computeNonce();
			messageTransaction.sign(tradeAccount);

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
