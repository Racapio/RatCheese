package com.ratcheese.mixin;

import com.ratcheese.CheeseDetector;
import com.ratcheese.RatCheeseConfig;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
	/**
	 * Makes cheese entities glow client-side, exactly like the vanilla glowing effect,
	 * so the whole vanilla outline pipeline (including the outline framebuffer) kicks in.
	 */
	@Inject(method = "isCurrentlyGlowing", at = @At("RETURN"), cancellable = true)
	private void ratcheese$cheeseGlow(CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ()
				&& RatCheeseConfig.get().highlightEnabled
				&& CheeseDetector.isCheese((Entity) (Object) this)) {
			cir.setReturnValue(true);
		}
	}
}
