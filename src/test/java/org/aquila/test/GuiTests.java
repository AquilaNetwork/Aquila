package org.aquila.test;

import java.awt.TrayIcon.MessageType;

import org.aquila.gui.SplashFrame;
import org.aquila.gui.SysTray;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class GuiTests {

	@Test
	public void testSplashFrame() throws InterruptedException {
		SplashFrame splashFrame = SplashFrame.getInstance();

		Thread.sleep(2000L);

		splashFrame.dispose();
	}

	@Test
	public void testSysTray() throws InterruptedException {
		SysTray.getInstance();

		SysTray.getInstance().showMessage("Testing...", "Tray icon should disappear in 10 seconds", MessageType.INFO);

		Thread.sleep(10_000L);

		SysTray.getInstance().dispose();
	}

}
