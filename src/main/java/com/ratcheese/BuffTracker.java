package com.ratcheese;

import com.ratcheese.achievements.AchievementManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Rat pet chat messages and tracks active Rat's Blessing buffs.
 *
 * Known message formats (the Magic Find number is followed by Hypixel's custom
 * private-use glyph, swallowed by \S* in the pattern):
 *   CHEESE! You buffed Zellion63 giving them +7 Magic Find for 60 seconds!
 *   RAT BLESSING! You collected some loot after Zellion63 killed a mob!
 *   CHEESE! You smell Cheese nearby!
 * Incoming-buff wording is not documented on the wiki, so several permissive
 * patterns plus a generic fallback are used.
 */
public class BuffTracker {
	/** Optional rank tag like "[MVP+] " followed by the player name. */
	private static final String NAME = "(?:\\[[^\\]]+] )?([A-Za-z0-9_]{1,16})";
	/**
	 * Matches "+7 Magic Find for 60 seconds" — including Hypixel's custom Magic Find
	 * glyph (a private-use unicode char) that sits right after the number, e.g.
	 * "+2.05 Magic Find for 20 seconds". \S* swallows any such glyphs.
	 */
	private static final String MF = "\\+([0-9.]+)\\S*\\s+Magic Find for ([0-9.]+) seconds";

	private static final Pattern OUTGOING = Pattern.compile("^CHEESE! You buffed " + NAME + " giving them " + MF + "!?$");
	private static final Pattern INCOMING_1 = Pattern.compile("^CHEESE! " + NAME + " buffed you,? giving you " + MF + "!?$");
	private static final Pattern INCOMING_2 = Pattern.compile("^(?:CHEESE|RAT BLESSING)! " + NAME + " (?:buffed|blessed|granted|gave) you,?(?: giving you)? " + MF + "!?$");
	private static final Pattern INCOMING_GENERIC = Pattern.compile("^(?:CHEESE|RAT BLESSING)! (?!You buffed).*?" + MF + "!?$");
	private static final Pattern GENERIC_NAME = Pattern.compile(NAME + " (?:buffed|blessed|granted|gave)");
	/** Action varies: "killed a mob", "mined a block", possibly more — match and capture any. */
	private static final Pattern LOOT = Pattern.compile("^RAT BLESSING! You collected some loot after " + NAME + " (.+?)!?$");

	private static final int DEFAULT_NAME_COLOR = 0xFFFFFF;

	public record ActiveBuff(String player, double magicFind, long expiresAt, long durationMillis, boolean incoming, int nameColor) {
		public int secondsLeft() {
			return (int) Math.max(0, Math.ceil((expiresAt - System.currentTimeMillis()) / 1000.0));
		}

		/** Remaining fraction of the buff, 1.0 (fresh) → 0.0 (expired). */
		public float progress() {
			if (durationMillis <= 0) return 0f;
			return Math.clamp((expiresAt - System.currentTimeMillis()) / (float) durationMillis, 0f, 1f);
		}
	}

	/** The cheese despawns roughly 10 seconds after spawning (measured in-game), plus chat latency slack. */
	private static final long SMELL_TIMEOUT_MS = 11_000;
	/** "Lightning sniff": collecting this fast after the smell message counts as a fast collect. */
	private static final long FAST_COLLECT_MS = 1_000;

	private static final List<ActiveBuff> BUFFS = new ArrayList<>();
	private static long smellUntil = 0;
	private static long lastSmellAt = 0;
	private static boolean smellFromTest = false;
	private static int lootShares = 0;
	private static String lastLootFrom = null;
	private static long lastLootAt = 0;

	public static void init() {
		ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
			if (!overlay) {
				try {
					onMessage(message);
				} catch (Exception ignored) {
					// Never break chat because of a parsing bug.
				}
			}
			return true;
		});
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());
		// Smelled cheese but never collected it → secret achievement.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (smellUntil != 0 && System.currentTimeMillis() > smellUntil) {
				smellUntil = 0;
				if (!smellFromTest) AchievementManager.onSmellMissed();
				smellFromTest = false;
			}
		});
	}

	private static void onMessage(Component message) {
		String text = ChatFormatting.stripFormatting(message.getString());
		if (text == null || text.isEmpty()) return;

		Matcher m = OUTGOING.matcher(text);
		if (m.matches()) {
			String name = m.group(1);
			double mf = parse(m.group(2));
			long now = System.currentTimeMillis();
			boolean fast = lastSmellAt > 0 && now - lastSmellAt <= FAST_COLLECT_MS;
			boolean incomingActive = BUFFS.stream().anyMatch(buff -> buff.incoming() && buff.expiresAt() > now);
			addBuff(name, mf, parse(m.group(3)), false, extractNameColor(message, name));
			AchievementManager.onCheeseCollected(name, mf, fast, incomingActive);
			return;
		}

		m = LOOT.matcher(text);
		if (m.matches()) {
			lootShares++;
			lastLootFrom = m.group(1);
			lastLootAt = System.currentTimeMillis();
			AchievementManager.onLootShare(m.group(2).startsWith("mined"));
			return;
		}
		if (text.startsWith("RAT BLESSING!") && text.contains("collected some loot")) {
			lootShares++;
			// Unknown format — don't show a stale (wrong) name on the HUD flash line.
			lastLootFrom = null;
			lastLootAt = System.currentTimeMillis();
			AchievementManager.onLootShare(false);
			return;
		}

		String lower = text.toLowerCase();
		if (lower.contains("you smell") && lower.contains("cheese")) {
			onSmell();
			smellFromTest = false;
			return;
		}

		m = INCOMING_1.matcher(text);
		if (!m.matches()) m = INCOMING_2.matcher(text);
		if (m.matches()) {
			String name = m.group(1);
			addBuff(name, parse(m.group(2)), parse(m.group(3)), true, extractNameColor(message, name));
			AchievementManager.onIncomingBuff();
			return;
		}
		m = INCOMING_GENERIC.matcher(text);
		if (m.matches()) {
			String name = "Rat";
			Matcher nameMatcher = GENERIC_NAME.matcher(text);
			if (nameMatcher.find()) name = nameMatcher.group(1);
			addBuff(name, parse(m.group(1)), parse(m.group(2)), true, extractNameColor(message, name));
			AchievementManager.onIncomingBuff();
		}
	}

	/**
	 * Pulls the player's rank color straight out of the formatted chat component:
	 * finds the styled segment containing the name and returns its text color.
	 */
	private static int extractNameColor(Component message, String name) {
		return message.<Integer>visit((style, part) -> {
			if (part.contains(name) && style.getColor() != null) {
				return Optional.of(style.getColor().getValue());
			}
			return Optional.empty();
		}, Style.EMPTY).orElse(DEFAULT_NAME_COLOR);
	}

	private static void onSmell() {
		lastSmellAt = System.currentTimeMillis();
		smellUntil = lastSmellAt + SMELL_TIMEOUT_MS;
		RatCheeseConfig config = RatCheeseConfig.get();
		Minecraft client = Minecraft.getInstance();
		if (config.smellAlertTitle) {
			client.gui.setTitle(Component.translatable("ratcheese.smell.title").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
		}
		if (config.smellAlertSound) {
			client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.5f, 1.0f));
		}
	}

	private static double parse(String number) {
		try {
			return Double.parseDouble(number);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static void addBuff(String player, double magicFind, double seconds, boolean incoming, int nameColor) {
		BUFFS.removeIf(buff -> buff.incoming() == incoming && buff.player().equalsIgnoreCase(player));
		long duration = (long) (seconds * 1000);
		BUFFS.add(new ActiveBuff(player, magicFind, System.currentTimeMillis() + duration, duration, incoming, nameColor));
		// An outgoing buff means we just collected the cheese we smelled.
		if (!incoming) smellUntil = 0;
	}

	/** Removes expired buffs and returns a snapshot of the active ones. */
	public static List<ActiveBuff> activeBuffs() {
		long now = System.currentTimeMillis();
		Iterator<ActiveBuff> it = BUFFS.iterator();
		while (it.hasNext()) {
			if (it.next().expiresAt() <= now) it.remove();
		}
		return List.copyOf(BUFFS);
	}

	public static boolean smellActive() {
		return smellUntil > System.currentTimeMillis();
	}

	public static int lootShares() {
		return lootShares;
	}

	public static String lastLootFrom() {
		return lastLootFrom;
	}

	public static boolean lootFlashActive() {
		return System.currentTimeMillis() - lastLootAt < 5_000;
	}

	public static void reset() {
		BUFFS.clear();
		smellUntil = 0;
		lastSmellAt = 0;
		smellFromTest = false;
		lastLootFrom = null;
		lastLootAt = 0;
		AchievementManager.onLobbyJoin();
	}

	public static void resetStats() {
		reset();
		lootShares = 0;
	}

	/** Fills the tracker with sample data so the HUD can be previewed with /ratcheese test. */
	public static void injectTestData() {
		addBuff("Zellion63", 7, 60, false, 0x55FFFF);
		addBuff("CoolRatFan", 6.05, 47, true, 0x55FF55);
		lootShares += 3;
		lastLootFrom = "Zellion63";
		lastLootAt = System.currentTimeMillis();
		onSmell();
		smellFromTest = true;
	}
}
