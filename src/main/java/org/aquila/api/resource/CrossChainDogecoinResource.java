package org.aquila.api.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.aquila.api.ApiError;
import org.aquila.api.ApiErrors;
import org.aquila.api.ApiExceptionFactory;
import org.aquila.api.Security;
import org.aquila.api.model.crosschain.DogecoinSendRequest;
import org.aquila.crosschain.Dogecoin;
import org.aquila.crosschain.ForeignBlockchainException;
import org.aquila.crosschain.SimpleTransaction;
import org.bitcoinj.core.Transaction;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/crosschain/doge")
@Tag(name = "Cross-Chain (Dogecoin)")
public class CrossChainDogecoinResource {

	@Context
	HttpServletRequest request;

	@GET
	@Path("/height")
	@Operation(
		summary = "Returns current Dogecoin block height",
		description = "Returns the height of the most recent block in the Dogecoin chain.",
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
	@ApiErrors({ApiError.FOREIGN_BLOCKCHAIN_NETWORK_ISSUE})
	public String getDogecoinHeight() {
		Dogecoin dogecoin = Dogecoin.getInstance();

		try {
			Integer height = dogecoin.getBlockchainHeight();
			if (height == null)
				throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.FOREIGN_BLOCKCHAIN_NETWORK_ISSUE);

			return height.toString();

		} catch (ForeignBlockchainException e) {
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.FOREIGN_BLOCKCHAIN_NETWORK_ISSUE);
		}
	}

	@POST
	@Path("/walletbalance")
	@Operation(
		summary = "Returns DOGE balance for hierarchical, deterministic BIP32 wallet",
		description = "Supply BIP32 'm' private/public key in base58, starting with 'xprv'/'xpub' for mainnet, 'tprv'/'tpub' for testnet",
		requestBody = @RequestBody(
			required = true,
			content = @Content(
				mediaType = MediaType.TEXT_PLAIN,
				schema = @Schema(
					type = "string",
					description = "BIP32 'm' private/public key in base58",
					example = "tpubD6NzVbkrYhZ4XTPc4btCZ6SMgn8CxmWkj6VBVZ1tfcJfMq4UwAjZbG8U74gGSypL9XBYk2R2BLbDBe8pcEyBKM1edsGQEPKXNbEskZozeZc"
				)
			)
		),
		responses = {
			@ApiResponse(
				content = @Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(type = "string", description = "balance (satoshis)"))
			)
		}
	)
	@ApiErrors({ApiError.INVALID_PRIVATE_KEY, ApiError.FOREIGN_BLOCKCHAIN_NETWORK_ISSUE})
	@SecurityRequirement(name = "apiKey")
	public String getDogecoinWalletBalance(@HeaderParam(Security.API_KEY_HEADER) String apiKey, String key58) {
		Security.checkApiCallAllowed(request);

		Dogecoin dogecoin = Dogecoin.getInstance();

		if (!dogecoin.isValidDeterministicKey(key58))
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_PRIVATE_KEY);

		try {
			Long balance = dogecoin.getWalletBalance(key58);
			if (balance == null)
				throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.FOREIGN_BLOCKCHAIN_NETWORK_ISSUE);

			return balance.toString();

		} catch (ForeignBlockchainException e) {
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.FOREIGN_BLOCKCHAIN_NETWORK_ISSUE);
		}
	}

	@POST
	@Path("/wallettransactions")
	@Operation(
		summary = "Returns transactions for hierarchical, deterministic BIP32 wallet",
		description = "Supply BIP32 'm' private/public key in base58, starting with 'xprv'/'xpub' for mainnet, 'tprv'/'tpub' for testnet",
		requestBody = @RequestBody(
			required = true,
			content = @Content(
				mediaType = MediaType.TEXT_PLAIN,
				schema = @Schema(
					type = "string",
					description = "BIP32 'm' private/public key in base58",
					example = "tpubD6NzVbkrYhZ4XTPc4btCZ6SMgn8CxmWkj6VBVZ1tfcJfMq4UwAjZbG8U74gGSypL9XBYk2R2BLbDBe8pcEyBKM1edsGQEPKXNbEskZozeZc"
				)
			)
		),
		responses = {
			@ApiResponse(
				content = @Content(array = @ArraySchema( schema = @Schema( implementation = SimpleTransaction.class ) ) )
			)
		}
	)
	@ApiErrors({ApiError.INVALID_PRIVATE_KEY, ApiError.FOREIGN_BLOCKCHAIN_NETWORK_ISSUE})
	@SecurityRequirement(name = "apiKey")
	public List<SimpleTransaction> getDogecoinWalletTransactions(@HeaderParam(Security.API_KEY_HEADER) String apiKey, String key58) {
		Security.checkApiCallAllowed(request);

		Dogecoin dogecoin = Dogecoin.getInstance();

		if (!dogecoin.isValidDeterministicKey(key58))
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_PRIVATE_KEY);

		try {
			return dogecoin.getWalletTransactions(key58);
		} catch (ForeignBlockchainException e) {
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.FOREIGN_BLOCKCHAIN_NETWORK_ISSUE);
		}
	}

	@POST
	@Path("/unusedaddress")
	@Operation(
		summary = "Returns first unused address for hierarchical, deterministic BIP32 wallet",
		description = "Supply BIP32 'm' private/public key in base58, starting with 'xprv'/'xpub' for mainnet, 'tprv'/'tpub' for testnet",
		requestBody = @RequestBody(
			required = true,
			content = @Content(
				mediaType = MediaType.TEXT_PLAIN,
				schema = @Schema(
					type = "string",
					description = "BIP32 'm' private/public key in base58",
					example = "tpubD6NzVbkrYhZ4XTPc4btCZ6SMgn8CxmWkj6VBVZ1tfcJfMq4UwAjZbG8U74gGSypL9XBYk2R2BLbDBe8pcEyBKM1edsGQEPKXNbEskZozeZc"
				)
			)
		),
		responses = {
			@ApiResponse(
				content = @Content(array = @ArraySchema( schema = @Schema( implementation = SimpleTransaction.class ) ) )
			)
		}
	)
	@ApiErrors({ApiError.INVALID_PRIVATE_KEY, ApiError.FOREIGN_BLOCKCHAIN_NETWORK_ISSUE})
	@SecurityRequirement(name = "apiKey")
	public String getUnusedDogecoinReceiveAddress(@HeaderParam(Security.API_KEY_HEADER) String apiKey, String key58) {
		Security.checkApiCallAllowed(request);

		Dogecoin dogecoin = Dogecoin.getInstance();

		if (!dogecoin.isValidDeterministicKey(key58))
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_PRIVATE_KEY);

		try {
			return dogecoin.getUnusedReceiveAddress(key58);
		} catch (ForeignBlockchainException e) {
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.FOREIGN_BLOCKCHAIN_NETWORK_ISSUE);
		}
	}

	@POST
	@Path("/send")
	@Operation(
		summary = "Sends DOGE from hierarchical, deterministic BIP32 wallet to specific address",
		description = "Currently only supports 'legacy' P2PKH Dogecoin addresses. Supply BIP32 'm' private key in base58, starting with 'xprv' for mainnet, 'tprv' for testnet",
		requestBody = @RequestBody(
			required = true,
			content = @Content(
				mediaType = MediaType.APPLICATION_JSON,
				schema = @Schema(
					implementation = DogecoinSendRequest.class
				)
			)
		),
		responses = {
			@ApiResponse(
				content = @Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(type = "string", description = "transaction hash"))
			)
		}
	)
	@ApiErrors({ApiError.INVALID_PRIVATE_KEY, ApiError.INVALID_CRITERIA, ApiError.INVALID_ADDRESS, ApiError.FOREIGN_BLOCKCHAIN_BALANCE_ISSUE, ApiError.FOREIGN_BLOCKCHAIN_NETWORK_ISSUE})
	@SecurityRequirement(name = "apiKey")
	public String sendBitcoin(@HeaderParam(Security.API_KEY_HEADER) String apiKey, DogecoinSendRequest dogecoinSendRequest) {
		Security.checkApiCallAllowed(request);

		if (dogecoinSendRequest.dogecoinAmount <= 0)
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_CRITERIA);

		if (dogecoinSendRequest.feePerByte != null && dogecoinSendRequest.feePerByte <= 0)
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_CRITERIA);

		Dogecoin dogecoin = Dogecoin.getInstance();

		if (!dogecoin.isValidAddress(dogecoinSendRequest.receivingAddress))
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_ADDRESS);

		if (!dogecoin.isValidDeterministicKey(dogecoinSendRequest.xprv58))
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_PRIVATE_KEY);

		Transaction spendTransaction = dogecoin.buildSpend(dogecoinSendRequest.xprv58,
				dogecoinSendRequest.receivingAddress,
				dogecoinSendRequest.dogecoinAmount,
				dogecoinSendRequest.feePerByte);

		if (spendTransaction == null)
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.FOREIGN_BLOCKCHAIN_BALANCE_ISSUE);

		try {
			dogecoin.broadcastTransaction(spendTransaction);
		} catch (ForeignBlockchainException e) {
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.FOREIGN_BLOCKCHAIN_NETWORK_ISSUE);
		}

		return spendTransaction.getTxId().toString();
	}

}
