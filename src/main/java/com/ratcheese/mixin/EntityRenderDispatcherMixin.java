package com.ratcheese.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ratcheese.CheeseDetector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
	/**
	 * Enlarges cheese entities. Injected right before the renderer-specific submit
	 * call: the dispatcher has already pushed and translated the pose stack (and pops
	 * it afterwards), so scaling here is safe and works for every entity type —
	 * armor stands, dropped items, item displays, etc.
	 */
	@Inject(method = "submit", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"))
	private void ratcheese$scaleCheese(EntityRenderState state, CameraRenderState camera, double x, double y, double z,
			PoseStack poseStack, SubmitNodeCollector collector, CallbackInfo ci) {
		float scale = state.getDataOrDefault(CheeseDetector.CHEESE_SCALE, 1.0f);
		if (scale != 1.0f) {
			poseStack.scale(scale, scale, scale);
		}
	}
}
