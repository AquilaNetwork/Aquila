package org.aquila.test.arbitrary;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.aquila.account.PrivateKeyAccount;
import org.aquila.arbitrary.ArbitraryDataFile;
import org.aquila.arbitrary.exception.MissingDataException;
import org.aquila.arbitrary.misc.Service;
import org.aquila.controller.arbitrary.ArbitraryDataManager;
import org.aquila.data.transaction.ArbitraryTransactionData;
import org.aquila.data.transaction.RegisterNameTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.repository.RepositoryManager;
import org.aquila.test.common.ArbitraryUtils;
import org.aquila.test.common.Common;
import org.aquila.test.common.TransactionUtils;
import org.aquila.test.common.transaction.TestTransaction;
import org.aquila.transaction.ArbitraryTransaction;
import org.aquila.transaction.RegisterNameTransaction;
import org.aquila.utils.Base58;
import org.aquila.utils.NTP;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class ArbitraryTransactionTests extends Common {

    @Before
    public void beforeTest() throws DataException, IllegalAccessException {
        Common.useDefaultSettings();
    }

    @Test
    public void testDifficultyTooLow() throws IllegalAccessException, DataException, IOException, MissingDataException {
        try (final Repository repository = RepositoryManager.getRepository()) {
            PrivateKeyAccount alice = Common.getTestAccount(repository, "alice");
            String publicKey58 = Base58.encode(alice.getPublicKey());
            String name = "TEST"; // Can be anything for this test
            String identifier = null; // Not used for this test
            Service service = Service.ARBITRARY_DATA;
            int chunkSize = 100;
            int dataLength = 900; // Actual data length will be longer due to encryption

            // Register the name to Alice
            RegisterNameTransactionData registerNameTransactionData = new RegisterNameTransactionData(TestTransaction.generateBase(alice), name, "");
            registerNameTransactionData.setFee(new RegisterNameTransaction(null, null).getUnitFee(registerNameTransactionData.getTimestamp()));
            TransactionUtils.signAndMint(repository, registerNameTransactionData, alice);

            // Set difficulty to 1
            FieldUtils.writeField(ArbitraryDataManager.getInstance(), "powDifficulty", 1, true);

            // Create PUT transaction
            Path path1 = ArbitraryUtils.generateRandomDataPath(dataLength);
            ArbitraryDataFile arbitraryDataFile = ArbitraryUtils.createAndMintTxn(repository, publicKey58, path1, name, identifier, ArbitraryTransactionData.Method.PUT, service, alice, chunkSize);

            // Check that nonce validation succeeds
            byte[] signature = arbitraryDataFile.getSignature();
            TransactionData transactionData = repository.getTransactionRepository().fromSignature(signature);
            ArbitraryTransaction transaction = new ArbitraryTransaction(repository, transactionData);
            assertTrue(transaction.isSignatureValid());

            // Increase difficulty to 15
            FieldUtils.writeField(ArbitraryDataManager.getInstance(), "powDifficulty", 15, true);

            // Make sure the nonce validation fails
            // Note: there is a very tiny chance this could succeed due to being extremely lucky
            // and finding a high difficulty nonce in the first couple of cycles. It will be rare
            // enough that we shouldn't need to account for it.
            assertFalse(transaction.isSignatureValid());

            // Reduce difficulty back to 1, to double check
            FieldUtils.writeField(ArbitraryDataManager.getInstance(), "powDifficulty", 1, true);
            assertTrue(transaction.isSignatureValid());

        }

    }

}
