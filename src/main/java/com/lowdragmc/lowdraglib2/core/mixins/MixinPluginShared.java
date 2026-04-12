package com.lowdragmc.lowdraglib2.core.mixins;

import net.fabricmc.loader.api.FabricLoader;

public interface MixinPluginShared {

	static boolean isModLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}

	boolean IS_OPT_LOAD = isModLoaded("optifine");
	boolean IS_SODIUM_LOAD = isModLoaded("sodium");
	boolean IS_JEI_LOAD = isModLoaded("jei");
	boolean IS_REI_LOAD = isModLoaded("rei");
	boolean IS_EMI_LOADED = isModLoaded("emi");
	boolean IS_RUBIDIUM_LOAD = IS_SODIUM_LOAD;
	boolean IS_IRIS_LOAD = isModLoaded("iris");
	boolean IS_OCULUS_LOAD = IS_IRIS_LOAD || isModLoaded("oculus");
	boolean IS_KJS_LOAD = isModLoaded("kubejs");

}
