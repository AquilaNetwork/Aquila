package org.aquila.api.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import io.swagger.v3.oas.annotations.media.Schema;

@XmlAccessorType(XmlAccessType.FIELD)
public class CrossChainSecretRequest {

	@Schema(description = "Private key to match AT's trade 'partner'", example = "C6wuddsBV3HzRrXUtezE7P5MoRXp5m3mEDokRDGZB6ry")
	public byte[] partnerPrivateKey;

	@Schema(description = "Aquila AT address")
	public String atAddress;

	@Schema(description = "Secret (32 bytes)", example = "FHMzten4he9jZ4HGb4297Utj6F5g2w7serjq2EnAg2s1")
	public byte[] secret;

	@Schema(description = "Aquila address for receiving UNCIA from AT")
	public String receivingAddress;

	public CrossChainSecretRequest() {
	}

}
