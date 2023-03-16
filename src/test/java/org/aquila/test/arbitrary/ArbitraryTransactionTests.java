package org.aquila.test.arbitrary;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.aquila.account.PrivateKeyAccount;
import org.aquila.arbitrary.ArbitraryDataFile;
import org.aquila.arbitrary.ArbitraryDataTransactionBuilder;
import org.aquila.arbitrary.exception.MissingDataException;
import org.aquila.arbitrary.misc.Service;
import org.aquila.controller.arbitrary.ArbitraryDataManager;
import org.aquila.data.transaction.ArbitraryTransactionData;
import org.aquila.data.transaction.RegisterNameTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.repository.RepositoryManager;
import org.aquila.transaction.ArbitraryTransaction;
import org.aquila.transaction.RegisterNameTransaction;
import org.aquila.transaction.Transaction;
import org.aquila.utils.Base58;
import org.aquila.utils.NTP;
import org.junit.Before;
import org.junit.Test;
import org.aquila.test.common.ArbitraryUtils;
import org.aquila.test.common.Common;
import org.aquila.test.common.TransactionUtils;
import org.aquila.test.common.transaction.TestTransaction;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class ArbitraryTransactionTests extends Common {

    @Before
    public void beforeTest() throws DataException, IllegalAccessException {
        Common.useDefaultSettings();
    }

    @Test
    public void testDifficultyTooLow() throws IllegalAccessException, DataException, IOException {
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

    @Test
    public void testNonceAndFee() throws IllegalAccessException, DataException, IOException {
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

            // Create PUT transaction, with a fee
            Path path1 = ArbitraryUtils.generateRandomDataPath(dataLength);
            long fee = 10000000; // sufficient
            boolean computeNonce = true;
            ArbitraryDataFile arbitraryDataFile = ArbitraryUtils.createAndMintTxn(repository, publicKey58, path1, name, identifier, ArbitraryTransactionData.Method.PUT, service, alice, chunkSize, fee, computeNonce, null, null, null, null);

            // Check that nonce validation succeeds
            byte[] signature = arbitraryDataFile.getSignature();
            TransactionData transactionData = repository.getTransactionRepository().fromSignature(signature);
            ArbitraryTransaction transaction = new ArbitraryTransaction(repository, transactionData);
            assertTrue(transaction.isSignatureValid());

            // Increase difficulty to 15
            FieldUtils.writeField(ArbitraryDataManager.getInstance(), "powDifficulty", 15, true);

            // Make sure that nonce validation still succeeds, as the fee has allowed us to avoid including a nonce
            assertTrue(transaction.isSignatureValid());
        }
    }

    @Test
    public void testNonceAndLowFee() throws IllegalAccessException, DataException, IOException {
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

            // Create PUT transaction, with a fee that is too low
            Path path1 = ArbitraryUtils.generateRandomDataPath(dataLength);
            long fee = 9999999; // insufficient
            boolean computeNonce = true;
            boolean insufficientFeeDetected = false;
            try {
                ArbitraryUtils.createAndMintTxn(repository, publicKey58, path1, name, identifier, ArbitraryTransactionData.Method.PUT, service, alice, chunkSize, fee, computeNonce, null, null, null, null);
            }
            catch (DataException e) {
                if (e.getMessage().contains("INSUFFICIENT_FEE")) {
                    insufficientFeeDetected = true;
                }
            }

            // Transaction should be invalid due to an insufficient fee
            assertTrue(insufficientFeeDetected);
        }
    }

    @Test
    public void testFeeNoNonce() throws IllegalAccessException, DataException, IOException {
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

            // Create PUT transaction, with a fee
            Path path1 = ArbitraryUtils.generateRandomDataPath(dataLength);
            long fee = 10000000; // sufficient
            boolean computeNonce = false;
            ArbitraryDataFile arbitraryDataFile = ArbitraryUtils.createAndMintTxn(repository, publicKey58, path1, name, identifier, ArbitraryTransactionData.Method.PUT, service, alice, chunkSize, fee, computeNonce, null, null, null, null);

            // Check that nonce validation succeeds, even though it wasn't computed. This is because we have included a sufficient fee.
            byte[] signature = arbitraryDataFile.getSignature();
            TransactionData transactionData = repository.getTransactionRepository().fromSignature(signature);
            ArbitraryTransaction transaction = new ArbitraryTransaction(repository, transactionData);
            assertTrue(transaction.isSignatureValid());

            // Increase difficulty to 15
            FieldUtils.writeField(ArbitraryDataManager.getInstance(), "powDifficulty", 15, true);

            // Make sure that nonce validation still succeeds, as the fee has allowed us to avoid including a nonce
            assertTrue(transaction.isSignatureValid());
        }
    }

    @Test
    public void testLowFeeNoNonce() throws IllegalAccessException, DataException, IOException {
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

            // Create PUT transaction, with a fee that is too low. Also, don't compute a nonce.
            Path path1 = ArbitraryUtils.generateRandomDataPath(dataLength);
            long fee = 9999999; // insufficient

            ArbitraryDataTransactionBuilder txnBuilder = new ArbitraryDataTransactionBuilder(
                    repository, publicKey58, fee, path1, name, ArbitraryTransactionData.Method.PUT, service, identifier, null, null, null, null);

            txnBuilder.setChunkSize(chunkSize);
            txnBuilder.build();
            ArbitraryTransactionData transactionData = txnBuilder.getArbitraryTransactionData();
            Transaction.ValidationResult result = TransactionUtils.signAndImport(repository, transactionData, alice);

            // Transaction should be invalid due to an insufficient fee
            assertEquals(Transaction.ValidationResult.INSUFFICIENT_FEE, result);
        }
    }

    @Test
    public void testZeroFeeNoNonce() throws IllegalAccessException, DataException, IOException {
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

            // Create PUT transaction, with a fee that is too low. Also, don't compute a nonce.
            Path path1 = ArbitraryUtils.generateRandomDataPath(dataLength);
            long fee = 0L;

            ArbitraryDataTransactionBuilder txnBuilder = new ArbitraryDataTransactionBuilder(
                    repository, publicKey58, fee, path1, name, ArbitraryTransactionData.Method.PUT, service, identifier, null, null, null, null);

            txnBuilder.setChunkSize(chunkSize);
            txnBuilder.build();
            ArbitraryTransactionData transactionData = txnBuilder.getArbitraryTransactionData();
            ArbitraryTransaction arbitraryTransaction = new ArbitraryTransaction(repository, transactionData);

            // Transaction should be invalid
            assertFalse(arbitraryTransaction.isSignatureValid());
        }
    }

    @Test
    public void testNonceAndFeeBeforeFeatureTrigger() throws IllegalAccessException, DataException, IOException {
        // Use v2-minting settings, as these are pre-feature-trigger
        Common.useSettings("test-settings-v2-minting.json");

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

            // Create PUT transaction, with a fee
            Path path1 = ArbitraryUtils.generateRandomDataPath(dataLength);
            long fee = 10000000; // sufficient
            boolean computeNonce = true;
            ArbitraryDataFile arbitraryDataFile = ArbitraryUtils.createAndMintTxn(repository, publicKey58, path1, name, identifier, ArbitraryTransactionData.Method.PUT, service, alice, chunkSize, fee, computeNonce, null, null, null, null);

            // Check that nonce validation succeeds
            byte[] signature = arbitraryDataFile.getSignature();
            TransactionData transactionData = repository.getTransactionRepository().fromSignature(signature);
            ArbitraryTransaction transaction = new ArbitraryTransaction(repository, transactionData);
            assertTrue(transaction.isSignatureValid());

            // Increase difficulty to 15
            FieldUtils.writeField(ArbitraryDataManager.getInstance(), "powDifficulty", 15, true);

            // Make sure the nonce validation fails, as we aren't allowing a fee to replace a nonce yet.
            // Note: there is a very tiny chance this could succeed due to being extremely lucky
            // and finding a high difficulty nonce in the first couple of cycles. It will be rare
            // enough that we shouldn't need to account for it.
            assertFalse(transaction.isSignatureValid());

            // Reduce difficulty back to 1, to double check
            FieldUtils.writeField(ArbitraryDataManager.getInstance(), "powDifficulty", 1, true);
            assertTrue(transaction.isSignatureValid());
        }
    }

    @Test
    public void testNonceAndInsufficientFeeBeforeFeatureTrigger() throws IllegalAccessException, DataException, IOException {
        // Use v2-minting settings, as these are pre-feature-trigger
        Common.useSettings("test-settings-v2-minting.json");

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

            // Create PUT transaction, with a fee
            Path path1 = ArbitraryUtils.generateRandomDataPath(dataLength);
            long fee = 9999999; // insufficient
            boolean computeNonce = true;
            ArbitraryDataFile arbitraryDataFile = ArbitraryUtils.createAndMintTxn(repository, publicKey58, path1, name, identifier, ArbitraryTransactionData.Method.PUT, service, alice, chunkSize, fee, computeNonce, null, null, null, null);

            // Check that nonce validation succeeds
            byte[] signature = arbitraryDataFile.getSignature();
            TransactionData transactionData = repository.getTransactionRepository().fromSignature(signature);
            ArbitraryTransaction transaction = new ArbitraryTransaction(repository, transactionData);
            assertTrue(transaction.isSignatureValid());

            // The transaction should be valid because we don't care about the fee (before the feature trigger)
            assertEquals(Transaction.ValidationResult.OK, transaction.isValidUnconfirmed());

            // Increase difficulty to 15
            FieldUtils.writeField(ArbitraryDataManager.getInstance(), "powDifficulty", 15, true);

            // Make sure the nonce validation fails, as we aren't allowing a fee to replace a nonce yet (and it was insufficient anyway)
            // Note: there is a very tiny chance this could succeed due to being extremely lucky
            // and finding a high difficulty nonce in the first couple of cycles. It will be rare
            // enough that we shouldn't need to account for it.
            assertFalse(transaction.isSignatureValid());

            // Reduce difficulty back to 1, to double check
            FieldUtils.writeField(ArbitraryDataManager.getInstance(), "powDifficulty", 1, true);
            assertTrue(transaction.isSignatureValid());
        }
    }

    @Test
    public void testNonceAndZeroFeeBeforeFeatureTrigger() throws IllegalAccessException, DataException, IOException {
        // Use v2-minting settings, as these are pre-feature-trigger
        Common.useSettings("test-settings-v2-minting.json");

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

            // Create PUT transaction, with a fee
            Path path1 = ArbitraryUtils.generateRandomDataPath(dataLength);
            long fee = 0L;
            boolean computeNonce = true;
            ArbitraryDataFile arbitraryDataFile = ArbitraryUtils.createAndMintTxn(repository, publicKey58, path1, name, identifier, ArbitraryTransactionData.Method.PUT, service, alice, chunkSize, fee, computeNonce, null, null, null, null);

            // Check that nonce validation succeeds
            byte[] signature = arbitraryDataFile.getSignature();
            TransactionData transactionData = repository.getTransactionRepository().fromSignature(signature);
            ArbitraryTransaction transaction = new ArbitraryTransaction(repository, transactionData);
            assertTrue(transaction.isSignatureValid());

            // The transaction should be valid because we don't care about the fee (before the feature trigger)
            assertEquals(Transaction.ValidationResult.OK, transaction.isValidUnconfirmed());

            // Increase difficulty to 15
            FieldUtils.writeField(ArbitraryDataManager.getInstance(), "powDifficulty", 15, true);

            // Make sure the nonce validation fails, as we aren't allowing a fee to replace a nonce yet (and it was insufficient anyway)
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
