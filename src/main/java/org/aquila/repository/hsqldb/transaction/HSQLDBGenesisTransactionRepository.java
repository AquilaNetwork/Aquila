package org.aquila.repository.hsqldb.transaction;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.aquila.data.transaction.BaseTransactionData;
import org.aquila.data.transaction.GenesisTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.hsqldb.HSQLDBRepository;
import org.aquila.repository.hsqldb.HSQLDBSaver;

public class HSQLDBGenesisTransactionRepository extends HSQLDBTransactionRepository {

	public HSQLDBGenesisTransactionRepository(HSQLDBRepository repository) {
		this.repository = repository;
	}

	TransactionData fromBase(BaseTransactionData baseTransactionData) throws DataException {
		String sql = "SELECT recipient, amount, asset_id FROM GenesisTransactions WHERE signature = ?";

		try (ResultSet resultSet = this.repository.checkedExecute(sql, baseTransactionData.getSignature())) {
			if (resultSet == null)
				return null;

			String recipient = resultSet.getString(1);
			long amount = resultSet.getLong(2);
			long assetId = resultSet.getLong(3);

			return new GenesisTransactionData(baseTransactionData, recipient, amount, assetId);
		} catch (SQLException e) {
			throw new DataException("Unable to fetch genesis transaction from repository", e);
		}
	}

	@Override
	public void save(TransactionData transactionData) throws DataException {
		GenesisTransactionData genesisTransactionData = (GenesisTransactionData) transactionData;

		HSQLDBSaver saveHelper = new HSQLDBSaver("GenesisTransactions");
		saveHelper.bind("signature", genesisTransactionData.getSignature()).bind("recipient", genesisTransactionData.getRecipient())
				.bind("amount", genesisTransactionData.getAmount()).bind("asset_id", genesisTransactionData.getAssetId());

		try {
			saveHelper.execute(this.repository);
		} catch (SQLException e) {
			throw new DataException("Unable to save genesis transaction into repository", e);
		}
	}

}
