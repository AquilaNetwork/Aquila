package org.aquila.transform.transaction;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.aquila.data.transaction.BaseTransactionData;
import org.aquila.data.transaction.GroupKickTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.group.Group;
import org.aquila.transaction.Transaction.TransactionType;
import org.aquila.transform.TransformationException;
import org.aquila.utils.Serialization;

import com.google.common.base.Utf8;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

public class GroupKickTransactionTransformer extends TransactionTransformer {

	// Property lengths
	private static final int GROUPID_LENGTH = INT_LENGTH;
	private static final int MEMBER_LENGTH = ADDRESS_LENGTH;
	private static final int REASON_SIZE_LENGTH = INT_LENGTH;

	private static final int EXTRAS_LENGTH = GROUPID_LENGTH + MEMBER_LENGTH + REASON_SIZE_LENGTH;

	protected static final TransactionLayout layout;

	static {
		layout = new TransactionLayout();
		layout.add("txType: " + TransactionType.GROUP_KICK.valueString, TransformationType.INT);
		layout.add("timestamp", TransformationType.TIMESTAMP);
		layout.add("transaction's groupID", TransformationType.INT);
		layout.add("reference", TransformationType.SIGNATURE);
		layout.add("group admin's public key", TransformationType.PUBLIC_KEY);
		layout.add("group ID", TransformationType.INT);
		layout.add("group member to kick", TransformationType.ADDRESS);
		layout.add("kick reason length", TransformationType.INT);
		layout.add("kick reason", TransformationType.STRING);
		layout.add("fee", TransformationType.AMOUNT);
		layout.add("signature", TransformationType.SIGNATURE);
	}

	public static TransactionData fromByteBuffer(ByteBuffer byteBuffer) throws TransformationException {
		long timestamp = byteBuffer.getLong();

		int txGroupId = byteBuffer.getInt();

		byte[] reference = new byte[REFERENCE_LENGTH];
		byteBuffer.get(reference);

		byte[] adminPublicKey = Serialization.deserializePublicKey(byteBuffer);

		int groupId = byteBuffer.getInt();

		String member = Serialization.deserializeAddress(byteBuffer);

		String reason = Serialization.deserializeSizedString(byteBuffer, Group.MAX_REASON_SIZE);

		long fee = byteBuffer.getLong();

		byte[] signature = new byte[SIGNATURE_LENGTH];
		byteBuffer.get(signature);

		BaseTransactionData baseTransactionData = new BaseTransactionData(timestamp, txGroupId, reference, adminPublicKey, fee, signature);

		return new GroupKickTransactionData(baseTransactionData, groupId, member, reason);
	}

	public static int getDataLength(TransactionData transactionData) throws TransformationException {
		GroupKickTransactionData groupKickTransactionData = (GroupKickTransactionData) transactionData;

		return getBaseLength(transactionData) + EXTRAS_LENGTH + Utf8.encodedLength(groupKickTransactionData.getReason());
	}

	public static byte[] toBytes(TransactionData transactionData) throws TransformationException {
		try {
			GroupKickTransactionData groupKickTransactionData = (GroupKickTransactionData) transactionData;

			ByteArrayOutputStream bytes = new ByteArrayOutputStream();

			transformCommonBytes(transactionData, bytes);

			bytes.write(Ints.toByteArray(groupKickTransactionData.getGroupId()));

			Serialization.serializeAddress(bytes, groupKickTransactionData.getMember());

			Serialization.serializeSizedString(bytes, groupKickTransactionData.getReason());

			bytes.write(Longs.toByteArray(groupKickTransactionData.getFee()));

			if (groupKickTransactionData.getSignature() != null)
				bytes.write(groupKickTransactionData.getSignature());

			return bytes.toByteArray();
		} catch (IOException | ClassCastException e) {
			throw new TransformationException(e);
		}
	}

}
