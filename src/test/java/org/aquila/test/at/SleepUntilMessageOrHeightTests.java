package org.aquila.test.at;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.aquila.account.Account;
import org.aquila.account.PrivateKeyAccount;
import org.aquila.asset.Asset;
import org.aquila.at.AquilaFunctionCode;
import org.aquila.block.Block;
import org.aquila.data.at.ATStateData;
import org.aquila.data.block.BlockData;
import org.aquila.data.transaction.BaseTransactionData;
import org.aquila.data.transaction.DeployAtTransactionData;
import org.aquila.data.transaction.MessageTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.group.Group;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.repository.RepositoryManager;
import org.aquila.transaction.DeployAtTransaction;
import org.aquila.transaction.MessageTransaction;
import org.aquila.transaction.Transaction;
import org.aquila.utils.BitTwiddling;
import org.ciyam.at.CompilationException;
import org.ciyam.at.FunctionCode;
import org.ciyam.at.MachineState;
import org.ciyam.at.OpCode;
import org.ciyam.at.Timestamp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.aquila.test.common.BlockUtils;
import org.aquila.test.common.Common;
import org.aquila.test.common.TransactionUtils;

public class SleepUntilMessageOrHeightTests extends Common {

	private static final byte[] messageData = new byte[] { 0x44 };
	private static final byte[] creationBytes = buildSleepUntilMessageOrHeightAT();
	private static final long fundingAmount = 1_00000000L;
	private static final long WAKE_HEIGHT = 10L;

	private Repository repository = null;
	private PrivateKeyAccount deployer;
	private DeployAtTransaction deployAtTransaction;
	private Account atAccount;
	private String atAddress;
	private byte[] rawNextTimestamp = new byte[32];
	private Transaction transaction;

	@Before
	public void before() throws DataException {
		Common.useDefaultSettings();

		this.repository = RepositoryManager.getRepository();
		this.deployer = Common.getTestAccount(repository, "alice");

		this.deployAtTransaction = doDeploy(repository, deployer, creationBytes, fundingAmount);
		this.atAccount = deployAtTransaction.getATAccount();
		this.atAddress = deployAtTransaction.getATAccount().getAddress();
	}

	@After
	public void after() throws DataException {
		if (this.repository != null)
			this.repository.close();

		this.repository = null;
	}

	@Test
	public void testDeploy() throws DataException {
			// Confirm initial value is zero
			extractNextTxTimestamp(repository, atAddress, rawNextTimestamp);
			assertArrayEquals(new byte[32], rawNextTimestamp);
	}

	@Test
	public void testFeelessSleep() throws DataException {
		// Mint block to allow AT to initialize and call SLEEP_UNTIL_MESSAGE
		BlockUtils.mintBlock(repository);

		// Fetch AT's balance for this height
		long preMintBalance = atAccount.getConfirmedBalance(Asset.UNCIA);

		// Mint block
		BlockUtils.mintBlock(repository);

		// Fetch new AT balance
		long postMintBalance = atAccount.getConfirmedBalance(Asset.UNCIA);

		assertEquals(preMintBalance, postMintBalance);
	}

	@Test
	public void testFeelessSleep2() throws DataException {
		// Mint block to allow AT to initialize and call SLEEP_UNTIL_MESSAGE
		BlockUtils.mintBlock(repository);

		// Fetch AT's balance for this height
		long preMintBalance = atAccount.getConfirmedBalance(Asset.UNCIA);

		// Mint several blocks
		for (int i = 0; i < 5; ++i)
			BlockUtils.mintBlock(repository);

		// Fetch new AT balance
		long postMintBalance = atAccount.getConfirmedBalance(Asset.UNCIA);

		assertEquals(preMintBalance, postMintBalance);
	}

	@Test
	public void testSleepUntilMessage() throws DataException {
		// Mint block to allow AT to initialize and call SLEEP_UNTIL_MESSAGE_OR_HEIGHT
		BlockUtils.mintBlock(repository);

		// Send message to AT
		transaction = sendMessage(repository, deployer, messageData, atAddress);
		BlockUtils.mintBlock(repository);

		// Mint block so AT executes and finds message
		BlockUtils.mintBlock(repository);

		// Confirm AT finds message
		assertTimestamp(repository, atAddress, transaction);
	}

	@Test
	public void testSleepUntilHeight() throws DataException {
		// AT deployment in block 2

		// Mint block to allow AT to initialize and call SLEEP_UNTIL_MESSAGE_OR_HEIGHT
		BlockUtils.mintBlock(repository); // height now 3

		// Fetch AT's balance for this height
		long preMintBalance = atAccount.getConfirmedBalance(Asset.UNCIA);

		// Mint several blocks
		for (int i = 3; i < WAKE_HEIGHT; ++i)
			BlockUtils.mintBlock(repository);

		// We should now be at WAKE_HEIGHT
		long height = repository.getBlockRepository().getBlockchainHeight();
		assertEquals(WAKE_HEIGHT, height);

		// AT should have woken and run at this height so balance should have changed

		// Fetch new AT balance
		long postMintBalance = atAccount.getConfirmedBalance(Asset.UNCIA);

		assertNotSame(preMintBalance, postMintBalance);

		// Confirm AT has no message
		extractNextTxTimestamp(repository, atAddress, rawNextTimestamp);
		assertArrayEquals(new byte[32], rawNextTimestamp);

		// Mint yet another block
		BlockUtils.mintBlock(repository);

		// AT should also have woken and run at this height so balance should have changed

		// Fetch new AT balance
		long postMint2Balance = atAccount.getConfirmedBalance(Asset.UNCIA);

		assertNotSame(postMintBalance, postMint2Balance);

		// Confirm AT still has no message
		extractNextTxTimestamp(repository, atAddress, rawNextTimestamp);
		assertArrayEquals(new byte[32], rawNextTimestamp);

	}

	private static byte[] buildSleepUntilMessageOrHeightAT() {
		// Labels for data segment addresses
		int addrCounter = 0;

		// Beginning of data segment for easy extraction
		final int addrNextTx = addrCounter;
		addrCounter += 4;

		final int addrNextTxIndex = addrCounter++;

		final int addrLastTxTimestamp = addrCounter++;

		final int addrWakeHeight = addrCounter++;

		// Data segment
		ByteBuffer dataByteBuffer = ByteBuffer.allocate(addrCounter * MachineState.VALUE_SIZE);

		// skip addrNextTx
		dataByteBuffer.position(dataByteBuffer.position() + 4 * MachineState.VALUE_SIZE);

		// Store pointer to addrNextTx at addrNextTxIndex
		dataByteBuffer.putLong(addrNextTx);

		// skip addrLastTxTimestamp
		dataByteBuffer.position(dataByteBuffer.position() + MachineState.VALUE_SIZE);

		// Store fixed wake height (block 10)
		dataByteBuffer.putLong(WAKE_HEIGHT);

		ByteBuffer codeByteBuffer = ByteBuffer.allocate(512);

		// Two-pass version
		for (int pass = 0; pass < 2; ++pass) {
			codeByteBuffer.clear();

			try {
				/* Initialization */

				// Use AT creation 'timestamp' as starting point for finding transactions sent to AT
				codeByteBuffer.put(OpCode.EXT_FUN_RET.compile(FunctionCode.GET_CREATION_TIMESTAMP, addrLastTxTimestamp));

				// Set restart position to after this opcode
				codeByteBuffer.put(OpCode.SET_PCS.compile());

				/* Loop, waiting for message to AT */

				/* Sleep until message arrives */
				codeByteBuffer.put(OpCode.EXT_FUN_DAT_2.compile(AquilaFunctionCode.SLEEP_UNTIL_MESSAGE_OR_HEIGHT.value, addrLastTxTimestamp, addrWakeHeight));

				// Find next transaction to this AT since the last one (if any)
				codeByteBuffer.put(OpCode.EXT_FUN_DAT.compile(FunctionCode.PUT_TX_AFTER_TIMESTAMP_INTO_A, addrLastTxTimestamp));

				// Copy A to data segment, starting at addrNextTx (as pointed to by addrNextTxIndex)
				codeByteBuffer.put(OpCode.EXT_FUN_DAT.compile(FunctionCode.GET_A_IND, addrNextTxIndex));

				// Stop if timestamp part of A is zero
				codeByteBuffer.put(OpCode.STZ_DAT.compile(addrNextTx));

				// Update our 'last found transaction's timestamp' using 'timestamp' from transaction
				codeByteBuffer.put(OpCode.EXT_FUN_RET.compile(FunctionCode.GET_TIMESTAMP_FROM_TX_IN_A, addrLastTxTimestamp));

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

	private DeployAtTransaction doDeploy(Repository repository, PrivateKeyAccount deployer, byte[] creationBytes, long fundingAmount) throws DataException {
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

	private void extractNextTxTimestamp(Repository repository, String atAddress, byte[] rawNextTimestamp) throws DataException {
		// Check AT result
		ATStateData atStateData = repository.getATRepository().getLatestATState(atAddress);
		byte[] stateData = atStateData.getStateData();

		byte[] dataBytes = MachineState.extractDataBytes(stateData);

		System.arraycopy(dataBytes, 0, rawNextTimestamp, 0, rawNextTimestamp.length);
	}

	private MessageTransaction sendMessage(Repository repository, PrivateKeyAccount sender, byte[] data, String recipient) throws DataException {
		long txTimestamp = System.currentTimeMillis();
		byte[] lastReference = sender.getLastReference();

		if (lastReference == null) {
			System.err.println(String.format("Aquila account %s has no last reference", sender.getAddress()));
			System.exit(2);
		}

		Long fee = null;
		int version = 4;
		int nonce = 0;
		long amount = 0;
		Long assetId = null; // because amount is zero

		BaseTransactionData baseTransactionData = new BaseTransactionData(txTimestamp, Group.NO_GROUP, lastReference, sender.getPublicKey(), fee, null);
		TransactionData messageTransactionData = new MessageTransactionData(baseTransactionData, version, nonce, recipient, amount, assetId, data, false, false);

		MessageTransaction messageTransaction = new MessageTransaction(repository, messageTransactionData);

		fee = messageTransaction.calcRecommendedFee();
		messageTransactionData.setFee(fee);

		TransactionUtils.signAndImportValid(repository, messageTransactionData, sender);

		return messageTransaction;
	}

	private void assertTimestamp(Repository repository, String atAddress, Transaction transaction) throws DataException {
		int height = transaction.getHeight();
		byte[] transactionSignature = transaction.getTransactionData().getSignature();

		BlockData blockData = repository.getBlockRepository().fromHeight(height);
		assertNotNull(blockData);

		Block block = new Block(repository, blockData);

		List<Transaction> blockTransactions = block.getTransactions();
		int sequence;
		for (sequence = blockTransactions.size() - 1; sequence >= 0; --sequence)
			if (Arrays.equals(blockTransactions.get(sequence).getTransactionData().getSignature(), transactionSignature))
				break;

		assertNotSame(-1, sequence);

		byte[] rawNextTimestamp = new byte[32];
		extractNextTxTimestamp(repository, atAddress, rawNextTimestamp);

		Timestamp expectedTimestamp = new Timestamp(height, sequence);
		Timestamp actualTimestamp = new Timestamp(BitTwiddling.longFromBEBytes(rawNextTimestamp, 0));

		assertEquals(String.format("Expected height %d, seq %d but was height %d, seq %d",
					height, sequence,
					actualTimestamp.blockHeight, actualTimestamp.transactionSequence
				),
				expectedTimestamp.longValue(),
				actualTimestamp.longValue());

		byte[] expectedPartialSignature = new byte[24];
		System.arraycopy(transactionSignature, 8, expectedPartialSignature, 0, expectedPartialSignature.length);

		byte[] actualPartialSignature = new byte[24];
		System.arraycopy(rawNextTimestamp, 8, actualPartialSignature, 0, actualPartialSignature.length);

		assertArrayEquals(expectedPartialSignature, actualPartialSignature);
	}

}
