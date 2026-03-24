package com.lowdragmc.lowdraglib2.registry;

import com.lowdragmc.lowdraglib2.Platform;
import net.neoforged.fml.loading.modscan.ModAnnotation;

import java.util.Map;

/**
 * Controls when an annotated element should be registered based on the runtime environment.
 */
public enum RegistrationEnvironment {
    /**
     * Always register, regardless of environment.
     */
    ALWAYS,
    /**
     * Only register in development environment.
     */
    DEV_ONLY,
    /**
     * Only register in production environment.
     */
    PRODUCTION_ONLY,
    /**
     * Do not register automatically. Must be registered manually.
     */
    MANUAL;

    /**
     * Whether this environment allows automatic registration in the current runtime.
     */
    public boolean shouldRegister() {
        return switch (this) {
            case ALWAYS -> true;
            case DEV_ONLY -> Platform.isDevEnv();
            case PRODUCTION_ONLY -> !Platform.isDevEnv();
            case MANUAL -> false;
        };
    }

    /**
     * Checks annotation data map for environment and legacy manual fields.
     * Use this in annotation filters for {@link AutoRegistry}.
     */
    public static boolean shouldRegister(Map<String, Object> annotationData) {
        // ASM scan stores enum values as ModAnnotation.EnumHolder(desc, value)
        if (annotationData.get("environment") instanceof ModAnnotation.EnumHolder envHolder) {
            return RegistrationEnvironment.valueOf(envHolder.value()).shouldRegister();
        }
        // Legacy: check deprecated manual field
        if (annotationData.get("manual") instanceof Boolean manual && manual) {
            return false;
        }
        return true;
    }
}
