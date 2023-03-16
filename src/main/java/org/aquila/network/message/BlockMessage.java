package org.aquila.network.message;

import java.nio.ByteBuffer;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aquila.data.at.ATStateData;
import org.aquila.data.block.BlockData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.transform.TransformationException;
import org.aquila.transform.block.BlockTransformation;
import org.aquila.transform.block.BlockTransformer;
import org.aquila.utils.Triple;

public class BlockMessage extends Message {

	private static final Logger LOGGER = LogManager.getLogger(BlockMessage.class);

	private final BlockData blockData;
	private final List<TransactionData> transactions;
	private final List<ATStateData> atStates;

	// No public constructor as we're an incoming-only message type.

	private BlockMessage(int id, BlockData blockData, List<TransactionData> transactions, List<ATStateData> atStates) {
		super(id, MessageType.BLOCK);

		this.blockData = blockData;
		this.transactions = transactions;
		this.atStates = atStates;
	}

	public BlockData getBlockData() {
		return this.blockData;
	}

	public List<TransactionData> getTransactions() {
		return this.transactions;
	}

	public List<ATStateData> getAtStates() {
		return this.atStates;
	}

	public static Message fromByteBuffer(int id, ByteBuffer byteBuffer) throws MessageException {
		try {
			int height = byteBuffer.getInt();

			BlockTransformation blockTransformation = BlockTransformer.fromByteBuffer(byteBuffer);

			BlockData blockData = blockTransformation.getBlockData();
			blockData.setHeight(height);

			return new BlockMessage(id, blockData, blockTransformation.getTransactions(), blockTransformation.getAtStates());
		} catch (TransformationException e) {
			LOGGER.info(String.format("Received garbled BLOCK message: %s", e.getMessage()));
			throw new MessageException(e.getMessage(), e);
		}
	}

}
