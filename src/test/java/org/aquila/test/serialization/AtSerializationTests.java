package org.aquila.test.serialization;

import com.google.common.hash.HashCode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.aquila.test.common.Common;
import org.aquila.test.common.transaction.AtTestTransaction;

import static org.junit.Assert.assertEquals;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.data.transaction.ATTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.repository.RepositoryManager;
import org.aquila.transaction.Transaction;
import org.aquila.transform.TransformationException;
import org.aquila.transform.transaction.TransactionTransformer;
import org.aquila.utils.Base58;

public class AtSerializationTests extends Common {

    @Before
    public void beforeTest() throws DataException {
        Common.useDefaultSettings();
    }

    @After
    public void afterTest() throws DataException {
        Common.orphanCheck();
    }


    @Test
    public void testPaymentTypeAtSerialization() throws DataException, TransformationException {
        try (final Repository repository = RepositoryManager.getRepository()) {

            // Build PAYMENT-type AT transaction
            PrivateKeyAccount signingAccount = Common.getTestAccount(repository, "alice");
            ATTransactionData transactionData = (ATTransactionData) AtTestTransaction.paymentType(repository, signingAccount, true);
            Transaction transaction = Transaction.fromData(repository, transactionData);
            transaction.sign(signingAccount);

            final int claimedLength = TransactionTransformer.getDataLength(transactionData);
            byte[] serializedTransaction = TransactionTransformer.toBytes(transactionData);
            assertEquals("Serialized PAYMENT-type AT transaction length differs from declared length", claimedLength, serializedTransaction.length);

            TransactionData deserializedTransactionData = TransactionTransformer.fromBytes(serializedTransaction);
            // Re-sign
            Transaction deserializedTransaction = Transaction.fromData(repository, deserializedTransactionData);
            deserializedTransaction.sign(signingAccount);
            assertEquals("Deserialized PAYMENT-type AT transaction signature differs", Base58.encode(transactionData.getSignature()), Base58.encode(deserializedTransactionData.getSignature()));

            // Re-serialize to check new length and bytes
            final int reclaimedLength = TransactionTransformer.getDataLength(deserializedTransactionData);
            assertEquals("Reserialized PAYMENT-type AT transaction declared length differs", claimedLength, reclaimedLength);

            byte[] reserializedTransaction = TransactionTransformer.toBytes(deserializedTransactionData);
            assertEquals("Reserialized PAYMENT-type AT transaction bytes differ", HashCode.fromBytes(serializedTransaction).toString(), HashCode.fromBytes(reserializedTransaction).toString());
        }
    }

    @Test
    public void testMessageTypeAtSerialization() throws DataException, TransformationException {
        try (final Repository repository = RepositoryManager.getRepository()) {

            // Build MESSAGE-type AT transaction
            PrivateKeyAccount signingAccount = Common.getTestAccount(repository, "alice");
            ATTransactionData transactionData = (ATTransactionData) AtTestTransaction.messageType(repository, signingAccount, true);
            Transaction transaction = Transaction.fromData(repository, transactionData);
            transaction.sign(signingAccount);

            // MESSAGE-type AT transactions are only fully supported since transaction V6
            assertEquals(6, Transaction.getVersionByTimestamp(transactionData.getTimestamp()));

            final int claimedLength = TransactionTransformer.getDataLength(transactionData);
            byte[] serializedTransaction = TransactionTransformer.toBytes(transactionData);
            assertEquals("Serialized MESSAGE-type AT transaction length differs from declared length", claimedLength, serializedTransaction.length);

            TransactionData deserializedTransactionData = TransactionTransformer.fromBytes(serializedTransaction);
            // Re-sign
            Transaction deserializedTransaction = Transaction.fromData(repository, deserializedTransactionData);
            deserializedTransaction.sign(signingAccount);
            assertEquals("Deserialized MESSAGE-type AT transaction signature differs", Base58.encode(transactionData.getSignature()), Base58.encode(deserializedTransactionData.getSignature()));

            // Re-serialize to check new length and bytes
            final int reclaimedLength = TransactionTransformer.getDataLength(deserializedTransactionData);
            assertEquals("Reserialized MESSAGE-type AT transaction declared length differs", claimedLength, reclaimedLength);

            byte[] reserializedTransaction = TransactionTransformer.toBytes(deserializedTransactionData);
            assertEquals("Reserialized MESSAGE-type AT transaction bytes differ", HashCode.fromBytes(serializedTransaction).toString(), HashCode.fromBytes(reserializedTransaction).toString());
        }
    }

}
