package com.ratcheese;

import com.ratcheese.hud.CheeseHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

public class RatCheeseMod implements ClientModInitializer {
	public static final String MOD_ID = "ratcheese";

	private static Screen pendingScreen;

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	/**
	 * Opens a screen on the next client tick. Opening directly from a chat command
	 * does not work because the chat screen closes right after the command runs,
	 * which would immediately close our screen too.
	 */
	public static void openScreenLater(Screen screen) {
		pendingScreen = screen;
	}

	@Override
	public void onInitializeClient() {
		RatCheeseConfig.load();
		com.ratcheese.achievements.AchievementManager.init();
		CheeseDetector.init();
		BuffTracker.init();
		CheeseHud.init();
		RatCheeseCommands.init();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (pendingScreen != null) {
				Screen screen = pendingScreen;
				pendingScreen = null;
				client.setScreen(screen);
			}
		});
	}
}
