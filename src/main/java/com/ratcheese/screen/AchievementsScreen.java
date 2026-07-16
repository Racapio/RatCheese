package com.ratcheese.screen;

import com.ratcheese.achievements.Achievement;
import com.ratcheese.achievements.AchievementManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class AchievementsScreen extends Screen {
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	private static final int CARD_HEIGHT = 30;
	private static final int ENTRY_HEIGHT = CARD_HEIGHT + 4;
	private static final int TOP = 48;
	private static final int BOTTOM_MARGIN = 40;

	private final @Nullable Screen parent;
	private double scroll = 0;

	public AchievementsScreen(@Nullable Screen parent) {
		super(Component.translatable("ratcheese.achievements.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		addRenderableWidget(Button.builder(Component.translatable("ratcheese.config.done"),
				button -> onClose()).bounds(width / 2 - 75, height - 30, 150, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		int listWidth = Math.min(360, width - 40);
		int left = (width - listWidth) / 2;
		int panelLeft = left - 6;
		int panelRight = left + listWidth + 6;
		int panelTop = TOP - 4;
		int panelBottom = height - BOTTOM_MARGIN + 4;

		// Header: title, unlocked count and an overall progress bar
		graphics.centeredText(font, title, width / 2, 10, CommonColors.WHITE);
		int total = Achievement.values().length;
		int unlockedCount = AchievementManager.unlockedCount();
		graphics.centeredText(font,
				Component.translatable("ratcheese.achievements.progress", unlockedCount, total).withStyle(ChatFormatting.GRAY),
				width / 2, 24, CommonColors.WHITE);
		int barWidth = 140;
		int barLeft = width / 2 - barWidth / 2;
		graphics.fill(barLeft, 36, barLeft + barWidth, 39, 0x80000000);
		int filled = Math.round(barWidth * (unlockedCount / (float) total));
		if (filled > 0) graphics.fill(barLeft, 36, barLeft + filled, 39, 0xFFFFAA00);

		// Panel behind the list
		graphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0x88000000);

		graphics.enableScissor(panelLeft, panelTop, panelRight, panelBottom);
		int y = TOP - (int) scroll;
		for (Achievement achievement : Achievement.values()) {
			if (y + CARD_HEIGHT > panelTop && y < panelBottom) {
				drawCard(graphics, achievement, left, y, listWidth);
			}
			y += ENTRY_HEIGHT;
		}
		graphics.disableScissor();

		// Scrollbar
		double maxScroll = maxScroll();
		if (maxScroll > 0) {
			int trackTop = panelTop + 2;
			int trackBottom = panelBottom - 2;
			int trackHeight = trackBottom - trackTop;
			int thumbHeight = Math.max(12, (int) (trackHeight * (trackHeight / (double) contentHeight())));
			int thumbY = trackTop + (int) ((trackHeight - thumbHeight) * (scroll / maxScroll));
			graphics.fill(panelRight + 3, trackTop, panelRight + 5, trackBottom, 0x40FFFFFF);
			graphics.fill(panelRight + 3, thumbY, panelRight + 5, thumbY + thumbHeight, 0xC0FFFFFF);
		}
	}

	private void drawCard(GuiGraphicsExtractor graphics, Achievement achievement, int x, int y, int width) {
		boolean unlocked = AchievementManager.isUnlocked(achievement);
		boolean hidden = achievement.secret && !unlocked;

		Integer rarityRgb = achievement.rarity.color.getColor();
		int rarityColor = 0xFF000000 | (rarityRgb == null ? 0xFFFFFF : rarityRgb);

		// Card background + border (rarity-colored when unlocked)
		int background = unlocked ? 0xC0182814 : 0xC0141414;
		int border = unlocked ? (0xA0000000 | (rarityColor & 0xFFFFFF)) : 0x60404040;
		graphics.fill(x, y, x + width, y + CARD_HEIGHT, background);
		graphics.fill(x, y, x + width, y + 1, border);
		graphics.fill(x, y + CARD_HEIGHT - 1, x + width, y + CARD_HEIGHT, border);
		graphics.fill(x, y, x + 1, y + CARD_HEIGHT, border);
		graphics.fill(x + width - 1, y, x + width, y + CARD_HEIGHT, border);

		// Rarity stripe on the left edge (dimmed while locked)
		graphics.fill(x + 1, y + 1, x + 4, y + CARD_HEIGHT - 1, unlocked ? rarityColor : (0x50000000 | (rarityColor & 0xFFFFFF)));

		Component name = hidden
				? Component.literal("???").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD)
				: unlocked ? achievement.title() : achievement.title().copy().withStyle(ChatFormatting.GRAY);
		Component desc = hidden
				? (achievement.hasHint()
						? achievement.hint().copy().withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
						: Component.translatable("ratcheese.achievement.hidden").withStyle(ChatFormatting.DARK_GRAY))
				: achievement.description().copy().withStyle(unlocked ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY);

		graphics.text(font, name, x + 9, y + 5, CommonColors.WHITE, true);
		graphics.text(font, desc, x + 9, y + 17, CommonColors.WHITE, false);

		if (unlocked) {
			Component check = Component.literal("✔").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
			graphics.text(font, check, x + width - font.width(check) - 8, y + 5, CommonColors.WHITE, true);
			Long unlockedAt = AchievementManager.unlockedAt(achievement);
			if (unlockedAt != null) {
				Component date = Component.literal(DATE_FORMAT.format(Instant.ofEpochMilli(unlockedAt).atZone(ZoneId.systemDefault())))
						.withStyle(ChatFormatting.DARK_GRAY);
				graphics.text(font, date, x + width - font.width(date) - 8, y + 17, CommonColors.WHITE, false);
			}
		} else if (hidden) {
			Component q = Component.literal("?").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD);
			graphics.text(font, q, x + width - font.width(q) - 8, y + (CARD_HEIGHT - 8) / 2, CommonColors.WHITE, true);
		} else {
			// Progress: value text + mini progress bar, right-aligned
			long goal = achievement.goal();
			long value = Math.min(AchievementManager.statValue(achievement.stat), goal);
			Component progress = Component.literal(value + "/" + goal).withStyle(ChatFormatting.GRAY);
			graphics.text(font, progress, x + width - font.width(progress) - 8, y + 5, CommonColors.WHITE, true);

			int miniWidth = 56;
			int miniLeft = x + width - miniWidth - 8;
			int miniTop = y + 19;
			graphics.fill(miniLeft, miniTop, miniLeft + miniWidth, miniTop + 4, 0x80000000);
			int miniFilled = (int) (miniWidth * (value / (double) goal));
			if (miniFilled > 0) graphics.fill(miniLeft, miniTop, miniLeft + miniFilled, miniTop + 4, rarityColor);
		}
	}

	private int contentHeight() {
		return Achievement.values().length * ENTRY_HEIGHT;
	}

	private double maxScroll() {
		int visible = height - TOP - BOTTOM_MARGIN;
		return Math.max(0, contentHeight() - visible);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		scroll = Math.clamp(scroll - verticalAmount * 16, 0, maxScroll());
		return true;
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}
}
