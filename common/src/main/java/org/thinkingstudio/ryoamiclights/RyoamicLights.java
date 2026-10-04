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

import org.lwjgl.glfw.GLFW;
import org.thinkingstudio.ryoamiclights.accessor.WorldRendererAccessor;
import org.thinkingstudio.ryoamiclights.api.DynamicLightHandlers;
import org.thinkingstudio.ryoamiclights.api.item.ItemLightSources;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

/**
 * Represents the RyoamicLights mod.
 *
 * @author LambdAurora
 * @version 2.3.2
 * @since 1.0.0
 */
public class RyoamicLights {
	public static final String NAMESPACE = "ryoamiclights";
	public KeyBinding keyBinding;
	private static final double MAX_RADIUS = 7.75;
	private static final double MAX_RADIUS_SQUARED = MAX_RADIUS * MAX_RADIUS;
	private static RyoamicLights INSTANCE;
	// Whether a class keeps Object's equals and hashCode, so that a set lookup can only match an instance that was added.
	// Looked up with method handles since reflection would load the types used by every public method of the class.
	private static final ClassValue<Boolean> COMPARES_BY_IDENTITY = new ClassValue<>() {
		@Override
		protected Boolean computeValue(Class<?> type) {
			try {
				var lookup = MethodHandles.publicLookup();
				var equals = lookup.revealDirect(lookup.findVirtual(type, "equals", MethodType.methodType(boolean.class, Object.class))).getDeclaringClass();
				var hashCode = lookup.revealDirect(lookup.findVirtual(type, "hashCode", MethodType.methodType(int.class))).getDeclaringClass();
				return equals == Object.class && hashCode == Object.class;
			} catch (Throwable t) {
				return false;
			}
		}
	};
	public final Logger logger = LogManager.getLogger(NAMESPACE);
	public final DynamicLightsConfig config = new DynamicLightsConfig(this);
	private final Set<DynamicLightSource> dynamicLightSources = new HashSet<>();
	private final ReentrantReadWriteLock lightSourcesLock = new ReentrantReadWriteLock();
	// Set before the first block entity is added to the light sources and never cleared.
	private volatile boolean blockEntityLightSourceAdded = false;
	private long lastUpdate = System.currentTimeMillis();
	private int lastUpdateCount = 0;

	public void clientInit() {
		this.log("Initializing RyoamicLights...");
		this.config.load();
		this.keyBinding = new KeyBinding("key." + NAMESPACE + ".dyn_light", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN,"key." + NAMESPACE + ".category");

		DynamicLightHandlers.registerDefaultHandlers();
	}

	/**
	 * Updates all light sources.
	 *
	 * @param renderer the renderer
	 */
	public void updateAll(@NotNull WorldRenderer renderer) {
		if (!this.config.getDynamicLightsMode().isEnabled())
			return;

		long now = System.currentTimeMillis();
		if (now >= this.lastUpdate + 50) {
			this.lastUpdate = now;
			this.lastUpdateCount = 0;

			this.lightSourcesLock.readLock().lock();
			for (var lightSource : this.dynamicLightSources) {
				if (lightSource.ryoamiclights$updateDynamicLight(renderer)) this.lastUpdateCount++;
			}
			this.lightSourcesLock.readLock().unlock();
		}
	}

	/**
	 * Returns the last number of dynamic light source updates.
	 *
	 * @return the last number of dynamic light source updates
	 */
	public int getLastUpdateCount() {
		return this.lastUpdateCount;
	}

	/**
	 * Returns the lightmap with combined light levels.
	 *
	 * @param pos the position
	 * @param lightmap the vanilla lightmap coordinates
	 * @return the modified lightmap coordinates
	 */
	public int getLightmapWithDynamicLight(@NotNull BlockPos pos, int lightmap) {
		return this.getLightmapWithDynamicLight(this.getDynamicLightLevel(pos), lightmap);
	}

	/**
	 * Returns the lightmap with combined light levels.
	 *
	 * @param entity the entity
	 * @param lightmap the vanilla lightmap coordinates
	 * @return the modified lightmap coordinates
	 */
	public int getLightmapWithDynamicLight(@NotNull Entity entity, int lightmap) {
		int posLightLevel = (int) this.getDynamicLightLevel(entity.getBlockPos());
		int entityLuminance = ((DynamicLightSource) entity).ryoamicLights$getLuminance();

		return this.getLightmapWithDynamicLight(Math.max(posLightLevel, entityLuminance), lightmap);
	}

	/**
	 * Returns the lightmap with combined light levels.
	 *
	 * @param dynamicLightLevel the dynamic light level
	 * @param lightmap the vanilla lightmap coordinates
	 * @return the modified lightmap coordinates
	 */
	public int getLightmapWithDynamicLight(double dynamicLightLevel, int lightmap) {
		if (dynamicLightLevel > 0) {
			// lightmap is (skyLevel << 20 | blockLevel << 4)

			// Get vanilla block light level.
			int blockLevel = LightmapTextureManager.getBlockLightCoordinates(lightmap);
			if (dynamicLightLevel > blockLevel) {
				// Equivalent to a << 4 bitshift with a little quirk: this one ensure more precision (more decimals are saved).
				int luminance = (int) (dynamicLightLevel * 16.0);
				lightmap &= 0xfff00000;
				lightmap |= luminance & 0x000fffff;
			}
		}

		return lightmap;
	}

	/**
	 * Returns the dynamic light level at the specified position.
	 *
	 * @param pos the position
	 * @return the dynamic light level at the specified position
	 */
	public double getDynamicLightLevel(@NotNull BlockPos pos) {
		double result = 0;
		this.lightSourcesLock.readLock().lock();
		for (var lightSource : this.dynamicLightSources) {
			result = maxDynamicLightLevel(pos, lightSource, result);
		}
		this.lightSourcesLock.readLock().unlock();

		return MathHelper.clamp(result, 0, 15);
	}

	/**
	 * Returns the dynamic light level generated by the light source at the specified position.
	 *
	 * @param pos the position
	 * @param lightSource the light source
	 * @param currentLightLevel the current surrounding dynamic light level
	 * @return the dynamic light level at the specified position
	 */
	public static double maxDynamicLightLevel(@NotNull BlockPos pos, @NotNull DynamicLightSource lightSource, double currentLightLevel) {
		int luminance = lightSource.ryoamicLights$getLuminance();
		if (luminance > 0) {
			// Can't use Entity#squaredDistanceTo because of eye Y coordinate.
			double dx = pos.getX() - lightSource.ryoamicLights$getDynamicLightX() + 0.5;
			double dy = pos.getY() - lightSource.ryoamicLights$getDynamicLightY() + 0.5;
			double dz = pos.getZ() - lightSource.ryoamicLights$getDynamicLightZ() + 0.5;

			double distanceSquared = dx * dx + dy * dy + dz * dz;
			// 7.75 because else we would have to update more chunks and that's not a good idea.
			// 15 (max range for blocks) would be too much and a bit cheaty.
			if (distanceSquared <= MAX_RADIUS_SQUARED) {
				double multiplier = 1.0 - Math.sqrt(distanceSquared) / MAX_RADIUS;
				double lightLevel = multiplier * (double) luminance;
				if (lightLevel > currentLightLevel) {
					return lightLevel;
				}
			}
		}
		return currentLightLevel;
	}

	/**
	 * Adds the light source to the tracked light sources.
	 *
	 * @param lightSource the light source to add
	 */
	public void addLightSource(@NotNull DynamicLightSource lightSource) {
		if (!lightSource.ryoamicLights$getDynamicLightWorld().isClient())
			return;
		if (!this.config.getDynamicLightsMode().isEnabled())
			return;
		if (this.containsLightSource(lightSource))
			return;
		if (lightSource instanceof BlockEntity)
			this.blockEntityLightSourceAdded = true;
		this.lightSourcesLock.writeLock().lock();
		this.dynamicLightSources.add(lightSource);
		this.lightSourcesLock.writeLock().unlock();
	}

	/**
	 * Returns whether the light source is tracked or not.
	 *
	 * @param lightSource the light source to check
	 * @return {@code true} if the light source is tracked, else {@code false}
	 */
	public boolean containsLightSource(@NotNull DynamicLightSource lightSource) {
		if (!lightSource.ryoamicLights$getDynamicLightWorld().isClient())
			return false;
		// While no block entity has been added, the set cannot contain this one and the lock is not needed.
		if (!this.blockEntityLightSourceAdded && lightSource instanceof BlockEntity && COMPARES_BY_IDENTITY.get(lightSource.getClass()))
			return false;

		boolean result;
		this.lightSourcesLock.readLock().lock();
		result = this.dynamicLightSources.contains(lightSource);
		this.lightSourcesLock.readLock().unlock();
		return result;
	}

	/**
	 * Returns the number of dynamic light sources that currently emit lights.
	 *
	 * @return the number of dynamic light sources emitting light
	 */
	public int getLightSourcesCount() {
		int result;

		this.lightSourcesLock.readLock().lock();
		result = this.dynamicLightSources.size();
		this.lightSourcesLock.readLock().unlock();

		return result;
	}

	/**
	 * Removes the light source from the tracked light sources.
	 *
	 * @param lightSource the light source to remove
	 */
	public void removeLightSource(@NotNull DynamicLightSource lightSource) {
		this.lightSourcesLock.writeLock().lock();

		var dynamicLightSources = this.dynamicLightSources.iterator();
		DynamicLightSource it;
		while (dynamicLightSources.hasNext()) {
			it = dynamicLightSources.next();
			if (it.equals(lightSource)) {
				dynamicLightSources.remove();
				if (MinecraftClient.getInstance().worldRenderer != null)
					lightSource.ryoamiclights$scheduleTrackedChunksRebuild(MinecraftClient.getInstance().worldRenderer);
				break;
			}
		}

		this.lightSourcesLock.writeLock().unlock();
	}

	/**
	 * Clears light sources.
	 */
	public void clearLightSources() {
		this.lightSourcesLock.writeLock().lock();

		var dynamicLightSources = this.dynamicLightSources.iterator();
		DynamicLightSource it;
		while (dynamicLightSources.hasNext()) {
			it = dynamicLightSources.next();
			dynamicLightSources.remove();
			if (MinecraftClient.getInstance().worldRenderer != null) {
				if (it.ryoamicLights$getLuminance() > 0)
					it.ryoamicLights$resetDynamicLight();
				it.ryoamiclights$scheduleTrackedChunksRebuild(MinecraftClient.getInstance().worldRenderer);
			}
		}

		this.lightSourcesLock.writeLock().unlock();
	}

	/**
	 * Removes light sources if the filter matches.
	 *
	 * @param filter the removal filter
	 */
	public void removeLightSources(@NotNull Predicate<DynamicLightSource> filter) {
		this.lightSourcesLock.writeLock().lock();

		var dynamicLightSources = this.dynamicLightSources.iterator();
		DynamicLightSource it;
		while (dynamicLightSources.hasNext()) {
			it = dynamicLightSources.next();
			if (filter.test(it)) {
				dynamicLightSources.remove();
				if (MinecraftClient.getInstance().worldRenderer != null) {
					if (it.ryoamicLights$getLuminance() > 0)
						it.ryoamicLights$resetDynamicLight();
					it.ryoamiclights$scheduleTrackedChunksRebuild(MinecraftClient.getInstance().worldRenderer);
				}
				break;
			}
		}

		this.lightSourcesLock.writeLock().unlock();
	}

	/**
	 * Removes entities light source from tracked light sources.
	 */
	public void removeEntitiesLightSource() {
		this.removeLightSources(lightSource -> (lightSource instanceof Entity && !(lightSource instanceof PlayerEntity)));
	}

	/**
	 * Removes Creeper light sources from tracked light sources.
	 */
	public void removeCreeperLightSources() {
		this.removeLightSources(entity -> entity instanceof CreeperEntity);
	}

	/**
	 * Removes TNT light sources from tracked light sources.
	 */
	public void removeTntLightSources() {
		this.removeLightSources(entity -> entity instanceof TntEntity);
	}

	/**
	 * Removes block entities light source from tracked light sources.
	 */
	public void removeBlockEntitiesLightSource() {
		this.removeLightSources(lightSource -> lightSource instanceof BlockEntity);
	}

	/**
	 * Prints a message to the terminal.
	 *
	 * @param info the message to print
	 */
	public void log(String info) {
		this.logger.info("[RyoamicLights] " + info);
	}

	/**
	 * Prints a warning message to the terminal.
	 *
	 * @param info the message to print
	 */
	public void warn(String info) {
		this.logger.warn("[RyoamicLights] " + info);
	}

	/**
	 * Schedules a chunk rebuild at the specified chunk position.
	 *
	 * @param renderer the renderer
	 * @param chunkPos the chunk position
	 */
	public static void scheduleChunkRebuild(@NotNull WorldRenderer renderer, @NotNull BlockPos chunkPos) {
		scheduleChunkRebuild(renderer, chunkPos.getX(), chunkPos.getY(), chunkPos.getZ());
	}

	/**
	 * Schedules a chunk rebuild at the specified chunk position.
	 *
	 * @param renderer the renderer
	 * @param chunkPos the packed chunk position
	 */
	public static void scheduleChunkRebuild(@NotNull WorldRenderer renderer, long chunkPos) {
		scheduleChunkRebuild(renderer, BlockPos.unpackLongX(chunkPos), BlockPos.unpackLongY(chunkPos), BlockPos.unpackLongZ(chunkPos));
	}

	public static void scheduleChunkRebuild(@NotNull WorldRenderer renderer, int x, int y, int z) {
		if (MinecraftClient.getInstance().world != null)
			((WorldRendererAccessor) renderer).ryoamiclights$scheduleChunkRebuild(x, y, z, false);
	}

	/**
	 * Updates the tracked chunk sets.
	 *
	 * @param chunkPos the packed chunk position
	 * @param old the set of old chunk coordinates to remove this chunk from it
	 * @param newPos the set of new chunk coordinates to add this chunk to it
	 */
	public static void updateTrackedChunks(@NotNull BlockPos chunkPos, @Nullable LongOpenHashSet old, @Nullable LongOpenHashSet newPos) {
		if (old != null || newPos != null) {
			long pos = chunkPos.asLong();
			if (old != null)
				old.remove(pos);
			if (newPos != null)
				newPos.add(pos);
		}
	}

	/**
	 * Updates the dynamic lights tracking.
	 *
	 * @param lightSource the light source
	 */
	public static void updateTracking(@NotNull DynamicLightSource lightSource) {
		boolean enabled = lightSource.ryoamicLights$isDynamicLightEnabled();
		int luminance = lightSource.ryoamicLights$getLuminance();

		if (!enabled && luminance > 0) {
			lightSource.ryoamicLights$setDynamicLightEnabled(true);
		} else if (enabled && luminance < 1) {
			lightSource.ryoamicLights$setDynamicLightEnabled(false);
		}
	}

	private static boolean isEyeSubmergedInFluid(LivingEntity entity) {
		if (!RyoamicLights.get().config.getWaterSensitiveCheck().get()) {
			return false;
		}

		var eyePos = BlockPos.ofFloored(entity.getX(), entity.getEyeY(), entity.getZ());
		return !entity.getWorld().getFluidState(eyePos).isEmpty();
	}

	public static int getLivingEntityLuminanceFromItems(LivingEntity entity) {
		boolean submergedInFluid = isEyeSubmergedInFluid(entity);
		int luminance = 0;

		for (var equipped : entity.getItemsEquipped()) {
			if (!equipped.isEmpty())
				luminance = Math.max(luminance, RyoamicLights.getLuminanceFromItemStack(equipped, submergedInFluid));
		}

		return luminance;
	}

	/**
	 * Returns the luminance from an item stack.
	 *
	 * @param stack the item stack
	 * @param submergedInWater {@code true} if the stack is submerged in water, else {@code false}
	 * @return the luminance of the item
	 */
	public static int getLuminanceFromItemStack(@NotNull ItemStack stack, boolean submergedInWater) {
		return ItemLightSources.getLuminance(stack, submergedInWater);
	}

	/**
	 * Returns the RyoamicLights mod instance.
	 *
	 * @return the mod instance
	 */
	public static RyoamicLights get() {
        if (INSTANCE == null) {
            INSTANCE = new RyoamicLights();
        }
		return INSTANCE;
	}
}
