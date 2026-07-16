package com.ratcheese.screen;

import com.ratcheese.RatCheeseConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import org.jspecify.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

public class ConfigScreen extends Screen {
	private record ColorPreset(String nameKey, int rgb) {}

	private static final ColorPreset[] COLORS = {
			new ColorPreset("ratcheese.color.yellow", 0xFFCC00),
			new ColorPreset("ratcheese.color.orange", 0xFF8800),
			new ColorPreset("ratcheese.color.red", 0xFF4444),
			new ColorPreset("ratcheese.color.green", 0x44FF44),
			new ColorPreset("ratcheese.color.aqua", 0x44FFFF),
			new ColorPreset("ratcheese.color.magenta", 0xFF44FF),
			new ColorPreset("ratcheese.color.white", 0xFFFFFF)
	};

	private final @Nullable Screen parent;

	public ConfigScreen(@Nullable Screen parent) {
		super(Component.translatable("ratcheese.config.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		RatCheeseConfig config = RatCheeseConfig.get();
		int columnWidth = Math.min(180, (width - 30) / 2);
		int leftX = width / 2 - columnWidth - 5;
		int rightX = width / 2 + 5;
		int y = 40;

		// Row 1: highlight toggle | glow color
		addRenderableWidget(toggleButton("ratcheese.config.highlight",
				() -> config.highlightEnabled, v -> config.highlightEnabled = v, leftX, y, columnWidth));
		addRenderableWidget(colorButton(config, rightX, y, columnWidth));
		y += 24;

		// Row 2: cheese scale | hud toggle
		addRenderableWidget(slider("ratcheese.config.cheeseScale", config.cheeseScale, 1.0, 5.0, 0.25,
				v -> config.cheeseScale = v, leftX, y, columnWidth));
		addRenderableWidget(toggleButton("ratcheese.config.hud",
				() -> config.hudEnabled, v -> config.hudEnabled = v, rightX, y, columnWidth));
		y += 24;

		// Row 3: hud scale | hud background
		addRenderableWidget(slider("ratcheese.config.hudScale", config.hudScale, 0.5, 3.0, 0.1,
				v -> config.hudScale = v, leftX, y, columnWidth));
		addRenderableWidget(toggleButton("ratcheese.config.hudBackground",
				() -> config.hudBackground, v -> config.hudBackground = v, rightX, y, columnWidth));
		y += 24;

		// Row 4: show outgoing | show incoming
		addRenderableWidget(toggleButton("ratcheese.config.showOutgoing",
				() -> config.showOutgoing, v -> config.showOutgoing = v, leftX, y, columnWidth));
		addRenderableWidget(toggleButton("ratcheese.config.showIncoming",
				() -> config.showIncoming, v -> config.showIncoming = v, rightX, y, columnWidth));
		y += 24;

		// Row 5: loot shares | smell alerts
		addRenderableWidget(toggleButton("ratcheese.config.showLootShares",
				() -> config.showLootShares, v -> config.showLootShares = v, leftX, y, columnWidth));
		addRenderableWidget(toggleButton("ratcheese.config.smellTitle",
				() -> config.smellAlertTitle, v -> config.smellAlertTitle = v, rightX, y, columnWidth));
		y += 24;

		// Row 6: smell sound | timer style
		addRenderableWidget(toggleButton("ratcheese.config.smellSound",
				() -> config.smellAlertSound, v -> config.smellAlertSound = v, leftX, y, columnWidth));
		addRenderableWidget(toggleButton("ratcheese.config.timerBars",
				() -> config.timerBars, v -> config.timerBars = v, rightX, y, columnWidth));
		y += 24;

		// Row 7: edit hud position | achievements
		addRenderableWidget(Button.builder(Component.translatable("ratcheese.config.editHud"),
				button -> minecraft.setScreen(new HudPositionScreen(this))).bounds(leftX, y, columnWidth, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("ratcheese.config.achievements"),
				button -> minecraft.setScreen(new AchievementsScreen(this))).bounds(rightX, y, columnWidth, 20).build());
		y += 28;

		addRenderableWidget(Button.builder(Component.translatable("ratcheese.config.done"),
				button -> onClose()).bounds(width / 2 - 75, y, 150, 20).build());
	}

	private Button toggleButton(String key, BooleanSupplier getter, Consumer<Boolean> setter, int x, int y, int width) {
		return Button.builder(toggleLabel(key, getter.getAsBoolean()), button -> {
			boolean newValue = !getter.getAsBoolean();
			setter.accept(newValue);
			button.setMessage(toggleLabel(key, newValue));
		}).bounds(x, y, width, 20).build();
	}

	private static Component toggleLabel(String key, boolean on) {
		return Component.translatable(key).append(": ").append(on
				? Component.translatable("ratcheese.on").withStyle(ChatFormatting.GREEN)
				: Component.translatable("ratcheese.off").withStyle(ChatFormatting.RED));
	}

	private Button colorButton(RatCheeseConfig config, int x, int y, int width) {
		return Button.builder(colorLabel(config.glowColor), button -> {
			int index = 0;
			for (int i = 0; i < COLORS.length; i++) {
				if (COLORS[i].rgb() == config.glowColor) {
					index = (i + 1) % COLORS.length;
					break;
				}
			}
			config.glowColor = COLORS[index].rgb();
			button.setMessage(colorLabel(config.glowColor));
		}).bounds(x, y, width, 20).build();
	}

	private static Component colorLabel(int rgb) {
		String nameKey = "ratcheese.color.custom";
		for (ColorPreset preset : COLORS) {
			if (preset.rgb() == rgb) {
				nameKey = preset.nameKey();
				break;
			}
		}
		return Component.translatable("ratcheese.config.color").append(": ")
				.append(Component.literal("■ ").withColor(rgb))
				.append(Component.translatable(nameKey));
	}

	private AbstractSliderButton slider(String key, double value, double min, double max, double step,
			DoubleConsumer onApply, int x, int y, int width) {
		return new AbstractSliderButton(x, y, width, 20, Component.empty(), (value - min) / (max - min)) {
			{
				updateMessage();
			}

			private double currentValue() {
				double raw = min + this.value * (max - min);
				return Math.round(raw / step) * step;
			}

			@Override
			protected void updateMessage() {
				setMessage(Component.translatable(key).append(": ")
						.append(Component.literal(String.format("%.2f", currentValue()) + "x").withStyle(ChatFormatting.YELLOW)));
			}

			@Override
			protected void applyValue() {
				onApply.accept(currentValue());
			}
		};
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(font, title, width / 2, 15, CommonColors.WHITE);
	}

	@Override
	public void onClose() {
		RatCheeseConfig.save();
		minecraft.setScreen(parent);
	}
}
