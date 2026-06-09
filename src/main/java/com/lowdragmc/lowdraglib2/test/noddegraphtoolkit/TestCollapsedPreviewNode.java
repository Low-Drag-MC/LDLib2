package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import net.minecraft.network.chat.Component;

@NodeAttribute(name = "test_collapsed_preview", group = "test", graphTypes = {TestGraph.class})
public class TestCollapsedPreviewNode extends TestPreviewNode {

    @Override
    public Component getDisplayName() {
        return Component.literal("CollapsedPreview");
    }

    @Override
    public boolean isNodePreviewExpandedByDefault() {
        return false;
    }
}
