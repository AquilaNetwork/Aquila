package org.aquila.test.serialization;

import com.google.common.hash.HashCode;
import org.junit.Before;
import org.junit.Test;
import org.aquila.test.common.Common;
import org.aquila.test.common.transaction.ChatTestTransaction;

import static org.junit.Assert.*;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.data.transaction.ChatTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.repository.RepositoryManager;
import org.aquila.transaction.Transaction;
import org.aquila.transform.TransformationException;
import org.aquila.transform.transaction.TransactionTransformer;
import org.aquila.utils.Base58;

public class ChatSerializationTests {

    @Before
    public void beforeTest() throws DataException {
        Common.useDefaultSettings();
    }


    @Test
    public void testChatSerializationWithChatReference() throws DataException, TransformationException {
        try (final Repository repository = RepositoryManager.getRepository()) {

            // Build MESSAGE-type AT transaction with chatReference
            PrivateKeyAccount signingAccount = Common.getTestAccount(repository, "alice");
            ChatTransactionData transactionData = (ChatTransactionData) ChatTestTransaction.randomTransaction(repository, signingAccount, true);
            Transaction transaction = Transaction.fromData(repository, transactionData);
            transaction.sign(signingAccount);

            assertNotNull(transactionData.getChatReference());

            final int claimedLength = TransactionTransformer.getDataLength(transactionData);
            byte[] serializedTransaction = TransactionTransformer.toBytes(transactionData);
            assertEquals("Serialized CHAT transaction length differs from declared length", claimedLength, serializedTransaction.length);

            TransactionData deserializedTransactionData = TransactionTransformer.fromBytes(serializedTransaction);
            // Re-sign
            Transaction deserializedTransaction = Transaction.fromData(repository, deserializedTransactionData);
            deserializedTransaction.sign(signingAccount);
            assertEquals("Deserialized CHAT transaction signature differs", Base58.encode(transactionData.getSignature()), Base58.encode(deserializedTransactionData.getSignature()));

            // Re-serialize to check new length and bytes
            final int reclaimedLength = TransactionTransformer.getDataLength(deserializedTransactionData);
            assertEquals("Reserialized CHAT transaction declared length differs", claimedLength, reclaimedLength);

            byte[] reserializedTransaction = TransactionTransformer.toBytes(deserializedTransactionData);
            assertEquals("Reserialized CHAT transaction bytes differ", HashCode.fromBytes(serializedTransaction).toString(), HashCode.fromBytes(reserializedTransaction).toString());

            // Deserialized chat reference must match initial chat reference
            ChatTransactionData deserializedChatTransactionData = (ChatTransactionData) deserializedTransactionData;
            assertNotNull(deserializedChatTransactionData.getChatReference());
            assertArrayEquals(deserializedChatTransactionData.getChatReference(), transactionData.getChatReference());
        }
    }

    @Test
    public void testChatSerializationWithoutChatReference() throws DataException, TransformationException {
        try (final Repository repository = RepositoryManager.getRepository()) {

            // Build MESSAGE-type AT transaction without chatReference
            PrivateKeyAccount signingAccount = Common.getTestAccount(repository, "alice");
            ChatTransactionData transactionData = (ChatTransactionData) ChatTestTransaction.randomTransaction(repository, signingAccount, true);
            transactionData.setChatReference(null);
            Transaction transaction = Transaction.fromData(repository, transactionData);
            transaction.sign(signingAccount);

            assertNull(transactionData.getChatReference());

            final int claimedLength = TransactionTransformer.getDataLength(transactionData);
            byte[] serializedTransaction = TransactionTransformer.toBytes(transactionData);
            assertEquals("Serialized CHAT transaction length differs from declared length", claimedLength, serializedTransaction.length);

            TransactionData deserializedTransactionData = TransactionTransformer.fromBytes(serializedTransaction);
            // Re-sign
            Transaction deserializedTransaction = Transaction.fromData(repository, deserializedTransactionData);
            deserializedTransaction.sign(signingAccount);
            assertEquals("Deserialized CHAT transaction signature differs", Base58.encode(transactionData.getSignature()), Base58.encode(deserializedTransactionData.getSignature()));

            // Re-serialize to check new length and bytes
            final int reclaimedLength = TransactionTransformer.getDataLength(deserializedTransactionData);
            assertEquals("Reserialized CHAT transaction declared length differs", claimedLength, reclaimedLength);

            byte[] reserializedTransaction = TransactionTransformer.toBytes(deserializedTransactionData);
            assertEquals("Reserialized CHAT transaction bytes differ", HashCode.fromBytes(serializedTransaction).toString(), HashCode.fromBytes(reserializedTransaction).toString());

            // Deserialized chat reference must match initial chat reference
            ChatTransactionData deserializedChatTransactionData = (ChatTransactionData) deserializedTransactionData;
            assertNull(deserializedChatTransactionData.getChatReference());
            assertArrayEquals(deserializedChatTransactionData.getChatReference(), transactionData.getChatReference());
        }
    }

}
