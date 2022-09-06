package org.aquila.data.arbitrary;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class ArbitraryResourceNameInfo {

	public String name;
	public List<ArbitraryResourceInfo> resources = new ArrayList<>();

	public ArbitraryResourceNameInfo() {
	}

}
