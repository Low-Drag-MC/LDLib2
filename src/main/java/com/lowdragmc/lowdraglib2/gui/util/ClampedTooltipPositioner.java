package com.lowdragmc.lowdraglib2.gui.util;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import org.joml.Vector2i;
import org.joml.Vector2ic;

/**
 * Vanilla's tooltip placement, kept inside all four edges.
 *
 * <p>{@link DefaultTooltipPositioner} offsets by {@code (+12, -12)} and clamps only the right and
 * bottom, so anything hovered within twelve units of the top gets a tooltip at a negative y whose
 * first rows are cut off — every control in a title bar.
 */
public class ClampedTooltipPositioner implements ClientTooltipPositioner {

    public static final ClientTooltipPositioner INSTANCE = new ClampedTooltipPositioner();

    /** Vanilla's own margin from an edge. */
    private static final int MARGIN = 4;
    private static final int CURSOR_GAP = 12;

    protected ClampedTooltipPositioner() {
    }

    @Override
    public Vector2ic positionTooltip(int screenWidth, int screenHeight, int mouseX, int mouseY,
                                     int tooltipWidth, int tooltipHeight) {
        var placed = DefaultTooltipPositioner.INSTANCE
                .positionTooltip(screenWidth, screenHeight, mouseX, mouseY, tooltipWidth, tooltipHeight);
        var x = placed.x();
        var y = placed.y();
        if (y < MARGIN) {
            // No room above: under the cursor rather than over it.
            y = mouseY + CURSOR_GAP;
        }
        // Low edge wins, so a tooltip larger than the surface keeps its beginning readable.
        x = Math.min(x, screenWidth - tooltipWidth - MARGIN);
        y = Math.min(y, screenHeight - tooltipHeight - MARGIN);
        return new Vector2i(Math.max(MARGIN, x), Math.max(MARGIN, y));
    }
}
