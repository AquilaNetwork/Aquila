package org.aquila.test.at.aquilafunctioncodes;

import com.google.common.primitives.Bytes;

import org.aquila.account.Account;
import org.aquila.account.PrivateKeyAccount;
import org.aquila.at.AquilaFunctionCode;
import org.aquila.data.at.ATStateData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.repository.RepositoryManager;
import org.aquila.transaction.DeployAtTransaction;
import org.aquila.utils.Base58;
import org.aquila.utils.BitTwiddling;
import org.ciyam.at.CompilationException;
import org.ciyam.at.FunctionCode;
import org.ciyam.at.MachineState;
import org.ciyam.at.OpCode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.aquila.test.common.AtUtils;
import org.aquila.test.common.BlockUtils;
import org.aquila.test.common.Common;
import org.aquila.test.common.TestAccount;

import java.nio.ByteBuffer;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GetAccountBlocksMintedTests extends Common {

    private static final Random RANDOM = new Random();
    private static final long fundingAmount = 1_00000000L;

    private Repository repository = null;
    private byte[] creationBytes = null;
    private PrivateKeyAccount deployer;
    private DeployAtTransaction deployAtTransaction;
    private String atAddress;

    @Before
    public void before() throws DataException {
        Common.useDefaultSettings();

        this.repository = RepositoryManager.getRepository();
        this.deployer = Common.getTestAccount(repository, "alice");

    }

    @After
    public void after() throws DataException {
        if (this.repository != null)
            this.repository.close();

        this.repository = null;
    }

    @Test
    public void testGetAccountBlocksMintedFromAddress() throws DataException {
        Account alice = Common.getTestAccount(repository, "alice");
        byte[] accountBytes = Bytes.ensureCapacity(Base58.decode(alice.getAddress()), 32, 0);

        this.creationBytes = buildGetAccountBlocksMintedAT(accountBytes);

        this.deployAtTransaction = AtUtils.doDeployAT(repository, deployer, creationBytes, fundingAmount);
        this.atAddress = deployAtTransaction.getATAccount().getAddress();

        // Mint a block to allow AT to run - Alice's blocksMinted is incremented AFTER block is processed / AT is run
        Integer expectedBlocksMinted = alice.getBlocksMinted();
        BlockUtils.mintBlock(repository);

        Integer extractedBlocksMinted = extractBlocksMinted(repository, atAddress);
        assertEquals(expectedBlocksMinted, extractedBlocksMinted);
    }

    @Test
    public void testGetAccountBlocksMintedFromPublicKey() throws DataException {
        TestAccount alice = Common.getTestAccount(repository, "alice");
        byte[] accountBytes = alice.getPublicKey();

        this.creationBytes = buildGetAccountBlocksMintedAT(accountBytes);

        this.deployAtTransaction = AtUtils.doDeployAT(repository, deployer, creationBytes, fundingAmount);
        this.atAddress = deployAtTransaction.getATAccount().getAddress();

        // Mint a block to allow AT to run - Alice's blocksMinted is incremented AFTER block is processed / AT is run
        Integer expectedBlocksMinted = alice.getBlocksMinted();
        BlockUtils.mintBlock(repository);

        Integer extractedBlocksMinted = extractBlocksMinted(repository, atAddress);
        assertEquals(expectedBlocksMinted, extractedBlocksMinted);
    }

    @Test
    public void testGetUnknownAccountBlocksMinted() throws DataException {
        byte[] accountBytes = new byte[32];
        RANDOM.nextBytes(accountBytes);

        this.creationBytes = buildGetAccountBlocksMintedAT(accountBytes);

        this.deployAtTransaction = AtUtils.doDeployAT(repository, deployer, creationBytes, fundingAmount);
        this.atAddress = deployAtTransaction.getATAccount().getAddress();

        // Mint a block to allow AT to run - Alice's blocksMinted is incremented AFTER block is processed / AT is run
        BlockUtils.mintBlock(repository);

        Integer extractedBlocksMinted = extractBlocksMinted(repository, atAddress);
        assertNull(extractedBlocksMinted);
    }

    private static byte[] buildGetAccountBlocksMintedAT(byte[] accountBytes) {
        // Labels for data segment addresses
        int addrCounter = 0;

        // Beginning of data segment for easy extraction
        final int addrBlocksMinted = addrCounter++;

        // accountBytes
        final int addrAccountBytes = addrCounter;
        addrCounter += 4;

        // Pointer to accountBytes so we can load them into B
        final int addrAccountBytesPointer = addrCounter++;

        // Data segment
        ByteBuffer dataByteBuffer = ByteBuffer.allocate(addrCounter * MachineState.VALUE_SIZE);

        // Write accountBytes
        dataByteBuffer.position(addrAccountBytes * MachineState.VALUE_SIZE);
        dataByteBuffer.put(accountBytes);

        // Store pointer to addrAccountbytes at addrAccountBytesPointer
        assertEquals(addrAccountBytesPointer * MachineState.VALUE_SIZE, dataByteBuffer.position());
        dataByteBuffer.putLong(addrAccountBytes);

        ByteBuffer codeByteBuffer = ByteBuffer.allocate(512);

        // Two-pass version
        for (int pass = 0; pass < 2; ++pass) {
            codeByteBuffer.clear();

            try {
                /* Initialization */

                // Copy accountBytes from data segment into B, starting at addrAccountBytes (as pointed to by addrAccountBytesPointer)
                codeByteBuffer.put(OpCode.EXT_FUN_DAT.compile(FunctionCode.SET_B_IND, addrAccountBytesPointer));

                // Get account's blocks minted count and save into addrBlocksMinted
                codeByteBuffer.put(OpCode.EXT_FUN_RET.compile(AquilaFunctionCode.GET_BLOCKS_MINTED_FROM_ACCOUNT_IN_B.value, addrBlocksMinted));

                // We're done
                codeByteBuffer.put(OpCode.FIN_IMD.compile());
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

    private Integer extractBlocksMinted(Repository repository, String atAddress) throws DataException {
        // Check AT result
        ATStateData atStateData = repository.getATRepository().getLatestATState(atAddress);
        byte[] stateData = atStateData.getStateData();

        byte[] dataBytes = MachineState.extractDataBytes(stateData);

        Long blocksMintedValue = BitTwiddling.longFromBEBytes(dataBytes, 0);
        if (blocksMintedValue == -1)
            return null;

        return blocksMintedValue.intValue();
    }
}
