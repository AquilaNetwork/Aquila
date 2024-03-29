package org.aquila.data.network;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.aquila.account.PublicKeyAccount;

// All properties to be converted to JSON via JAXB
@XmlAccessorType(XmlAccessType.FIELD)
public class OnlineAccountData {

	protected long timestamp;
	protected byte[] signature;
	protected byte[] publicKey;
	protected Integer nonce;

	@XmlTransient
	private int hash;

	// Constructors

	// necessary for JAXB serialization
	protected OnlineAccountData() {
	}

	public OnlineAccountData(long timestamp, byte[] signature, byte[] publicKey, Integer nonce) {
		this.timestamp = timestamp;
		this.signature = signature;
		this.publicKey = publicKey;
		this.nonce = nonce;
	}

	public OnlineAccountData(long timestamp, byte[] signature, byte[] publicKey) {
		this(timestamp, signature, publicKey, null);
	}

	public long getTimestamp() {
		return this.timestamp;
	}

	public byte[] getSignature() {
		return this.signature;
	}

	public byte[] getPublicKey() {
		return this.publicKey;
	}

	public Integer getNonce() {
		return this.nonce;
	}

	// For JAXB
	@XmlElement(name = "address")
	protected String getAddress() {
		return new PublicKeyAccount(null, this.publicKey).getAddress();
	}

	// Comparison

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;

		if (!(other instanceof OnlineAccountData))
			return false;

		OnlineAccountData otherOnlineAccountData = (OnlineAccountData) other;

		// Very quick comparison
		if (otherOnlineAccountData.timestamp != this.timestamp)
			return false;

		if (!Arrays.equals(otherOnlineAccountData.publicKey, this.publicKey))
			return false;

		// We don't compare signature because it's not our remit to verify and newer aggregate signatures use random nonces

		return true;
	}

	@Override
	public int hashCode() {
		int h = this.hash;
		if (h == 0) {
			this.hash = h = Long.hashCode(this.timestamp)
					^ Arrays.hashCode(this.publicKey);
			// We don't use signature because newer aggregate signatures use random nonces
		}
		return h;
	}

}
