package org.aquila.network.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.aquila.data.network.OnlineAccountData;
import org.aquila.transform.Transformer;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

public class OnlineAccountsMessage extends Message {
	private static final int MAX_ACCOUNT_COUNT = 5000;

	private List<OnlineAccountData> onlineAccounts;

	public OnlineAccountsMessage(List<OnlineAccountData> onlineAccounts) {
		super(MessageType.ONLINE_ACCOUNTS);

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();

		try {
			bytes.write(Ints.toByteArray(onlineAccounts.size()));

			for (OnlineAccountData onlineAccountData : onlineAccounts) {
				bytes.write(Longs.toByteArray(onlineAccountData.getTimestamp()));

				bytes.write(onlineAccountData.getSignature());

				bytes.write(onlineAccountData.getPublicKey());
			}
		} catch (IOException e) {
			throw new AssertionError("IOException shouldn't occur with ByteArrayOutputStream");
		}

		this.dataBytes = bytes.toByteArray();
		this.checksumBytes = Message.generateChecksum(this.dataBytes);
	}

	private OnlineAccountsMessage(int id, List<OnlineAccountData> onlineAccounts) {
		super(id, MessageType.ONLINE_ACCOUNTS);

		this.onlineAccounts = onlineAccounts.stream().limit(MAX_ACCOUNT_COUNT).collect(Collectors.toList());
	}

	public List<OnlineAccountData> getOnlineAccounts() {
		return this.onlineAccounts;
	}

	public static Message fromByteBuffer(int id, ByteBuffer bytes) {
		final int accountCount = bytes.getInt();

		List<OnlineAccountData> onlineAccounts = new ArrayList<>(accountCount);

		for (int i = 0; i < Math.min(MAX_ACCOUNT_COUNT, accountCount); ++i) {
			long timestamp = bytes.getLong();

			byte[] signature = new byte[Transformer.SIGNATURE_LENGTH];
			bytes.get(signature);

			byte[] publicKey = new byte[Transformer.PUBLIC_KEY_LENGTH];
			bytes.get(publicKey);

			OnlineAccountData onlineAccountData = new OnlineAccountData(timestamp, signature, publicKey);
			onlineAccounts.add(onlineAccountData);
		}

		return new OnlineAccountsMessage(id, onlineAccounts);
	}

}
