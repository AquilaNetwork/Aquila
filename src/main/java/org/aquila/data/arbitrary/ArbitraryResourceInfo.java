package org.aquila.data.arbitrary;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.aquila.arbitrary.misc.Service;

import java.util.Objects;

@XmlAccessorType(XmlAccessType.FIELD)
public class ArbitraryResourceInfo {

	public String name;
	public Service service;
	public String identifier;
	public ArbitraryResourceStatus status;
	public ArbitraryResourceMetadata metadata;

	public Long size;

	public ArbitraryResourceInfo() {
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;

		if (!(o instanceof ArbitraryResourceInfo))
			return false;

		ArbitraryResourceInfo other = (ArbitraryResourceInfo) o;

		return Objects.equals(this.name, other.name) &&
				Objects.equals(this.service, other.service) &&
				Objects.equals(this.identifier, other.identifier);
	}

}
