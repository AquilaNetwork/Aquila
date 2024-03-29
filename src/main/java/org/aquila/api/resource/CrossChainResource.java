package org.aquila.api.resource;

import com.google.common.primitives.Longs;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.*;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.aquila.api.ApiError;
import org.aquila.api.ApiErrors;
import org.aquila.api.ApiExceptionFactory;
import org.aquila.api.Security;
import org.aquila.api.model.CrossChainCancelRequest;
import org.aquila.api.model.CrossChainTradeSummary;
import org.aquila.controller.tradebot.TradeBot;
import org.aquila.crosschain.ACCT;
import org.aquila.crosschain.AcctMode;
import org.aquila.crosschain.SupportedBlockchain;
import org.aquila.crypto.Crypto;
import org.aquila.data.at.ATData;
import org.aquila.data.at.ATStateData;
import org.aquila.data.crosschain.CrossChainTradeData;
import org.aquila.data.transaction.BaseTransactionData;
import org.aquila.data.transaction.MessageTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.group.Group;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.repository.RepositoryManager;
import org.aquila.transaction.MessageTransaction;
import org.aquila.transaction.Transaction.ValidationResult;
import org.aquila.transform.TransformationException;
import org.aquila.transform.Transformer;
import org.aquila.transform.transaction.MessageTransactionTransformer;
import org.aquila.utils.Amounts;
import org.aquila.utils.Base58;
import org.aquila.utils.ByteArray;
import org.aquila.utils.NTP;

@Path("/crosschain")
@Tag(name = "Cross-Chain")
public class CrossChainResource {

	@Context
	HttpServletRequest request;

	@GET
	@Path("/tradeoffers")
	@Operation(
		summary = "Find cross-chain trade offers",
		responses = {
			@ApiResponse(
				content = @Content(
					array = @ArraySchema(
						schema = @Schema(
							implementation = CrossChainTradeData.class
						)
					)
				)
			)
		}
	)
	@ApiErrors({ApiError.INVALID_CRITERIA, ApiError.REPOSITORY_ISSUE})
	public List<CrossChainTradeData> getTradeOffers(
			@Parameter(
				description = "Limit to specific blockchain",
				example = "LITECOIN",
				schema = @Schema(implementation = SupportedBlockchain.class)
			) @QueryParam("foreignBlockchain") SupportedBlockchain foreignBlockchain,
			@Parameter( ref = "limit") @QueryParam("limit") Integer limit,
			@Parameter( ref = "offset" ) @QueryParam("offset") Integer offset,
			@Parameter( ref = "reverse" ) @QueryParam("reverse") Boolean reverse) {
		// Impose a limit on 'limit'
		if (limit != null && limit > 100)
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_CRITERIA);

		final boolean isExecutable = true;
		List<CrossChainTradeData> crossChainTrades = new ArrayList<>();

		try (final Repository repository = RepositoryManager.getRepository()) {
			Map<ByteArray, Supplier<ACCT>> acctsByCodeHash = SupportedBlockchain.getFilteredAcctMap(foreignBlockchain);

			for (Map.Entry<ByteArray, Supplier<ACCT>> acctInfo : acctsByCodeHash.entrySet()) {
				byte[] codeHash = acctInfo.getKey().value;
				ACCT acct = acctInfo.getValue().get();

				List<ATData> atsData = repository.getATRepository().getATsByFunctionality(codeHash, isExecutable, limit, offset, reverse);

				for (ATData atData : atsData) {
					CrossChainTradeData crossChainTradeData = acct.populateTradeData(repository, atData);
					if (crossChainTradeData.mode == AcctMode.OFFERING) {
						crossChainTrades.add(crossChainTradeData);
					}
				}
			}

			// Sort the trades by timestamp
			if (reverse != null && reverse) {
				crossChainTrades.sort((a, b) -> Longs.compare(b.creationTimestamp, a.creationTimestamp));
			}
			else {
				crossChainTrades.sort((a, b) -> Longs.compare(a.creationTimestamp, b.creationTimestamp));
			}

			if (limit != null && limit > 0) {
				// Make sure to not return more than the limit
				int upperLimit = Math.min(limit, crossChainTrades.size());
				crossChainTrades = crossChainTrades.subList(0, upperLimit);
			}

			crossChainTrades.stream().forEach(CrossChainResource::decorateTradeDataWithPresence);

			return crossChainTrades;
		} catch (DataException e) {
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.REPOSITORY_ISSUE, e);
		}
	}

	@GET
	@Path("/trade/{ataddress}")
	@Operation(
		summary = "Show detailed trade info",
		responses = {
			@ApiResponse(
				content = @Content(
					schema = @Schema(
						implementation = CrossChainTradeData.class
					)
				)
			)
		}
	)
	@ApiErrors({ApiError.ADDRESS_UNKNOWN, ApiError.INVALID_CRITERIA, ApiError.REPOSITORY_ISSUE})
	public CrossChainTradeData getTrade(@PathParam("ataddress") String atAddress) {
		try (final Repository repository = RepositoryManager.getRepository()) {
			ATData atData = repository.getATRepository().fromATAddress(atAddress);
			if (atData == null)
				throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.ADDRESS_UNKNOWN);

			ACCT acct = SupportedBlockchain.getAcctByCodeHash(atData.getCodeHash());
			if (acct == null)
				throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_CRITERIA);

			CrossChainTradeData crossChainTradeData = acct.populateTradeData(repository, atData);

			decorateTradeDataWithPresence(crossChainTradeData);

			return crossChainTradeData;
		} catch (DataException e) {
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.REPOSITORY_ISSUE, e);
		}
	}

	@GET
	@Path("/trades")
	@Operation(
		summary = "Find completed cross-chain trades",
		description = "Returns summary info about successfully completed cross-chain trades",
		responses = {
			@ApiResponse(
				content = @Content(
					array = @ArraySchema(
						schema = @Schema(
							implementation = CrossChainTradeSummary.class
						)
					)
				)
			)
		}
	)
	@ApiErrors({ApiError.INVALID_CRITERIA, ApiError.REPOSITORY_ISSUE})
	public List<CrossChainTradeSummary> getCompletedTrades(
			@Parameter(
					description = "Limit to specific blockchain",
					example = "LITECOIN",
					schema = @Schema(implementation = SupportedBlockchain.class)
				) @QueryParam("foreignBlockchain") SupportedBlockchain foreignBlockchain,
			@Parameter(
				description = "Only return trades that completed on/after this timestamp (milliseconds since epoch)",
				example = "1597310000000"
			) @QueryParam("minimumTimestamp") Long minimumTimestamp,
			@Parameter( ref = "limit") @QueryParam("limit") Integer limit,
			@Parameter( ref = "offset" ) @QueryParam("offset") Integer offset,
			@Parameter( ref = "reverse" ) @QueryParam("reverse") Boolean reverse) {
		// Impose a limit on 'limit'
		if (limit != null && limit > 100)
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_CRITERIA);

		// minimumTimestamp (if given) needs to be positive
		if (minimumTimestamp != null && minimumTimestamp <= 0)
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_CRITERIA);

		final Boolean isFinished = Boolean.TRUE;

		try (final Repository repository = RepositoryManager.getRepository()) {
			Integer minimumFinalHeight = null;

			if (minimumTimestamp != null) {
				minimumFinalHeight = repository.getBlockRepository().getHeightFromTimestamp(minimumTimestamp);
				// If not found in the block repository it will return either 0 or 1
				if (minimumFinalHeight == 0 || minimumFinalHeight == 1) {
					// Try the archive
					minimumFinalHeight = repository.getBlockArchiveRepository().getHeightFromTimestamp(minimumTimestamp);
				}

				if (minimumFinalHeight == 0)
					// We don't have any blocks since minimumTimestamp, let alone trades, so nothing to return
					return Collections.emptyList();

				// height returned from repository is for block BEFORE timestamp
				// but we want trades AFTER timestamp so bump height accordingly
				minimumFinalHeight++;
			}

			List<CrossChainTradeSummary> crossChainTrades = new ArrayList<>();

			Map<ByteArray, Supplier<ACCT>> acctsByCodeHash = SupportedBlockchain.getFilteredAcctMap(foreignBlockchain);

			for (Map.Entry<ByteArray, Supplier<ACCT>> acctInfo : acctsByCodeHash.entrySet()) {
				byte[] codeHash = acctInfo.getKey().value;
				ACCT acct = acctInfo.getValue().get();

				List<ATStateData> atStates = repository.getATRepository().getMatchingFinalATStates(codeHash,
						isFinished, acct.getModeByteOffset(), (long) AcctMode.REDEEMED.value, minimumFinalHeight,
						limit, offset, reverse);

				for (ATStateData atState : atStates) {
					CrossChainTradeData crossChainTradeData = acct.populateTradeData(repository, atState);

					// We also need block timestamp for use as trade timestamp
					long timestamp = repository.getBlockRepository().getTimestampFromHeight(atState.getHeight());
					if (timestamp == 0) {
						// Try the archive
						timestamp = repository.getBlockArchiveRepository().getTimestampFromHeight(atState.getHeight());
					}

					CrossChainTradeSummary crossChainTradeSummary = new CrossChainTradeSummary(crossChainTradeData, timestamp);
					crossChainTrades.add(crossChainTradeSummary);
				}
			}

			// Sort the trades by timestamp
			if (reverse != null && reverse) {
				crossChainTrades.sort((a, b) -> Longs.compare(b.getTradeTimestamp(), a.getTradeTimestamp()));
			}
			else {
				crossChainTrades.sort((a, b) -> Longs.compare(a.getTradeTimestamp(), b.getTradeTimestamp()));
			}

			if (limit != null && limit > 0) {
				// Make sure to not return more than the limit
				int upperLimit = Math.min(limit, crossChainTrades.size());
				crossChainTrades = crossChainTrades.subList(0, upperLimit);
			}

			return crossChainTrades;
		} catch (DataException e) {
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.REPOSITORY_ISSUE, e);
		}
	}

	@GET
	@Path("/price/{blockchain}")
	@Operation(
		summary = "Request current estimated trading price",
		description = "Returns price based on most recent completed trades. Price is expressed in terms of UNCIA per unit foreign currency.",
		responses = {
			@ApiResponse(
				content = @Content(
					schema = @Schema(
						type = "number"
					)
				)
			)
		}
	)
	@ApiErrors({ApiError.INVALID_CRITERIA, ApiError.REPOSITORY_ISSUE})
	public long getTradePriceEstimate(
			@Parameter(
					description = "foreign blockchain",
					example = "LITECOIN",
					schema = @Schema(implementation = SupportedBlockchain.class)
				) @PathParam("blockchain") SupportedBlockchain foreignBlockchain,
			@Parameter(
					description = "Maximum number of trades to include in price calculation",
					example = "10",
					schema = @Schema(type = "integer", defaultValue = "10")
			) @QueryParam("maxtrades") Integer maxtrades,
			@Parameter(
					description = "Display price in terms of foreign currency per unit UNCIA",
					example = "false",
					schema = @Schema(type = "boolean", defaultValue = "false")
			) @QueryParam("inverse") Boolean inverse) {
		// foreignBlockchain is required
		if (foreignBlockchain == null)
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_CRITERIA);

		// We want both a minimum of 5 trades and enough trades to span at least 4 hours
		int minimumCount = 5;
		int maximumCount = maxtrades != null ? maxtrades : 10;
		long minimumPeriod = 4 * 60 * 60 * 1000L; // ms
		Boolean isFinished = Boolean.TRUE;
		boolean useInversePrice = (inverse != null && inverse == true);

		try (final Repository repository = RepositoryManager.getRepository()) {
			Map<ByteArray, Supplier<ACCT>> acctsByCodeHash = SupportedBlockchain.getFilteredAcctMap(foreignBlockchain);

			long totalForeign = 0;
			long totalUncia = 0;

			Map<Long, CrossChainTradeData> reverseSortedTradeData = new TreeMap<>(Collections.reverseOrder());

			// Collect recent AT states for each ACCT version
			for (Map.Entry<ByteArray, Supplier<ACCT>> acctInfo : acctsByCodeHash.entrySet()) {
				byte[] codeHash = acctInfo.getKey().value;
				ACCT acct = acctInfo.getValue().get();

				List<ATStateData> atStates = repository.getATRepository().getMatchingFinalATStatesQuorum(codeHash,
						isFinished, acct.getModeByteOffset(), (long) AcctMode.REDEEMED.value, minimumCount, maximumCount, minimumPeriod);

				for (ATStateData atState : atStates) {
					// We also need block timestamp for use as trade timestamp
					long timestamp = repository.getBlockRepository().getTimestampFromHeight(atState.getHeight());
					if (timestamp == 0) {
						// Try the archive
						timestamp = repository.getBlockArchiveRepository().getTimestampFromHeight(atState.getHeight());
					}

					CrossChainTradeData crossChainTradeData = acct.populateTradeData(repository, atState);
					reverseSortedTradeData.put(timestamp, crossChainTradeData);
				}
			}

			// Loop through the sorted map and calculate the average price
			// Also remove elements beyond the maxtrades limit
			Set set = reverseSortedTradeData.entrySet();
			Iterator i = set.iterator();
			int index = 0;
			while (i.hasNext()) {
				Map.Entry tradeDataMap = (Map.Entry)i.next();
				CrossChainTradeData crossChainTradeData = (CrossChainTradeData) tradeDataMap.getValue();

				if (maxtrades != null && index >= maxtrades) {
					// We've reached the limit
					break;
				}

				totalForeign += crossChainTradeData.expectedForeignAmount;
				totalUncia += crossChainTradeData.unciaAmount;
				index++;
			}

			return useInversePrice ? Amounts.scaledDivide(totalForeign, totalUncia) : Amounts.scaledDivide(totalUncia, totalForeign);
		} catch (DataException e) {
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.REPOSITORY_ISSUE, e);
		}
	}

	@DELETE
	@Path("/tradeoffer")
	@Operation(
		summary = "Builds raw, unsigned 'cancel' MESSAGE transaction that cancels cross-chain trade offer",
		description = "Specify address of cross-chain AT that needs to be cancelled.<br>"
			+ "AT needs to be in 'offer' mode. Messages sent to an AT in 'trade' mode will be ignored.<br>"
			+ "Performs MESSAGE proof-of-work.<br>"
			+ "You need to sign output with AT creator's private key otherwise the MESSAGE transaction will be invalid.",
		requestBody = @RequestBody(
			required = true,
			content = @Content(
				mediaType = MediaType.APPLICATION_JSON,
				schema = @Schema(
					implementation = CrossChainCancelRequest.class
				)
			)
		),
		responses = {
			@ApiResponse(
				content = @Content(
					schema = @Schema(
						type = "string"
					)
				)
			)
		}
	)
	@ApiErrors({ApiError.INVALID_PUBLIC_KEY, ApiError.INVALID_ADDRESS, ApiError.INVALID_CRITERIA, ApiError.REPOSITORY_ISSUE})
	@SecurityRequirement(name = "apiKey")
	public String cancelTrade(@HeaderParam(Security.API_KEY_HEADER) String apiKey, CrossChainCancelRequest cancelRequest) {
		Security.checkApiCallAllowed(request);

		byte[] creatorPublicKey = cancelRequest.creatorPublicKey;

		if (creatorPublicKey == null || creatorPublicKey.length != Transformer.PUBLIC_KEY_LENGTH)
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_PUBLIC_KEY);

		if (cancelRequest.atAddress == null || !Crypto.isValidAtAddress(cancelRequest.atAddress))
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_ADDRESS);

		try (final Repository repository = RepositoryManager.getRepository()) {
			ATData atData = fetchAtDataWithChecking(repository, cancelRequest.atAddress);

			ACCT acct = SupportedBlockchain.getAcctByCodeHash(atData.getCodeHash());
			if (acct == null)
				throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_ADDRESS);

			CrossChainTradeData crossChainTradeData = acct.populateTradeData(repository, atData);

			if (crossChainTradeData.mode != AcctMode.OFFERING)
				throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_CRITERIA);

			// Does supplied public key match AT creator's public key?
			if (!Arrays.equals(creatorPublicKey, atData.getCreatorPublicKey()))
				throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_PUBLIC_KEY);

			// Good to make MESSAGE

			String atCreatorAddress = Crypto.toAddress(creatorPublicKey);
			byte[] messageData = acct.buildCancelMessage(atCreatorAddress);

			byte[] messageTransactionBytes = buildAtMessage(repository, creatorPublicKey, cancelRequest.atAddress, messageData);

			return Base58.encode(messageTransactionBytes);
		} catch (DataException e) {
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.REPOSITORY_ISSUE, e);
		}
	}

	private ATData fetchAtDataWithChecking(Repository repository, String atAddress) throws DataException {
		ATData atData = repository.getATRepository().fromATAddress(atAddress);
		if (atData == null)
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.ADDRESS_UNKNOWN);

		// No point sending message to AT that's finished
		if (atData.getIsFinished())
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_CRITERIA);

		return atData;
	}

	private byte[] buildAtMessage(Repository repository, byte[] senderPublicKey, String atAddress, byte[] messageData) throws DataException {
		long txTimestamp = NTP.getTime();

		// senderPublicKey could be ephemeral trade public key where there is no corresponding account and hence no reference
		String senderAddress = Crypto.toAddress(senderPublicKey);
		byte[] lastReference = repository.getAccountRepository().getLastReference(senderAddress);
		final boolean requiresPoW = lastReference == null;

		if (requiresPoW) {
			Random random = new Random();
			lastReference = new byte[Transformer.SIGNATURE_LENGTH];
			random.nextBytes(lastReference);
		}

		int version = 4;
		int nonce = 0;
		long amount = 0L;
		Long assetId = null; // no assetId as amount is zero
		Long fee = 0L;

		BaseTransactionData baseTransactionData = new BaseTransactionData(txTimestamp, Group.NO_GROUP, lastReference, senderPublicKey, fee, null);
		TransactionData messageTransactionData = new MessageTransactionData(baseTransactionData, version, nonce, atAddress, amount, assetId, messageData, false, false);

		MessageTransaction messageTransaction = new MessageTransaction(repository, messageTransactionData);

		if (requiresPoW) {
			messageTransaction.computeNonce();
		} else {
			fee = messageTransaction.calcRecommendedFee();
			messageTransactionData.setFee(fee);
		}

		ValidationResult result = messageTransaction.isValidUnconfirmed();
		if (result != ValidationResult.OK)
			throw TransactionsResource.createTransactionInvalidException(request, result);

		try {
			return MessageTransactionTransformer.toBytes(messageTransactionData);
		} catch (TransformationException e) {
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.TRANSFORMATION_ERROR, e);
		}
	}

	private static void decorateTradeDataWithPresence(CrossChainTradeData crossChainTradeData) {
		TradeBot.getInstance().decorateTradeDataWithPresence(crossChainTradeData);
	}
}
