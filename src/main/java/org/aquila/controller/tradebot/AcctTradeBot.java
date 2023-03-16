package org.aquila.controller.tradebot;

import java.util.List;

import org.aquila.api.model.crosschain.TradeBotCreateRequest;
import org.aquila.crosschain.ACCT;
import org.aquila.crosschain.ForeignBlockchainException;
import org.aquila.data.at.ATData;
import org.aquila.data.crosschain.CrossChainTradeData;
import org.aquila.data.crosschain.TradeBotData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;

public interface AcctTradeBot {

	public enum ResponseResult { OK, BALANCE_ISSUE, NETWORK_ISSUE, TRADE_ALREADY_EXISTS }

	/** Returns list of state names for trade-bot entries that have ended, e.g. redeemed, refunded or cancelled. */
	public List<String> getEndStates();

	public byte[] createTrade(Repository repository, TradeBotCreateRequest tradeBotCreateRequest) throws DataException;

	public ResponseResult startResponse(Repository repository, ATData atData, ACCT acct,
			CrossChainTradeData crossChainTradeData, String foreignKey, String receivingAddress) throws DataException;

	public boolean canDelete(Repository repository, TradeBotData tradeBotData) throws DataException;

	public void progress(Repository repository, TradeBotData tradeBotData) throws DataException, ForeignBlockchainException;

}
