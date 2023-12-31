package org.thinkingstudio.ryoamiclights.neoforge;

import org.thinkingstudio.ryoamiclights.RyoamicLightsCompat;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class RyoamicLightsNeoForgeMixinPlugin implements IMixinConfigPlugin {
    private final Object2BooleanMap<String> conditionalMixins = new Object2BooleanOpenHashMap<>();

    public RyoamicLightsNeoForgeMixinPlugin() {
        boolean embeddiumInstalled = RyoamicLightsCompat.isEmbeddiumInstalled();
        this.conditionalMixins.put("embeddium.mixin.org.thinkingstudio.ryoamiclights.neoforge.ArrayLightDataCacheMixin", embeddiumInstalled);
        this.conditionalMixins.put("embeddium.mixin.org.thinkingstudio.ryoamiclights.neoforge.FlatLightPipelineMixin", embeddiumInstalled);
        this.conditionalMixins.put("embeddium.mixin.org.thinkingstudio.ryoamiclights.neoforge.LightDataAccessMixin", embeddiumInstalled);

        boolean forgifiedFabricApiInstalled = RyoamicLightsCompat.isForgifiedFabricApiInstalled();
        this.conditionalMixins.put("fabricapi.mixin.org.thinkingstudio.ryoamiclights.neoforge.AoCalculatorMixin", forgifiedFabricApiInstalled);
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return this.conditionalMixins.getOrDefault(mixinClassName, true);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
