package org.aquila.test.common;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.asset.Asset;
import org.aquila.data.transaction.BaseTransactionData;
import org.aquila.data.transaction.DeployAtTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.group.Group;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.transaction.DeployAtTransaction;
import org.ciyam.at.CompilationException;
import org.ciyam.at.MachineState;
import org.ciyam.at.OpCode;

import java.nio.ByteBuffer;

public class AtUtils {

    public static byte[] buildSimpleAT() {
        // Pretend we use 4 values in data segment
        int addrCounter = 4;

        // Data segment
        ByteBuffer dataByteBuffer = ByteBuffer.allocate(addrCounter * MachineState.VALUE_SIZE);

        ByteBuffer codeByteBuffer = ByteBuffer.allocate(512);

        // Two-pass version
        for (int pass = 0; pass < 2; ++pass) {
            codeByteBuffer.clear();

            try {
                // Stop and wait for next block
                codeByteBuffer.put(OpCode.STP_IMD.compile());
            } catch (CompilationException e) {
                throw new IllegalStateException("Unable to compile AT?", e);
            }
        }

        codeByteBuffer.flip();

        byte[] codeBytes = new byte[codeByteBuffer.limit()];
        codeByteBuffer.get(codeBytes);

        final short ciyamAtVersion = 2;
        final short numCallStackPages = 0;
        final short numUserStackPages = 0;
        final long minActivationAmount = 0L;

        return MachineState.toCreationBytes(ciyamAtVersion, codeBytes, dataByteBuffer.array(), numCallStackPages, numUserStackPages, minActivationAmount);
    }

    public static DeployAtTransaction doDeployAT(Repository repository, PrivateKeyAccount deployer, byte[] creationBytes, long fundingAmount) throws DataException {
        long txTimestamp = System.currentTimeMillis();
        byte[] lastReference = deployer.getLastReference();

        if (lastReference == null) {
            System.err.println(String.format("Aquila account %s has no last reference", deployer.getAddress()));
            System.exit(2);
        }

        Long fee = null;
        String name = "Test AT";
        String description = "Test AT";
        String atType = "Test";
        String tags = "TEST";

        BaseTransactionData baseTransactionData = new BaseTransactionData(txTimestamp, Group.NO_GROUP, lastReference, deployer.getPublicKey(), fee, null);
        TransactionData deployAtTransactionData = new DeployAtTransactionData(baseTransactionData, name, description, atType, tags, creationBytes, fundingAmount, Asset.UNCIA);

        DeployAtTransaction deployAtTransaction = new DeployAtTransaction(repository, deployAtTransactionData);

        fee = deployAtTransaction.calcRecommendedFee();
        deployAtTransactionData.setFee(fee);

        TransactionUtils.signAndMint(repository, deployAtTransactionData, deployer);

        return deployAtTransaction;
    }
}
