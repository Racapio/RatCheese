package com.ratcheese.achievements;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * RatCheese achievements. Structure inspired by ScathaPro (enum + goal + secret flag)
 * and SBO (rarity colors, unlock effects).
 */
public enum Achievement {
	CHEESE_1("cheese_1", Rarity.COMMON, 1, Stat.CHEESE, false),
	CHEESE_10("cheese_10", Rarity.RARE, 10, Stat.CHEESE, false),
	CHEESE_50("cheese_50", Rarity.EPIC, 50, Stat.CHEESE, false),
	CHEESE_100("cheese_100", Rarity.LEGENDARY, 100, Stat.CHEESE, false),
	LOOT_1("loot_1", Rarity.COMMON, 1, Stat.LOOT, false),
	LOOT_25("loot_25", Rarity.EPIC, 25, Stat.LOOT, false),
	LOOT_100("loot_100", Rarity.LEGENDARY, 100, Stat.LOOT, false),
	UNIQUE_5("unique_5", Rarity.RARE, 5, Stat.UNIQUE_PLAYERS, false),
	UNIQUE_25("unique_25", Rarity.LEGENDARY, 25, Stat.UNIQUE_PLAYERS, false),
	INCOMING_1("incoming_1", Rarity.RARE, 1, Stat.INCOMING, false),
	MF_MAX("mf_max", Rarity.LEGENDARY, 1, Stat.MAX_MF_BUFFS, true),
	SMELL_MISS("smell_miss", Rarity.COMMON, 1, Stat.SMELL_MISSES, true),

	FAST_COLLECT("fast_collect", Rarity.RARE, 1, Stat.FAST_COLLECTS, false),
	MINED_LOOT("mined_loot", Rarity.COMMON, 1, Stat.MINED_LOOT, false),
	TOTAL_MF_100("total_mf_100", Rarity.RARE, 100, Stat.TOTAL_MF, false),
	TOTAL_MF_500("total_mf_500", Rarity.LEGENDARY, 500, Stat.TOTAL_MF, false),
	BEST_FRIEND("best_friend", Rarity.EPIC, 10, Stat.BEST_FRIEND, false),
	LOBBY_FEVER("lobby_fever", Rarity.EPIC, 10, Stat.LOBBY_BEST, false),
	FAVORITE("favorite", Rarity.RARE, 3, Stat.SAME_STREAK_BEST, true),
	JACKPOT("jackpot", Rarity.EPIC, 1, Stat.JACKPOTS, true),
	NIGHT_RAT("night_rat", Rarity.RARE, 1, Stat.NIGHT_CHEESE, true),
	CONSPIRACY("conspiracy", Rarity.EPIC, 1, Stat.CONSPIRACY, true),
	MISS_STREAK("miss_streak", Rarity.RARE, 3, Stat.MISS_STREAK_BEST, true),
	// Must stay last: its goal is "all other achievements".
	COMPLETIONIST("completionist", Rarity.LEGENDARY, 0, Stat.COMPLETIONIST, false);

	public enum Rarity {
		COMMON(ChatFormatting.WHITE),
		RARE(ChatFormatting.BLUE),
		EPIC(ChatFormatting.LIGHT_PURPLE),
		LEGENDARY(ChatFormatting.GOLD);

		public final ChatFormatting color;

		Rarity(ChatFormatting color) {
			this.color = color;
		}
	}

	/** Which persistent counter this achievement tracks. */
	public enum Stat {
		CHEESE, LOOT, UNIQUE_PLAYERS, INCOMING, MAX_MF_BUFFS, SMELL_MISSES,
		FAST_COLLECTS, MINED_LOOT, TOTAL_MF, BEST_FRIEND, LOBBY_BEST,
		SAME_STREAK_BEST, JACKPOTS, NIGHT_CHEESE, CONSPIRACY, MISS_STREAK_BEST,
		COMPLETIONIST
	}

	public final String id;
	public final Rarity rarity;
	private final long goal;
	public final Stat stat;
	public final boolean secret;

	Achievement(String id, Rarity rarity, long goal, Stat stat, boolean secret) {
		this.id = id;
		this.rarity = rarity;
		this.goal = goal;
		this.stat = stat;
		this.secret = secret;
	}

	/** Secret achievements that show a cryptic hint instead of the generic "find it yourself". */
	private static final java.util.Set<Achievement> HINTED =
			java.util.EnumSet.of(SMELL_MISS, FAVORITE, JACKPOT, NIGHT_RAT, MISS_STREAK);

	public boolean hasHint() {
		return HINTED.contains(this);
	}

	public Component hint() {
		return Component.translatable("ratcheese.achievement." + id + ".hint");
	}

	public long goal() {
		// "Unlock everything else" — computed here because values() is not
		// available inside the enum constructor.
		return stat == Stat.COMPLETIONIST ? values().length - 1 : goal;
	}

	public Component title() {
		return Component.translatable("ratcheese.achievement." + id).withStyle(rarity.color);
	}

	public Component description() {
		return Component.translatable("ratcheese.achievement." + id + ".desc");
	}
}
