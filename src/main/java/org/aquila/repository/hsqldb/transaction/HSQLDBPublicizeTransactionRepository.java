package org.aquila.repository.hsqldb.transaction;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.aquila.data.transaction.BaseTransactionData;
import org.aquila.data.transaction.PublicizeTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.hsqldb.HSQLDBRepository;
import org.aquila.repository.hsqldb.HSQLDBSaver;

public class HSQLDBPublicizeTransactionRepository extends HSQLDBTransactionRepository {

	public HSQLDBPublicizeTransactionRepository(HSQLDBRepository repository) {
		this.repository = repository;
	}

	TransactionData fromBase(BaseTransactionData baseTransactionData) throws DataException {
		String sql = "SELECT nonce FROM PublicizeTransactions WHERE signature = ?";

		try (ResultSet resultSet = this.repository.checkedExecute(sql, baseTransactionData.getSignature())) {
			if (resultSet == null)
				return null;

			int nonce = resultSet.getInt(1);

			return new PublicizeTransactionData(baseTransactionData, nonce);
		} catch (SQLException e) {
			throw new DataException("Unable to fetch publicize transaction from repository", e);
		}
	}

	@Override
	public void save(TransactionData transactionData) throws DataException {
		PublicizeTransactionData publicizeTransactionData = (PublicizeTransactionData) transactionData;

		HSQLDBSaver saveHelper = new HSQLDBSaver("PublicizeTransactions");

		saveHelper.bind("signature", publicizeTransactionData.getSignature())
				.bind("nonce", publicizeTransactionData.getNonce());

		try {
			saveHelper.execute(this.repository);
		} catch (SQLException e) {
			throw new DataException("Unable to save publicize transaction into repository", e);
		}
	}

}
