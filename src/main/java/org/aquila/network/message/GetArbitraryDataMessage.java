package org.aquila.network.message;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.aquila.transform.Transformer;

public class GetArbitraryDataMessage extends Message {

	private byte[] signature;

	public GetArbitraryDataMessage(byte[] signature) {
		super(MessageType.GET_ARBITRARY_DATA);

		this.dataBytes = Arrays.copyOf(signature, signature.length);
		this.checksumBytes = Message.generateChecksum(this.dataBytes);
	}

	private GetArbitraryDataMessage(int id, byte[] signature) {
		super(id, MessageType.GET_ARBITRARY_DATA);

		this.signature = signature;
	}

	public byte[] getSignature() {
		return this.signature;
	}

	public static Message fromByteBuffer(int id, ByteBuffer bytes) {
		byte[] signature = new byte[Transformer.SIGNATURE_LENGTH];

		bytes.get(signature);

		return new GetArbitraryDataMessage(id, signature);
	}

}
