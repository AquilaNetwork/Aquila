package org.aquila.data.crosschain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.aquila.crosschain.AcctMode;

import io.swagger.v3.oas.annotations.media.Schema;

// All properties to be converted to JSON via JAXB
@XmlAccessorType(XmlAccessType.FIELD)
public class CrossChainTradeData {

	// Properties

	@Schema(description = "AT's Aquila address")
	public String aquilaAtAddress;

	@Schema(description = "AT creator's Aquila address")
	public String aquilaCreator;

	@Schema(description = "AT creator's ephemeral trading key-pair represented as Aquila address")
	public String aquilaCreatorTradeAddress;

	@Deprecated
	@Schema(description = "DEPRECATED: use creatorForeignPKH instead")
	public byte[] creatorBitcoinPKH;

	@Schema(description = "AT creator's foreign blockchain trade public-key-hash (PKH)")
	public byte[] creatorForeignPKH;

	@Schema(description = "Timestamp when AT was created (milliseconds since epoch)")
	public long creationTimestamp;

	@Schema(description = "Suggested trade timeout (minutes)", example = "10080")
	public int tradeTimeout;

	@Schema(description = "AT's current UNCIA balance")
	@XmlJavaTypeAdapter(value = org.aquila.api.AmountTypeAdapter.class)
	public long unciaBalance;

	@Schema(description = "HASH160 of 32-byte secret-A")
	public byte[] hashOfSecretA;

	@Schema(description = "HASH160 of 32-byte secret-B")
	public byte[] hashOfSecretB;

	@Schema(description = "Final UPP payment that will be sent to Aquila trade partner")
	@XmlJavaTypeAdapter(value = org.aquila.api.AmountTypeAdapter.class)
	public long unciaAmount;

	@Schema(description = "Trade partner's Aquila address (trade begins when this is set)")
	public String aquilaPartnerAddress;

	@Schema(description = "Timestamp when AT switched to trade mode")
	public Long tradeModeTimestamp;

	@Schema(description = "How long from AT creation until AT triggers automatic refund to AT creator (minutes)")
	public Integer refundTimeout;

	@Schema(description = "Actual Aquila block height when AT will automatically refund to AT creator (after trade begins)")
	public Integer tradeRefundHeight;

	@Deprecated
	@Schema(description = "DEPRECATED: use expectedForeignAmount instread")
	@XmlJavaTypeAdapter(value = org.aquila.api.AmountTypeAdapter.class)
	public long expectedBitcoin;

	@Schema(description = "Amount, in foreign blockchain currency, that AT creator expects trade partner to pay out (excluding miner fees)")
	@XmlJavaTypeAdapter(value = org.aquila.api.AmountTypeAdapter.class)
	public long expectedForeignAmount;

	@Schema(description = "Current AT execution mode")
	public AcctMode mode;

	@Schema(description = "Suggested P2SH-A nLockTime based on trade timeout")
	public Integer lockTimeA;

	@Schema(description = "Suggested P2SH-B nLockTime based on trade timeout")
	public Integer lockTimeB;

	@Deprecated
	@Schema(description = "DEPRECATED: use partnerForeignPKH instead")
	public byte[] partnerBitcoinPKH;

	@Schema(description = "Trade partner's foreign blockchain public-key-hash (PKH)")
	public byte[] partnerForeignPKH;

	@Schema(description = "Trade partner's Aquila receiving address")
	public String aquilaPartnerReceivingAddress;

	public String foreignBlockchain;

	public String acctName;

	@Schema(description = "Timestamp when AT creator's trade-bot presence expires")
	public Long creatorPresenceExpiry;

	@Schema(description = "Timestamp when trade partner's trade-bot presence expires")
	public Long partnerPresenceExpiry;

	// Constructors

	// Necessary for JAXB
	public CrossChainTradeData() {
	}

	public void duplicateDeprecated() {
		this.creatorBitcoinPKH = this.creatorForeignPKH;
		this.expectedBitcoin = this.expectedForeignAmount;
		this.partnerBitcoinPKH = this.partnerForeignPKH;
	}

}
