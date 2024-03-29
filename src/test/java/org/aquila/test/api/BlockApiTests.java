package org.aquila.test.api;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.aquila.account.PrivateKeyAccount;
import org.aquila.api.ApiError;
import org.aquila.api.resource.BlocksResource;
import org.aquila.block.GenesisBlock;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.repository.RepositoryManager;
import org.aquila.utils.Base58;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.aquila.test.common.ApiCommon;
import org.aquila.test.common.BlockUtils;
import org.aquila.test.common.Common;

public class BlockApiTests extends ApiCommon {

	private BlocksResource blocksResource;

	@Before
	public void buildResource() {
		this.blocksResource = (BlocksResource) ApiCommon.buildResource(BlocksResource.class);
	}

	@Test
	public void testResource() {
		assertNotNull(this.blocksResource);
	}

	@Test
	public void testGetBlock() throws DataException {
		try (final Repository repository = RepositoryManager.getRepository()) {
			byte[] signatureBytes = GenesisBlock.getInstance(repository).getSignature();
			String signature = Base58.encode(signatureBytes);

			assertNotNull(this.blocksResource.getBlock(signature, true));
		}
	}

	@Test
	public void testGetBlockTransactions() throws DataException {
		try (final Repository repository = RepositoryManager.getRepository()) {
			byte[] signatureBytes = GenesisBlock.getInstance(repository).getSignature();
			String signature = Base58.encode(signatureBytes);

			assertNotNull(this.blocksResource.getBlockTransactions(signature, null, null, null));
			assertNotNull(this.blocksResource.getBlockTransactions(signature, 1, 1, true));
		}
	}

	@Test
	public void testGetHeight() {
		assertNotNull(this.blocksResource.getHeight());
	}

	@Test
	public void testGetBlockHeight() throws DataException {
		try (final Repository repository = RepositoryManager.getRepository()) {
			byte[] signatureBytes = GenesisBlock.getInstance(repository).getSignature();
			String signature = Base58.encode(signatureBytes);

			assertNotNull(this.blocksResource.getHeight(signature));
		}
	}

	@Test
	public void testGetBlockByHeight() {
		assertNotNull(this.blocksResource.getByHeight(1, true));
	}

	@Test
	@Ignore(value = "Doesn't work, to be fixed later")
	public void testGetBlockByTimestamp() throws DataException {
		assertNotNull(this.blocksResource.getByTimestamp(System.currentTimeMillis(), false));
	}

	@Test
	public void testGetBlockRange() {
		assertNotNull(this.blocksResource.getBlockRange(1, 1, false, false));

		List<Integer> testValues = Arrays.asList(null, Integer.valueOf(1));

		for (Integer startHeight : testValues)
			for (Integer endHeight : testValues)
				for (Integer count : testValues) {
					if (startHeight != null && endHeight != null && count != null) {
						assertApiError(ApiError.INVALID_CRITERIA, () -> this.blocksResource.getBlockSummaries(startHeight, endHeight, count));
						continue;
					}

					assertNotNull(this.blocksResource.getBlockSummaries(startHeight, endHeight, count));
				}
	}

	@Test
	public void testGetBlockSigners() throws DataException {
		try (final Repository repository = RepositoryManager.getRepository()) {
			PrivateKeyAccount mintingAccount = Common.getTestAccount(repository, "alice-reward-share");
			BlockUtils.mintBlock(repository);

			List<String> addresses = Arrays.asList(aliceAddress, mintingAccount.getAddress(), bobAddress);

			assertNotNull(this.blocksResource.getBlockSigners(Collections.emptyList(), null, null, null));
			assertNotNull(this.blocksResource.getBlockSigners(addresses, null, null, null));
			assertNotNull(this.blocksResource.getBlockSigners(Collections.emptyList(), 1, 1, true));
			assertNotNull(this.blocksResource.getBlockSigners(addresses, 1, 1, true));
		}
	}

	@Test
	public void testGetBlockSummariesBySigner() throws DataException {
		try (final Repository repository = RepositoryManager.getRepository()) {
			BlockUtils.mintBlock(repository);

			assertNotNull(this.blocksResource.getBlockSummariesBySigner(aliceAddress, null, null, null));
			assertNotNull(this.blocksResource.getBlockSummariesBySigner(aliceAddress, 1, 1, true));
		}
	}

}
