package org.aquila.repository.hsqldb.transaction;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.aquila.data.transaction.BaseTransactionData;
import org.aquila.data.transaction.RegisterNameTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.hsqldb.HSQLDBRepository;
import org.aquila.repository.hsqldb.HSQLDBSaver;

public class HSQLDBRegisterNameTransactionRepository extends HSQLDBTransactionRepository {

	public HSQLDBRegisterNameTransactionRepository(HSQLDBRepository repository) {
		this.repository = repository;
	}

	TransactionData fromBase(BaseTransactionData baseTransactionData) throws DataException {
		String sql = "SELECT name, reduced_name, data FROM RegisterNameTransactions WHERE signature = ?";

		try (ResultSet resultSet = this.repository.checkedExecute(sql, baseTransactionData.getSignature())) {
			if (resultSet == null)
				return null;

			String name = resultSet.getString(1);
			String reducedName = resultSet.getString(2);
			String data = resultSet.getString(3);

			return new RegisterNameTransactionData(baseTransactionData, name, data, reducedName);
		} catch (SQLException e) {
			throw new DataException("Unable to fetch register name transaction from repository", e);
		}
	}

	@Override
	public void save(TransactionData transactionData) throws DataException {
		RegisterNameTransactionData registerNameTransactionData = (RegisterNameTransactionData) transactionData;

		HSQLDBSaver saveHelper = new HSQLDBSaver("RegisterNameTransactions");

		saveHelper.bind("signature", registerNameTransactionData.getSignature()).bind("registrant", registerNameTransactionData.getRegistrantPublicKey())
				.bind("name", registerNameTransactionData.getName()).bind("data", registerNameTransactionData.getData())
				.bind("reduced_name", registerNameTransactionData.getReducedName());

		try {
			saveHelper.execute(this.repository);
		} catch (SQLException e) {
			throw new DataException("Unable to save register name transaction into repository", e);
		}
	}

}
