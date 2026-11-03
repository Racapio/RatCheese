package com.ratcheese.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Version bridge for client APIs Mojang reshuffled between releases.
 * This copy targets Minecraft 26.2 and 26.3, where screen handling moved to
 * {@code Gui} and titles / HUD visibility moved to the new {@code Hud} class.
 */
public final class Mc {
	private Mc() {}

	public static void setScreen(@Nullable Screen screen) {
		Minecraft.getInstance().gui.setScreen(screen);
	}

	public static @Nullable Screen screen() {
		return Minecraft.getInstance().gui.screen();
	}

	/** True when the player hid the HUD with F1. */
	public static boolean hudHidden() {
		return Minecraft.getInstance().gui.hud.isHidden();
	}

	public static void showTitle(Component title, @Nullable Component subtitle) {
		var hud = Minecraft.getInstance().gui.hud;
		hud.resetTitleTimes();
		if (subtitle != null) hud.setSubtitle(subtitle);
		hud.setTitle(title);
	}

	public static Vec3 cameraPosition() {
		return Minecraft.getInstance().gameRenderer.mainCamera().position();
	}
}
