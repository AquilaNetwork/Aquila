package org.aquila.test.apps;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.aquila.block.BlockChain;
import org.aquila.controller.Controller;
import org.aquila.repository.DataException;
import org.aquila.repository.RepositoryFactory;
import org.aquila.repository.RepositoryManager;
import org.aquila.repository.hsqldb.HSQLDBRepositoryFactory;
import org.aquila.settings.Settings;

public class orphan {

	public static void main(String[] args) {
		if (args.length < 1 || args.length > 2) {
			System.err.println("usage: orphan [<settings-file>] <new-blockchain-tip-height>");
			System.exit(1);
		}

		Security.insertProviderAt(new BouncyCastleProvider(), 0);

		int argIndex = 0;

		if (args.length > 1) {
			Settings.fileInstance(args[argIndex++]);
		} else {
			// Load/check settings, which potentially sets up blockchain config, etc.
			Settings.getInstance();
		}

		int targetHeight = Integer.parseInt(args[argIndex]);

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

		try {
			BlockChain.orphan(targetHeight);
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
