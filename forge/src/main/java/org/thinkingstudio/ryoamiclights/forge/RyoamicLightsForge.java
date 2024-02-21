/*
 * Copyright © 2020~2024 LambdAurora <email@lambdaurora.dev>
 * Copyright © 2024 ThinkingStudio
 *
 * This file is part of RyoamicLights.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package org.thinkingstudio.ryoamiclights.forge;

import dev.architectury.platform.Platform;
import net.minecraft.client.MinecraftClient;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.NetworkConstants;
import org.jetbrains.annotations.NotNull;
import org.thinkingstudio.ryoamiclights.RyoamicLights;
import org.thinkingstudio.ryoamiclights.forge.api.DynamicLightsInitializerEvent;
import org.thinkingstudio.ryoamiclights.gui.SettingsScreen;

@Mod(RyoamicLights.NAMESPACE)
public class RyoamicLightsForge {
    public RyoamicLightsForge() {
        if (FMLLoader.getDist().isClient()) {
            this.onInitializeClient();
        }
    }

    public void onInitializeClient() {
        new RyoamicLights().clientInit();
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::renderWorldLastEvent);
        MinecraftForge.EVENT_BUS.post(new DynamicLightsInitializerEvent());
        Platform.getMod(RyoamicLights.NAMESPACE).registerConfigurationScreen(SettingsScreen::new);
    }

    public void renderWorldLastEvent(@NotNull RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) {
            MinecraftClient.getInstance().getProfiler().swap("dynamic_lighting");
            RyoamicLights.get().updateAll(event.getLevelRenderer());
        }
    }
}
