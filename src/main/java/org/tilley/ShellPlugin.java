package org.tilley;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.feature.window.ResizeableWindow;
import org.rusherhack.client.api.plugin.Plugin;

public class ShellPlugin extends Plugin {
	
	@Override
	public void onLoad() {
		
		this.getLogger().info("Loaded RusherShell plugin!");

		String operatingSystem = System.getProperty("os.name").toLowerCase();
		if (operatingSystem.contains("linux")) {
			this.getLogger().info("Detected a Linux-based OS");
		}
		else {
			this.getLogger().warn("Warning: detected a non-linux OS ("+operatingSystem+"). Some features may not work.");
		}

		final ShellOutputElement shellOutputElement = new ShellOutputElement();
		RusherHackAPI.getHudManager().registerFeature(shellOutputElement);

		final ResizeableWindow LinuxTerminalWindow = new LinuxTerminalWindow();
		RusherHackAPI.getWindowManager().registerFeature(LinuxTerminalWindow);

	}
	
	@Override
	public void onUnload() {
		this.getLogger().info("RusherShell plugin unloaded!");
	}
	
}