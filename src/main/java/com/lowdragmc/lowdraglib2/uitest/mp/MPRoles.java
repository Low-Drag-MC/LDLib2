package com.lowdragmc.lowdraglib2.uitest.mp;

/**
 * The role ↔ username convention shared by every process and the orchestrator's Gradle wiring.
 * A role is the short label scenarios use ({@code "A"}, {@code "B"}); the username is what the
 * client launches with and what the server's player list shows.
 */
public final class MPRoles {

    /** Kept short and offline-mode friendly: usernames must be ≤16 chars of {@code [A-Za-z0-9_]}. */
    public static final String USERNAME_PREFIX = "LDTest";

    private MPRoles() {
    }

    public static String usernameFor(String role) {
        return USERNAME_PREFIX + role;
    }
}
