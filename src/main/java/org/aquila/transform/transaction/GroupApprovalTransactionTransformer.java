package org.aquila.transform.transaction;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.aquila.data.transaction.BaseTransactionData;
import org.aquila.data.transaction.GroupApprovalTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.transaction.Transaction.TransactionType;
import org.aquila.transform.TransformationException;
import org.aquila.utils.Serialization;

import com.google.common.primitives.Longs;

public class GroupApprovalTransactionTransformer extends TransactionTransformer {

	// Property lengths
	private static final int PENDING_SIGNATURE_LENGTH = SIGNATURE_LENGTH;
	private static final int APPROVAL_LENGTH = BOOLEAN_LENGTH;

	private static final int EXTRAS_LENGTH = PENDING_SIGNATURE_LENGTH + APPROVAL_LENGTH;

	protected static final TransactionLayout layout;

	static {
		layout = new TransactionLayout();
		layout.add("txType: " + TransactionType.GROUP_APPROVAL.valueString, TransformationType.INT);
		layout.add("timestamp", TransformationType.TIMESTAMP);
		layout.add("transaction's groupID", TransformationType.INT);
		layout.add("reference", TransformationType.SIGNATURE);
		layout.add("group admin's public key", TransformationType.PUBLIC_KEY);
		layout.add("pending transaction's signature", TransformationType.SIGNATURE);
		layout.add("approval decision", TransformationType.BOOLEAN);
		layout.add("fee", TransformationType.AMOUNT);
		layout.add("signature", TransformationType.SIGNATURE);
	}

	public static TransactionData fromByteBuffer(ByteBuffer byteBuffer) throws TransformationException {
		long timestamp = byteBuffer.getLong();

		int txGroupId = byteBuffer.getInt();

		byte[] reference = new byte[REFERENCE_LENGTH];
		byteBuffer.get(reference);

		byte[] adminPublicKey = Serialization.deserializePublicKey(byteBuffer);

		byte[] pendingSignature = new byte[SIGNATURE_LENGTH];
		byteBuffer.get(pendingSignature);

		boolean approval = byteBuffer.get() != 0;

		long fee = byteBuffer.getLong();

		byte[] signature = new byte[SIGNATURE_LENGTH];
		byteBuffer.get(signature);

		BaseTransactionData baseTransactionData = new BaseTransactionData(timestamp, txGroupId, reference, adminPublicKey, fee, signature);

		return new GroupApprovalTransactionData(baseTransactionData, pendingSignature, approval);
	}

	public static int getDataLength(TransactionData transactionData) throws TransformationException {
		return getBaseLength(transactionData) + EXTRAS_LENGTH;
	}

	public static byte[] toBytes(TransactionData transactionData) throws TransformationException {
		try {
			GroupApprovalTransactionData groupApprovalTransactionData = (GroupApprovalTransactionData) transactionData;

			ByteArrayOutputStream bytes = new ByteArrayOutputStream();

			transformCommonBytes(transactionData, bytes);

			bytes.write(groupApprovalTransactionData.getPendingSignature());

			bytes.write((byte) (groupApprovalTransactionData.getApproval() ? 1 : 0));

			bytes.write(Longs.toByteArray(groupApprovalTransactionData.getFee()));

			if (groupApprovalTransactionData.getSignature() != null)
				bytes.write(groupApprovalTransactionData.getSignature());

			return bytes.toByteArray();
		} catch (IOException | ClassCastException e) {
			throw new TransformationException(e);
		}
	}

}
