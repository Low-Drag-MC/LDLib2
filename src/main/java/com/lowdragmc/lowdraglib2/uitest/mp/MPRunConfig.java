package com.lowdragmc.lowdraglib2.uitest.mp;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * A game process's multi-process launch parameters, read from system properties the {@code mpServer}
 * / {@code mpClientX} Gradle runs set. Only the identity lives here — the run's <em>content</em>
 * (selection, client roles, game port) arrives over the control channel in the hub's
 * {@link MPMessages#CONFIG} message, so there is exactly one source of truth for it.
 */
public final class MPRunConfig {

    public static final String PROP_ROLE = "ldlib2.mptest.role";
    public static final String PROP_HUB = "ldlib2.mptest.hub";
    public static final String PROP_OUT = "ldlib2.mptest.out";

    private final String role;
    private final String hubHost;
    private final int hubPort;
    private final Path outDir;

    private MPRunConfig(String role, String hubHost, int hubPort, Path outDir) {
        this.role = role;
        this.hubHost = hubHost;
        this.hubPort = hubPort;
        this.outDir = outDir;
    }

    /**
     * @return {@code null} unless both a role and a hub address are present — launching an mp run
     *         task by hand without an orchestrator degrades to an ordinary client/server launch
     */
    @Nullable
    public static MPRunConfig fromSystemProperties() {
        var role = System.getProperty(PROP_ROLE, "").trim();
        var hub = System.getProperty(PROP_HUB, "").trim();
        if (role.isEmpty() || hub.isEmpty()) return null;

        int colon = hub.lastIndexOf(':');
        if (colon <= 0) return null;
        int hubPort;
        try {
            hubPort = Integer.parseInt(hub.substring(colon + 1));
        } catch (NumberFormatException e) {
            return null;
        }

        var out = System.getProperty(PROP_OUT, "").trim();
        var outDir = out.isEmpty()
                ? Path.of("ldlib2-mptest", role).toAbsolutePath()
                : Path.of(out).toAbsolutePath();
        return new MPRunConfig(role, hub.substring(0, colon), hubPort, outDir);
    }

    public String role() {
        return role;
    }

    public boolean isServer() {
        return MPMessages.SERVER_ROLE.equals(role);
    }

    public String hubHost() {
        return hubHost;
    }

    public int hubPort() {
        return hubPort;
    }

    public Path outDir() {
        return outDir;
    }
}
