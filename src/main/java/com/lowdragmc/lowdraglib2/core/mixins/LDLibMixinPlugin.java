package com.lowdragmc.lowdraglib2.core.mixins;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * @author KilaBash
 * @date 2023/2/11
 * @implNote LDLibMixinPlugin
 */
public class LDLibMixinPlugin implements IMixinConfigPlugin, MixinPluginShared {
    @Override
    public void onLoad(String mixinPackage) {

    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains("com.lowdragmc.lowdraglib.core.mixins.jei")) {
            return IS_JEI_LOAD;
        } else if (mixinClassName.contains("com.lowdragmc.lowdraglib.core.mixins.rei")) {
            return IS_REI_LOAD;
        } else if (mixinClassName.contains("com.lowdragmc.lowdraglib.core.mixins.emi")) {
            return IS_MEI_LOAD;
        }
        return true;
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
