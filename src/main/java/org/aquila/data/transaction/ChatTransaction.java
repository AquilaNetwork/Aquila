package org.aquila.transaction;

import java.util.Collections;
import java.util.List;

import org.aquila.account.Account;
import org.aquila.account.PublicKeyAccount;
import org.aquila.asset.Asset;
import org.aquila.crypto.Crypto;
import org.aquila.crypto.MemoryPoW;
import org.aquila.data.naming.NameData;
import org.aquila.data.transaction.ChatTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.group.Group;
import org.aquila.list.ResourceListManager;
import org.aquila.repository.DataException;
import org.aquila.repository.GroupRepository;
import org.aquila.repository.Repository;
import org.aquila.transform.TransformationException;
import org.aquila.transform.transaction.ChatTransactionTransformer;
import org.aquila.transform.transaction.TransactionTransformer;

public class ChatTransaction extends Transaction {

	// Properties
	private ChatTransactionData chatTransactionData;

	// Other useful constants
	public static final int MAX_DATA_SIZE = 256;
	public static final int POW_BUFFER_SIZE = 8 * 1024 * 1024; // bytes
	public static final int POW_DIFFICULTY_WITH_UNCIA = 8; // leading zero bits
	public static final int POW_DIFFICULTY_NO_UNCIA = 12; // leading zero bits

	// Constructors

	public ChatTransaction(Repository repository, TransactionData transactionData) {
		super(repository, transactionData);

		this.chatTransactionData = (ChatTransactionData) this.transactionData;
	}

	// More information

	@Override
	public List<String> getRecipientAddresses() throws DataException {
		String recipientAddress = this.chatTransactionData.getRecipient();
		if (recipientAddress == null)
			return Collections.emptyList();

		return Collections.singletonList(recipientAddress);
	}

	// Navigation

	public Account getSender() {
		return this.getCreator();
	}

	public Account getRecipient() {
		String recipientAddress = chatTransactionData.getRecipient();
		if (recipientAddress == null)
			return null;

		return new Account(this.repository, recipientAddress);
	}

	// Processing

	public void computeNonce() throws DataException {
		byte[] transactionBytes;

		try {
			transactionBytes = TransactionTransformer.toBytesForSigning(this.transactionData);
		} catch (TransformationException e) {
			throw new RuntimeException("Unable to transform transaction to byte array for verification", e);
		}

		// Clear nonce from transactionBytes
		ChatTransactionTransformer.clearNonce(transactionBytes);

		int difficulty = this.getSender().getConfirmedBalance(Asset.UNCIA) > 0 ? POW_DIFFICULTY_WITH_UNCIA : POW_DIFFICULTY_NO_UNCIA;

		// Calculate nonce
		this.chatTransactionData.setNonce(MemoryPoW.compute2(transactionBytes, POW_BUFFER_SIZE, difficulty));
	}

	/**
	 * Returns whether CHAT transaction has valid txGroupId.
	 * <p>
	 * For CHAT transactions, a non-NO_GROUP txGroupId represents
	 * sending to a group, rather than to everyone.
	 * <p>
	 * If txGroupId is not NO_GROUP, then the sender needs to be
	 * a member of that group. The recipient, if supplied, also
	 * needs to be a member of that group.
	 */
	@Override
	protected boolean isValidTxGroupId() throws DataException {
		int txGroupId = this.transactionData.getTxGroupId();

		// txGroupId represents recipient group, unless NO_GROUP

		// Anyone can use NO_GROUP
		if (txGroupId == Group.NO_GROUP)
			return true;

		// Group even exist?
		if (!this.repository.getGroupRepository().groupExists(txGroupId))
			return false;

		GroupRepository groupRepository = this.repository.getGroupRepository();

		// Is transaction's creator is group member?
		PublicKeyAccount creator = this.getCreator();
		if (!groupRepository.memberExists(txGroupId, creator.getAddress()))
			return false;

		// If recipient address present, check they belong to group too.
		String recipient = this.chatTransactionData.getRecipient();
		if (recipient != null && !groupRepository.memberExists(txGroupId, recipient))
			return false;

		return true;
	}

	@Override
	public ValidationResult isFeeValid() throws DataException {
		if (this.transactionData.getFee() < 0)
			return ValidationResult.NEGATIVE_FEE;

		return ValidationResult.OK;
	}

	@Override
	public boolean hasValidReference() throws DataException {
		return true;
	}

	@Override
	public void preProcess() throws DataException {
		// Nothing to do
	}

	@Override
	public ValidationResult isValid() throws DataException {
		// Nonce checking is done via isSignatureValid() as that method is only called once per import

		// Check for blocked author by address
		ResourceListManager listManager = ResourceListManager.getInstance();
		if (listManager.listContains("blockedAddresses", this.chatTransactionData.getSender(), true)) {
			return ValidationResult.ADDRESS_BLOCKED;
		}

		// Check for blocked author by registered name
		List<NameData> names = this.repository.getNameRepository().getNamesByOwner(this.chatTransactionData.getSender());
		if (names != null && names.size() > 0) {
			for (NameData nameData : names) {
				if (nameData != null && nameData.getName() != null) {
					if (listManager.listContains("blockedNames", nameData.getName(), false)) {
						return ValidationResult.NAME_BLOCKED;
					}
				}
			}
		}

		// If we exist in the repository then we've been imported as unconfirmed,
		// but we don't want to make it into a block, so return fake non-OK result.
		if (this.repository.getTransactionRepository().exists(this.chatTransactionData.getSignature()))
			return ValidationResult.INVALID_BUT_OK;

		// If we have a recipient, check it is a valid address
		String recipientAddress = chatTransactionData.getRecipient();
		if (recipientAddress != null && !Crypto.isValidAddress(recipientAddress))
			return ValidationResult.INVALID_ADDRESS;

		// Check data length
		if (chatTransactionData.getData().length < 1 || chatTransactionData.getData().length > MAX_DATA_SIZE)
			return ValidationResult.INVALID_DATA_LENGTH;

		return ValidationResult.OK;
	}

	@Override
	public boolean isSignatureValid() {
		byte[] signature = this.transactionData.getSignature();
		if (signature == null)
			return false;

		byte[] transactionBytes;

		try {
			transactionBytes = ChatTransactionTransformer.toBytesForSigning(this.transactionData);
		} catch (TransformationException e) {
			throw new RuntimeException("Unable to transform transaction to byte array for verification", e);
		}

		if (!Crypto.verify(this.transactionData.getCreatorPublicKey(), signature, transactionBytes))
			return false;

		int nonce = this.chatTransactionData.getNonce();

		// Clear nonce from transactionBytes
		ChatTransactionTransformer.clearNonce(transactionBytes);

		int difficulty;
		try {
			difficulty = this.getSender().getConfirmedBalance(Asset.UNCIA) > 0 ? POW_DIFFICULTY_WITH_UNCIA : POW_DIFFICULTY_NO_UNCIA;
		} catch (DataException e) {
			return false;
		}

		// Check nonce
		return MemoryPoW.verify2(transactionBytes, POW_BUFFER_SIZE, difficulty, nonce);
	}

	/**
	 * Ensure there's at least a skeleton account so people
	 * can retrieve sender's public key using address, even if all their messages
	 * expire.
	 */
	@Override
	protected void onImportAsUnconfirmed() throws DataException {
		this.getCreator().ensureAccount();
	}

	@Override
	public void process() throws DataException {
		throw new DataException("CHAT transactions should never be processed");
	}

	@Override
	public void orphan() throws DataException {
		throw new DataException("CHAT transactions should never be orphaned");
	}

}
