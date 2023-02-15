package org.aquila.repository;

import org.aquila.arbitrary.misc.Service;
import org.aquila.data.arbitrary.ArbitraryResourceInfo;
import org.aquila.data.arbitrary.ArbitraryResourceNameInfo;
import org.aquila.data.network.ArbitraryPeerData;
import org.aquila.data.transaction.ArbitraryTransactionData;
import org.aquila.data.transaction.ArbitraryTransactionData.*;

import java.util.List;

public interface ArbitraryRepository {

	public boolean isDataLocal(byte[] signature) throws DataException;

	public byte[] fetchData(byte[] signature) throws DataException;

	public void save(ArbitraryTransactionData arbitraryTransactionData) throws DataException;

	public void delete(ArbitraryTransactionData arbitraryTransactionData) throws DataException;

	public List<ArbitraryTransactionData> getArbitraryTransactions(String name, Service service, String identifier, long since) throws DataException;

	public ArbitraryTransactionData getLatestTransaction(String name, Service service, Method method, String identifier) throws DataException;


	public List<ArbitraryResourceInfo> getArbitraryResources(Service service, String identifier, List<String> names, boolean defaultResource, Integer limit, Integer offset, Boolean reverse) throws DataException;

	public List<ArbitraryResourceInfo> searchArbitraryResources(Service service, String query, boolean defaultResource, Integer limit, Integer offset, Boolean reverse) throws DataException;

	public List<ArbitraryResourceNameInfo> getArbitraryResourceCreatorNames(Service service, String identifier, boolean defaultResource, Integer limit, Integer offset, Boolean reverse) throws DataException;

}
