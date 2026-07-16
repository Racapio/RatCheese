package com.ratcheese;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RatCheeseConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger("ratcheese");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("ratcheese.json");

	private static RatCheeseConfig instance;

	// --- Cheese highlight ---
	public boolean highlightEnabled = true;
	/** RGB, no alpha. Default is cheese yellow. */
	public int glowColor = 0xFFCC00;
	/** How much bigger the cheese is rendered, 1.0 - 5.0. */
	public double cheeseScale = 3.0;
	/** Radius around the player scanned for cheese entities, blocks. */
	public double scanRadius = 48.0;
	/** Case-insensitive markers matched against entity/item names and item model ids. */
	public List<String> nameMarkers = new ArrayList<>(List.of("cheese"));
	/** The known texture hash of the cheese head Hypixel spawns (found in-game via /ratcheese scan). */
	public static final String CHEESE_HEAD_TEXTURE = "3d9c8dd245a6845d8704145bab5fac60a30765e1291e70e98e25d9fec41b26c5";
	/** Player-head texture hashes (or base64 fragments) that mark a cheese entity. Extendable via /ratcheese scan. */
	public List<String> headTextures = new ArrayList<>(List.of(CHEESE_HEAD_TEXTURE));

	// --- HUD ---
	public boolean hudEnabled = true;
	public int hudX = 5;
	public int hudY = 40;
	/** 0.5 - 3.0 */
	public double hudScale = 1.0;
	public boolean hudBackground = true;
	/** Show buff time as a shrinking bar (ScathaPro style) instead of a numeric countdown. */
	public boolean timerBars = false;
	public boolean showOutgoing = true;
	public boolean showIncoming = true;
	public boolean showLootShares = true;

	// --- Alerts ---
	public boolean smellAlertTitle = true;
	public boolean smellAlertSound = true;

	public static RatCheeseConfig get() {
		if (instance == null) load();
		return instance;
	}

	public static void load() {
		if (Files.exists(FILE)) {
			try {
				instance = GSON.fromJson(Files.readString(FILE), RatCheeseConfig.class);
			} catch (Exception e) {
				LOGGER.error("[RatCheese] Failed to read config, using defaults", e);
			}
		}
		if (instance == null) instance = new RatCheeseConfig();
		instance.clamp();
	}

	public static void save() {
		get().clamp();
		try {
			Files.writeString(FILE, GSON.toJson(get()));
		} catch (IOException e) {
			LOGGER.error("[RatCheese] Failed to save config", e);
		}
	}

	private void clamp() {
		cheeseScale = Math.clamp(cheeseScale, 1.0, 5.0);
		hudScale = Math.clamp(hudScale, 0.5, 3.0);
		scanRadius = Math.clamp(scanRadius, 8.0, 128.0);
		if (nameMarkers == null) nameMarkers = new ArrayList<>(List.of("cheese"));
		// Ship the known cheese texture by default; only add it to empty lists so
		// deliberately customized configs are left alone.
		if (headTextures == null || headTextures.isEmpty()) headTextures = new ArrayList<>(List.of(CHEESE_HEAD_TEXTURE));
		glowColor &= 0xFFFFFF;
	}
}
