package com.ratcheese.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Version bridge for client APIs Mojang reshuffled between releases.
 * This copy targets Minecraft 26.1.x; see src/compat/v26_2 for 26.2+.
 */
public final class Mc {
	private Mc() {}

	public static void setScreen(@Nullable Screen screen) {
		Minecraft.getInstance().setScreen(screen);
	}

	public static @Nullable Screen screen() {
		return Minecraft.getInstance().screen;
	}

	/** True when the player hid the HUD with F1. */
	public static boolean hudHidden() {
		return Minecraft.getInstance().options.hideGui;
	}

	public static void showTitle(Component title, @Nullable Component subtitle) {
		var gui = Minecraft.getInstance().gui;
		gui.resetTitleTimes();
		if (subtitle != null) gui.setSubtitle(subtitle);
		gui.setTitle(title);
	}

	public static Vec3 cameraPosition() {
		return Minecraft.getInstance().gameRenderer.getMainCamera().position();
	}
}
