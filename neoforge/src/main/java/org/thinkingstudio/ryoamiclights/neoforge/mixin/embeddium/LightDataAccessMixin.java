/*
 * Copyright © 2023 LambdAurora <email@lambdaurora.dev>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package org.thinkingstudio.ryoamiclights.neoforge.mixin.embeddium;

import org.thinkingstudio.ryoamiclights.util.SodiumDynamicLightHandler;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.embeddium.client.model.light.data.LightDataAccess", remap = false)
public abstract class LightDataAccessMixin {
	@Dynamic
	@Inject(method = "getLightmap", at = @At("RETURN"), remap = false, require = 0, cancellable = true)
	private static void lambdynlights$getLightmap(int word, CallbackInfoReturnable<Integer> cir) {
		int lightmap = SodiumDynamicLightHandler.getLightmap(SodiumDynamicLightHandler.pos.get(), word, cir.getReturnValueI());
		cir.setReturnValue(lightmap);
	}
}