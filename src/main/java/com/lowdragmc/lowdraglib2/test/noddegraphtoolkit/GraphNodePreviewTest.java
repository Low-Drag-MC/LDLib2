package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.SpawnFlags;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Vector2f;

/**
 * Node preview system: lifecycle (auto-create / ORPHAN skip), duplication, dependency wiring, and
 * persistence of the expanded state. UI rendering is validated manually.
 */
@GameTestHolder(LDLib2.MOD_ID)
public class GraphNodePreviewTest {

    // ------------------------------------------------------------------
    // 1. A node that hasNodePreview auto-creates a preview model that shows
    //    up in getDependentModels; a plain node has none.
    // ------------------------------------------------------------------
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void previewAutoCreatedAndDependent(GameTestHelper helper) {
        LDLib2.LOGGER.info("Start previewAutoCreatedAndDependent");

        var graph = new TestGraph();
        var node = graph.graphModel.createNodeModel(new TestPreviewNode(), new Vector2f(0, 0));

        var preview = node.getNodePreviewModel();
        if (preview == null) { helper.fail("preview not auto-created for hasNodePreview node"); return; }
        boolean inDeps = node.getDependentModels().anyMatch(m -> m == preview);
        if (!inDeps) { helper.fail("preview model missing from getDependentModels"); return; }
        if (!preview.isExpanded()) { helper.fail("preview should default to expanded"); return; }

        // A node without preview reports null.
        var plain = graph.graphModel.createNodeModel(new TestAddNode(), new Vector2f(50, 0));
        if (plain.getNodePreviewModel() != null) {
            helper.fail("non-preview node should report null preview"); return;
        }

        LDLib2.LOGGER.info("End previewAutoCreatedAndDependent - PASSED");
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 2. ORPHAN spawn does not create a preview.
    // ------------------------------------------------------------------
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void orphanSpawnSkipsPreview(GameTestHelper helper) {
        LDLib2.LOGGER.info("Start orphanSpawnSkipsPreview");

        var graph = new TestGraph();
        var node = graph.graphModel.createNodeWithType(CustomNodeModelImpl.class, "p",
                new Vector2f(0, 0), null,
                n -> ((CustomNodeModelImpl) n).initCustomNode(new TestPreviewNode()), SpawnFlags.ORPHAN);

        if (node.getNodePreviewModel() != null) {
            helper.fail("ORPHAN spawn should not create a preview"); return;
        }

        LDLib2.LOGGER.info("End orphanSpawnSkipsPreview - PASSED");
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 3. Duplicating a node copies the preview expanded state.
    // ------------------------------------------------------------------
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void duplicateCopiesExpandedState(GameTestHelper helper) {
        LDLib2.LOGGER.info("Start duplicateCopiesExpandedState");

        var graph = new TestGraph();
        var src = graph.graphModel.createNodeModel(new TestPreviewNode(), new Vector2f(0, 0));
        src.setPreviewExpanded(false);

        var dst = graph.graphModel.createNodeModel(new TestPreviewNode(), new Vector2f(50, 0));
        dst.onDuplicateNode(src);

        if (dst.isPreviewExpanded()) {
            helper.fail("duplicated node should inherit collapsed (expanded=false) preview state"); return;
        }
        if (dst.getNodePreviewModel() == null || dst.getNodePreviewModel().isExpanded()) {
            helper.fail("duplicated preview model should mirror expanded=false"); return;
        }

        LDLib2.LOGGER.info("End duplicateCopiesExpandedState - PASSED");
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 4. Expanded state persists across serialize/deserialize, and the
    //    preview model is recreated on load (syncNodePreview).
    // ------------------------------------------------------------------
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void expandedStatePersistsRoundTrip(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        LDLib2.LOGGER.info("Start expandedStatePersistsRoundTrip");

        var graph = new TestGraph();
        var node = graph.graphModel.createNodeModel(new TestPreviewNode(), new Vector2f(0, 0));
        node.setPreviewExpanded(false);

        var serialized = graph.graphModel.serializeNBT(provider);
        var graph2 = new TestGraph();
        graph2.graphModel.deserializeNBT(provider, serialized);

        AbstractNodeModel restored = null;
        for (var n : graph2.graphModel.getNodeModels()) {
            if (n != null && n.getUid().equals(node.getUid())) { restored = n; break; }
        }
        if (restored == null) { helper.fail("preview node not found after deserialize"); return; }
        if (restored.getNodePreviewModel() == null) {
            helper.fail("preview model not recreated on load (syncNodePreview)"); return;
        }
        if (restored.isPreviewExpanded()) {
            helper.fail("expanded=false not persisted across round-trip"); return;
        }
        if (restored.getNodePreviewModel().isExpanded()) {
            helper.fail("restored preview model expanded state not synced from node"); return;
        }

        LDLib2.LOGGER.info("End expandedStatePersistsRoundTrip - PASSED");
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 5. A node can choose whether its preview starts expanded. The default
    //    remains expanded; overriding the API can start collapsed.
    // ------------------------------------------------------------------
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void previewDefaultExpandedApi(GameTestHelper helper) {
        LDLib2.LOGGER.info("Start previewDefaultExpandedApi");

        var graph = new TestGraph();
        var defaultExpanded = graph.graphModel.createNodeModel(new TestPreviewNode(), new Vector2f(0, 0));
        if (!defaultExpanded.isPreviewExpanded()) {
            helper.fail("preview should default to expanded when node does not override the API"); return;
        }
        if (defaultExpanded.getNodePreviewModel() == null || !defaultExpanded.getNodePreviewModel().isExpanded()) {
            helper.fail("preview model should mirror default expanded state"); return;
        }

        var defaultCollapsed = graph.graphModel.createNodeModel(new TestCollapsedPreviewNode(), new Vector2f(50, 0));
        if (defaultCollapsed.isPreviewExpanded()) {
            helper.fail("preview should default to collapsed when node overrides the API"); return;
        }
        if (defaultCollapsed.getNodePreviewModel() == null || defaultCollapsed.getNodePreviewModel().isExpanded()) {
            helper.fail("preview model should mirror default collapsed state"); return;
        }

        LDLib2.LOGGER.info("End previewDefaultExpandedApi - PASSED");
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 6. Persisted user state wins over the node's default preview state.
    // ------------------------------------------------------------------
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void previewExpandedStateOverridesDefaultOnLoad(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        LDLib2.LOGGER.info("Start previewExpandedStateOverridesDefaultOnLoad");

        var graph = new TestGraph();
        var node = graph.graphModel.createNodeModel(new TestCollapsedPreviewNode(), new Vector2f(0, 0));
        node.setPreviewExpanded(true);

        var serialized = graph.graphModel.serializeNBT(provider);
        var graph2 = new TestGraph();
        graph2.graphModel.deserializeNBT(provider, serialized);

        AbstractNodeModel restored = null;
        for (var n : graph2.graphModel.getNodeModels()) {
            if (n != null && n.getUid().equals(node.getUid())) { restored = n; break; }
        }
        if (restored == null) { helper.fail("collapsed-default preview node not found after deserialize"); return; }
        if (!restored.isPreviewExpanded()) {
            helper.fail("persisted expanded=true should override default collapsed state"); return;
        }
        if (restored.getNodePreviewModel() == null || !restored.getNodePreviewModel().isExpanded()) {
            helper.fail("restored preview model should mirror persisted expanded=true"); return;
        }

        LDLib2.LOGGER.info("End previewExpandedStateOverridesDefaultOnLoad - PASSED");
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 7. Duplication copies source state instead of reapplying the target
    //    node's default preview state.
    // ------------------------------------------------------------------
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void duplicatePreviewStateOverridesDefault(GameTestHelper helper) {
        LDLib2.LOGGER.info("Start duplicatePreviewStateOverridesDefault");

        var graph = new TestGraph();
        var src = graph.graphModel.createNodeModel(new TestCollapsedPreviewNode(), new Vector2f(0, 0));
        src.setPreviewExpanded(true);

        var dst = graph.graphModel.createNodeModel(new TestCollapsedPreviewNode(), new Vector2f(50, 0));
        dst.onDuplicateNode(src);

        if (!dst.isPreviewExpanded()) {
            helper.fail("duplicated preview should inherit source expanded=true over default collapsed"); return;
        }
        if (dst.getNodePreviewModel() == null || !dst.getNodePreviewModel().isExpanded()) {
            helper.fail("duplicated preview model should mirror inherited expanded=true"); return;
        }

        LDLib2.LOGGER.info("End duplicatePreviewStateOverridesDefault - PASSED");
        helper.succeed();
    }
}
