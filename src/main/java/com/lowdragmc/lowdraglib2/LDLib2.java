package com.lowdragmc.lowdraglib2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lowdragmc.lowdraglib2.client.ClientProxy;
import com.lowdragmc.lowdraglib2.core.mixins.MixinPluginShared;
import com.lowdragmc.lowdraglib2.integration.emi.EMIPlugin;
import com.lowdragmc.lowdraglib2.integration.rei.REIPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

@Mod(LDLib2.MOD_ID)
public class LDLib2 {
    public static final String MOD_ID = "ldlib2";
    public static final String NAME = "LowDragLib2";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    public static final String MODID_JEI = "jei";
    public static final String MODID_RUBIDIUM = "rubidium";
    public static final String MODID_REI = "roughlyenoughitems";
    public static final String MODID_EMI = "emi";
    public static final RandomSource RANDOM = RandomSource.createThreadSafe();
    public static final Gson GSON = new GsonBuilder().create();
    private static File assetsLocation;

    public LDLib2(IEventBus eventBus, ModContainer modContainer) {
        LDLib2.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            new ClientProxy(eventBus);
        } else {
            new CommonProxy(eventBus);
        }
    }

    public static void init() {
        LOGGER.info("{} is initializing on platform: {}", NAME, Platform.platformName());
        getAssetsDir();
    }

    public static File getAssetsDir() {
        if (assetsLocation == null) {
            assetsLocation = new File(Platform.getGamePath().toFile(), "assets");
            if (assetsLocation.mkdir()) {
                LOGGER.info("Created ldlib2 config folder");
            }
        }
        return assetsLocation;
    }

    public static boolean isValidResourceLocation(String string) {
        int i = string.indexOf(":");
        if (i == -1) {
            for (int j = 0; j < string.length(); j++) {
                if (!ResourceLocation.isAllowedInResourceLocation(string.charAt(j))) {
                    return false;
                }
            }
        } else {
            var namespace = string.substring(0, i);
            var path = string.substring(i + 1);
            return ResourceLocation.isValidNamespace(namespace) && ResourceLocation.isValidPath(path);
        }
        return true;

    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static boolean isClient() {
        return Platform.isClient();
    }

    public static boolean isRemote() {
        if (isClient()) {
            return Minecraft.getInstance().isSameThread();
        }
        return false;
    }

    public static boolean isModLoaded(String mod) {
        return Platform.isModLoaded(mod);
    }

    public static boolean isJeiLoaded() {
        if (!isEmiLoaded() && !isReiLoaded()) {
            return isModLoaded(MODID_JEI);
        }
        return false;
    }

    public static boolean isReiLoaded() {
        return isModLoaded(MODID_REI) && (!Platform.isClient() || REIPlugin.isReiEnabled());
    }

    public static boolean isEmiLoaded() {
        return isModLoaded(MODID_EMI) && (!Platform.isClient() || EMIPlugin.isEmiEnabled());
    }

    public static boolean isKubejsLoaded() {
        return Platform.isModLoaded("kubejs");
    }

    public static boolean isIrisLoaded() {
        return MixinPluginShared.IS_IRIS_LOAD;
    }

    public static boolean isOculusLoaded() {
        return MixinPluginShared.IS_OCULUS_LOAD;
    }

    public static boolean isOptifineLoaded() {
        return MixinPluginShared.IS_OPT_LOAD;
    }

}
