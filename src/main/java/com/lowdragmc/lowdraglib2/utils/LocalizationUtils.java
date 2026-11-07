package com.lowdragmc.lowdraglib2.utils;


import com.lowdragmc.lowdraglib2.LDLib2;
import lombok.experimental.UtilityClass;
import net.minecraft.locale.Language;

import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.Map;

@UtilityClass
public final class LocalizationUtils {
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
     * This function formats through the loaded language when called on client
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
        }
        // what I18n.get does: Language is common code, so the client-only class isn't needed
        var value = Language.getInstance().getOrDefault(localisationKey);
        try {
            return String.format(Locale.ROOT, value, substitutions);
        } catch (IllegalFormatException e) {
            return "Format error: " + value;
        }
    }

    /**
     * This function checks existence in the loaded language when called on client
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
        return LDLib2.isClient() && Language.getInstance().has(localisationKey);
    }
}
