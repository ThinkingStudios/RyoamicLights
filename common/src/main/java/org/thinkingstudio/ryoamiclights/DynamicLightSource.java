/*
 * Copyright © 2020~2024 LambdAurora <email@lambdaurora.dev>
 * Copyright © 2024 ThinkingStudio
 *
 * This file is part of RyoamicLights.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package org.thinkingstudio.ryoamiclights;

import net.minecraft.client.render.WorldRenderer;
import net.minecraft.world.World;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a dynamic light source.
 *
 * @author LambdAurora
 * @version 1.3.3
 * @since 1.0.0
 */
public interface DynamicLightSource {
	/**
	 * Returns the dynamic light source X coordinate.
	 *
	 * @return the X coordinate
	 */
	double ryoamicLights$getDynamicLightX();

	/**
	 * Returns the dynamic light source Y coordinate.
	 *
	 * @return the Y coordinate
	 */
	double ryoamicLights$getDynamicLightY();

	/**
	 * Returns the dynamic light source Z coordinate.
	 *
	 * @return the Z coordinate
	 */
	double ryoamicLights$getDynamicLightZ();

	/**
	 * Returns the dynamic light source world.
	 *
	 * @return the world instance
	 */
	World ryoamicLights$getDynamicLightWorld();

	/**
	 * Returns whether the dynamic light is enabled or not.
	 *
	 * @return {@code true} if the dynamic light is enabled, else {@code false}
	 */
	default boolean ryoamicLights$isDynamicLightEnabled() {
		return RyoamicLights.get().config.getDynamicLightsMode().isEnabled() && RyoamicLights.get().containsLightSource(this);
	}

	/**
	 * Sets whether the dynamic light is enabled or not.
	 * <p>
	 * Note: please do not call this function in your mod or you will break things.
	 *
	 * @param enabled {@code true} if the dynamic light is enabled, else {@code false}
	 */
	@ApiStatus.Internal
	default void ryoamicLights$setDynamicLightEnabled(boolean enabled) {
		this.ryoamicLights$resetDynamicLight();
		if (enabled)
			RyoamicLights.get().addLightSource(this);
		else
			RyoamicLights.get().removeLightSource(this);
	}

	void ryoamicLights$resetDynamicLight();

	/**
	 * Returns the luminance of the light source.
	 * The maximum is 15, below 1 values are ignored.
	 *
	 * @return the luminance of the light source
	 */
	int ryoamicLights$getLuminance();

	/**
	 * Executed at each tick.
	 */
	void ryoamicLights$dynamicLightTick();

	/**
	 * Returns whether this dynamic light source should update.
	 *
	 * @return {@code true} if this dynamic light source should update, else {@code false}
	 */
	boolean ryoamicLights$shouldUpdateDynamicLight();

	boolean ryoamiclights$updateDynamicLight(@NotNull WorldRenderer renderer);

	void ryoamiclights$scheduleTrackedChunksRebuild(@NotNull WorldRenderer renderer);
}
