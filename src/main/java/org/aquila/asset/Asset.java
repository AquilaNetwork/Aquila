package org.aquila.asset;

import org.aquila.crypto.Crypto;
import org.aquila.data.asset.AssetData;
import org.aquila.data.transaction.IssueAssetTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.data.transaction.UpdateAssetTransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.utils.Amounts;

public class Asset {

	/**
	 * UNCIA coins are just another asset but with fixed assetId of zero.
	 */
	public static final long UNCIA = 0L;

	/** Hard-coded asset representing legacy QORA held in old QORA1 blockchain. */
	public static final long LEGACY_QORA = 1L;
	/** Hard-coded asset representing UNCIA gained from holding legacy QORA. */
	public static final long UNCIA_FROM_QORA = 2L;

	// Other useful constants

	public static final int MIN_NAME_SIZE = 3;
	public static final int MAX_NAME_SIZE = 40;
	public static final int MAX_DESCRIPTION_SIZE = 4000;
	public static final int MAX_DATA_SIZE = 400000;

	public static final long MAX_QUANTITY = 10_000_000_000L * Amounts.MULTIPLIER; // but also to 8 decimal places

	// Properties
	private Repository repository;
	private AssetData assetData;

	// Constructors

	public Asset(Repository repository, AssetData assetData) {
		this.repository = repository;
		this.assetData = assetData;
	}

	public Asset(Repository repository, IssueAssetTransactionData issueAssetTransactionData) {
		this.repository = repository;

		String ownerAddress = Crypto.toAddress(issueAssetTransactionData.getCreatorPublicKey());

		// NOTE: transaction's reference is used to look up newly assigned assetID on creation!
		this.assetData = new AssetData(ownerAddress, issueAssetTransactionData.getAssetName(),
				issueAssetTransactionData.getDescription(), issueAssetTransactionData.getQuantity(),
				issueAssetTransactionData.isDivisible(), issueAssetTransactionData.getData(),
				issueAssetTransactionData.isUnspendable(), issueAssetTransactionData.getTxGroupId(),
				issueAssetTransactionData.getSignature(), issueAssetTransactionData.getReducedAssetName());
	}

	public Asset(Repository repository, long assetId) throws DataException {
		this.repository = repository;
		this.assetData = this.repository.getAssetRepository().fromAssetId(assetId);
	}

	// Getters/setters

	public AssetData getAssetData() {
		return this.assetData;
	}

	// Processing

	public void issue() throws DataException {
		this.repository.getAssetRepository().save(this.assetData);
	}

	public void deissue() throws DataException {
		this.repository.getAssetRepository().delete(this.assetData.getAssetId());
	}

	public void update(UpdateAssetTransactionData updateAssetTransactionData) throws DataException {
		// Update reference in transaction data
		updateAssetTransactionData.setOrphanReference(this.assetData.getReference());

		// New reference is this transaction's signature
		this.assetData.setReference(updateAssetTransactionData.getSignature());

		// Update asset's owner, description and data
		this.assetData.setOwner(updateAssetTransactionData.getNewOwner());

		if (!updateAssetTransactionData.getNewDescription().isEmpty())
			this.assetData.setDescription(updateAssetTransactionData.getNewDescription());

		if (!updateAssetTransactionData.getNewData().isEmpty())
			this.assetData.setData(updateAssetTransactionData.getNewData());

		// Save updated asset
		this.repository.getAssetRepository().save(this.assetData);
	}

	public void revert(UpdateAssetTransactionData updateAssetTransactionData) throws DataException {
		// Previous asset reference is taken from this transaction's cached copy
		this.assetData.setReference(updateAssetTransactionData.getOrphanReference());

		/*
		 * It's possible the previous transaction might be an UPDATE_ASSET that didn't change
		 * description/data fields and so we have to keep going back until we find an actual value,
		 * even to the original ISSUE_ASSET transaction if necessary.
		 * 
		 * So we need to keep track of whether we still need
		 * a previous description and/or data so we can stop looking.
		 */
		boolean needDescription = updateAssetTransactionData.getNewDescription() != null;
		boolean needData = updateAssetTransactionData.getNewData() != null;

		byte[] previousTransactionSignature = this.assetData.getReference();

		// There's always at least one round to potentially revert owner
		do {
			// Previous owner, description and/or data taken from referenced transaction
			TransactionData previousTransactionData = this.repository.getTransactionRepository()
					.fromSignature(previousTransactionSignature);

			if (previousTransactionData == null)
				throw new IllegalStateException("Missing referenced transaction when orphaning UPDATE_ASSET");

			switch (previousTransactionData.getType()) {
				case ISSUE_ASSET: {
					IssueAssetTransactionData previousIssueAssetTransactionData = (IssueAssetTransactionData) previousTransactionData;

					String ownerAddress = Crypto.toAddress(previousIssueAssetTransactionData.getCreatorPublicKey());
					this.assetData.setOwner(ownerAddress);

					if (needDescription) {
						this.assetData.setDescription(previousIssueAssetTransactionData.getDescription());
						needDescription = false;
					}

					if (needData) {
						this.assetData.setData(previousIssueAssetTransactionData.getData());
						needData = false;
					}
					break;
				}

				case UPDATE_ASSET: {
					UpdateAssetTransactionData previousUpdateAssetTransactionData = (UpdateAssetTransactionData) previousTransactionData;

					this.assetData.setOwner(previousUpdateAssetTransactionData.getNewOwner());

					if (needDescription && !previousUpdateAssetTransactionData.getNewDescription().isEmpty()) {
						this.assetData.setDescription(previousUpdateAssetTransactionData.getNewDescription());
						needDescription = false;
					}

					if (needData && !previousUpdateAssetTransactionData.getNewData().isEmpty()) {
						this.assetData.setData(previousUpdateAssetTransactionData.getNewData());
						needData = false;
					}

					// Get signature for previous transaction in chain, just in case we need it
					if (needDescription || needData)
						previousTransactionSignature = previousUpdateAssetTransactionData.getOrphanReference();

					break;
				}

				default:
					throw new IllegalStateException("Invalid referenced transaction when orphaning UPDATE_ASSET");
			}

		} while (needDescription || needData);

		// Save reverted asset
		this.repository.getAssetRepository().save(this.assetData);

		// Remove reference to previous asset-changing transaction
		updateAssetTransactionData.setOrphanReference(null);
	}

}
