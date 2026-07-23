package com.ratcheese;

import com.mojang.authlib.properties.Property;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Base64;
import java.util.Locale;

/**
 * Periodically scans entities around the player and remembers which ones look like
 * the cheese spawned by the Rat pet. Hypixel builds the cheese out of props
 * (armor stands / dropped items carrying a "cheese" item model, name or head texture),
 * so detection is heuristic and configurable via name markers and head texture hashes.
 */
public class CheeseDetector {
	/** Attached to cheese render states; read by the render dispatcher mixin to enlarge them. */
	public static final RenderStateDataKey<Float> CHEESE_SCALE = RenderStateDataKey.create(() -> "ratcheese cheese scale");

	private static final IntOpenHashSet CHEESE_IDS = new IntOpenHashSet();
	private static int tickCounter = 0;

	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(CheeseDetector::tick);
	}

	public static boolean isCheese(Entity entity) {
		return !CHEESE_IDS.isEmpty() && CHEESE_IDS.contains(entity.getId());
	}

	private static void tick(Minecraft client) {
		if (++tickCounter % 5 != 0) return;

		CHEESE_IDS.clear();
		if (client.level == null || client.player == null) return;

		RatCheeseConfig config = RatCheeseConfig.get();
		if (!config.highlightEnabled) return;

		AABB box = client.player.getBoundingBox().inflate(config.scanRadius);
		for (Entity entity : client.level.getEntities(client.player, box)) {
			// Line-of-sight gate: highlighting entities hidden behind opaque blocks
			// counts as X-ray (Modrinth content rule 3.3a), so hidden cheese is
			// neither glowed nor enlarged.
			if (isCheeseEntity(entity, config) && isVisible(client, entity)) {
				CHEESE_IDS.add(entity.getId());
			}
		}
	}

	/** True if the camera has an unobstructed view of the entity (glass etc. does not block). */
	private static boolean isVisible(Minecraft client, Entity entity) {
		if (client.player == null || client.level == null) return false;
		Vec3 from = client.gameRenderer.getMainCamera().position();
		AABB box = entity.getBoundingBox();
		Vec3 center = box.getCenter();
		Vec3[] targets = {center, new Vec3(center.x, box.maxY, center.z)};
		for (Vec3 to : targets) {
			BlockHitResult hit = client.level.clip(new ClipContext(from, to,
					ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, client.player));
			if (hit.getType() == HitResult.Type.MISS) return true;
		}
		return false;
	}

	private static boolean isCheeseEntity(Entity entity, RatCheeseConfig config) {
		Component customName = entity.getCustomName();
		if (customName != null && containsMarker(customName.getString(), config)) return true;

		if (entity instanceof ArmorStand stand) {
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				if (!stand.hasItemInSlot(slot)) continue;
				if (isCheeseItem(stand.getItemBySlot(slot), config)) return true;
			}
		} else if (entity instanceof ItemEntity item) {
			return isCheeseItem(item.getItem(), config);
		}

		return false;
	}

	public static boolean isCheeseItem(ItemInstance stack, RatCheeseConfig config) {
		if (stack == null) return false;

		Component customName = stack.get(DataComponents.CUSTOM_NAME);
		if (customName != null && containsMarker(customName.getString(), config)) return true;

		Component itemName = stack.get(DataComponents.ITEM_NAME);
		if (itemName != null && containsMarker(itemName.getString(), config)) return true;

		Identifier model = stack.get(DataComponents.ITEM_MODEL);
		if (model != null && containsMarker(model.toString(), config)) return true;

		return matchesHeadTexture(stack, config);
	}

	private static boolean containsMarker(String text, RatCheeseConfig config) {
		if (text == null || text.isEmpty()) return false;
		String lower = text.toLowerCase(Locale.ROOT);
		for (String marker : config.nameMarkers) {
			if (!marker.isBlank() && lower.contains(marker.toLowerCase(Locale.ROOT))) return true;
		}
		return false;
	}

	private static boolean matchesHeadTexture(ItemInstance stack, RatCheeseConfig config) {
		if (config.headTextures.isEmpty() || !stack.is(Items.PLAYER_HEAD)) return false;

		String texture = getHeadTextureValue(stack);
		if (texture.isEmpty()) return false;
		String decoded = decodeBase64(texture);

		for (String marker : config.headTextures) {
			if (marker.isBlank()) continue;
			if (texture.contains(marker) || decoded.contains(marker)) return true;
		}
		return false;
	}

	/** Returns the raw base64 "textures" property of a player head, or "". */
	public static String getHeadTextureValue(ItemInstance stack) {
		if (!stack.is(Items.PLAYER_HEAD)) return "";
		ResolvableProfile profile = stack.get(DataComponents.PROFILE);
		if (profile == null) return "";
		return profile.partialProfile().properties().get("textures").stream()
				.filter(java.util.Objects::nonNull)
				.map(Property::value)
				.findFirst()
				.orElse("");
	}

	public static String decodeBase64(String value) {
		try {
			return new String(Base64.getDecoder().decode(value));
		} catch (IllegalArgumentException e) {
			return "";
		}
	}
}
