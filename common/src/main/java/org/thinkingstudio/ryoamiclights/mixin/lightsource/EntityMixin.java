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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.*;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin implements DynamicLightSource {
	@Shadow
	public World world;

	@Shadow
	public abstract double getX();

	@Shadow
	public abstract double getEyeY();

	@Shadow
	public abstract double getZ();

	@Shadow
	public abstract double getY();

	@Shadow
	public abstract boolean isOnFire();

	@Shadow
	public abstract EntityType<?> getType();

	@Shadow
	public abstract BlockPos getBlockPos();

	@Shadow
	public abstract boolean isRemoved();

	@Shadow
	public abstract ChunkPos getChunkPos();

	@Unique
	protected int ryoamiclights$luminance = 0;
	@Unique
	private int ryoamiclights$lastLuminance = 0;
	@Unique
	private long ryoamiclights$lastUpdate = 0;
	@Unique
	private double ryoamiclights$prevX;
	@Unique
	private double ryoamiclights$prevY;
	@Unique
	private double ryoamiclights$prevZ;
	@Unique
	private LongOpenHashSet ryoamiclights$trackedLitChunkPos = new LongOpenHashSet();

	@Inject(method = "tick", at = @At("TAIL"))
	public void onTick(CallbackInfo ci) {
		// We do not want to update the entity on the server.
		if (this.world.isClient()) {
			if (this.isRemoved()) {
				this.ryoamicLights$setDynamicLightEnabled(false);
			} else {
				this.ryoamicLights$dynamicLightTick();
				if ((!RyoamicLights.get().config.getEntitiesLightSource().get() && this.getType() != EntityType.PLAYER)
						|| !DynamicLightHandlers.canLightUp((Entity) (Object) this))
					this.ryoamiclights$luminance = 0;
				RyoamicLights.updateTracking(this);
			}
		}
	}

	@Inject(method = "remove", at = @At("TAIL"))
	public void onRemove(CallbackInfo ci) {
		if (this.world.isClient())
			this.ryoamicLights$setDynamicLightEnabled(false);
	}

	@Override
	public double ryoamicLights$getDynamicLightX() {
		return this.getX();
	}

	@Override
	public double ryoamicLights$getDynamicLightY() {
		return this.getEyeY();
	}

	@Override
	public double ryoamicLights$getDynamicLightZ() {
		return this.getZ();
	}

	@Override
	public World ryoamicLights$getDynamicLightWorld() {
		return this.world;
	}

	@Override
	public void ryoamicLights$resetDynamicLight() {
		this.ryoamiclights$lastLuminance = 0;
	}

	@Override
	public boolean ryoamicLights$shouldUpdateDynamicLight() {
		var mode = RyoamicLights.get().config.getDynamicLightsMode();
		if (!mode.isEnabled())
			return false;
		if (mode.hasDelay()) {
			long currentTime = System.currentTimeMillis();
			if (currentTime < this.ryoamiclights$lastUpdate + mode.getDelay()) {
				return false;
			}

			this.ryoamiclights$lastUpdate = currentTime;
		}
		return true;
	}

	@Override
	public void ryoamicLights$dynamicLightTick() {
		this.ryoamiclights$luminance = this.isOnFire() ? 15 : 0;

		int luminance = DynamicLightHandlers.getLuminanceFrom((Entity) (Object) this);
		if (luminance > this.ryoamiclights$luminance)
			this.ryoamiclights$luminance = luminance;
	}

	@Override
	public int ryoamicLights$getLuminance() {
		return this.ryoamiclights$luminance;
	}

	@Override
	public boolean ryoamiclights$updateDynamicLight(@NotNull WorldRenderer renderer) {
		if (!this.ryoamicLights$shouldUpdateDynamicLight())
			return false;
		double deltaX = this.getX() - this.ryoamiclights$prevX;
		double deltaY = this.getY() - this.ryoamiclights$prevY;
		double deltaZ = this.getZ() - this.ryoamiclights$prevZ;

		int luminance = this.ryoamicLights$getLuminance();

		if (Math.abs(deltaX) > 0.1D || Math.abs(deltaY) > 0.1D || Math.abs(deltaZ) > 0.1D || luminance != this.ryoamiclights$lastLuminance) {
			this.ryoamiclights$prevX = this.getX();
			this.ryoamiclights$prevY = this.getY();
			this.ryoamiclights$prevZ = this.getZ();
			this.ryoamiclights$lastLuminance = luminance;

			var newPos = new LongOpenHashSet();

			if (luminance > 0) {
				var entityChunkPos = this.getChunkPos();
				var chunkPos = new Mutable(entityChunkPos.x, ChunkSectionPos.getSectionCoord(this.getEyeY()), entityChunkPos.z);

				RyoamicLights.scheduleChunkRebuild(renderer, chunkPos);
				RyoamicLights.updateTrackedChunks(chunkPos, this.ryoamiclights$trackedLitChunkPos, newPos);

				var directionX = (this.getBlockPos().getX() & 15) >= 8 ? Direction.EAST : Direction.WEST;
				var directionY = (MathHelper.floor(this.getEyeY()) & 15) >= 8 ? Direction.UP : Direction.DOWN;
				var directionZ = (this.getBlockPos().getZ() & 15) >= 8 ? Direction.SOUTH : Direction.NORTH;

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
					RyoamicLights.scheduleChunkRebuild(renderer, chunkPos);
					RyoamicLights.updateTrackedChunks(chunkPos, this.ryoamiclights$trackedLitChunkPos, newPos);
				}
			}

			// Schedules the rebuild of removed chunks.
			this.ryoamiclights$scheduleTrackedChunksRebuild(renderer);
			// Update tracked lit chunks.
			this.ryoamiclights$trackedLitChunkPos = newPos;
			return true;
		}
		return false;
	}

	@Override
	public void ryoamiclights$scheduleTrackedChunksRebuild(@NotNull WorldRenderer renderer) {
		if (MinecraftClient.getInstance().world == this.world)
			for (LongIterator iterator = this.ryoamiclights$trackedLitChunkPos.iterator(); iterator.hasNext(); ) {
				long pos = iterator.nextLong();
				RyoamicLights.scheduleChunkRebuild(renderer, pos);
			}
	}
}
