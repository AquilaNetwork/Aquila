package org.aquila.repository.hsqldb.transaction;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.aquila.data.transaction.BaseTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.data.transaction.UpdateNameTransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.hsqldb.HSQLDBRepository;
import org.aquila.repository.hsqldb.HSQLDBSaver;

public class HSQLDBUpdateNameTransactionRepository extends HSQLDBTransactionRepository {

	public HSQLDBUpdateNameTransactionRepository(HSQLDBRepository repository) {
		this.repository = repository;
	}

	TransactionData fromBase(BaseTransactionData baseTransactionData) throws DataException {
		String sql = "SELECT name, new_name, new_data, reduced_new_name, name_reference FROM UpdateNameTransactions WHERE signature = ?";

		try (ResultSet resultSet = this.repository.checkedExecute(sql, baseTransactionData.getSignature())) {
			if (resultSet == null)
				return null;

			String name = resultSet.getString(1);
			String newName = resultSet.getString(2);
			String newData = resultSet.getString(3);
			String reducedNewName = resultSet.getString(4);
			byte[] nameReference = resultSet.getBytes(5);

			return new UpdateNameTransactionData(baseTransactionData, name, newName, newData, reducedNewName, nameReference);
		} catch (SQLException e) {
			throw new DataException("Unable to fetch update name transaction from repository", e);
		}
	}

	@Override
	public void save(TransactionData transactionData) throws DataException {
		UpdateNameTransactionData updateNameTransactionData = (UpdateNameTransactionData) transactionData;

		HSQLDBSaver saveHelper = new HSQLDBSaver("UpdateNameTransactions");

		saveHelper.bind("signature", updateNameTransactionData.getSignature()).bind("owner", updateNameTransactionData.getOwnerPublicKey())
				.bind("name", updateNameTransactionData.getName()).bind("new_name", updateNameTransactionData.getNewName())
				.bind("new_data", updateNameTransactionData.getNewData()).bind("reduced_new_name", updateNameTransactionData.getReducedNewName())
				.bind("name_reference", updateNameTransactionData.getNameReference());

		try {
			saveHelper.execute(this.repository);
		} catch (SQLException e) {
			throw new DataException("Unable to save update name transaction into repository", e);
		}
	}

}
