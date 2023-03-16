package org.aquila.network.message;

import com.google.common.primitives.Ints;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.aquila.block.Block;
import org.aquila.transform.TransformationException;
import org.aquila.transform.block.BlockTransformer;

// This is an OUTGOING-only Message which more readily lends itself to being cached
public class CachedBlockV2Message extends Message implements Cloneable {

	public CachedBlockV2Message(Block block) throws TransformationException {
		super(MessageType.BLOCK_V2);

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();

		try {
			bytes.write(Ints.toByteArray(block.getBlockData().getHeight()));

			bytes.write(BlockTransformer.toBytes(block));
		} catch (IOException e) {
			throw new AssertionError("IOException shouldn't occur with ByteArrayOutputStream");
		}

		this.dataBytes = bytes.toByteArray();
		this.checksumBytes = Message.generateChecksum(this.dataBytes);
	}

	public CachedBlockV2Message(byte[] cachedBytes) {
		super(MessageType.BLOCK_V2);

		this.dataBytes = cachedBytes;
		this.checksumBytes = Message.generateChecksum(this.dataBytes);
	}

	public static Message fromByteBuffer(int id, ByteBuffer byteBuffer) {
		throw new UnsupportedOperationException("CachedBlockMessageV2 is for outgoing messages only");
	}

}
