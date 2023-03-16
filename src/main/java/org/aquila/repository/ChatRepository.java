package org.aquila.repository;

import java.util.List;

import org.aquila.data.chat.ActiveChats;
import org.aquila.data.chat.ChatMessage;
import org.aquila.data.transaction.ChatTransactionData;

public interface ChatRepository {

	/**
	 * Returns CHAT messages matching criteria.
	 * <p>
	 * Expects EITHER non-null txGroupID OR non-null sender and recipient addresses.
	 */
	public List<ChatMessage> getMessagesMatchingCriteria(Long before, Long after,
			Integer txGroupId, byte[] reference, byte[] chatReferenceBytes, Boolean hasChatReference,
			List<String> involving, String senderAddress, Integer limit, Integer offset, Boolean reverse) throws DataException;

	public ChatMessage toChatMessage(ChatTransactionData chatTransactionData) throws DataException;

	public ActiveChats getActiveChats(String address) throws DataException;

}
