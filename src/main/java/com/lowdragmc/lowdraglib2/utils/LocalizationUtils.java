package com.lowdragmc.lowdraglib2.utils;


import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor_outdated.data.resource.Resource;
import lombok.experimental.UtilityClass;
import net.minecraft.client.resources.language.I18n;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public final class LocalizationUtils {
    public static Resource<String> RESOURCE;
    private final static Map<String, String> DYNAMIC_LANG = new HashMap<>();

    public static void setResource(Resource<String> resource) {
        RESOURCE = resource;
    }

    public static void clearResource() {
        RESOURCE = null;
    }

    public static void appendDynamicLang(Map<String, String> dynamicLang) {
        DYNAMIC_LANG.putAll(dynamicLang);
    }

    public static boolean hasDynamicLang(String key) {
        return DYNAMIC_LANG.containsKey(key);
    }

    public static String getDynamicLang(String key) {
        return DYNAMIC_LANG.get(key);
    }

    /**
     * This function calls `net.minecraft.client.resources.I18n.format` when called on client
     * or `net.minecraft.util.text.translation.I18n.translateToLocalFormatted` when called on server.
     * <ul>
     *  <li>It is intended that translations should be done using `I18n` on the client.</li>
     *  <li>For setting up translations on the server you should use `TextComponentTranslatable`.</li>
     *  <li>`LocalisationUtils` is only for cases where some kind of translation is required on the server and there is no client/player in context.</li>
     *  <li>`LocalisationUtils` is "best effort" and will probably only work properly with en-us.</li>
     * </ul>
     *
     * @param localisationKey the localisation key passed to the underlying format function
     * @param substitutions   the substitutions passed to the underlying format function
     * @return the localized string.
     */
    public static String format(String localisationKey, Object... substitutions) {
        if (!LDLib2.isClient()) {
            return String.format(localisationKey, substitutions);
        } else {
            if (RESOURCE != null && RESOURCE.hasBuiltinResource(localisationKey)) {
                return RESOURCE.getBuiltinResource(localisationKey);
            }
            return I18n.get(localisationKey, substitutions);
        }
    }

    /**
     * This function calls `net.minecraft.client.resources.I18n.hasKey` when called on client
     * or `net.minecraft.util.text.translation.I18n.canTranslate` when called on server.
     * <ul>
     *  <li>It is intended that translations should be done using `I18n` on the client.</li>
     *  <li>For setting up translations on the server you should use `TextComponentTranslatable`.</li>
     *  <li>`LocalisationUtils` is only for cases where some kind of translation is required on the server and there is no client/player in context.</li>
     *  <li>`LocalisationUtils` is "best effort" and will probably only work properly with en-us.</li>
     * </ul>
     *
     * @param localisationKey the localisation key passed to the underlying hasKey function
     * @return a boolean indicating if the given localisation key has localisations
     */
    public static boolean exist(String localisationKey) {
        if (LDLib2.isClient()) {
            return I18n.exists(localisationKey);
        } else {
            return false;
        }
    }
}
