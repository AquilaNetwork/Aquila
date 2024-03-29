package org.aquila.test.crosschain.litecoinv1;

import java.math.BigDecimal;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.asset.Asset;
import org.aquila.controller.Controller;
import org.aquila.crosschain.LitecoinACCTv1;
import org.aquila.data.transaction.BaseTransactionData;
import org.aquila.data.transaction.DeployAtTransactionData;
import org.aquila.group.Group;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.repository.RepositoryFactory;
import org.aquila.repository.RepositoryManager;
import org.aquila.repository.hsqldb.HSQLDBRepositoryFactory;
import org.aquila.transaction.DeployAtTransaction;
import org.aquila.transform.TransformationException;
import org.aquila.transform.transaction.TransactionTransformer;
import org.aquila.utils.Amounts;
import org.aquila.utils.Base58;
import org.bitcoinj.core.ECKey;
import org.aquila.test.crosschain.apps.Common;

import com.google.common.hash.HashCode;

public class DeployAT {

	private static void usage(String error) {
		if (error != null)
			System.err.println(error);

		System.err.println(String.format("usage: DeployAT <your Aquila PRIVATE key> <UNCIA amount> <AT funding amount> <LTC amount> <trade-timeout>"));
		System.err.println("A trading key-pair will be generated for you!");
		System.err.println(String.format("example: DeployAT "
				+ "7Eztjz2TsxwbrWUYEaSdLbASKQGTfK2rR7ViFc5gaiZw \\\n"
				+ "\t10 \\\n"
				+ "\t10.1 \\\n"
				+ "\t0.00864200 \\\n"
				+ "\t120"));
		System.exit(1);
	}

	public static void main(String[] args) {
		if (args.length != 5)
			usage(null);

		Common.init();

		byte[] creatorPrivateKey = null;
		long redeemAmount = 0;
		long fundingAmount = 0;
		long expectedLitecoin = 0;
		int tradeTimeout = 0;

		int argIndex = 0;
		try {
			creatorPrivateKey = Base58.decode(args[argIndex++]);
			if (creatorPrivateKey.length != 32)
				usage("Refund private key must be 32 bytes");

			redeemAmount = new BigDecimal(args[argIndex++]).setScale(8).unscaledValue().longValue();
			if (redeemAmount <= 0)
				usage("UNCIA amount must be positive");

			fundingAmount = new BigDecimal(args[argIndex++]).setScale(8).unscaledValue().longValue();
			if (fundingAmount <= redeemAmount)
				usage("AT funding amount must be greater than UNCIA redeem amount");

			expectedLitecoin = new BigDecimal(args[argIndex++]).setScale(8).unscaledValue().longValue();
			if (expectedLitecoin <= 0)
				usage("Expected LTC amount must be positive");

			tradeTimeout = Integer.parseInt(args[argIndex++]);
			if (tradeTimeout < 60 || tradeTimeout > 50000)
				usage("Trade timeout (minutes) must be between 60 and 50000");
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
			PrivateKeyAccount creatorAccount = new PrivateKeyAccount(repository, creatorPrivateKey);
			System.out.println(String.format("Creator Aquila address: %s", creatorAccount.getAddress()));
			System.out.println(String.format("UNCIA redeem amount: %s", Amounts.prettyAmount(redeemAmount)));
			System.out.println(String.format("AT funding amount: %s", Amounts.prettyAmount(fundingAmount)));

			// Generate trading key-pair
			byte[] tradePrivateKey = new ECKey().getPrivKeyBytes();
			PrivateKeyAccount tradeAccount = new PrivateKeyAccount(repository, tradePrivateKey);
			byte[] litecoinPublicKeyHash = ECKey.fromPrivate(tradePrivateKey).getPubKeyHash();

			System.out.println(String.format("Trade private key: %s", HashCode.fromBytes(tradePrivateKey)));

			// Deploy AT
			byte[] creationBytes = LitecoinACCTv1.buildAquilaAT(tradeAccount.getAddress(), litecoinPublicKeyHash, redeemAmount, expectedLitecoin, tradeTimeout);
			System.out.println("AT creation bytes: " + HashCode.fromBytes(creationBytes).toString());

			long txTimestamp = System.currentTimeMillis();
			byte[] lastReference = creatorAccount.getLastReference();

			if (lastReference == null) {
				System.err.println(String.format("Aquila account %s has no last reference", creatorAccount.getAddress()));
				System.exit(2);
			}

			Long fee = null;
			String name = "UNCIA-LTC cross-chain trade";
			String description = String.format("Aquila-Litecoin cross-chain trade");
			String atType = "ACCT";
			String tags = "UNCIA-LTC ACCT";

			BaseTransactionData baseTransactionData = new BaseTransactionData(txTimestamp, Group.NO_GROUP, lastReference, creatorAccount.getPublicKey(), fee, null);
			DeployAtTransactionData deployAtTransactionData = new DeployAtTransactionData(baseTransactionData, name, description, atType, tags, creationBytes, fundingAmount, Asset.UNCIA);

			DeployAtTransaction deployAtTransaction = new DeployAtTransaction(repository, deployAtTransactionData);

			fee = deployAtTransaction.calcRecommendedFee();
			deployAtTransactionData.setFee(fee);

			deployAtTransaction.sign(creatorAccount);

			byte[] signedBytes = null;
			try {
				signedBytes = TransactionTransformer.toBytes(deployAtTransactionData);
			} catch (TransformationException e) {
				System.err.println(String.format("Unable to convert transaction to base58: %s", e.getMessage()));
				System.exit(2);
			}

			DeployAtTransaction.ensureATAddress(deployAtTransactionData);
			String atAddress = deployAtTransactionData.getAtAddress();

			System.out.println(String.format("%nSigned transaction in base58, ready for POST /transactions/process:%n%s", Base58.encode(signedBytes)));

			System.out.println(String.format("AT address: %s", atAddress));
		} catch (DataException e) {
			System.err.println(String.format("Repository issue: %s", e.getMessage()));
			System.exit(2);
		}
	}

}
