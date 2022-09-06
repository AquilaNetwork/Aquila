package org.aquila.api.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Arrays;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.aquila.account.PublicKeyAccount;
import org.aquila.api.ApiError;
import org.aquila.api.ApiErrors;
import org.aquila.api.ApiExceptionFactory;
import org.aquila.api.Security;
import org.aquila.api.model.CrossChainBuildRequest;
import org.aquila.api.model.CrossChainDualSecretRequest;
import org.aquila.api.model.CrossChainTradeRequest;
import org.aquila.asset.Asset;
import org.aquila.crosschain.BitcoinACCTv1;
import org.aquila.crosschain.Bitcoiny;
import org.aquila.crosschain.AcctMode;
import org.aquila.crypto.Crypto;
import org.aquila.data.at.ATData;
import org.aquila.data.crosschain.CrossChainTradeData;
import org.aquila.data.transaction.BaseTransactionData;
import org.aquila.data.transaction.DeployAtTransactionData;
import org.aquila.data.transaction.MessageTransactionData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.group.Group;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.repository.RepositoryManager;
import org.aquila.transaction.DeployAtTransaction;
import org.aquila.transaction.MessageTransaction;
import org.aquila.transaction.Transaction;
import org.aquila.transaction.Transaction.TransactionType;
import org.aquila.transaction.Transaction.ValidationResult;
import org.aquila.transform.TransformationException;
import org.aquila.transform.Transformer;
import org.aquila.transform.transaction.DeployAtTransactionTransformer;
import org.aquila.transform.transaction.MessageTransactionTransformer;
import org.aquila.utils.Base58;
import org.aquila.utils.NTP;

@Path("/crosschain/BitcoinACCTv1")
@Tag(name = "Cross-Chain (BitcoinACCTv1)")
public class CrossChainBitcoinACCTv1Resource {

	@Context
	HttpServletRequest request;

	@POST
	@Path("/build")
	@Operation(
		summary = "Build Bitcoin cross-chain trading AT",
		description = "Returns raw, unsigned DEPLOY_AT transaction",
		requestBody = @RequestBody(
			required = true,
			content = @Content(
				mediaType = MediaType.APPLICATION_JSON,
				schema = @Schema(
					implementation = CrossChainBuildRequest.class
				)
			)
		),
		responses = {
			@ApiResponse(
				content = @Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(type = "string"))
			)
		}
	)
	@ApiErrors({ApiError.INVALID_PUBLIC_KEY, ApiError.INVALID_DATA, ApiError.INVALID_REFERENCE, ApiError.TRANSFORMATION_ERROR, ApiError.REPOSITORY_ISSUE})
	@SecurityRequirement(name = "apiKey")
	public String buildTrade(@HeaderParam(Security.API_KEY_HEADER) String apiKey, CrossChainBuildRequest tradeRequest) {
		Security.checkApiCallAllowed(request);

		byte[] creatorPublicKey = tradeRequest.creatorPublicKey;

		if (creatorPublicKey == null || creatorPublicKey.length != Transformer.PUBLIC_KEY_LENGTH)
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_PUBLIC_KEY);

		if (tradeRequest.hashOfSecretB == null || tradeRequest.hashOfSecretB.length != Bitcoiny.HASH160_LENGTH)
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_DATA);

		if (tradeRequest.tradeTimeout == null)
			tradeRequest.tradeTimeout = 7 * 24 * 60; // 7 days
		else
			if (tradeRequest.tradeTimeout < 10 || tradeRequest.tradeTimeout > 50000)
				throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_DATA);

		if (tradeRequest.unciaAmount <= 0)
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_DATA);

		if (tradeRequest.fundingUnciaAmount <= 0)
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_DATA);

		// funding amount must exceed initial + final
		if (tradeRequest.fundingUnciaAmount <= tradeRequest.unciaAmount)
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_DATA);

		if (tradeRequest.bitcoinAmount <= 0)
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_DATA);

		try (final Repository repository = RepositoryManager.getRepository()) {
			PublicKeyAccount creatorAccount = new PublicKeyAccount(repository, creatorPublicKey);

			byte[] creationBytes = BitcoinACCTv1.buildAquilaAT(creatorAccount.getAddress(), tradeRequest.bitcoinPublicKeyHash, tradeRequest.hashOfSecretB,
					tradeRequest.unciaAmount, tradeRequest.bitcoinAmount, tradeRequest.tradeTimeout);

			long txTimestamp = NTP.getTime();
			byte[] lastReference = creatorAccount.getLastReference();
			if (lastReference == null)
				throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_REFERENCE);

			long fee = 0;
			String name = "UNCIA-BTC cross-chain trade";
			String description = "Aquila-Bitcoin cross-chain trade";
			String atType = "ACCT";
			String tags = "UNCIA-BTC ACCT";

			BaseTransactionData baseTransactionData = new BaseTransactionData(txTimestamp, Group.NO_GROUP, lastReference, creatorAccount.getPublicKey(), fee, null);
			TransactionData deployAtTransactionData = new DeployAtTransactionData(baseTransactionData, name, description, atType, tags, creationBytes, tradeRequest.fundingUnciaAmount, Asset.UNCIA);

			Transaction deployAtTransaction = new DeployAtTransaction(repository, deployAtTransactionData);

			fee = deployAtTransaction.calcRecommendedFee();
			deployAtTransactionData.setFee(fee);

			ValidationResult result = deployAtTransaction.isValidUnconfirmed();
			if (result != ValidationResult.OK)
				throw TransactionsResource.createTransactionInvalidException(request, result);

			byte[] bytes = DeployAtTransactionTransformer.toBytes(deployAtTransactionData);
			return Base58.encode(bytes);
		} catch (TransformationException e) {
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.TRANSFORMATION_ERROR, e);
		} catch (DataException e) {
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.REPOSITORY_ISSUE, e);
		}
	}

	@POST
	@Path("/trademessage")
	@Operation(
		summary = "Builds raw, unsigned 'trade' MESSAGE transaction that sends cross-chain trade recipient address, triggering 'trade' mode",
		description = "Specify address of cross-chain AT that needs to be messaged, and signature of 'offer' MESSAGE from trade partner.<br>"
			+ "AT needs to be in 'offer' mode. Messages sent to an AT in any other mode will be ignored, but still cost fees to send!<br>"
			+ "You need to sign output with trade private key otherwise the MESSAGE transaction will be invalid.",
		requestBody = @RequestBody(
			required = true,
			content = @Content(
				mediaType = MediaType.APPLICATION_JSON,
				schema = @Schema(
					implementation = CrossChainTradeRequest.class
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
	public String buildTradeMessage(@HeaderParam(Security.API_KEY_HEADER) String apiKey, CrossChainTradeRequest tradeRequest) {
		Security.checkApiCallAllowed(request);

		byte[] tradePublicKey = tradeRequest.tradePublicKey;

		if (tradePublicKey == null || tradePublicKey.length != Transformer.PUBLIC_KEY_LENGTH)
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_PUBLIC_KEY);

		if (tradeRequest.atAddress == null || !Crypto.isValidAtAddress(tradeRequest.atAddress))
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_ADDRESS);

		if (tradeRequest.messageTransactionSignature == null || !Crypto.isValidAddress(tradeRequest.messageTransactionSignature))
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_ADDRESS);

		try (final Repository repository = RepositoryManager.getRepository()) {
			ATData atData = fetchAtDataWithChecking(repository, tradeRequest.atAddress);
			CrossChainTradeData crossChainTradeData = BitcoinACCTv1.getInstance().populateTradeData(repository, atData);

			if (crossChainTradeData.mode != AcctMode.OFFERING)
				throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_CRITERIA);

			// Does supplied public key match trade public key?
			if (!Crypto.toAddress(tradePublicKey).equals(crossChainTradeData.aquilaCreatorTradeAddress))
				throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_PUBLIC_KEY);

			TransactionData transactionData = repository.getTransactionRepository().fromSignature(tradeRequest.messageTransactionSignature);
			if (transactionData == null)
				throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.TRANSACTION_UNKNOWN);

			if (transactionData.getType() != TransactionType.MESSAGE)
				throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.TRANSACTION_INVALID);

			MessageTransactionData messageTransactionData = (MessageTransactionData) transactionData;
			byte[] messageData = messageTransactionData.getData();
			BitcoinACCTv1.OfferMessageData offerMessageData = BitcoinACCTv1.extractOfferMessageData(messageData);
			if (offerMessageData == null)
				throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.TRANSACTION_INVALID);

			// Good to make MESSAGE

			byte[] aliceForeignPublicKeyHash = offerMessageData.partnerBitcoinPKH;
			byte[] hashOfSecretA = offerMessageData.hashOfSecretA;
			int lockTimeA = (int) offerMessageData.lockTimeA;

			String aliceNativeAddress = Crypto.toAddress(messageTransactionData.getCreatorPublicKey());
			int lockTimeB = BitcoinACCTv1.calcLockTimeB(messageTransactionData.getTimestamp(), lockTimeA);

			byte[] outgoingMessageData = BitcoinACCTv1.buildTradeMessage(aliceNativeAddress, aliceForeignPublicKeyHash, hashOfSecretA, lockTimeA, lockTimeB);
			byte[] messageTransactionBytes = buildAtMessage(repository, tradePublicKey, tradeRequest.atAddress, outgoingMessageData);

			return Base58.encode(messageTransactionBytes);
		} catch (DataException e) {
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.REPOSITORY_ISSUE, e);
		}
	}

	@POST
	@Path("/redeemmessage")
	@Operation(
		summary = "Builds raw, unsigned 'redeem' MESSAGE transaction that sends secrets to AT, releasing funds to partner",
		description = "Specify address of cross-chain AT that needs to be messaged, both 32-byte secrets and an address for receiving UNCIA from AT.<br>"
			+ "AT needs to be in 'trade' mode. Messages sent to an AT in any other mode will be ignored, but still cost fees to send!<br>"
			+ "You need to sign output with account the AT considers the trade 'partner' otherwise the MESSAGE transaction will be invalid.",
		requestBody = @RequestBody(
			required = true,
			content = @Content(
				mediaType = MediaType.APPLICATION_JSON,
				schema = @Schema(
					implementation = CrossChainDualSecretRequest.class
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
	@ApiErrors({ApiError.INVALID_PUBLIC_KEY, ApiError.INVALID_ADDRESS, ApiError.INVALID_DATA, ApiError.INVALID_CRITERIA, ApiError.REPOSITORY_ISSUE})
	@SecurityRequirement(name = "apiKey")
	public String buildRedeemMessage(@HeaderParam(Security.API_KEY_HEADER) String apiKey, CrossChainDualSecretRequest secretRequest) {
		Security.checkApiCallAllowed(request);

		byte[] partnerPublicKey = secretRequest.partnerPublicKey;

		if (partnerPublicKey == null || partnerPublicKey.length != Transformer.PUBLIC_KEY_LENGTH)
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_PUBLIC_KEY);

		if (secretRequest.atAddress == null || !Crypto.isValidAtAddress(secretRequest.atAddress))
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_ADDRESS);

		if (secretRequest.secretA == null || secretRequest.secretA.length != BitcoinACCTv1.SECRET_LENGTH)
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_DATA);

		if (secretRequest.secretB == null || secretRequest.secretB.length != BitcoinACCTv1.SECRET_LENGTH)
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_DATA);

		if (secretRequest.receivingAddress == null || !Crypto.isValidAddress(secretRequest.receivingAddress))
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_ADDRESS);

		try (final Repository repository = RepositoryManager.getRepository()) {
			ATData atData = fetchAtDataWithChecking(repository, secretRequest.atAddress);
			CrossChainTradeData crossChainTradeData = BitcoinACCTv1.getInstance().populateTradeData(repository, atData);

			if (crossChainTradeData.mode != AcctMode.TRADING)
				throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_CRITERIA);

			String partnerAddress = Crypto.toAddress(partnerPublicKey);

			// MESSAGE must come from address that AT considers trade partner
			if (!crossChainTradeData.aquilaPartnerAddress.equals(partnerAddress))
				throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_ADDRESS);

			// Good to make MESSAGE

			byte[] messageData = BitcoinACCTv1.buildRedeemMessage(secretRequest.secretA, secretRequest.secretB, secretRequest.receivingAddress);
			byte[] messageTransactionBytes = buildAtMessage(repository, partnerPublicKey, secretRequest.atAddress, messageData);

			return Base58.encode(messageTransactionBytes);
		} catch (DataException e) {
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.REPOSITORY_ISSUE, e);
		}
	}

	private ATData fetchAtDataWithChecking(Repository repository, String atAddress) throws DataException {
		ATData atData = repository.getATRepository().fromATAddress(atAddress);
		if (atData == null)
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.ADDRESS_UNKNOWN);

		// Must be correct AT - check functionality using code hash
		if (!Arrays.equals(atData.getCodeHash(), BitcoinACCTv1.CODE_BYTES_HASH))
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_CRITERIA);

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

}
