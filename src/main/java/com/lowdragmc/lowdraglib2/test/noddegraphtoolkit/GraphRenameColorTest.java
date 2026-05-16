package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.resource.FilePath;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.VariableKind;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.IHasElementColor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.IHasName;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.SpawnFlags;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.SubgraphNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireModel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Vector2f;

/**
 * Coverage for the rename + color UI plumbing landed alongside SubgraphNodeModel work. UI
 * surfaces (right-click menu, inline TextField, color popup) are user-verified; the model layer
 * lives here.
 */
@GameTestHolder(LDLib2.MOD_ID)
public class GraphRenameColorTest {

    // ------------------------------------------------------------------
    // 1. Color storage on AbstractNodeModel: setColor → getElementColor + persistence
    // ------------------------------------------------------------------
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void abstractNodeModelColorStoragePersisted(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();

        var graph = new TestGraph();
        var add = graph.graphModel.createNodeModel(new TestAddNode(), new Vector2f(0, 0));
        if (add.hasUserColor()) { helper.fail("fresh node should not have user color"); return; }
        var defaultColor = add.getElementColor();

        add.setColor(0xFF112233);
        if (!add.hasUserColor()) { helper.fail("setColor should mark userColor"); return; }
        if (add.getElementColor() != 0xFF112233) {
            helper.fail("element color not stored: 0x" + Integer.toHexString(add.getElementColor()));
            return;
        }

        // Persist + reload
        var tag = graph.graphModel.serializeNBT(provider);
        var graph2 = new TestGraph();
        graph2.graphModel.deserializeNBT(provider, tag);

        com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel restored = null;
        for (var n : graph2.graphModel.getNodeModels()) {
            if (n != null && n.getUid().equals(add.getUid())) { restored = n; break; }
        }
        if (restored == null) { helper.fail("node not restored"); return; }
        if (!restored.hasUserColor() || restored.getElementColor() != 0xFF112233) {
            helper.fail("color not preserved across round-trip");
            return;
        }

        // unused but kept for clarity
        var _d = defaultColor;
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 2. Renamable judgment: capability + IHasName combined
    // ------------------------------------------------------------------
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void renamableJudgmentRespectsCapabilityAndInterface(GameTestHelper helper) {
        var graph = new TestGraph();
        var gm = graph.graphModel;

        var pm = gm.createPlacemat("p", new Vector2f(0, 0), new Vector2f(100, 100));
        var sn = gm.createStickyNote(new Vector2f(0, 0));
        var addNode = gm.createNodeModel(new TestAddNode(), new Vector2f(0, 0));
        var sub = gm.createLocalSubgraphInstance();
        gm.addLocalSubgraph(sub);
        var subNode = gm.createNodeWithType(SubgraphNodeModel.class, "s",
                new Vector2f(0, 0), null, n -> n.setLocalSubgraph(sub), SpawnFlags.DEFAULT);
        var variable = (VariableDeclarationModel) gm.createVariable("v", int.class, 0, VariableKind.LOCAL);

        // Renamable + IHasName
        if (!(pm.isRenamable() && pm instanceof IHasName)) { helper.fail("Placemat renamable"); return; }
        if (!(subNode.isRenamable() && subNode instanceof IHasName)) { helper.fail("Subgraph renamable"); return; }
        if (!(variable.isRenamable() && variable instanceof IHasName)) { helper.fail("Variable renamable"); return; }

        // NOT renamable
        if (addNode.isRenamable()) { helper.fail("TestAddNode should not be renamable"); return; }
        // StickyNote: not renamable (per design — content is the body)
        if (sn.isRenamable()) { helper.fail("StickyNote should not be renamable"); return; }

        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 3. SubgraphNodeModel title follows name (no more hard-coded setTitle)
    // ------------------------------------------------------------------
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void subgraphNodeTitleFollowsName(GameTestHelper helper) {
        var graph = new TestGraph();
        var sub = graph.graphModel.createLocalSubgraphInstance();
        graph.graphModel.addLocalSubgraph(sub);
        var node = graph.graphModel.createNodeWithType(SubgraphNodeModel.class, "Subgraph",
                new Vector2f(0, 0), null, n -> n.setLocalSubgraph(sub), SpawnFlags.DEFAULT);

        // Initially: title falls back to translatable(getName())
        var initial = node.getTitle().getString();
        if (!initial.equals("Subgraph") && !initial.contains("Subgraph")) {
            // translatable in test env without resource bundle resolves to key string
            helper.fail("Initial title did not follow name 'Subgraph': " + initial);
            return;
        }

        node.setName("Foo");
        var after = node.getTitle().getString();
        if (!after.equals("Foo") && !after.contains("Foo")) {
            helper.fail("Title did not follow renamed 'Foo': " + after);
            return;
        }

        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 4. IHasElementColor setters round-trip
    //    (Placemat already had storage; this verifies the contract uniformly)
    // ------------------------------------------------------------------
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void colorableSettersRoundTrip(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var graph = new TestGraph();
        var pm = graph.graphModel.createPlacemat("p", new Vector2f(0, 0), new Vector2f(100, 100));

        if (!(pm instanceof IHasElementColor)) { helper.fail("Placemat should be IHasElementColor"); return; }
        pm.setColor(0xFFAA5500);
        if (pm.getElementColor() != 0xFFAA5500) {
            helper.fail("placemat color not stored"); return;
        }
        if (!pm.hasUserColor()) { helper.fail("placemat userColor flag not raised"); return; }

        var tag = graph.graphModel.serializeNBT(provider);
        var graph2 = new TestGraph();
        graph2.graphModel.deserializeNBT(provider, tag);
        var restored = graph2.graphModel.getPlacematModels().get(0);
        if (restored.getElementColor() != 0xFFAA5500 || !restored.hasUserColor()) {
            helper.fail("placemat color not preserved across round-trip");
            return;
        }

        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 5. WireModel should be neither renamable nor colorable
    // ------------------------------------------------------------------
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void wireIsNeitherRenamableNorColorable(GameTestHelper helper) {
        var graph = new TestGraph();
        var floatType = com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandleHelpers.fromType(Float.class);
        var c1 = graph.graphModel.createConstantNode("c1", new Vector2f(0, 0), floatType, 1f);
        var add = graph.graphModel.createNodeModel(new TestAddNode(), new Vector2f(50, 0));
        var fromPort = ((com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ConstantNodeModel) c1.getNodeModel()).getOutputPort();
        var toPort = add.getInputsById().get("in1");
        WireModel wire = graph.graphModel.createWire(toPort, fromPort);

        if (wire.isRenamable()) { helper.fail("wire should not be renamable"); return; }
        if (wire.isColorable()) { helper.fail("wire should not be colorable"); return; }

        // and the FilePath import is just to keep imports stable — silence
        var _p = new FilePath("test/unused.tag");

        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 6. resetColor clears userColor flag and reverts to the default value;
    //    persistence and round-trip preserve the cleared state too.
    // ------------------------------------------------------------------
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void resetColorRestoresDefault(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var graph = new TestGraph();

        // Node case (AbstractNodeModel)
        var add = graph.graphModel.createNodeModel(new TestAddNode(), new Vector2f(0, 0));
        var nodeDefault = add.getDefaultColor();
        add.setColor(0xFF112233);
        if (!add.hasUserColor() || add.getElementColor() != 0xFF112233) {
            helper.fail("precondition: node setColor failed"); return;
        }
        add.resetColor();
        if (add.hasUserColor()) {
            helper.fail("node hasUserColor still true after resetColor"); return;
        }
        if (add.getElementColor() != nodeDefault) {
            helper.fail("node elementColor not back to default after reset: 0x"
                    + Integer.toHexString(add.getElementColor()));
            return;
        }

        // Placemat case
        var pm = graph.graphModel.createPlacemat("p", new Vector2f(0, 0), new Vector2f(100, 100));
        var pmDefault = pm.getDefaultColor();
        pm.setColor(0xFF99AA66);
        if (!pm.hasUserColor() || pm.getElementColor() != 0xFF99AA66) {
            helper.fail("precondition: placemat setColor failed"); return;
        }
        pm.resetColor();
        if (pm.hasUserColor() || pm.getElementColor() != pmDefault) {
            helper.fail("placemat reset did not restore defaults"); return;
        }

        // resetColor on a model that was never customized is a no-op (no exception, no STYLE).
        pm.resetColor();
        if (pm.hasUserColor()) { helper.fail("idempotent reset broke state"); return; }

        // Round-trip after reset persists hasUserColor=false
        var tag = graph.graphModel.serializeNBT(provider);
        var graph2 = new TestGraph();
        graph2.graphModel.deserializeNBT(provider, tag);
        for (var n : graph2.graphModel.getNodeModels()) {
            if (n != null && n.getUid().equals(add.getUid())) {
                if (n.hasUserColor()) {
                    helper.fail("hasUserColor leaked through round-trip after reset"); return;
                }
                break;
            }
        }

        helper.succeed();
    }
}
