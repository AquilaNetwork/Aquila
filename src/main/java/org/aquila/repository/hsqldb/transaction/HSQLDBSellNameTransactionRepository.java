package org.aquila.repository.hsqldb.transaction;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.aquila.data.transaction.BaseTransactionData;
import org.aquila.data.transaction.SellNameTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.hsqldb.HSQLDBRepository;
import org.aquila.repository.hsqldb.HSQLDBSaver;

public class HSQLDBSellNameTransactionRepository extends HSQLDBTransactionRepository {

	public HSQLDBSellNameTransactionRepository(HSQLDBRepository repository) {
		this.repository = repository;
	}

	TransactionData fromBase(BaseTransactionData baseTransactionData) throws DataException {
		String sql = "SELECT name, amount FROM SellNameTransactions WHERE signature = ?";

		try (ResultSet resultSet = this.repository.checkedExecute(sql, baseTransactionData.getSignature())) {
			if (resultSet == null)
				return null;

			String name = resultSet.getString(1);
			long amount = resultSet.getLong(2);

			return new SellNameTransactionData(baseTransactionData, name, amount);
		} catch (SQLException e) {
			throw new DataException("Unable to fetch sell name transaction from repository", e);
		}
	}

	@Override
	public void save(TransactionData transactionData) throws DataException {
		SellNameTransactionData sellNameTransactionData = (SellNameTransactionData) transactionData;

		HSQLDBSaver saveHelper = new HSQLDBSaver("SellNameTransactions");

		saveHelper.bind("signature", sellNameTransactionData.getSignature()).bind("owner", sellNameTransactionData.getOwnerPublicKey())
				.bind("name", sellNameTransactionData.getName()).bind("amount", sellNameTransactionData.getAmount());

		try {
			saveHelper.execute(this.repository);
		} catch (SQLException e) {
			throw new DataException("Unable to save sell name transaction into repository", e);
		}
	}

}
