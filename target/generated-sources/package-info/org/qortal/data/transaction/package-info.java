// Only edit org/aquila/data/package-info.java
// Other package-info.java files are generated using above file

@XmlJavaTypeAdapters({
	@XmlJavaTypeAdapter(
		type = byte[].class,
		value = org.aquila.api.Base58TypeAdapter.class
	), @XmlJavaTypeAdapter(
		type = java.math.BigDecimal.class,
		value = org.aquila.api.BigDecimalTypeAdapter.class
	)
})
package org.aquila.data.transaction;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
