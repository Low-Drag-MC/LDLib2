package com.lowdragmc.lowdraglib2.gui.ui.layout;

import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A Selector's preview label has no intrinsic width and relies on being stretched. Relaying out a content-sized
 * container above it (what ScrollerView does after its first pass) must not leave it at a measured width of 0.
 */
public class StretchInIntrinsicLayoutTest {

    private static TaffyStyle style() {
        return TaffyLayoutStyle.DEFAULT_TAFFY_STYLE.copy();
    }

    @Test
    void stretchedLabelKeepsItsWidthWhenItsContainerRelayouts() {
        var tree = new TaffyTree();
        tree.disableRounding();

        var label = style();
        label.size = TaffySize.of(TaffyDimension.AUTO, TaffyDimension.length(9));
        var labelNode = tree.newLeaf(label);

        var preview = style();
        preview.flex = 1;
        preview.size = TaffySize.of(TaffyDimension.AUTO, TaffyDimension.percent(1));
        var previewNode = tree.newWithChildren(preview, labelNode);

        var display = style();
        display.flexDirection = FlexDirection.ROW;
        display.size = TaffySize.of(TaffyDimension.percent(1), TaffyDimension.percent(1));
        var displayNode = tree.newWithChildren(display, previewNode);

        var selector = style();
        selector.size = TaffySize.of(TaffyDimension.AUTO, TaffyDimension.length(14));
        var selectorNode = tree.newWithChildren(selector, displayNode);

        var container = style();
        container.minSize = TaffySize.of(TaffyDimension.length(100), TaffyDimension.ZERO);
        var containerNode = tree.newWithChildren(container, selectorNode);

        NodeId root = tree.newWithChildren(style(), containerNode);
        var space = TaffySize.of(AvailableSpace.MAX_CONTENT, AvailableSpace.MAX_CONTENT);

        tree.computeLayout(root, space);
        assertEquals(100, tree.getLayout(labelNode).size().width, 0.01f);

        tree.markDirty(containerNode);
        tree.computeLayout(root, space);
        assertEquals(100, tree.getLayout(previewNode).size().width, 0.01f);
        assertEquals(100, tree.getLayout(labelNode).size().width, 0.01f);
    }
}
