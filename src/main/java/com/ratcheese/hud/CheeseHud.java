package com.ratcheese.hud;

import com.ratcheese.BuffTracker;
import com.ratcheese.RatCheeseConfig;
import com.ratcheese.RatCheeseMod;
import com.ratcheese.screen.HudPositionScreen;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

public class CheeseHud {
	private static final int PADDING = 4;
	private static final int LINE_HEIGHT = 10;
	private static final int BAR_HEIGHT = 3;
	private static final int BAR_GAP = 3;

	private static final int COLOR_OUTGOING = 0xFF55FF55;
	private static final int COLOR_INCOMING = 0xFF55FFFF;

	/** One HUD row: text, and optionally a shrinking timer bar below it (progress < 0 = no bar). */
	public record Row(Component text, float progress, int barColor) {
		public static Row text(Component text) {
			return new Row(text, -1f, 0);
		}

		public boolean hasBar() {
			return progress >= 0f;
		}
	}

	public static void init() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE, RatCheeseMod.id("buff_hud"), CheeseHud::render);
	}

	private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft client = Minecraft.getInstance();
		RatCheeseConfig config = RatCheeseConfig.get();
		if (!config.hudEnabled || client.player == null || client.options.hideGui) return;
		// The position screen draws its own preview.
		if (client.screen instanceof HudPositionScreen) return;

		List<Row> rows = buildRows(false);
		if (rows.isEmpty()) return;

		draw(graphics, client.font, rows, config);
	}

	public static void draw(GuiGraphicsExtractor graphics, Font font, List<Row> rows, RatCheeseConfig config) {
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(config.hudX, config.hudY);
		float scale = (float) config.hudScale;
		pose.scale(scale, scale);

		int width = boxWidth(font, rows);
		int height = boxHeight(rows);

		if (config.hudBackground) {
			graphics.fill(0, 0, width, height, 0xA0101010);
			// Thin accent border in the glow color
			int border = 0x80000000 | (config.glowColor & 0xFFFFFF);
			graphics.fill(0, 0, width, 1, border);
			graphics.fill(0, height - 1, width, height, border);
			graphics.fill(0, 0, 1, height, border);
			graphics.fill(width - 1, 0, width, height, border);
		}

		int y = PADDING;
		for (Row row : rows) {
			graphics.text(font, row.text(), PADDING, y, CommonColors.WHITE, true);
			y += LINE_HEIGHT;
			if (row.hasBar()) {
				int innerWidth = width - PADDING * 2;
				int filled = Math.round(innerWidth * Math.clamp(row.progress(), 0f, 1f));
				graphics.fill(PADDING, y, PADDING + innerWidth, y + BAR_HEIGHT, 0x60000000);
				if (filled > 0) {
					graphics.fill(PADDING, y, PADDING + filled, y + BAR_HEIGHT, row.barColor());
				}
				y += BAR_HEIGHT + BAR_GAP;
			}
		}

		pose.popMatrix();
	}

	public static int boxWidth(Font font, List<Row> rows) {
		int max = 0;
		for (Row row : rows) {
			max = Math.max(max, font.width(row.text()));
		}
		return max + PADDING * 2;
	}

	public static int boxHeight(List<Row> rows) {
		int height = PADDING * 2 - 2;
		for (Row row : rows) {
			height += LINE_HEIGHT + (row.hasBar() ? BAR_HEIGHT + BAR_GAP : 0);
		}
		return height;
	}

	public static List<Row> buildRows(boolean preview) {
		RatCheeseConfig config = RatCheeseConfig.get();
		List<Row> rows = new ArrayList<>();
		List<BuffTracker.ActiveBuff> buffs = preview ? previewBuffs() : BuffTracker.activeBuffs();

		if (preview || BuffTracker.smellActive()) {
			rows.add(Row.text(Component.translatable("ratcheese.hud.smell").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)));
		}

		for (BuffTracker.ActiveBuff buff : buffs) {
			if (buff.incoming() && !config.showIncoming) continue;
			if (!buff.incoming() && !config.showOutgoing) continue;

			// Name in the player's rank color (taken from the chat message), MF value in aqua
			// like the Magic Find stat, the rest of the line in gray so nothing blends together.
			Component name = Component.literal(buff.player()).withColor(buff.nameColor());
			Component mf = Component.literal("+" + trimNumber(buff.magicFind()) + "✯ MF").withStyle(ChatFormatting.AQUA);
			String key = buff.incoming() ? "ratcheese.hud.incoming" : "ratcheese.hud.outgoing";
			if (config.timerBars) {
				Component text = Component.translatable(key + ".short", name, mf).withStyle(ChatFormatting.GRAY);
				rows.add(new Row(text, buff.progress(), buff.incoming() ? COLOR_INCOMING : COLOR_OUTGOING));
			} else {
				Component timer = Component.translatable("ratcheese.hud.seconds", buff.secondsLeft()).withStyle(ChatFormatting.YELLOW);
				Component text = Component.translatable(key, name, mf, timer).withStyle(ChatFormatting.GRAY);
				rows.add(Row.text(text));
			}
		}

		int lootShares = preview ? 3 : BuffTracker.lootShares();
		if (config.showLootShares && lootShares > 0) {
			rows.add(Row.text(Component.translatable("ratcheese.hud.lootshares", lootShares).withStyle(ChatFormatting.GOLD)));
		}
		if (config.showLootShares && (preview || BuffTracker.lootFlashActive())) {
			String from = preview ? "Zellion63" : BuffTracker.lastLootFrom();
			if (from != null) {
				rows.add(Row.text(Component.translatable("ratcheese.hud.loot", from).withStyle(ChatFormatting.LIGHT_PURPLE)));
			}
		}

		if (!rows.isEmpty() || preview) {
			rows.addFirst(Row.text(Component.translatable("ratcheese.hud.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
		}
		return rows;
	}

	private static List<BuffTracker.ActiveBuff> previewBuffs() {
		long now = System.currentTimeMillis();
		return List.of(
				new BuffTracker.ActiveBuff("Zellion63", 7, now + 43_000, 60_000, false, 0x55FFFF),
				new BuffTracker.ActiveBuff("CoolRatFan", 6.05, now + 12_000, 47_000, true, 0x55FF55)
		);
	}

	private static String trimNumber(double value) {
		return value == Math.floor(value) ? String.valueOf((int) value) : String.valueOf(value);
	}
}
