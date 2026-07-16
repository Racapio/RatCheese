package com.ratcheese.mixin;

import com.ratcheese.CheeseDetector;
import com.ratcheese.RatCheeseConfig;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
	/**
	 * Marks cheese render states: colors the glow outline and attaches the
	 * configured scale factor, which {@link EntityRenderDispatcherMixin} applies
	 * to the pose stack (works for armor stands, dropped items and displays alike).
	 */
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void ratcheese$markCheese(Entity entity, EntityRenderState state, float partialTicks, CallbackInfo ci) {
		RatCheeseConfig config = RatCheeseConfig.get();
		if (!config.highlightEnabled || !CheeseDetector.isCheese(entity)) return;

		state.outlineColor = ARGB.opaque(config.glowColor);
		if (config.cheeseScale > 1.0) {
			state.setData(CheeseDetector.CHEESE_SCALE, (float) config.cheeseScale);
		}
	}
}
