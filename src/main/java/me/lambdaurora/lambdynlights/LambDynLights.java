/*
 * Copyright © 2020 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package me.lambdaurora.lambdynlights;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import me.lambdaurora.lambdynlights.accessor.WorldRendererAccessor;
import me.lambdaurora.lambdynlights.api.DynamicLightHandlers;
import me.lambdaurora.lambdynlights.api.item.ItemLightSources;
import me.lambdaurora.lambdynlights.gui.SettingsScreen;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ReloadableResourceManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

/**
 * Represents the LambDynamicLights mod.
 *
 * @author LambdAurora
 * @version 1.3.4
 * @since 1.0.0
 */
@Mod(LambDynLights.MODID)
public class LambDynLights {
    public static final String MODID = "ryoamiclights";
    private static final double MAX_RADIUS = 7.75;
    private static LambDynLights INSTANCE;
    public final Logger logger = LogManager.getLogger(MODID);
    public final DynamicLightsConfig config = new DynamicLightsConfig(this);
    private final ConcurrentLinkedQueue<DynamicLightSource> dynamicLightSources = new ConcurrentLinkedQueue<>();
    private long lastUpdate = System.currentTimeMillis();
    private int lastUpdateCount = 0;

    public LambDynLights() {
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        if (FMLLoader.getDist().isClient()) {
            this.onInitializeClient();

        }
    }

    public void onInitializeClient() {
        INSTANCE = this;
        this.log("Initializing LambDynamicLights...");

        this.config.load();

        if (MinecraftClient.getInstance() != null) {
            ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
            if (resourceManager instanceof ReloadableResourceManager) {
                ReloadableResourceManager reloadableResourceManager = (ReloadableResourceManager) resourceManager;
                reloadableResourceManager.registerReloader((SynchronousResourceReloader) ItemLightSources::load);
            }
        }

        MinecraftForge.EVENT_BUS.addListener(this::renderWorldLastEvent);
        ModList.get().getModContainerById(MODID).orElseThrow(RuntimeException::new).registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY, () -> ((minecraftClient, screen) -> new SettingsScreen(screen)));

        DynamicLightHandlers.registerDefaultHandlers();
    }

    @SubscribeEvent
    public void renderWorldLastEvent(@NotNull RenderWorldLastEvent event) {
        MinecraftClient.getInstance().getProfiler().push("dynamic_lighting");
        get().updateAll(event.getContext());
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

            for (DynamicLightSource lightSource : this.dynamicLightSources) {
                if (lightSource.ryoamicLights$updateDynamicLight(renderer)) this.lastUpdateCount++;
            }
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
        for (DynamicLightSource lightSource : this.dynamicLightSources) {
            result = maxDynamicLightLevel(pos, lightSource, result);
        }

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

            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            // 7.75 because else we would have to update more chunks and that's not a good idea.
            // 15 (max range for blocks) would be too much and a bit cheaty.
            if (distance <= MAX_RADIUS) {
                double multiplier = 1.0 - distance / MAX_RADIUS;
                double lightLevel = multiplier * (double) luminance;
                if (lightLevel > currentLightLevel) {
                    currentLightLevel = lightLevel;
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
        this.dynamicLightSources.add(lightSource);
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
        return this.dynamicLightSources.contains(lightSource);
    }

    /**
     * Returns the number of dynamic light sources that currently emit lights.
     *
     * @return the number of dynamic light sources emitting light
     */
    public int getLightSourcesCount() {
        return this.dynamicLightSources.size();
    }

    /**
     * Removes the light source from the tracked light sources.
     *
     * @param lightSource the light source to remove
     */
    public void removeLightSource(@NotNull DynamicLightSource lightSource) {
        Iterator<DynamicLightSource> dynamicLightSources = this.dynamicLightSources.iterator();
        DynamicLightSource it;
        while (dynamicLightSources.hasNext()) {
            it = dynamicLightSources.next();
            if (it.equals(lightSource)) {
                dynamicLightSources.remove();
                if (MinecraftClient.getInstance().worldRenderer != null)
                    lightSource.ryoamicLights$scheduleTrackedChunksRebuild(MinecraftClient.getInstance().worldRenderer);
                break;
            }
        }
    }

    /**
     * Clears light sources.
     */
    public void clearLightSources() {
        Iterator<DynamicLightSource> dynamicLightSources = this.dynamicLightSources.iterator();
        DynamicLightSource it;
        while (dynamicLightSources.hasNext()) {
            it = dynamicLightSources.next();
            dynamicLightSources.remove();
            if (MinecraftClient.getInstance().worldRenderer != null) {
                if (it.ryoamicLights$getLuminance() > 0)
                    it.ryoamicLights$resetDynamicLight();
                it.ryoamicLights$scheduleTrackedChunksRebuild(MinecraftClient.getInstance().worldRenderer);
            }
        }
    }

    /**
     * Removes light sources if the filter matches.
     *
     * @param filter the removal filter
     */
    public void removeLightSources(@NotNull Predicate<DynamicLightSource> filter) {
        Iterator<DynamicLightSource> dynamicLightSources = this.dynamicLightSources.iterator();
        DynamicLightSource it;
        while (dynamicLightSources.hasNext()) {
            it = dynamicLightSources.next();
            if (filter.test(it)) {
                dynamicLightSources.remove();
                if (MinecraftClient.getInstance().worldRenderer != null) {
                    if (it.ryoamicLights$getLuminance() > 0)
                        it.ryoamicLights$resetDynamicLight();
                    it.ryoamicLights$scheduleTrackedChunksRebuild(MinecraftClient.getInstance().worldRenderer);
                }
                break;
            }
        }
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
        this.logger.info("[LambDynLights] " + info);
    }

    /**
     * Prints a warning message to the terminal.
     *
     * @param info the message to print
     */
    public void warn(String info) {
        this.logger.warn("[LambDynLights] " + info);
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
            ((WorldRendererAccessor) renderer).lambdynlights_scheduleChunkRebuild(x, y, z, false);
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
        if (!lightSource.ryoamicLights$isDynamicLightEnabled() && lightSource.ryoamicLights$getLuminance() > 0) {
            lightSource.ryoamicLights$setDynamicLightEnabled(true);
        } else if (lightSource.ryoamicLights$isDynamicLightEnabled() && lightSource.ryoamicLights$getLuminance() < 1) {
            lightSource.ryoamicLights$setDynamicLightEnabled(false);
        }
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
     * Returns the LambDynamicLights mod instance.
     *
     * @return the mod instance
     */
    public static LambDynLights get() {
        return INSTANCE;
    }
}