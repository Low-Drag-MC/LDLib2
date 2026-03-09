package com.lowdragmc.lowdraglib2.utils;


import com.lowdragmc.lowdraglib2.LDLib2;
import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public final class LocalizationUtils {
    private static final String CLIENT_I18N_CLASS = "net.minecraft" + ".client.resources.language.I18n";
    private final static Map<String, String> DYNAMIC_LANG = new HashMap<>();

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
     * This function formats via the client translation runtime when called on client
     * and falls back to best-effort server formatting otherwise.
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
            return invokeClientFormat(localisationKey, substitutions);
        }
    }

    /**
     * This function checks existence via the client translation runtime when called on client
     * and returns false on server.
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
            return invokeClientExists(localisationKey);
        } else {
            return false;
        }
    }

    private static String invokeClientFormat(String localisationKey, Object... substitutions) {
        try {
            var i18n = Class.forName(CLIENT_I18N_CLASS);
            var method = i18n.getMethod("get", String.class, Object[].class);
            return (String) method.invoke(null, localisationKey, substitutions);
        } catch (ReflectiveOperationException e) {
            return String.format(localisationKey, substitutions);
        }
    }

    private static boolean invokeClientExists(String localisationKey) {
        try {
            var i18n = Class.forName(CLIENT_I18N_CLASS);
            var method = i18n.getMethod("exists", String.class);
            return (boolean) method.invoke(null, localisationKey);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
