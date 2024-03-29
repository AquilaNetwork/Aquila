package org.aquila.test;

import org.aquila.crypto.MemoryPoW;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.Random;

@Ignore
public class MemoryPoWTests {

	private static final int workBufferLength = 8 * 1024 * 1024;

	@Test
	public void testCompute() {
		Random random = new Random();

		byte[] data = new byte[256];
		random.nextBytes(data);

		final int difficulty = 8;

		long startTime = System.currentTimeMillis();

		int nonce = MemoryPoW.compute2(data, workBufferLength, difficulty);

		long finishTime = System.currentTimeMillis();

		assertNotNull(nonce);

		System.out.println(String.format("Memory-hard PoW (buffer size: %dKB, leading zeros: %d) took %dms, nonce: %d", workBufferLength / 1024,
				difficulty,
				finishTime - startTime,
				nonce));

		assertTrue(MemoryPoW.verify2(data, workBufferLength, difficulty, nonce));
	}

	@Test
	public void testMultipleComputes() {
		Random random = new Random();

		final int sampleSize = 20;
		final long stddevDivisor = sampleSize * (sampleSize - 1);

		for (int difficulty = 8; difficulty < 16; difficulty += 2) {
			byte[] data = new byte[256];
			long[] times = new long[sampleSize];

			long timesS1 = 0;
			long timesS2 = 0;

			int maxNonce = 0;

			for (int i = 0; i < sampleSize; ++i) {
				random.nextBytes(data);

				final long startTime = System.currentTimeMillis();
				int nonce = MemoryPoW.compute2(data, workBufferLength, difficulty);
				times[i] = System.currentTimeMillis() - startTime;

				timesS1 += times[i];
				timesS2 += times[i] * times[i];

				if (nonce > maxNonce)
					maxNonce = nonce;
			}

			double stddev = (double) Math.sqrt( (sampleSize * timesS2 - timesS1 * timesS1) / stddevDivisor );

			System.out.println(String.format("Difficulty: %d, %d timings, mean: %d ms, stddev: %.2f ms, max nonce: %d",
					difficulty,
					sampleSize,
					timesS1 / sampleSize,
					stddev,
					maxNonce));
		}
	}

	@Test
	public void testKnownCompute2() {
		byte[] data = new byte[] { (byte) 0xaa, (byte) 0xbb, (byte) 0xcc };

		int difficulty = 8;
		int expectedNonce = 326;
		int nonce = MemoryPoW.compute2(data, workBufferLength, difficulty);

		System.out.println(String.format("Difficulty %d, nonce: %d", difficulty, nonce));
		assertEquals(expectedNonce, nonce);

		difficulty = 14;
		expectedNonce = 11032;
		nonce = MemoryPoW.compute2(data, workBufferLength, difficulty);

		System.out.println(String.format("Difficulty %d, nonce: %d", difficulty, nonce));
		assertEquals(expectedNonce, nonce);
	}

	@Test
	public void testKnownVerify() {
		byte[] data = new byte[] { (byte) 0xaa, (byte) 0xbb, (byte) 0xcc };

		int difficulty = 8;
		int expectedNonce = 326;
		assertTrue(MemoryPoW.verify2(data, workBufferLength, difficulty, expectedNonce));

		difficulty = 14;
		expectedNonce = 11032;
		assertTrue(MemoryPoW.verify2(data, workBufferLength, difficulty, expectedNonce));
	}

}
