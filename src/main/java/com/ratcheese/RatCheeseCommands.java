package com.ratcheese;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.ratcheese.screen.AchievementsScreen;
import com.ratcheese.screen.ConfigScreen;
import com.ratcheese.screen.HudPositionScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class RatCheeseCommands {
	private static final Pattern TEXTURE_HASH = Pattern.compile("texture/([a-f0-9]+)");

	public static void init() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
				literal("ratcheese")
						.executes(context -> {
							RatCheeseMod.openScreenLater(new ConfigScreen(null));
							return 1;
						})
						.then(literal("hud").executes(context -> {
							RatCheeseMod.openScreenLater(new HudPositionScreen(null));
							return 1;
						}))
						.then(literal("achievements").executes(context -> {
							RatCheeseMod.openScreenLater(new AchievementsScreen(null));
							return 1;
						}))
						.then(literal("test").executes(context -> {
							BuffTracker.injectTestData();
							context.getSource().sendFeedback(prefix().append(Component.translatable("ratcheese.command.test")));
							return 1;
						}))
						.then(literal("reset").executes(context -> {
							BuffTracker.resetStats();
							context.getSource().sendFeedback(prefix().append(Component.translatable("ratcheese.command.reset")));
							return 1;
						}))
						.then(literal("scan").executes(context -> scan(context.getSource())))
						.then(literal("addtexture").then(argument("hash", StringArgumentType.greedyString()).executes(context -> {
							String hash = StringArgumentType.getString(context, "hash").trim();
							RatCheeseConfig config = RatCheeseConfig.get();
							if (!hash.isEmpty() && !config.headTextures.contains(hash)) {
								config.headTextures.add(hash);
								RatCheeseConfig.save();
							}
							context.getSource().sendFeedback(prefix().append(Component.translatable("ratcheese.command.addtexture", hash)));
							return 1;
						})))
						.then(literal("addname").then(argument("marker", StringArgumentType.greedyString()).executes(context -> {
							String marker = StringArgumentType.getString(context, "marker").trim();
							RatCheeseConfig config = RatCheeseConfig.get();
							if (!marker.isEmpty() && !config.nameMarkers.contains(marker)) {
								config.nameMarkers.add(marker);
								RatCheeseConfig.save();
							}
							context.getSource().sendFeedback(prefix().append(Component.translatable("ratcheese.command.addname", marker)));
							return 1;
						})))
		));
	}

	private static MutableComponent prefix() {
		return Component.literal("[RatCheese] ").withStyle(ChatFormatting.GOLD);
	}

	/**
	 * Debug helper: lists nearby entities that could be the cheese (armor stands,
	 * dropped items, named entities) together with their names, item model ids and
	 * head texture hashes. Head hashes are clickable to add them to the config,
	 * so the exact cheese texture can be learned in-game while standing next to one.
	 */
	private static int scan(FabricClientCommandSource source) {
		var player = source.getPlayer();
		var level = source.getLevel();
		if (player == null || level == null) return 0;

		source.sendFeedback(prefix().append(Component.translatable("ratcheese.command.scan.header")));
		int found = 0;

		List<Entity> entities = level.getEntities(player, player.getBoundingBox().inflate(8));
		for (Entity entity : entities) {
			String type = entity.getClass().getSimpleName();
			String name = entity.getCustomName() != null ? entity.getCustomName().getString() : "";
			boolean cheese = CheeseDetector.isCheese(entity);

			if (entity instanceof ArmorStand stand) {
				for (EquipmentSlot slot : EquipmentSlot.values()) {
					if (!stand.hasItemInSlot(slot)) continue;
					ItemInstance stack = stand.getItemBySlot(slot);
					found++;
					source.sendFeedback(describeItem(type, name, slot.getName(), stack, cheese));
				}
				if (!name.isEmpty() && !stand.hasItemInSlot(EquipmentSlot.HEAD)) {
					found++;
					source.sendFeedback(describeEntity(type, name, cheese));
				}
			} else if (entity instanceof ItemEntity item) {
				found++;
				source.sendFeedback(describeItem(type, name, "item", item.getItem(), cheese));
			} else if (!name.isEmpty()) {
				found++;
				source.sendFeedback(describeEntity(type, name, cheese));
			}
		}

		if (found == 0) {
			source.sendFeedback(prefix().append(Component.translatable("ratcheese.command.scan.empty")));
		}
		return 1;
	}

	private static Component describeEntity(String type, String name, boolean cheese) {
		return Component.literal(" - " + type + " ").withStyle(cheese ? ChatFormatting.GREEN : ChatFormatting.GRAY)
				.append(Component.literal("\"" + name + "\"").withStyle(ChatFormatting.WHITE));
	}

	private static Component describeItem(String type, String entityName, String where, ItemInstance stack, boolean cheese) {
		var line = Component.literal(" - " + type + " [" + where + "] ")
				.withStyle(cheese ? ChatFormatting.GREEN : ChatFormatting.GRAY);

		Component customName = stack.get(DataComponents.CUSTOM_NAME);
		if (customName != null) {
			line.append(Component.literal("\"" + customName.getString() + "\" ").withStyle(ChatFormatting.WHITE));
		} else if (!entityName.isEmpty()) {
			line.append(Component.literal("\"" + entityName + "\" ").withStyle(ChatFormatting.WHITE));
		}

		Identifier model = stack.get(DataComponents.ITEM_MODEL);
		if (model != null) {
			line.append(Component.literal(model + " ").withStyle(ChatFormatting.AQUA));
		}

		String texture = CheeseDetector.getHeadTextureValue(stack);
		if (!texture.isEmpty()) {
			String decoded = CheeseDetector.decodeBase64(texture);
			Matcher matcher = TEXTURE_HASH.matcher(decoded);
			if (matcher.find()) {
				String hash = matcher.group(1);
				String shortHash = hash.length() > 12 ? hash.substring(0, 12) + "…" : hash;
				line.append(Component.literal("head:" + shortHash + " ").withStyle(ChatFormatting.YELLOW));
				line.append(Component.literal("[+]").withStyle(style -> style
						.withColor(ChatFormatting.GREEN)
						.withClickEvent(new ClickEvent.RunCommand("/ratcheese addtexture " + hash))));
			}
		}

		return line;
	}
}
