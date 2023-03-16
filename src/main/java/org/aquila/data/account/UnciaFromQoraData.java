package org.aquila.data.account;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

// All properties to be converted to JSON via JAXB
@XmlAccessorType(XmlAccessType.FIELD)
public class UnciaFromQoraData {

	// Properties

	private String address;

	// Not always present
	@XmlJavaTypeAdapter(value = org.aquila.api.AmountTypeAdapter.class)
	private Long finalUnciaFromQora;

	// Not always present
	private Integer finalBlockHeight;

	// Constructors

	// necessary for JAXB
	protected UnciaFromQoraData() {
	}

	public UnciaFromQoraData(String address, Long finalUnciaFromQora, Integer finalBlockHeight) {
		this.address = address;
		this.finalUnciaFromQora = finalUnciaFromQora;
		this.finalBlockHeight = finalBlockHeight;
	}

	// Getters/Setters

	public String getAddress() {
		return this.address;
	}

	public Long getFinalUnciaFromQora() {
		return this.finalUnciaFromQora;
	}

	public void setFinalUnciaFromQora(Long finalUnciaFromQora) {
		this.finalUnciaFromQora = finalUnciaFromQora;
	}

	public Integer getFinalBlockHeight() {
		return this.finalBlockHeight;
	}

	public void setFinalBlockHeight(Integer finalBlockHeight) {
		this.finalBlockHeight = finalBlockHeight;
	}

}
