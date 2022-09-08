package org.aquila.test.api;

import static org.junit.Assert.*;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.aquila.api.resource.AdminResource;
import org.aquila.repository.DataException;
import org.aquila.settings.Settings;
import org.aquila.test.common.ApiCommon;
import org.aquila.test.common.Common;

public class AdminApiTests extends ApiCommon {

	private AdminResource adminResource;

	@Before
	public void beforeTest() throws DataException {
		Common.useDefaultSettings();
	}

	@Before
	public void buildResource() {
		this.adminResource = (AdminResource) ApiCommon.buildResource(AdminResource.class);
	}

	@Test
	public void testInfo() {
		assertNotNull(this.adminResource.info());
	}

	@Test
	public void testSummary() throws IllegalAccessException {
		// Set localAuthBypassEnabled to true, since we don't need to test authentication here
		FieldUtils.writeField(Settings.getInstance(), "localAuthBypassEnabled", true, true);

		assertNotNull(this.adminResource.summary("testApiKey"));
	}

	@Test
	public void testGetMintingAccounts() {
		assertNotNull(this.adminResource.getMintingAccounts());
	}

}
