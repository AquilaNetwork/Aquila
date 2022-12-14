package org.aquila.repository.hsqldb.transaction;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.aquila.data.transaction.BaseTransactionData;
import org.aquila.data.transaction.CancelAssetOrderTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.repository.DataException;
import org.aquila.repository.hsqldb.HSQLDBRepository;
import org.aquila.repository.hsqldb.HSQLDBSaver;

public class HSQLDBCancelAssetOrderTransactionRepository extends HSQLDBTransactionRepository {

	public HSQLDBCancelAssetOrderTransactionRepository(HSQLDBRepository repository) {
		this.repository = repository;
	}

	TransactionData fromBase(BaseTransactionData baseTransactionData) throws DataException {
		String sql = "SELECT asset_order_id FROM CancelAssetOrderTransactions WHERE signature = ?";

		try (ResultSet resultSet = this.repository.checkedExecute(sql, baseTransactionData.getSignature())) {
			if (resultSet == null)
				return null;

			byte[] assetOrderId = resultSet.getBytes(1);

			return new CancelAssetOrderTransactionData(baseTransactionData, assetOrderId);
		} catch (SQLException e) {
			throw new DataException("Unable to fetch cancel order transaction from repository", e);
		}
	}

	@Override
	public void save(TransactionData transactionData) throws DataException {
		CancelAssetOrderTransactionData cancelOrderTransactionData = (CancelAssetOrderTransactionData) transactionData;

		HSQLDBSaver saveHelper = new HSQLDBSaver("CancelAssetOrderTransactions");

		saveHelper.bind("signature", cancelOrderTransactionData.getSignature()).bind("creator", cancelOrderTransactionData.getCreatorPublicKey())
				.bind("asset_order_id", cancelOrderTransactionData.getOrderId());

		try {
			saveHelper.execute(this.repository);
		} catch (SQLException e) {
			throw new DataException("Unable to save cancel order transaction into repository", e);
		}
	}

}
