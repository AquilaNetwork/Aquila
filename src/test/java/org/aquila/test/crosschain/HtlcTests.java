package org.aquila.test.crosschain;

import static org.junit.Assert.*;

import org.aquila.crosschain.Bitcoin;
import org.aquila.crosschain.BitcoinyHTLC;
import org.aquila.crosschain.ForeignBlockchainException;
import org.aquila.crypto.Crypto;
import org.aquila.repository.DataException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.aquila.test.common.Common;

import com.google.common.primitives.Longs;

public class HtlcTests extends Common {

	private Bitcoin bitcoin;

	@Before
	public void beforeTest() throws DataException {
		Common.useDefaultSettings(); // TestNet3
		bitcoin = Bitcoin.getInstance();
	}

	@After
	public void afterTest() {
		Bitcoin.resetForTesting();
		bitcoin = null;
	}

	@Test
	public void testFindHtlcSecret() throws ForeignBlockchainException {
		// This actually exists on TEST3 but can take a while to fetch
		String p2shAddress = "2N8WCg52ULCtDSMjkgVTm5mtPdCsUptkHWE";

		byte[] expectedSecret = "This string is exactly 32 bytes!".getBytes();
		byte[] secret = BitcoinyHTLC.findHtlcSecret(bitcoin, p2shAddress);

		assertNotNull(secret);
		assertArrayEquals("secret incorrect", expectedSecret, secret);
	}

	@Test
	@Ignore(value = "Doesn't work, to be fixed later")
	public void testHtlcSecretCaching() throws ForeignBlockchainException {
		String p2shAddress = "2N8WCg52ULCtDSMjkgVTm5mtPdCsUptkHWE";
		byte[] expectedSecret = "This string is exactly 32 bytes!".getBytes();

		do {
			// We need to perform fresh setup for 1st test
			Bitcoin.resetForTesting();
			bitcoin = Bitcoin.getInstance();

			long now = System.currentTimeMillis();
			long timestampBoundary = now / 30_000L;

			byte[] secret1 = BitcoinyHTLC.findHtlcSecret(bitcoin, p2shAddress);
			long executionPeriod1 = System.currentTimeMillis() - now;

			assertNotNull(secret1);
			assertArrayEquals("secret1 incorrect", expectedSecret, secret1);

			assertTrue("1st execution period should not be instant!", executionPeriod1 > 10);

			byte[] secret2 = BitcoinyHTLC.findHtlcSecret(bitcoin, p2shAddress);
			long executionPeriod2 = System.currentTimeMillis() - now - executionPeriod1;

			assertNotNull(secret2);
			assertArrayEquals("secret2 incorrect", expectedSecret, secret2);

			// Test is only valid if we've called within same timestampBoundary
			if (System.currentTimeMillis() / 30_000L != timestampBoundary)
				continue;

			assertArrayEquals(secret1, secret2);

			assertTrue("2st execution period should be effectively instant!", executionPeriod2 < 10);
		} while (false);
	}

	@Test
	public void testDetermineHtlcStatus() throws ForeignBlockchainException {
		// This actually exists on TEST3 but can take a while to fetch
		String p2shAddress = "2N8WCg52ULCtDSMjkgVTm5mtPdCsUptkHWE";

		BitcoinyHTLC.Status htlcStatus = BitcoinyHTLC.determineHtlcStatus(bitcoin.getBlockchainProvider(), p2shAddress, 1L);
		assertNotNull(htlcStatus);

		System.out.println(String.format("HTLC %s status: %s", p2shAddress, htlcStatus.name()));
	}

	@Test
	public void testHtlcStatusCaching() throws ForeignBlockchainException {
		do {
			// We need to perform fresh setup for 1st test
			Bitcoin.resetForTesting();
			bitcoin = Bitcoin.getInstance();

			long now = System.currentTimeMillis();
			long timestampBoundary = now / 30_000L;

			// Won't ever exist
			String p2shAddress = bitcoin.deriveP2shAddress(Crypto.hash160(Longs.toByteArray(now)));

			BitcoinyHTLC.Status htlcStatus1 = BitcoinyHTLC.determineHtlcStatus(bitcoin.getBlockchainProvider(), p2shAddress, 1L);
			long executionPeriod1 = System.currentTimeMillis() - now;

			assertNotNull(htlcStatus1);
			assertTrue("1st execution period should not be instant!", executionPeriod1 > 10);

			BitcoinyHTLC.Status htlcStatus2 = BitcoinyHTLC.determineHtlcStatus(bitcoin.getBlockchainProvider(), p2shAddress, 1L);
			long executionPeriod2 = System.currentTimeMillis() - now - executionPeriod1;

			assertNotNull(htlcStatus2);
			assertEquals(htlcStatus1, htlcStatus2);

			// Test is only valid if we've called within same timestampBoundary
			if (System.currentTimeMillis() / 30_000L != timestampBoundary)
				continue;

			assertTrue("2st execution period should be effectively instant!", executionPeriod2 < 10);
		} while (false);
	}

}
