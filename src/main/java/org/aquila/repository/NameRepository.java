package org.aquila.repository;

import java.util.List;

import org.aquila.data.naming.NameData;

public interface NameRepository {

	public NameData fromName(String name) throws DataException;

	public boolean nameExists(String name) throws DataException;

	public NameData fromReducedName(String reducedName) throws DataException;

	public boolean reducedNameExists(String reducedName) throws DataException;

	public List<NameData> getAllNames(Integer limit, Integer offset, Boolean reverse) throws DataException;

	public default List<NameData> getAllNames() throws DataException {
		return getAllNames(null, null, null);
	}

	public List<NameData> getNamesForSale(Integer limit, Integer offset, Boolean reverse) throws DataException;

	public default List<NameData> getNamesForSale() throws DataException {
		return getNamesForSale(null, null, null);
	}

	public List<NameData> getNamesByOwner(String address, Integer limit, Integer offset, Boolean reverse) throws DataException;

	public default List<NameData> getNamesByOwner(String address) throws DataException {
		return getNamesByOwner(address, null, null, null);
	}

	public List<String> getRecentNames(long startTimestamp) throws DataException;

	public void save(NameData nameData) throws DataException;

	public void delete(String name) throws DataException;

}
