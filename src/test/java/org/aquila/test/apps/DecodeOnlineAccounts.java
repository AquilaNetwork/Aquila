package org.aquila.test.apps;

import java.math.BigDecimal;
import java.security.Security;

import org.aquila.block.BlockChain;
import org.aquila.controller.Controller;
import org.aquila.data.account.RewardShareData;
import org.aquila.gui.Gui;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.repository.RepositoryFactory;
import org.aquila.repository.RepositoryManager;
import org.aquila.repository.hsqldb.HSQLDBRepositoryFactory;
import org.aquila.settings.Settings;
import org.aquila.transform.block.BlockTransformer;
import org.aquila.utils.Base58;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.roaringbitmap.IntIterator;

import io.druid.extendedset.intset.ConciseSet;

public class DecodeOnlineAccounts {

	private static void usage() {
		System.err.println("Usage: DecodeOnlineAccounts [<settings-file>] <base58-encoded-accounts>");
		System.err.println("Example: DecodeOnlineAccounts 4GmR5B");
		System.err.println("Example: DecodeOnlineAccounts settings-test.json 4GmR5B");
		System.exit(1);
	}

	public static void main(String[] args) {
		if (args.length < 1 || args.length > 2)
			usage();

		byte[] encodedOnlineAccounts = Base58.decode(args[args.length - 1]);

		ConciseSet accountIndexes = BlockTransformer.decodeOnlineAccounts(encodedOnlineAccounts);

		String delimiter = "";
		System.out.print("Account indexes: ");

		IntIterator iterator = accountIndexes.iterator();
		while (iterator.hasNext()) {
			int accountIndex = iterator.next();

			System.out.print(String.format("%s%d", delimiter, accountIndex));
			delimiter = ", ";
		}
		System.out.println();

		Security.insertProviderAt(new BouncyCastleProvider(), 0);
		Security.insertProviderAt(new BouncyCastleJsseProvider(), 1);

		// Load/check settings, which potentially sets up blockchain config, etc.
		try {
			if (args.length > 1)
				Settings.fileInstance(args[0]);
			else
				Settings.getInstance();
		} catch (Throwable t) {
			Gui.getInstance().fatalError("Settings file", t.getMessage());
			return; // Not System.exit() so that GUI can display error
		}

		try {
			RepositoryFactory repositoryFactory = new HSQLDBRepositoryFactory(Controller.getRepositoryUrl());
			RepositoryManager.setRepositoryFactory(repositoryFactory);
		} catch (DataException e) {
			System.err.println("Couldn't connect to repository: " + e.getMessage());
			System.exit(2);
		}

		try {
			BlockChain.validate();
		} catch (DataException e) {
			System.err.println("Couldn't validate repository: " + e.getMessage());
			System.exit(2);
		}

		try (final Repository repository = RepositoryManager.getRepository()) {
			iterator = accountIndexes.iterator();
			while (iterator.hasNext()) {
				int accountIndex = iterator.next();

				RewardShareData rewardShareData = repository.getAccountRepository().getRewardShareByIndex(accountIndex);

				System.out.println(String.format("Reward-share public key: %s, minter: %s, recipient: %s, share: %s",
						Base58.encode(rewardShareData.getRewardSharePublicKey()),
						rewardShareData.getMintingAccount(), rewardShareData.getRecipient(),
						BigDecimal.valueOf(rewardShareData.getSharePercent(), 2).toPlainString()));
			}
		} catch (DataException e) {
			e.printStackTrace();
		}

		try {
			RepositoryManager.closeRepositoryFactory();
		} catch (DataException e) {
			e.printStackTrace();
		}

	}

}
