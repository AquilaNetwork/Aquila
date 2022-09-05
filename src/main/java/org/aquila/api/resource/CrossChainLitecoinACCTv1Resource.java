package org.aquila.api.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.aquila.account.PrivateKeyAccount;
import org.aquila.api.ApiError;
import org.aquila.api.ApiErrors;
import org.aquila.api.ApiExceptionFactory;
import org.aquila.api.Security;
import org.aquila.api.model.CrossChainSecretRequest;
import org.aquila.crosschain.AcctMode;
import org.aquila.crosschain.LitecoinACCTv1;
import org.aquila.crypto.Crypto;
import org.aquila.data.at.ATData;
import org.aquila.data.crosschain.CrossChainTradeData;
import org.aquila.group.Group;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.repository.RepositoryManager;
import org.aquila.transaction.MessageTransaction;
import org.aquila.transaction.Transaction.ValidationResult;
import org.aquila.transform.TransformationException;
import org.aquila.transform.Transformer;
import org.aquila.transform.transaction.MessageTransactionTransformer;
import org.aquila.utils.Base58;
import org.aquila.utils.NTP;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.Random;

@Path("/crosschain/LitecoinACCTv1")
@Tag(name = "Cross-Chain (LitecoinACCTv1)")
public class CrossChainLitecoinACCTv1Resource {

	@Context
	HttpServletRequest request;

	@POST
	@Path("/redeemmessage")
	@Operation(
		summary = "Signs and broadcasts a 'redeem' MESSAGE transaction that sends secrets to AT, releasing funds to partner",
		description = "Specify address of cross-chain AT that needs to be messaged, Alice's trade private key, the 32-byte secret,<br>"
			+ "and an address for receiving QORT from AT. All of these can be found in Alice's trade bot data.<br>"
			+ "AT needs to be in 'trade' mode. Messages sent to an AT in any other mode will be ignored, but still cost fees to send!<br>"
			+ "You need to use the private key that the AT considers the trade 'partner' otherwise the MESSAGE transaction will be invalid.",
		requestBody = @RequestBody(
			required = true,
			content = @Content(
				mediaType = MediaType.APPLICATION_JSON,
				schema = @Schema(
					implementation = CrossChainSecretRequest.class
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
	public boolean buildRedeemMessage(@HeaderParam(Security.API_KEY_HEADER) String apiKey, CrossChainSecretRequest secretRequest) {
		Security.checkApiCallAllowed(request);

		byte[] partnerPrivateKey = secretRequest.partnerPrivateKey;

		if (partnerPrivateKey == null || partnerPrivateKey.length != Transformer.PRIVATE_KEY_LENGTH)
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_PRIVATE_KEY);

		if (secretRequest.atAddress == null || !Crypto.isValidAtAddress(secretRequest.atAddress))
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_ADDRESS);

		if (secretRequest.secret == null || secretRequest.secret.length != LitecoinACCTv1.SECRET_LENGTH)
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_DATA);

		if (secretRequest.receivingAddress == null || !Crypto.isValidAddress(secretRequest.receivingAddress))
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_ADDRESS);

		try (final Repository repository = RepositoryManager.getRepository()) {
			ATData atData = fetchAtDataWithChecking(repository, secretRequest.atAddress);
			CrossChainTradeData crossChainTradeData = LitecoinACCTv1.getInstance().populateTradeData(repository, atData);

			if (crossChainTradeData.mode != AcctMode.TRADING)
				throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_CRITERIA);

			byte[] partnerPublicKey = new PrivateKeyAccount(null, partnerPrivateKey).getPublicKey();
			String partnerAddress = Crypto.toAddress(partnerPublicKey);

			// MESSAGE must come from address that AT considers trade partner
			if (!crossChainTradeData.aquilaPartnerAddress.equals(partnerAddress))
				throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_ADDRESS);

			// Good to make MESSAGE

			byte[] messageData = LitecoinACCTv1.buildRedeemMessage(secretRequest.secret, secretRequest.receivingAddress);

			PrivateKeyAccount sender = new PrivateKeyAccount(repository, partnerPrivateKey);
			MessageTransaction messageTransaction = MessageTransaction.build(repository, sender, Group.NO_GROUP, secretRequest.atAddress, messageData, false, false);

			messageTransaction.computeNonce();
			messageTransaction.sign(sender);

			// reset repository state to prevent deadlock
			repository.discardChanges();
			ValidationResult result = messageTransaction.importAsUnconfirmed();

			if (result != ValidationResult.OK)
				throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.TRANSACTION_INVALID);

			return true;
		} catch (DataException e) {
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.REPOSITORY_ISSUE, e);
		}
	}

	private ATData fetchAtDataWithChecking(Repository repository, String atAddress) throws DataException {
		ATData atData = repository.getATRepository().fromATAddress(atAddress);
		if (atData == null)
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.ADDRESS_UNKNOWN);

		// Must be correct AT - check functionality using code hash
		if (!Arrays.equals(atData.getCodeHash(), LitecoinACCTv1.CODE_BYTES_HASH))
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_CRITERIA);

		// No point sending message to AT that's finished
		if (atData.getIsFinished())
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.INVALID_CRITERIA);

		return atData;
	}

}
