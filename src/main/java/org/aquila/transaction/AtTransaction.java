package org.aquila.transaction;

import java.util.Arrays;
import java.util.List;

import org.aquila.account.Account;
import org.aquila.asset.Asset;
import org.aquila.block.BlockChain;
import org.aquila.crypto.Crypto;
import org.aquila.data.asset.AssetData;
import org.aquila.data.transaction.ATTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.transform.TransformationException;
import org.aquila.transform.Transformer;
import org.aquila.transform.transaction.AtTransactionTransformer;
import org.aquila.utils.Amounts;

import com.google.common.primitives.Bytes;

public class AtTransaction extends Transaction {

	// Properties

	private ATTransactionData atTransactionData;
	private Account atAccount = null;
	private Account recipientAccount = null;

	// Other useful constants
	public static final int MAX_DATA_SIZE = 256;

	// Constructors

	public AtTransaction(Repository repository, TransactionData transactionData) {
		super(repository, transactionData);

		this.atTransactionData = (ATTransactionData) this.transactionData;

		// Check whether we need to generate the ATTransaction's pseudo-signature
		if (this.atTransactionData.getSignature() == null) {
			// Signature is SHA2-256 of serialized transaction data, duplicated to make standard signature size of 64 bytes.
			try {
				byte[] digest = Crypto.digest(AtTransactionTransformer.toBytes(transactionData));
				byte[] signature = Bytes.concat(digest, digest);
				this.atTransactionData.setSignature(signature);
			} catch (TransformationException e) {
				throw new RuntimeException("Couldn't transform AT Transaction into bytes", e);
			}
		}
	}

	// More information

	@Override
	public List<String> getRecipientAddresses() throws DataException {
		return Arrays.asList(this.atTransactionData.getATAddress(), this.atTransactionData.getRecipient());
	}

	// Navigation

	public Account getATAccount() {
		if (this.atAccount == null)
			this.atAccount = new Account(this.repository, this.atTransactionData.getATAddress());

		return this.atAccount;
	}

	public Account getRecipient() {
		if (this.recipientAccount == null)
			this.recipientAccount = new Account(this.repository, this.atTransactionData.getRecipient());

		return this.recipientAccount;
	}

	// Processing

	@Override
	public boolean hasValidReference() throws DataException {
		// Disable reference checking after feature trigger timestamp
		if (this.atTransactionData.getTimestamp() >= BlockChain.getInstance().getDisableReferenceTimestamp()) {
			// Allow any value as long as it is the correct length
			return this.atTransactionData.getReference() != null &&
					this.atTransactionData.getReference().length == Transformer.SIGNATURE_LENGTH;
		}

		// Check reference is correct, using AT account, not transaction creator which is null account
		Account atAccount = getATAccount();
		return Arrays.equals(atAccount.getLastReference(), atTransactionData.getReference());
	}

	@Override
	public void preProcess() throws DataException {
		// Nothing to do
	}

	@Override
	public ValidationResult isValid() throws DataException {
		// Check recipient address is valid
		if (!Crypto.isValidAddress(this.atTransactionData.getRecipient()))
			return ValidationResult.INVALID_ADDRESS;

		Long amount = this.atTransactionData.getAmount();
		Long assetId = this.atTransactionData.getAssetId();
		byte[] message = this.atTransactionData.getMessage();

		boolean hasPayment = amount != null && assetId != null;
		boolean hasMessage = message != null; // empty message OK

		// We can only have either message or payment, not both, nor neither
		if ((hasMessage && hasPayment) || (!hasMessage && !hasPayment))
			return ValidationResult.INVALID_AT_TRANSACTION;

		if (hasMessage && message.length > MAX_DATA_SIZE)
			return ValidationResult.INVALID_DATA_LENGTH;

		// If we have no payment then we're done
		if (!hasPayment)
			return ValidationResult.OK;

		// Check amount is zero or positive
		if (amount < 0)
			return ValidationResult.NEGATIVE_AMOUNT;

		AssetData assetData = this.repository.getAssetRepository().fromAssetId(assetId);
		// Check asset even exists
		if (assetData == null)
			return ValidationResult.ASSET_DOES_NOT_EXIST;

		// Check asset amount is integer if asset is not divisible
		if (!assetData.isDivisible() && amount % Amounts.MULTIPLIER != 0)
			return ValidationResult.INVALID_AMOUNT;

		Account sender = getATAccount();
		// Check sender has enough of asset
		if (sender.getConfirmedBalance(assetId) < amount)
			return ValidationResult.NO_BALANCE;

		return ValidationResult.OK;
	}

	@Override
	public void process() throws DataException {
		Long amount = this.atTransactionData.getAmount();

		if (amount != null) {
			Account sender = getATAccount();
			Account recipient = getRecipient();

			long assetId = this.atTransactionData.getAssetId();

			// Update sender's balance due to amount
			sender.modifyAssetBalance(assetId, - amount);

			// Update recipient's balance
			recipient.modifyAssetBalance(assetId, amount);
		}
	}

	@Override
	public void processReferencesAndFees() throws DataException {
		getATAccount().setLastReference(this.atTransactionData.getSignature());

		if (this.atTransactionData.getAmount() != null) {
			Account recipient = getRecipient();
			long assetId = this.atTransactionData.getAssetId();

			// For UNCIA amounts only: if recipient has no reference yet, then this is their starting reference
			if (assetId == Asset.UNCIA && recipient.getLastReference() == null)
				// In Qora1 last reference was set to 64-bytes of zero
				// In Aquila we use AT-Transaction's signature, which makes more sense
				recipient.setLastReference(this.atTransactionData.getSignature());
		}
	}

	@Override
	public void orphan() throws DataException {
		Long amount = this.atTransactionData.getAmount();

		if (amount != null) {
			Account sender = getATAccount();
			Account recipient = getRecipient();

			long assetId = this.atTransactionData.getAssetId();

			// Update sender's balance due to amount
			sender.modifyAssetBalance(assetId, amount);

			// Update recipient's balance
			recipient.modifyAssetBalance(assetId, - amount);
		}

		// As AT_TRANSACTIONs are really part of a block, the caller (Block) will probably delete this transaction after orphaning
	}

	@Override
	public void orphanReferencesAndFees() throws DataException {
		getATAccount().setLastReference(this.atTransactionData.getReference());

		if (this.atTransactionData.getAmount() != null) {
			Account recipient = getRecipient();

			long assetId = this.atTransactionData.getAssetId();

			/*
			 * For UNCIA amounts only: If recipient's last reference is this transaction's signature, then they can't have made any transactions of their own
			 * (which would have changed their last reference) thus this is their first reference so remove it.
			 */
			if (assetId == Asset.UNCIA && Arrays.equals(recipient.getLastReference(), this.atTransactionData.getSignature()))
				recipient.setLastReference(null);
		}
	}

}
