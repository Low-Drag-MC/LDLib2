package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.VariableKind;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.group.GroupModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.group.IGroupItemModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModelBase;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Objects;

@GameTestHolder(LDLib2.MOD_ID)
public class GraphHierarchySerializationTest {

    /**
     * Tests that variables are correctly placed in sections after serialization round-trip.
     */
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void variableSectionHierarchy(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var graph = new TestGraph();
        var graphModel = graph.graphModel;

        // Create variables — they should be auto-inserted into the default section
        var var1 = graphModel.createVariable("alpha", float.class, 1.0f, VariableKind.LOCAL);
        var var2 = graphModel.createVariable("beta", float.class, 2.0f, VariableKind.LOCAL);
        var var3 = graphModel.createVariable("gamma", float.class, 3.0f, VariableKind.LOCAL);

        // Verify original hierarchy
        var defaultSection = graphModel.getSectionModel(GraphModel.DEFAULT_SECTION_NAME);
        assertNotNull(helper, "default section exists", defaultSection);
        assertEq(helper, "section item count before serialize", 3, defaultSection.getItems().size());

        // Serialize
        CompoundTag serialized = graphModel.serializeNBT(provider);

        // Deserialize into new graph
        var graph2 = new TestGraph();
        var graphModel2 = graph2.graphModel;
        graphModel2.deserializeNBT(provider, serialized);

        // Verify variables exist
        assertEq(helper, "variable count", 3, graphModel2.getGraphVariableModels().size());

        // Verify section hierarchy is rebuilt
        var section2 = graphModel2.getSectionModel(GraphModel.DEFAULT_SECTION_NAME);
        assertNotNull(helper, "default section exists after deserialize", section2);
        assertEq(helper, "section item count after deserialize", 3, section2.getItems().size());

        // Verify order and names
        var itemNames = section2.getItems().stream().map(IGroupItemModel::getName).toList();
        assertEq(helper, "first item name", "alpha", itemNames.get(0));
        assertEq(helper, "second item name", "beta", itemNames.get(1));
        assertEq(helper, "third item name", "gamma", itemNames.get(2));

        // Verify parentGroup is set
        for (var item : section2.getItems()) {
            if (item.getParentGroup() != section2) {
                helper.fail("Item '" + item.getName() + "' has wrong parentGroup after deserialize");
                return;
            }
        }

        helper.succeed();
    }

    /**
     * Tests that nested groups (section -> group -> variables) survive serialization.
     */
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void nestedGroupHierarchy(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var graph = new TestGraph();
        var graphModel = graph.graphModel;

        // Create variables
        var var1 = (VariableDeclarationModelBase) graphModel.createVariable("x", float.class, 1.0f, VariableKind.LOCAL);
        var var2 = (VariableDeclarationModelBase) graphModel.createVariable("y", float.class, 2.0f, VariableKind.LOCAL);
        var var3 = (VariableDeclarationModelBase) graphModel.createVariable("z", float.class, 3.0f, VariableKind.LOCAL);

        // Create a group and move var1, var2 into it
        var defaultSection = graphModel.getSectionModel(GraphModel.DEFAULT_SECTION_NAME);
        assertNotNull(helper, "default section", defaultSection);
        var group = graphModel.createGroup("MyGroup", List.of(var1, var2));
        // Insert the group into the section
        defaultSection.insertItem(group, defaultSection.getItems().size());

        // Now hierarchy is: section -> [var3, group -> [var1, var2]]
        assertEq(helper, "section items (var3 + group)", 2, defaultSection.getItems().size());
        assertEq(helper, "group items (var1 + var2)", 2, group.getItems().size());

        // Serialize
        CompoundTag serialized = graphModel.serializeNBT(provider);

        // Deserialize
        var graph2 = new TestGraph();
        var graphModel2 = graph2.graphModel;
        graphModel2.deserializeNBT(provider, serialized);

        var section2 = graphModel2.getSectionModel(GraphModel.DEFAULT_SECTION_NAME);
        assertNotNull(helper, "section after deserialize", section2);
        assertEq(helper, "section items after deserialize", 2, section2.getItems().size());

        // Find the group
        GroupModel restoredGroup = null;
        for (var item : section2.getItems()) {
            if (item instanceof GroupModel g && "MyGroup".equals(g.getName())) {
                restoredGroup = g;
                break;
            }
        }
        assertNotNull(helper, "restored group", restoredGroup);
        assertEq(helper, "group items after deserialize", 2, restoredGroup.getItems().size());

        // Verify group children names
        var groupItemNames = restoredGroup.getItems().stream().map(IGroupItemModel::getName).toList();
        assertEq(helper, "group child 1", "x", groupItemNames.get(0));
        assertEq(helper, "group child 2", "y", groupItemNames.get(1));

        // Verify the top-level var3 is still in the section
        var topLevelVar = section2.getItems().stream()
                .filter(i -> !(i instanceof GroupModel))
                .findFirst().orElse(null);
        assertNotNull(helper, "top-level var3 in section", topLevelVar);
        assertEq(helper, "top-level var name", "z", topLevelVar.getName());

        helper.succeed();
    }

    /**
     * Tests that the hierarchy survives a double round-trip (serialize -> deserialize -> serialize -> deserialize).
     */
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void doubleRoundTripHierarchy(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var graph = new TestGraph();
        var graphModel = graph.graphModel;

        var var1 = (VariableDeclarationModelBase) graphModel.createVariable("a", float.class, 1.0f, VariableKind.LOCAL);
        var var2 = (VariableDeclarationModelBase) graphModel.createVariable("b", float.class, 2.0f, VariableKind.LOCAL);
        var group = graphModel.createGroup("G1", List.of(var1));
        var defaultSection = graphModel.getSectionModel(GraphModel.DEFAULT_SECTION_NAME);
        defaultSection.insertItem(group, defaultSection.getItems().size());

        // First round-trip
        CompoundTag tag1 = graphModel.serializeNBT(provider);
        var graph2 = new TestGraph();
        graph2.graphModel.deserializeNBT(provider, tag1);

        // Second round-trip
        CompoundTag tag2 = graph2.graphModel.serializeNBT(provider);
        var graph3 = new TestGraph();
        graph3.graphModel.deserializeNBT(provider, tag2);

        var section = graph3.graphModel.getSectionModel(GraphModel.DEFAULT_SECTION_NAME);
        assertNotNull(helper, "section after double round-trip", section);
        assertEq(helper, "section items after double round-trip", 2, section.getItems().size());

        // Find the group
        var restoredGroup = section.getItems().stream()
                .filter(i -> i instanceof GroupModel)
                .map(i -> (GroupModel) i)
                .findFirst().orElse(null);
        assertNotNull(helper, "group after double round-trip", restoredGroup);
        assertEq(helper, "group name", "G1", restoredGroup.getName());
        assertEq(helper, "group items", 1, restoredGroup.getItems().size());
        assertEq(helper, "group child name", "a", restoredGroup.getItems().get(0).getName());

        // Verify tag stability
        if (!tag1.equals(tag2)) {
            LDLib2.LOGGER.warn("Double round-trip produced different NBT");
        }

        helper.succeed();
    }

    // --- Helpers ---

    private static void assertNotNull(GameTestHelper helper, String label, Object value) {
        if (value == null) {
            helper.fail(label + " is null");
        }
    }

    private static void assertEq(GameTestHelper helper, String label, int expected, int actual) {
        if (expected != actual) {
            helper.fail(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertEq(GameTestHelper helper, String label, String expected, String actual) {
        if (!Objects.equals(expected, actual)) {
            helper.fail(label + ": expected '" + expected + "', got '" + actual + "'");
        }
    }
}
