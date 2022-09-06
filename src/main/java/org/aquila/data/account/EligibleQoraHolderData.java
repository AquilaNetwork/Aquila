package org.aquila.data.account;

public class EligibleQoraHolderData {

	// Properties

	private String address;

	private long qoraBalance;
	private long unciaFromQoraBalance;

	private Long finalUnciaFromQora;
	private Integer finalBlockHeight;

	// Constructors

	public EligibleQoraHolderData(String address, long qoraBalance, long unciaFromQoraBalance, Long finalUnciaFromQora,
			Integer finalBlockHeight) {
		this.address = address;
		this.qoraBalance = qoraBalance;
		this.unciaFromQoraBalance = unciaFromQoraBalance;
		this.finalUnciaFromQora = finalUnciaFromQora;
		this.finalBlockHeight = finalBlockHeight;
	}

	// Getters/Setters

	public String getAddress() {
		return this.address;
	}

	public long getQoraBalance() {
		return this.qoraBalance;
	}

	public long getUnciaFromQoraBalance() {
		return this.unciaFromQoraBalance;
	}

	public Long getFinalUnciaFromQora() {
		return this.finalUnciaFromQora;
	}

	public Integer getFinalBlockHeight() {
		return this.finalBlockHeight;
	}

}
