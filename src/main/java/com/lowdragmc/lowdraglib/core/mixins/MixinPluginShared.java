package com.lowdragmc.lowdraglib.core.mixins;

public interface MixinPluginShared {

	static boolean isClassFound(String className) {
		try {
			Class.forName(className, false, Thread.currentThread().getContextClassLoader());
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	boolean IS_OPT_LOAD = isClassFound("optifine.OptiFineTranformationService");

	boolean IS_SODIUM_LOAD = isClassFound("me.jellysquid.mods.sodium.mixin.SodiumMixinPlugin");
	boolean IS_JEI_LOAD = isClassFound("mezz.jei.api.JeiPlugin");
	boolean IS_REI_LOAD = isClassFound("me.shedaniel.rei.api.common.plugins.REIPlugin");
	boolean IS_MEI_LOAD = isClassFound("dev.emi.emi.api.EmiPlugin");
	boolean IS_EMI_LOADED = IS_MEI_LOAD;
	boolean IS_RUBIDIUM_LOAD = IS_SODIUM_LOAD;
	boolean IS_IRIS_LOAD = isClassFound("net.coderbot.iris.compat.sodium.mixin.IrisSodiumCompatMixinPlugin")
			|| isClassFound("net.irisshaders.iris.compat.sodium.mixin.IrisSodiumCompatMixinPlugin");
	boolean IS_OCULUS_LOAD = IS_IRIS_LOAD;
	boolean IS_KJS_LOAD = isClassFound("dev.latvian.mods.kubejs.KubeJS");

}
