package org.aquila.api;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.http.HttpServletRequest;

import org.aquila.arbitrary.ArbitraryDataResource;
import org.aquila.arbitrary.misc.Service;
import org.aquila.controller.arbitrary.ArbitraryDataRenderManager;
import org.aquila.settings.Settings;

public abstract class Security {

	public static final String API_KEY_HEADER = "X-API-KEY";

	public static void checkApiCallAllowed(HttpServletRequest request) {
		// We may want to allow automatic authentication for local requests, if enabled in settings
		boolean localAuthBypassEnabled = Settings.getInstance().isLocalAuthBypassEnabled();
		if (localAuthBypassEnabled) {
			try {
				InetAddress remoteAddr = InetAddress.getByName(request.getRemoteAddr());
				if (remoteAddr.isLoopbackAddress()) {
					// Request originates from loopback address, so allow it
					return;
				}
			} catch (UnknownHostException e) {
				// Ignore failure, and fallback to API key authentication
			}
		}

		// Retrieve the API key
		ApiKey apiKey = Security.getApiKey(request);
		if (!apiKey.generated()) {
			// Not generated an API key yet, so disallow sensitive API calls
			throw ApiExceptionFactory.INSTANCE.createCustomException(request, ApiError.UNAUTHORIZED, "API key not generated");
		}

		// We require an API key to be passed
		String passedApiKey = request.getHeader(API_KEY_HEADER);
		if (passedApiKey == null) {
			// Try query string - this is needed to avoid a CORS preflight. See: https://stackoverflow.com/a/43881141
			passedApiKey = request.getParameter("apiKey");
		}
		if (passedApiKey == null) {
			throw ApiExceptionFactory.INSTANCE.createCustomException(request, ApiError.UNAUTHORIZED, "Missing 'X-API-KEY' header");
		}

		// The API keys must match
		if (!apiKey.toString().equals(passedApiKey)) {
			throw ApiExceptionFactory.INSTANCE.createCustomException(request, ApiError.UNAUTHORIZED, "API key invalid");
		}
	}

	public static void disallowLoopbackRequests(HttpServletRequest request) {
		try {
			InetAddress remoteAddr = InetAddress.getByName(request.getRemoteAddr());
			if (remoteAddr.isLoopbackAddress()) {
				throw ApiExceptionFactory.INSTANCE.createCustomException(request, ApiError.UNAUTHORIZED, "Local requests not allowed");
			}
		} catch (UnknownHostException e) {
			throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.UNAUTHORIZED);
		}
	}

	public static void disallowLoopbackRequestsIfAuthBypassEnabled(HttpServletRequest request) {
		if (Settings.getInstance().isLocalAuthBypassEnabled()) {
			try {
				InetAddress remoteAddr = InetAddress.getByName(request.getRemoteAddr());
				if (remoteAddr.isLoopbackAddress()) {
					throw ApiExceptionFactory.INSTANCE.createCustomException(request, ApiError.UNAUTHORIZED, "Local requests not allowed when localAuthBypassEnabled is enabled in settings");
				}
			} catch (UnknownHostException e) {
				throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.UNAUTHORIZED);
			}
		}
	}

	public static void requirePriorAuthorization(HttpServletRequest request, String resourceId, Service service, String identifier) {
		ArbitraryDataResource resource = new ArbitraryDataResource(resourceId, null, service, identifier);
		if (!ArbitraryDataRenderManager.getInstance().isAuthorized(resource)) {
			throw ApiExceptionFactory.INSTANCE.createCustomException(request, ApiError.UNAUTHORIZED, "Call /render/authorize first");
		}
	}

	public static void requirePriorAuthorizationOrApiKey(HttpServletRequest request, String resourceId, Service service, String identifier) {
		try {
			Security.checkApiCallAllowed(request);

		} catch (ApiException e) {
			// API call wasn't allowed, but maybe it was pre-authorized
			Security.requirePriorAuthorization(request, resourceId, service, identifier);
		}
	}

	public static ApiKey getApiKey(HttpServletRequest request) {
		ApiKey apiKey = ApiService.getInstance().getApiKey();
		if (apiKey == null) {
			try {
				apiKey = new ApiKey();
			} catch (IOException e) {
				// Couldn't load API key - so we need to treat it as not generated, and therefore unauthorized
				throw ApiExceptionFactory.INSTANCE.createException(request, ApiError.UNAUTHORIZED);
			}
			ApiService.getInstance().setApiKey(apiKey);
		}
		return apiKey;
	}

}
