package org.aquila.api.websocket;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

import org.aquila.controller.tradebot.TradeBot;
import org.aquila.crosschain.SupportedBlockchain;
import org.aquila.data.crosschain.TradeBotData;
import org.aquila.event.Event;
import org.aquila.event.EventBus;
import org.aquila.event.Listener;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.repository.RepositoryManager;
import org.aquila.utils.Base58;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

@WebSocket
@SuppressWarnings("serial")
public class TradeBotWebSocket extends ApiWebSocket implements Listener {

	/** Cache of trade-bot entry states, keyed by trade-bot entry's "trade private key" (base58) */
	private static final Map<String, Integer> PREVIOUS_STATES = new HashMap<>();

	private static final Map<Session, String> sessionBlockchain = Collections.synchronizedMap(new HashMap<>());

	@Override
	public void configure(WebSocketServletFactory factory) {
		factory.register(TradeBotWebSocket.class);

		try (final Repository repository = RepositoryManager.getRepository()) {
			List<TradeBotData> tradeBotEntries = repository.getCrossChainRepository().getAllTradeBotData();
			if (tradeBotEntries == null)
				// How do we properly fail here?
				return;

			PREVIOUS_STATES.putAll(tradeBotEntries.stream().collect(Collectors.toMap(entry -> Base58.encode(entry.getTradePrivateKey()), TradeBotData::getStateValue)));
		} catch (DataException e) {
			// No output this time
		}

		EventBus.INSTANCE.addListener(this::listen);
	}

	@Override
	public void listen(Event event) {
		if (!(event instanceof TradeBot.StateChangeEvent))
			return;

		TradeBotData tradeBotData = ((TradeBot.StateChangeEvent) event).getTradeBotData();
		String tradePrivateKey58 = Base58.encode(tradeBotData.getTradePrivateKey());

		synchronized (PREVIOUS_STATES) {
			Integer previousStateValue = PREVIOUS_STATES.get(tradePrivateKey58);
			if (previousStateValue != null && previousStateValue == tradeBotData.getStateValue())
				// Not changed
				return;

			PREVIOUS_STATES.put(tradePrivateKey58, tradeBotData.getStateValue());
		}

		List<TradeBotData> tradeBotEntries = Collections.singletonList(tradeBotData);

		for (Session session : getSessions()) {
			// Only send if this session has this/no preferred blockchain
			String preferredBlockchain = sessionBlockchain.get(session);

			if (preferredBlockchain == null || preferredBlockchain.equals(tradeBotData.getForeignBlockchain()))
				sendEntries(session, tradeBotEntries);
		}
	}

	@OnWebSocketConnect
	@Override
	public void onWebSocketConnect(Session session) {
		Map<String, List<String>> queryParams = session.getUpgradeRequest().getParameterMap();
		final boolean excludeInitialData = queryParams.get("excludeInitialData") != null;

		List<String> foreignBlockchains = queryParams.get("foreignBlockchain");
		final String foreignBlockchain = foreignBlockchains == null ? null : foreignBlockchains.get(0);

		// Make sure blockchain (if any) is valid
		if (foreignBlockchain != null && SupportedBlockchain.fromString(foreignBlockchain) == null) {
			session.close(4003, "unknown blockchain: " + foreignBlockchain);
			return;
		}

		// save session's preferred blockchain (if any)
		sessionBlockchain.put(session, foreignBlockchain);



		// Maybe send all known trade-bot entries
		try (final Repository repository = RepositoryManager.getRepository()) {
			List<TradeBotData> tradeBotEntries = new ArrayList<>();

			// We might need to exclude the initial data from the response
			if (!excludeInitialData) {
				tradeBotEntries = repository.getCrossChainRepository().getAllTradeBotData();

				// Optional filtering
				if (foreignBlockchain != null)
					tradeBotEntries = tradeBotEntries.stream()
							.filter(tradeBotData -> tradeBotData.getForeignBlockchain().equals(foreignBlockchain))
							.collect(Collectors.toList());
			}

			if (!sendEntries(session, tradeBotEntries)) {
				session.close(4002, "websocket issue");
				return;
			}
		} catch (DataException e) {
			session.close(4001, "repository issue fetching trade-bot entries");
			return;
		}

		super.onWebSocketConnect(session);
	}

	@OnWebSocketClose
	@Override
	public void onWebSocketClose(Session session, int statusCode, String reason) {
		// clean up
		sessionBlockchain.remove(session);

		super.onWebSocketClose(session, statusCode, reason);
	}

	@OnWebSocketError
	public void onWebSocketError(Session session, Throwable throwable) {
		/* ignored */
	}

	@OnWebSocketMessage
	public void onWebSocketMessage(Session session, String message) {
		/* ignored */
	}

	private boolean sendEntries(Session session, List<TradeBotData> tradeBotEntries) {
		try {
			StringWriter stringWriter = new StringWriter();
			marshall(stringWriter, tradeBotEntries);

			String output = stringWriter.toString();
			session.getRemote().sendStringByFuture(output);
		} catch (IOException e) {
			// No output this time?
			return false;
		}

		return true;
	}

}
