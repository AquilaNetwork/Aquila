package org.aquila.transform.transaction;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.aquila.data.transaction.BaseTransactionData;
import org.aquila.data.transaction.RemoveGroupAdminTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.transaction.Transaction.TransactionType;
import org.aquila.transform.TransformationException;
import org.aquila.utils.Serialization;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

public class RemoveGroupAdminTransactionTransformer extends TransactionTransformer {

	// Property lengths
	private static final int GROUPID_LENGTH = INT_LENGTH;
	private static final int MEMBER_LENGTH = ADDRESS_LENGTH;

	private static final int EXTRAS_LENGTH = GROUPID_LENGTH + MEMBER_LENGTH;

	protected static final TransactionLayout layout;

	static {
		layout = new TransactionLayout();
		layout.add("txType: " + TransactionType.REMOVE_GROUP_ADMIN.valueString, TransformationType.INT);
		layout.add("timestamp", TransformationType.TIMESTAMP);
		layout.add("transaction's groupID", TransformationType.INT);
		layout.add("reference", TransformationType.SIGNATURE);
		layout.add("group owner's public key", TransformationType.PUBLIC_KEY);
		layout.add("group ID", TransformationType.INT);
		layout.add("admin to demote", TransformationType.ADDRESS);
		layout.add("fee", TransformationType.AMOUNT);
		layout.add("signature", TransformationType.SIGNATURE);
	}

	public static TransactionData fromByteBuffer(ByteBuffer byteBuffer) throws TransformationException {
		long timestamp = byteBuffer.getLong();

		int txGroupId = byteBuffer.getInt();

		byte[] reference = new byte[REFERENCE_LENGTH];
		byteBuffer.get(reference);

		byte[] ownerPublicKey = Serialization.deserializePublicKey(byteBuffer);

		int groupId = byteBuffer.getInt();

		String admin = Serialization.deserializeAddress(byteBuffer);

		long fee = byteBuffer.getLong();

		byte[] signature = new byte[SIGNATURE_LENGTH];
		byteBuffer.get(signature);

		BaseTransactionData baseTransactionData = new BaseTransactionData(timestamp, txGroupId, reference, ownerPublicKey, fee, signature);

		return new RemoveGroupAdminTransactionData(baseTransactionData, groupId, admin);
	}

	public static int getDataLength(TransactionData transactionData) throws TransformationException {
		return getBaseLength(transactionData) + EXTRAS_LENGTH;
	}

	public static byte[] toBytes(TransactionData transactionData) throws TransformationException {
		try {
			RemoveGroupAdminTransactionData removeGroupAdminTransactionData = (RemoveGroupAdminTransactionData) transactionData;

			ByteArrayOutputStream bytes = new ByteArrayOutputStream();

			transformCommonBytes(transactionData, bytes);

			bytes.write(Ints.toByteArray(removeGroupAdminTransactionData.getGroupId()));

			Serialization.serializeAddress(bytes, removeGroupAdminTransactionData.getAdmin());

			bytes.write(Longs.toByteArray(removeGroupAdminTransactionData.getFee()));

			if (removeGroupAdminTransactionData.getSignature() != null)
				bytes.write(removeGroupAdminTransactionData.getSignature());

			return bytes.toByteArray();
		} catch (IOException | ClassCastException e) {
			throw new TransformationException(e);
		}
	}

}
