package org.aquila.api.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.aquila.data.crosschain.CrossChainTradeData;

import io.swagger.v3.oas.annotations.media.Schema;

// All properties to be converted to JSON via JAXB
@XmlAccessorType(XmlAccessType.FIELD)
public class CrossChainTradeSummary {

	private long tradeTimestamp;

	@XmlJavaTypeAdapter(value = org.aquila.api.AmountTypeAdapter.class)
	private long unciaAmount;

	@Deprecated
	@Schema(description = "DEPRECATED: use foreignAmount instead")
	@XmlJavaTypeAdapter(value = org.aquila.api.AmountTypeAdapter.class)
	private long btcAmount;

	@XmlJavaTypeAdapter(value = org.aquila.api.AmountTypeAdapter.class)
	private long foreignAmount;

	private String atAddress;

	private String sellerAddress;

	private String buyerReceivingAddress;

	protected CrossChainTradeSummary() {
		/* For JAXB */
	}

	public CrossChainTradeSummary(CrossChainTradeData crossChainTradeData, long timestamp) {
		this.tradeTimestamp = timestamp;
		this.unciaAmount = crossChainTradeData.unciaAmount;
		this.foreignAmount = crossChainTradeData.expectedForeignAmount;
		this.btcAmount = this.foreignAmount;
		this.sellerAddress = crossChainTradeData.aquilaCreator;
		this.buyerReceivingAddress = crossChainTradeData.aquilaPartnerReceivingAddress;
		this.atAddress = crossChainTradeData.aquilaAtAddress;
	}

	public long getTradeTimestamp() {
		return this.tradeTimestamp;
	}

	public long getUnciaAmount() {
		return this.unciaAmount;
	}

	public long getBtcAmount() {
		return this.btcAmount;
	}

	public long getForeignAmount() { return this.foreignAmount; }

	public String getAtAddress() { return this.atAddress; }

	public String getSellerAddress() { return this.sellerAddress; }

	public String getBuyerReceivingAddressAddress() { return this.buyerReceivingAddress; }
}
