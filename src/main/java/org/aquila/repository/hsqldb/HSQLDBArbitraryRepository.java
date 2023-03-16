package org.aquila.repository.hsqldb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aquila.arbitrary.ArbitraryDataFile;
import org.aquila.arbitrary.misc.Service;
import org.aquila.crypto.Crypto;
import org.aquila.data.arbitrary.ArbitraryResourceInfo;
import org.aquila.data.arbitrary.ArbitraryResourceNameInfo;
import org.aquila.data.network.ArbitraryPeerData;
import org.aquila.data.transaction.ArbitraryTransactionData;
import org.aquila.data.transaction.BaseTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.data.transaction.ArbitraryTransactionData.*;
import org.aquila.repository.ArbitraryRepository;
import org.aquila.repository.DataException;
import org.aquila.transaction.Transaction.ApprovalStatus;
import org.aquila.utils.Base58;
import org.bouncycastle.util.Longs;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HSQLDBArbitraryRepository implements ArbitraryRepository {

	private static final Logger LOGGER = LogManager.getLogger(HSQLDBArbitraryRepository.class);

	private static final int MAX_RAW_DATA_SIZE = 255; // size of VARBINARY

	protected HSQLDBRepository repository;
	
	public HSQLDBArbitraryRepository(HSQLDBRepository repository) {
		this.repository = repository;
	}

	private ArbitraryTransactionData getTransactionData(byte[] signature) throws DataException {
		TransactionData transactionData = this.repository.getTransactionRepository().fromSignature(signature);
		if (transactionData == null)
			return null;

		return (ArbitraryTransactionData) transactionData;
	}

	@Override
	public boolean isDataLocal(byte[] signature) throws DataException {
		ArbitraryTransactionData transactionData = getTransactionData(signature);
		if (transactionData == null) {
			return false;
		}

		// Raw data is always available
		if (transactionData.getDataType() == DataType.RAW_DATA) {
			return true;
		}

		// Load hashes
		byte[] hash = transactionData.getData();
		byte[] metadataHash = transactionData.getMetadataHash();

		// Load data file(s)
		ArbitraryDataFile arbitraryDataFile = ArbitraryDataFile.fromHash(hash, signature);
		arbitraryDataFile.setMetadataHash(metadataHash);

		// Check if we already have the complete data file or all chunks
		if (arbitraryDataFile.allFilesExist()) {
			return true;
		}

		return false;
	}

	@Override
	public byte[] fetchData(byte[] signature) {
		try {
			ArbitraryTransactionData transactionData = getTransactionData(signature);
			if (transactionData == null) {
				return null;
			}

			// Raw data is always available
			if (transactionData.getDataType() == DataType.RAW_DATA) {
				return transactionData.getData();
			}

			// Load hashes
			byte[] digest = transactionData.getData();
			byte[] metadataHash = transactionData.getMetadataHash();

			// Load data file(s)
			ArbitraryDataFile arbitraryDataFile = ArbitraryDataFile.fromHash(digest, signature);
			arbitraryDataFile.setMetadataHash(metadataHash);

			// If we have the complete data file, return it
			if (arbitraryDataFile.exists()) {
				// Ensure the file's size matches the size reported by the transaction (throws a DataException if not)
				arbitraryDataFile.validateFileSize(transactionData.getSize());

				return arbitraryDataFile.getBytes();
			}

			// Alternatively, if we have all the chunks, combine them into a single file
			if (arbitraryDataFile.allChunksExist()) {
				arbitraryDataFile.join();

				// Verify that the combined hash matches the expected hash
				if (!digest.equals(arbitraryDataFile.digest())) {
					LOGGER.info(String.format("Hash mismatch for transaction: %s", Base58.encode(signature)));
					return null;
				}

				// Ensure the file's size matches the size reported by the transaction
				arbitraryDataFile.validateFileSize(transactionData.getSize());

				return arbitraryDataFile.getBytes();
			}

		} catch (DataException e) {
			LOGGER.info("Unable to fetch data for transaction {}: {}", Base58.encode(signature), e.getMessage());
			return null;
		}

		return null;
	}

	@Override
	public void save(ArbitraryTransactionData arbitraryTransactionData) throws DataException {
		// Already hashed? Nothing to do
		if (arbitraryTransactionData.getDataType() == DataType.DATA_HASH) {
			return;
		}

		// Trivial-sized payloads can remain in raw form
		if (arbitraryTransactionData.getDataType() == DataType.RAW_DATA && arbitraryTransactionData.getData().length <= MAX_RAW_DATA_SIZE) {
			return;
		}

		throw new IllegalStateException(String.format("Supplied data is larger than maximum size (%d bytes). Please use ArbitraryDataWriter.", MAX_RAW_DATA_SIZE));
	}

	@Override
	public void delete(ArbitraryTransactionData arbitraryTransactionData) throws DataException {
		// No need to do anything if we still only have raw data, and hence nothing saved in filesystem
		if (arbitraryTransactionData.getDataType() == DataType.RAW_DATA) {
			return;
		}

		// Load hashes
		byte[] hash = arbitraryTransactionData.getData();
		byte[] metadataHash = arbitraryTransactionData.getMetadataHash();

		// Load data file(s)
		byte[] signature = arbitraryTransactionData.getSignature();
		ArbitraryDataFile arbitraryDataFile = ArbitraryDataFile.fromHash(hash, signature);
		arbitraryDataFile.setMetadataHash(metadataHash);

		// Delete file and chunks
		arbitraryDataFile.deleteAll();
	}

	@Override
	public List<ArbitraryTransactionData> getArbitraryTransactions(String name, Service service, String identifier, long since) throws DataException {
		String sql = "SELECT type, reference, signature, creator, created_when, fee, " +
				"tx_group_id, block_height, approval_status, approval_height, " +
				"version, nonce, service, size, is_data_raw, data, metadata_hash, " +
				"name, identifier, update_method, secret, compression FROM ArbitraryTransactions " +
				"JOIN Transactions USING (signature) " +
				"WHERE lower(name) = ? AND service = ?" +
				"AND (identifier = ? OR (identifier IS NULL AND ? IS NULL))" +
				"AND created_when >= ? ORDER BY created_when ASC";
		List<ArbitraryTransactionData> arbitraryTransactionData = new ArrayList<>();

		try (ResultSet resultSet = this.repository.checkedExecute(sql, name.toLowerCase(), service.value, identifier, identifier, since)) {
			if (resultSet == null)
				return null;

			do {
				//TransactionType type = TransactionType.valueOf(resultSet.getInt(1));

				byte[] reference = resultSet.getBytes(2);
				byte[] signature = resultSet.getBytes(3);
				byte[] creatorPublicKey = resultSet.getBytes(4);
				long timestamp = resultSet.getLong(5);

				Long fee = resultSet.getLong(6);
				if (fee == 0 && resultSet.wasNull())
					fee = null;

				int txGroupId = resultSet.getInt(7);

				Integer blockHeight = resultSet.getInt(8);
				if (blockHeight == 0 && resultSet.wasNull())
					blockHeight = null;

				ApprovalStatus approvalStatus = ApprovalStatus.valueOf(resultSet.getInt(9));
				Integer approvalHeight = resultSet.getInt(10);
				if (approvalHeight == 0 && resultSet.wasNull())
					approvalHeight = null;

				BaseTransactionData baseTransactionData = new BaseTransactionData(timestamp, txGroupId, reference, creatorPublicKey, fee, approvalStatus, blockHeight, approvalHeight, signature);

				int version = resultSet.getInt(11);
				int nonce = resultSet.getInt(12);
				Service serviceResult = Service.valueOf(resultSet.getInt(13));
				int size = resultSet.getInt(14);
				boolean isDataRaw = resultSet.getBoolean(15); // NOT NULL, so no null to false
				DataType dataType = isDataRaw ? DataType.RAW_DATA : DataType.DATA_HASH;
				byte[] data = resultSet.getBytes(16);
				byte[] metadataHash = resultSet.getBytes(17);
				String nameResult = resultSet.getString(18);
				String identifierResult = resultSet.getString(19);
				Method method = Method.valueOf(resultSet.getInt(20));
				byte[] secret = resultSet.getBytes(21);
				Compression compression = Compression.valueOf(resultSet.getInt(22));
				// FUTURE: get payments from signature if needed. Avoiding for now to reduce database calls.

				ArbitraryTransactionData transactionData = new ArbitraryTransactionData(baseTransactionData,
						version, serviceResult, nonce, size, nameResult, identifierResult, method, secret,
						compression, data, dataType, metadataHash, null);

				arbitraryTransactionData.add(transactionData);
			} while (resultSet.next());

			return arbitraryTransactionData;
		} catch (SQLException e) {
			throw new DataException("Unable to fetch arbitrary transactions from repository", e);
		}
	}

	@Override
	public ArbitraryTransactionData getLatestTransaction(String name, Service service, Method method, String identifier) throws DataException {
		StringBuilder sql = new StringBuilder(1024);

		sql.append("SELECT type, reference, signature, creator, created_when, fee, " +
				"tx_group_id, block_height, approval_status, approval_height, " +
				"version, nonce, service, size, is_data_raw, data, metadata_hash, " +
				"name, identifier, update_method, secret, compression FROM ArbitraryTransactions " +
				"JOIN Transactions USING (signature) " +
				"WHERE lower(name) = ? AND service = ? " +
				"AND (identifier = ? OR (identifier IS NULL AND ? IS NULL))");

		if (method != null) {
			sql.append(" AND update_method = ");
			sql.append(method.value);
		}

		sql.append("ORDER BY created_when DESC LIMIT 1");

		try (ResultSet resultSet = this.repository.checkedExecute(sql.toString(), name.toLowerCase(), service.value, identifier, identifier)) {
			if (resultSet == null)
				return null;

			//TransactionType type = TransactionType.valueOf(resultSet.getInt(1));

			byte[] reference = resultSet.getBytes(2);
			byte[] signature = resultSet.getBytes(3);
			byte[] creatorPublicKey = resultSet.getBytes(4);
			long timestamp = resultSet.getLong(5);

			Long fee = resultSet.getLong(6);
			if (fee == 0 && resultSet.wasNull())
				fee = null;

			int txGroupId = resultSet.getInt(7);

			Integer blockHeight = resultSet.getInt(8);
			if (blockHeight == 0 && resultSet.wasNull())
				blockHeight = null;

			ApprovalStatus approvalStatus = ApprovalStatus.valueOf(resultSet.getInt(9));
			Integer approvalHeight = resultSet.getInt(10);
			if (approvalHeight == 0 && resultSet.wasNull())
				approvalHeight = null;

			BaseTransactionData baseTransactionData = new BaseTransactionData(timestamp, txGroupId, reference, creatorPublicKey, fee, approvalStatus, blockHeight, approvalHeight, signature);

			int version = resultSet.getInt(11);
			int nonce = resultSet.getInt(12);
			Service serviceResult = Service.valueOf(resultSet.getInt(13));
			int size = resultSet.getInt(14);
			boolean isDataRaw = resultSet.getBoolean(15); // NOT NULL, so no null to false
			DataType dataType = isDataRaw ? DataType.RAW_DATA : DataType.DATA_HASH;
			byte[] data = resultSet.getBytes(16);
			byte[] metadataHash = resultSet.getBytes(17);
			String nameResult = resultSet.getString(18);
			String identifierResult = resultSet.getString(19);
			Method methodResult = Method.valueOf(resultSet.getInt(20));
			byte[] secret = resultSet.getBytes(21);
			Compression compression = Compression.valueOf(resultSet.getInt(22));
			// FUTURE: get payments from signature if needed. Avoiding for now to reduce database calls.

			ArbitraryTransactionData transactionData = new ArbitraryTransactionData(baseTransactionData,
					version, serviceResult, nonce, size, nameResult, identifierResult, methodResult, secret,
					compression, data, dataType, metadataHash, null);

			return transactionData;
		} catch (SQLException e) {
			throw new DataException("Unable to fetch arbitrary transactions from repository", e);
		}
	}

	@Override
	public List<ArbitraryResourceInfo> getArbitraryResources(Service service, String identifier, List<String> names,
															 boolean defaultResource, Integer limit, Integer offset, Boolean reverse) throws DataException {
		StringBuilder sql = new StringBuilder(512);
		List<Object> bindParams = new ArrayList<>();

		sql.append("SELECT name, service, identifier, MAX(size) AS max_size FROM ArbitraryTransactions WHERE 1=1");

		if (service != null) {
			sql.append(" AND service = ");
			sql.append(service.value);
		}

		if (defaultResource) {
			// Default resource requested - use NULL identifier
			sql.append(" AND identifier IS NULL");
		}
		else {
			// Non-default resource requested
			// Use an exact match identifier, or list all if supplied identifier is null
			sql.append(" AND (identifier = ? OR (? IS NULL))");
			bindParams.add(identifier);
			bindParams.add(identifier);
		}

		if (names != null && !names.isEmpty()) {
			sql.append(" AND name IN (?");
			bindParams.add(names.get(0));

			for (int i = 1; i < names.size(); ++i) {
				sql.append(", ?");
				bindParams.add(names.get(i));
			}

			sql.append(")");
		}

		sql.append(" GROUP BY name, service, identifier ORDER BY name COLLATE SQL_TEXT_UCC_NO_PAD");

		if (reverse != null && reverse) {
			sql.append(" DESC");
		}

		HSQLDBRepository.limitOffsetSql(sql, limit, offset);

		List<ArbitraryResourceInfo> arbitraryResources = new ArrayList<>();

		try (ResultSet resultSet = this.repository.checkedExecute(sql.toString(), bindParams.toArray())) {
			if (resultSet == null)
				return null;

			do {
				String nameResult = resultSet.getString(1);
				Service serviceResult = Service.valueOf(resultSet.getInt(2));
				String identifierResult = resultSet.getString(3);
				Integer sizeResult = resultSet.getInt(4);

				// We should filter out resources without names
				if (nameResult == null) {
					continue;
				}

				ArbitraryResourceInfo arbitraryResourceInfo = new ArbitraryResourceInfo();
				arbitraryResourceInfo.name = nameResult;
				arbitraryResourceInfo.service = serviceResult;
				arbitraryResourceInfo.identifier = identifierResult;
				arbitraryResourceInfo.size = Longs.valueOf(sizeResult);

				arbitraryResources.add(arbitraryResourceInfo);
			} while (resultSet.next());

			return arbitraryResources;
		} catch (SQLException e) {
			throw new DataException("Unable to fetch arbitrary transactions from repository", e);
		}
	}

	@Override
	public List<ArbitraryResourceInfo> searchArbitraryResources(Service service, String query,
															 boolean defaultResource, Integer limit, Integer offset, Boolean reverse) throws DataException {
		StringBuilder sql = new StringBuilder(512);
		List<Object> bindParams = new ArrayList<>();

		// For now we are searching anywhere in the fields
		// Note that this will bypass any indexes so may not scale well
		// Longer term we probably want to copy resources to their own table anyway
		String queryWildcard = String.format("%%%s%%", query.toLowerCase());

		sql.append("SELECT name, service, identifier, MAX(size) AS max_size FROM ArbitraryTransactions WHERE 1=1");

		if (service != null) {
			sql.append(" AND service = ");
			sql.append(service.value);
		}

		if (defaultResource) {
			// Default resource requested - use NULL identifier and search name only
			sql.append(" AND LCASE(name) LIKE ? AND identifier IS NULL");
			bindParams.add(queryWildcard);
		}
		else {
			// Non-default resource requested
			// In this case we search the identifier as well as the name
			sql.append(" AND (LCASE(name) LIKE ? OR LCASE(identifier) LIKE ?)");
			bindParams.add(queryWildcard);
			bindParams.add(queryWildcard);
		}

		sql.append(" GROUP BY name, service, identifier ORDER BY name COLLATE SQL_TEXT_UCC_NO_PAD");

		if (reverse != null && reverse) {
			sql.append(" DESC");
		}

		HSQLDBRepository.limitOffsetSql(sql, limit, offset);

		List<ArbitraryResourceInfo> arbitraryResources = new ArrayList<>();

		try (ResultSet resultSet = this.repository.checkedExecute(sql.toString(), bindParams.toArray())) {
			if (resultSet == null)
				return null;

			do {
				String nameResult = resultSet.getString(1);
				Service serviceResult = Service.valueOf(resultSet.getInt(2));
				String identifierResult = resultSet.getString(3);
				Integer sizeResult = resultSet.getInt(4);

				// We should filter out resources without names
				if (nameResult == null) {
					continue;
				}

				ArbitraryResourceInfo arbitraryResourceInfo = new ArbitraryResourceInfo();
				arbitraryResourceInfo.name = nameResult;
				arbitraryResourceInfo.service = serviceResult;
				arbitraryResourceInfo.identifier = identifierResult;
				arbitraryResourceInfo.size = Longs.valueOf(sizeResult);

				arbitraryResources.add(arbitraryResourceInfo);
			} while (resultSet.next());

			return arbitraryResources;
		} catch (SQLException e) {
			throw new DataException("Unable to fetch arbitrary transactions from repository", e);
		}
	}

	@Override
	public List<ArbitraryResourceNameInfo> getArbitraryResourceCreatorNames(Service service, String identifier,
																			boolean defaultResource, Integer limit, Integer offset, Boolean reverse) throws DataException {
		StringBuilder sql = new StringBuilder(512);

		sql.append("SELECT name FROM ArbitraryTransactions WHERE 1=1");

		if (service != null) {
			sql.append(" AND service = ");
			sql.append(service.value);
		}

		if (defaultResource) {
			// Default resource requested - use NULL identifier
			// The AND ? IS NULL AND ? IS NULL is a hack to make use of the identifier params in checkedExecute()
			identifier = null;
			sql.append(" AND (identifier IS NULL AND ? IS NULL AND ? IS NULL)");
		}
		else {
			// Non-default resource requested
			// Use an exact match identifier, or list all if supplied identifier is null
			sql.append(" AND (identifier = ? OR (? IS NULL))");
		}

		sql.append(" GROUP BY name ORDER BY name COLLATE SQL_TEXT_UCC_NO_PAD");

		if (reverse != null && reverse) {
			sql.append(" DESC");
		}

		HSQLDBRepository.limitOffsetSql(sql, limit, offset);

		List<ArbitraryResourceNameInfo> arbitraryResources = new ArrayList<>();

		try (ResultSet resultSet = this.repository.checkedExecute(sql.toString(), identifier, identifier)) {
			if (resultSet == null)
				return null;

			do {
				String name = resultSet.getString(1);

				// We should filter out resources without names
				if (name == null) {
					continue;
				}

				ArbitraryResourceNameInfo arbitraryResourceNameInfo = new ArbitraryResourceNameInfo();
				arbitraryResourceNameInfo.name = name;

				arbitraryResources.add(arbitraryResourceNameInfo);
			} while (resultSet.next());

			return arbitraryResources;
		} catch (SQLException e) {
			throw new DataException("Unable to fetch arbitrary transactions from repository", e);
		}
	}

}
