package org.aquila.api.model.crosschain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.aquila.crosschain.SupportedBlockchain;

import io.swagger.v3.oas.annotations.media.Schema;

@XmlAccessorType(XmlAccessType.FIELD)
public class TradeBotCreateRequest {

	@Schema(description = "Trade creator's public key", example = "2zR1WFsbM7akHghqSCYKBPk6LDP8aKiQSRS1FrwoLvoB")
	public byte[] creatorPublicKey;

	@Schema(description = "UNCIA amount paid out on successful trade", example = "80.40000000", type = "number")
	@XmlJavaTypeAdapter(value = org.aquila.api.AmountTypeAdapter.class)
	public long unciaAmount;

	@Schema(description = "UNCIA amount funding AT, including covering AT execution fees", example = "80.50000000", type = "number")
	@XmlJavaTypeAdapter(value = org.aquila.api.AmountTypeAdapter.class)
	public long fundingUnciaAmount;

	@Deprecated
	@Schema(description = "Bitcoin amount wanted in return. DEPRECATED: use foreignAmount instead", example = "0.00864200", type = "number", hidden = true)
	@XmlJavaTypeAdapter(value = org.aquila.api.AmountTypeAdapter.class)
	public Long bitcoinAmount;

	@Schema(description = "Foreign blockchain. Note: default (BITCOIN) to be removed in the future", example = "BITCOIN", implementation = SupportedBlockchain.class)
	public SupportedBlockchain foreignBlockchain;

	@Schema(description = "Foreign blockchain amount wanted in return", example = "0.00864200", type = "number")
	@XmlJavaTypeAdapter(value = org.aquila.api.AmountTypeAdapter.class)
	public Long foreignAmount;

	@Schema(description = "Suggested trade timeout (minutes)", example = "10080")
	public int tradeTimeout;

	@Schema(description = "Foreign blockchain address for receiving", example = "1BitcoinEaterAddressDontSendf59kuE")
	public String receivingAddress;

	public TradeBotCreateRequest() {
	}

}
