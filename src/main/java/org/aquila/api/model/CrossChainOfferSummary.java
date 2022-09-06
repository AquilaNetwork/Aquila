package org.aquila.api.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.aquila.crosschain.AcctMode;
import org.aquila.data.crosschain.CrossChainTradeData;

import io.swagger.v3.oas.annotations.media.Schema;

// All properties to be converted to JSON via JAXB
@XmlAccessorType(XmlAccessType.FIELD)
public class CrossChainOfferSummary {

	// Properties

	@Schema(description = "AT's Aquila address")
	private String aquilaAtAddress;

	@Schema(description = "AT creator's Aquila address")
	private String aquilaCreator;

	@Schema(description = "AT creator's ephemeral trading key-pair represented as Aquila address")
	private String aquilaCreatorTradeAddress;

	@XmlJavaTypeAdapter(value = org.aquila.api.AmountTypeAdapter.class)
	private long unciaAmount;

	@Schema(description = "Bitcoin amount - DEPRECATED: use foreignAmount")
	@XmlJavaTypeAdapter(value = org.aquila.api.AmountTypeAdapter.class)
	@Deprecated
	private long btcAmount;

	@Schema(description = "Foreign blockchain amount")
	@XmlJavaTypeAdapter(value = org.aquila.api.AmountTypeAdapter.class)
	private long foreignAmount;

	@Schema(description = "Suggested trade timeout (minutes)", example = "10080")
	private int tradeTimeout;

	@Schema(description = "Current AT execution mode")
	private AcctMode mode;

	private long timestamp;

	@Schema(description = "Trade partner's Aquila receiving address")
	private String partnerAquilaReceivingAddress;

	private String foreignBlockchain;

	private String acctName;

	protected CrossChainOfferSummary() {
		/* For JAXB */
	}

	public CrossChainOfferSummary(CrossChainTradeData crossChainTradeData, long timestamp) {
		this.aquilaAtAddress = crossChainTradeData.aquilaAtAddress;
		this.aquilaCreator = crossChainTradeData.aquilaCreator;
		this.aquilaCreatorTradeAddress = crossChainTradeData.aquilaCreatorTradeAddress;
		this.unciaAmount = crossChainTradeData.unciaAmount;
		this.foreignAmount = crossChainTradeData.expectedForeignAmount;
		this.btcAmount = this.foreignAmount; // Duplicate for deprecated field
		this.tradeTimeout = crossChainTradeData.tradeTimeout;
		this.mode = crossChainTradeData.mode;
		this.timestamp = timestamp;
		this.partnerAquilaReceivingAddress = crossChainTradeData.aquilaPartnerReceivingAddress;
		this.foreignBlockchain = crossChainTradeData.foreignBlockchain;
		this.acctName = crossChainTradeData.acctName;
	}

	public String getAquilaAtAddress() {
		return this.aquilaAtAddress;
	}

	public String getAquilaCreator() {
		return this.aquilaCreator;
	}

	public String getAquilaCreatorTradeAddress() {
		return this.aquilaCreatorTradeAddress;
	}

	public long getUnciaAmount() {
		return this.unciaAmount;
	}

	public long getBtcAmount() {
		return this.btcAmount;
	}

	public long getForeignAmount() {
		return this.foreignAmount;
	}

	public int getTradeTimeout() {
		return this.tradeTimeout;
	}

	public AcctMode getMode() {
		return this.mode;
	}

	public long getTimestamp() {
		return this.timestamp;
	}

	public String getPartnerAquilaReceivingAddress() {
		return this.partnerAquilaReceivingAddress;
	}

	public String getForeignBlockchain() {
		return this.foreignBlockchain;
	}

	public String getAcctName() {
		return this.acctName;
	}

	// For debugging mostly

	public String toString() {
		return String.format("%s: %s", this.aquilaAtAddress, this.mode);
	}

}
