package com.ratcheese.achievements;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.sounds.SoundEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Tracks lifetime rat/cheese stats and unlocks achievements.
 * Unlock effects follow SBO's style: on-screen title, chat message with a
 * hover description, and a level-up / challenge-complete sound.
 */
public class AchievementManager {
	private static final Logger LOGGER = LoggerFactory.getLogger("ratcheese");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("ratcheese_achievements.json");

	/** Persistent lifetime stats + unlocked achievement ids. */
	public static class Data {
		public long cheese = 0;
		public long loot = 0;
		public long incoming = 0;
		public long maxMfBuffs = 0;
		public long smellMisses = 0;
		public double totalMf = 0;
		public long fastCollects = 0;
		public long minedLoot = 0;
		public long lobbyBest = 0;
		public long sameStreakBest = 0;
		public long jackpots = 0;
		public long nightCheese = 0;
		public long conspiracy = 0;
		public long missStreakBest = 0;
		public Set<String> uniquePlayers = new LinkedHashSet<>();
		public Map<String, Long> buffCounts = new HashMap<>();
		public Set<String> unlocked = new LinkedHashSet<>();
		/** id → unlock time (epoch millis). Older entries may miss (unlocked before this field existed). */
		public Map<String, Long> unlockedTimes = new HashMap<>();
	}

	private static Data data = new Data();

	// Transient (session/lobby) trackers
	private static String lastBuffedPlayer = null;
	private static int sameStreak = 0;
	private static int lobbyCheese = 0;
	private static int missStreak = 0;
	private static final ArrayDeque<Long> recentLoot = new ArrayDeque<>();

	public static void init() {
		load();
	}

	public static Data data() {
		return data;
	}

	// --- Event hooks (called from BuffTracker) ---

	/**
	 * @param fastFromSmell  collected within 5 seconds of the smell message
	 * @param incomingActive an incoming buff was active while collecting
	 */
	public static void onCheeseCollected(String player, double magicFind, boolean fastFromSmell, boolean incomingActive) {
		data.cheese++;
		data.totalMf += magicFind;
		data.uniquePlayers.add(player.toLowerCase(Locale.ROOT));
		data.buffCounts.merge(player.toLowerCase(Locale.ROOT), 1L, Long::sum);
		if (magicFind >= 7) data.maxMfBuffs++;
		if (fastFromSmell) data.fastCollects++;
		if (incomingActive) data.conspiracy++;

		int hour = LocalTime.now().getHour();
		if (hour >= 3 && hour < 5) data.nightCheese++;

		lobbyCheese++;
		data.lobbyBest = Math.max(data.lobbyBest, lobbyCheese);

		if (player.equalsIgnoreCase(lastBuffedPlayer)) sameStreak++;
		else sameStreak = 1;
		lastBuffedPlayer = player;
		data.sameStreakBest = Math.max(data.sameStreakBest, sameStreak);

		missStreak = 0;
		checkAndSave();
	}

	/** @param mined the loot came from the buffed player mining a block */
	public static void onLootShare(boolean mined) {
		data.loot++;
		if (mined) data.minedLoot++;

		long now = System.currentTimeMillis();
		recentLoot.addLast(now);
		recentLoot.removeIf(t -> now - t > 30_000);
		if (recentLoot.size() == 3) data.jackpots++;

		checkAndSave();
	}

	public static void onIncomingBuff() {
		data.incoming++;
		checkAndSave();
	}

	public static void onSmellMissed() {
		data.smellMisses++;
		missStreak++;
		data.missStreakBest = Math.max(data.missStreakBest, missStreak);
		checkAndSave();
	}

	/** Resets per-lobby trackers (called on server join / world switch). */
	public static void onLobbyJoin() {
		lobbyCheese = 0;
		sameStreak = 0;
		lastBuffedPlayer = null;
		missStreak = 0;
		recentLoot.clear();
	}

	// --- Progress ---

	public static long statValue(Achievement.Stat stat) {
		return switch (stat) {
			case CHEESE -> data.cheese;
			case LOOT -> data.loot;
			case UNIQUE_PLAYERS -> data.uniquePlayers.size();
			case INCOMING -> data.incoming;
			case MAX_MF_BUFFS -> data.maxMfBuffs;
			case SMELL_MISSES -> data.smellMisses;
			case FAST_COLLECTS -> data.fastCollects;
			case MINED_LOOT -> data.minedLoot;
			case TOTAL_MF -> (long) data.totalMf;
			case BEST_FRIEND -> data.buffCounts.values().stream().mapToLong(Long::longValue).max().orElse(0);
			case LOBBY_BEST -> data.lobbyBest;
			case SAME_STREAK_BEST -> data.sameStreakBest;
			case JACKPOTS -> data.jackpots;
			case NIGHT_CHEESE -> data.nightCheese;
			case CONSPIRACY -> data.conspiracy;
			case MISS_STREAK_BEST -> data.missStreakBest;
			case COMPLETIONIST -> {
				long count = 0;
				for (Achievement achievement : Achievement.values()) {
					if (achievement.stat != Achievement.Stat.COMPLETIONIST && isUnlocked(achievement)) count++;
				}
				yield count;
			}
		};
	}

	public static boolean isUnlocked(Achievement achievement) {
		return data.unlocked.contains(achievement.id);
	}

	/** Unlock time in epoch millis, or null if unknown (unlocked before timestamps existed). */
	public static Long unlockedAt(Achievement achievement) {
		return data.unlockedTimes.get(achievement.id);
	}

	public static int unlockedCount() {
		int count = 0;
		for (Achievement achievement : Achievement.values()) {
			if (isUnlocked(achievement)) count++;
		}
		return count;
	}

	private static void checkAndSave() {
		// COMPLETIONIST is last in the enum, so unlocks from this pass count toward it.
		for (Achievement achievement : Achievement.values()) {
			if (!isUnlocked(achievement) && statValue(achievement.stat) >= achievement.goal()) {
				data.unlocked.add(achievement.id);
				data.unlockedTimes.put(achievement.id, System.currentTimeMillis());
				showUnlockEffects(achievement);
			}
		}
		save();
	}

	private static void showUnlockEffects(Achievement achievement) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) return;

		client.gui.resetTitleTimes();
		client.gui.setSubtitle(Component.translatable("ratcheese.achievement.unlocked").withStyle(ChatFormatting.GREEN));
		client.gui.setTitle(achievement.title());

		boolean big = achievement.rarity == Achievement.Rarity.LEGENDARY;
		client.getSoundManager().play(SimpleSoundInstance.forUI(
				big ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.PLAYER_LEVELUP, 1.0f));

		Component name = achievement.title().copy()
				.withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(achievement.description())));
		client.player.sendSystemMessage(
				Component.literal("[RatCheese] ").withStyle(ChatFormatting.GOLD)
						.append(Component.translatable("ratcheese.achievement.unlockedChat").withStyle(ChatFormatting.GREEN))
						.append(" ")
						.append(name));
	}

	// --- Persistence ---

	private static void load() {
		if (!Files.exists(FILE)) return;
		try {
			Data loaded = GSON.fromJson(Files.readString(FILE), Data.class);
			if (loaded != null) {
				if (loaded.uniquePlayers == null) loaded.uniquePlayers = new LinkedHashSet<>();
				if (loaded.buffCounts == null) loaded.buffCounts = new HashMap<>();
				if (loaded.unlocked == null) loaded.unlocked = new LinkedHashSet<>();
				if (loaded.unlockedTimes == null) loaded.unlockedTimes = new HashMap<>();
				data = loaded;
			}
		} catch (Exception e) {
			LOGGER.error("[RatCheese] Failed to read achievements data", e);
		}
	}

	private static void save() {
		try {
			Files.writeString(FILE, GSON.toJson(data));
		} catch (IOException e) {
			LOGGER.error("[RatCheese] Failed to save achievements data", e);
		}
	}
}
