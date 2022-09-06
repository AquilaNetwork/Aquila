package org.aquila.crosschain;

import org.aquila.data.at.ATData;
import org.aquila.data.at.ATStateData;
import org.aquila.data.crosschain.CrossChainTradeData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;

public interface ACCT {

	public byte[] getCodeBytesHash();

	public int getModeByteOffset();

	public ForeignBlockchain getBlockchain();

	public CrossChainTradeData populateTradeData(Repository repository, ATData atData) throws DataException;

	public CrossChainTradeData populateTradeData(Repository repository, ATStateData atStateData) throws DataException;

	public byte[] buildCancelMessage(String creatorAquilaAddress);

	public byte[] findSecretA(Repository repository, CrossChainTradeData crossChainTradeData) throws DataException;

}
