package com.ratcheese.screen;

import com.ratcheese.RatCheeseConfig;
import com.ratcheese.hud.CheeseHud;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Drag-and-drop HUD position editor, UX inspired by SkyHanni's GuiPositionEditor:
 * hover highlight, live coordinates, scroll wheel to scale, arrow keys to nudge,
 * right click to reset.
 */
public class HudPositionScreen extends Screen {
	private static final int BORDER = 2;

	private final @Nullable Screen parent;
	private boolean dragging = false;
	private double grabOffsetX;
	private double grabOffsetY;

	public HudPositionScreen(@Nullable Screen parent) {
		super(Component.translatable("ratcheese.hudposition.title"));
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
		graphics.centeredText(font, title, width / 2, 15, CommonColors.WHITE);

		RatCheeseConfig config = RatCheeseConfig.get();
		boolean hovered = isOverHud(mouseX, mouseY);

		// Hints in the middle of the screen
		graphics.centeredText(font, Component.translatable("ratcheese.hudposition.hint1"), width / 2, height / 2 - 10, CommonColors.GRAY);
		graphics.centeredText(font, Component.translatable("ratcheese.hudposition.hint2"), width / 2, height / 2 + 2, CommonColors.GRAY);

		// Highlight box behind the HUD (SkyHanni style: gray, brighter when hovered/dragged)
		int w = scaledWidth();
		int h = scaledHeight();
		int boxColor = (dragging || hovered) ? 0x80F0F0F0 : 0x80404040;
		graphics.fill(config.hudX - BORDER, config.hudY - BORDER, config.hudX + w + BORDER, config.hudY + h + BORDER, boxColor);

		// The HUD preview itself
		List<CheeseHud.Row> rows = CheeseHud.buildRows(true);
		CheeseHud.draw(graphics, font, rows, config);

		// Live position / scale info next to the box
		if (dragging || hovered) {
			Component info = Component.translatable("ratcheese.hudposition.info",
					config.hudX, config.hudY, String.format("%.2f", config.hudScale)).withStyle(ChatFormatting.YELLOW);
			int infoY = config.hudY + h + BORDER + 4;
			if (infoY + 10 > height) infoY = config.hudY - 14;
			graphics.text(font, info, config.hudX, infoY, CommonColors.WHITE);
		}
	}

	private boolean isOverHud(double x, double y) {
		RatCheeseConfig config = RatCheeseConfig.get();
		int w = scaledWidth();
		int h = scaledHeight();
		return x >= config.hudX - BORDER && x <= config.hudX + w + BORDER
				&& y >= config.hudY - BORDER && y <= config.hudY + h + BORDER;
	}

	private int scaledWidth() {
		return (int) (CheeseHud.boxWidth(font, CheeseHud.buildRows(true)) * RatCheeseConfig.get().hudScale);
	}

	private int scaledHeight() {
		return (int) (CheeseHud.boxHeight(CheeseHud.buildRows(true)) * RatCheeseConfig.get().hudScale);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		RatCheeseConfig config = RatCheeseConfig.get();
		if (click.button() == 0 && isOverHud(click.x(), click.y())) {
			dragging = true;
			grabOffsetX = click.x() - config.hudX;
			grabOffsetY = click.y() - config.hudY;
			return true;
		} else if (click.button() == 1 && isOverHud(click.x(), click.y())) {
			config.hudX = 5;
			config.hudY = 40;
			return true;
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
		if (dragging && click.button() == 0) {
			moveTo((int) (click.x() - grabOffsetX), (int) (click.y() - grabOffsetY));
			return true;
		}
		return super.mouseDragged(click, offsetX, offsetY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		dragging = false;
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (isOverHud(mouseX, mouseY) && verticalAmount != 0) {
			RatCheeseConfig config = RatCheeseConfig.get();
			config.hudScale = Math.clamp(config.hudScale + (verticalAmount > 0 ? 0.1 : -0.1), 0.5, 3.0);
			// Keep the box on screen after resizing
			moveTo(config.hudX, config.hudY);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
		RatCheeseConfig config = RatCheeseConfig.get();
		switch (input.key()) {
			case GLFW.GLFW_KEY_LEFT -> { moveTo(config.hudX - 1, config.hudY); return true; }
			case GLFW.GLFW_KEY_RIGHT -> { moveTo(config.hudX + 1, config.hudY); return true; }
			case GLFW.GLFW_KEY_UP -> { moveTo(config.hudX, config.hudY - 1); return true; }
			case GLFW.GLFW_KEY_DOWN -> { moveTo(config.hudX, config.hudY + 1); return true; }
			default -> { return super.keyPressed(input); }
		}
	}

	private void moveTo(int x, int y) {
		RatCheeseConfig config = RatCheeseConfig.get();
		config.hudX = Math.clamp(x, 0, Math.max(0, width - scaledWidth()));
		config.hudY = Math.clamp(y, 0, Math.max(0, height - scaledHeight()));
	}

	@Override
	public void onClose() {
		RatCheeseConfig.save();
		minecraft.setScreen(parent);
	}
}
