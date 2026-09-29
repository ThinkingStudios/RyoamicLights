/*
 * Copyright © 2020~2024 LambdAurora <email@lambdaurora.dev>
 * Copyright © 2024 ThinkingStudio
 *
 * This file is part of RyoamicLights.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package org.thinkingstudio.ryoamiclights.mixin.lightsource;

import org.thinkingstudio.ryoamiclights.DynamicLightSource;
import org.thinkingstudio.ryoamiclights.RyoamicLights;
import org.thinkingstudio.ryoamiclights.api.DynamicLightHandlers;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin implements DynamicLightSource {
	@Final
	@Shadow
	protected BlockPos pos;

	@Shadow
	@Nullable
	protected World world;

	@Shadow
	protected boolean removed;

	@Unique
	private int luminance = 0;
	@Unique
	private int lastLuminance = 0;
	@Unique
	private long lastUpdate = 0;
	@Unique
	private final LongOpenHashSet ryoamiclights$trackedLitChunkPos = new LongOpenHashSet();

	@Override
	public double ryoamicLights$getDynamicLightX() {
		return this.pos.getX() + 0.5;
	}

	@Override
	public double ryoamicLights$getDynamicLightY() {
		return this.pos.getY() + 0.5;
	}

	@Override
	public double ryoamicLights$getDynamicLightZ() {
		return this.pos.getZ() + 0.5;
	}

	@Override
	public World ryoamicLights$getDynamicLightWorld() {
		return this.world;
	}

	@Inject(method = "markRemoved", at = @At("TAIL"))
	private void onRemoved(CallbackInfo ci) {
		this.ryoamicLights$setDynamicLightEnabled(false);
	}

	@Override
	public void ryoamicLights$resetDynamicLight() {
		this.lastLuminance = 0;
	}

	@Override
	public void ryoamicLights$dynamicLightTick() {
		// We do not want to update the entity on the server.
		if (this.world == null || !this.world.isClient())
			return;
		if (!this.removed) {
			this.luminance = DynamicLightHandlers.getLuminanceFrom((BlockEntity) (Object) this);
			RyoamicLights.updateTracking(this);

			if (!this.ryoamicLights$isDynamicLightEnabled()) {
				this.lastLuminance = 0;
			}
		}
	}

	@Override
	public int ryoamicLights$getLuminance() {
		return this.luminance;
	}

	@Override
	public boolean ryoamicLights$shouldUpdateDynamicLight() {
		var mode = RyoamicLights.get().config.getDynamicLightsMode();
		if (!mode.isEnabled())
			return false;
		if (mode.hasDelay()) {
			long currentTime = System.currentTimeMillis();
			if (currentTime < this.lastUpdate + mode.getDelay()) {
				return false;
			}

			this.lastUpdate = currentTime;
		}
		return true;
	}

	@Override
	public boolean ryoamiclights$updateDynamicLight(@NotNull WorldRenderer renderer) {
		if (!this.ryoamicLights$shouldUpdateDynamicLight())
			return false;

		int luminance = this.ryoamicLights$getLuminance();

		if (luminance != this.lastLuminance) {
			this.lastLuminance = luminance;

			if (this.ryoamiclights$trackedLitChunkPos.isEmpty()) {
				var chunkPos = new Mutable(MathHelper.floorDiv(this.pos.getX(), 16),
						MathHelper.floorDiv(this.pos.getY(), 16),
						MathHelper.floorDiv(this.pos.getZ(), 16));

				RyoamicLights.updateTrackedChunks(chunkPos, null, this.ryoamiclights$trackedLitChunkPos);

				var directionX = (this.pos.getX() & 15) >= 8 ? Direction.EAST : Direction.WEST;
				var directionY = (this.pos.getY() & 15) >= 8 ? Direction.UP : Direction.DOWN;
				var directionZ = (this.pos.getZ() & 15) >= 8 ? Direction.SOUTH : Direction.NORTH;

				for (int i = 0; i < 7; i++) {
					if (i % 4 == 0) {
						chunkPos.move(directionX); // X
					} else if (i % 4 == 1) {
						chunkPos.move(directionZ); // XZ
					} else if (i % 4 == 2) {
						chunkPos.move(directionX.getOpposite()); // Z
					} else {
						chunkPos.move(directionZ.getOpposite()); // origin
						chunkPos.move(directionY); // Y
					}
					RyoamicLights.updateTrackedChunks(chunkPos, null, this.ryoamiclights$trackedLitChunkPos);
				}
			}

			// Schedules the rebuild of chunks.
			this.ryoamiclights$scheduleTrackedChunksRebuild(renderer);
			return true;
		}
		return false;
	}

	@Override
	public void ryoamiclights$scheduleTrackedChunksRebuild(@NotNull WorldRenderer renderer) {
		if (this.world == MinecraftClient.getInstance().world)
			for (LongIterator iterator = this.ryoamiclights$trackedLitChunkPos.iterator(); iterator.hasNext(); ) {
				long pos = iterator.nextLong();
				RyoamicLights.scheduleChunkRebuild(renderer, pos);
			}
	}
}
