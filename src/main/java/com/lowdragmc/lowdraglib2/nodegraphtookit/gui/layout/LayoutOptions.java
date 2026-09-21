package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.layout;

/**
 * Tuning shared by every {@link IGraphLayout}.
 *
 * @param direction     which way the graph flows
 * @param layerSpacing  gap between consecutive layers along the flow axis
 * @param nodeSpacing   gap between neighbours stacked within one layer
 * @param iterations    refinement passes — crossing-reduction sweeps for the layered algorithm,
 *                      a multiplier on the simulation length for the force-directed one
 * @param seed          randomness seed, so a layout that shuffles is at least reproducible
 */
public record LayoutOptions(LayoutDirection direction,
                            float layerSpacing,
                            float nodeSpacing,
                            int iterations,
                            long seed) {

    public static LayoutOptions defaults() {
        return new LayoutOptions(LayoutDirection.LEFT_RIGHT, 80f, 32f, 8, 0x5EEDL);
    }

    public LayoutOptions withDirection(LayoutDirection value) {
        return new LayoutOptions(value, layerSpacing, nodeSpacing, iterations, seed);
    }

    public LayoutOptions withSpacing(float layer, float node) {
        return new LayoutOptions(direction, layer, node, iterations, seed);
    }

    public LayoutOptions withIterations(int value) {
        return new LayoutOptions(direction, layerSpacing, nodeSpacing, value, seed);
    }
}
