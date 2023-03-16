package org.aquila.test;

import static org.junit.Assert.*;

import org.aquila.utils.Amounts;
import org.junit.Test;

public class AmountsTests {

	@Test
	public void testPrettyAmount() {
		testPrettyAmount(1L, "0.00000001");
		testPrettyAmount(100000L, "0.00100000");
		testPrettyAmount(100000000L, "1.00000000");
		testPrettyAmount(1000000000L, "10.00000000");
	}

	private void testPrettyAmount(long amount, String prettyAmount) {
		assertEquals(prettyAmount, Amounts.prettyAmount(amount));
	}

}
