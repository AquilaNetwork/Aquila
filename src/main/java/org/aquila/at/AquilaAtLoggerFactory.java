package org.aquila.at;

import org.ciyam.at.AtLogger;

public class AquilaAtLoggerFactory implements org.ciyam.at.AtLoggerFactory {

	private static AquilaAtLoggerFactory instance;

	private AquilaAtLoggerFactory() {
	}

	public static synchronized AquilaAtLoggerFactory getInstance() {
		if (instance == null)
			instance = new AquilaAtLoggerFactory();

		return instance;
	}

	@Override
	public AtLogger create(final Class<?> loggerName) {
		return AquilaAtLogger.create(loggerName);
	}

}
