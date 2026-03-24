package com.lowdragmc.lowdraglib2.test.gametest.nodegraph;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.VariableKind;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.group.GroupModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.group.IGroupItemModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModelBase;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestGraph;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.List;
import java.util.Objects;

public final class GraphHierarchySerializationGameTest {
    private static final String VARIABLE_SECTION_PATH = "graph_hierarchy_variable_section";
    private static final String NESTED_GROUP_PATH = "graph_hierarchy_nested_group";
    private static final String DOUBLE_ROUND_TRIP_PATH = "graph_hierarchy_double_round_trip";

    private GraphHierarchySerializationGameTest() {
    }

    static void registerFunctions() {
        NodeGraphGameTests.registerFunction(VARIABLE_SECTION_PATH, GraphHierarchySerializationGameTest::variableSectionHierarchy);
        NodeGraphGameTests.registerFunction(NESTED_GROUP_PATH, GraphHierarchySerializationGameTest::nestedGroupHierarchy);
        NodeGraphGameTests.registerFunction(DOUBLE_ROUND_TRIP_PATH, GraphHierarchySerializationGameTest::doubleRoundTripHierarchy);
    }

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = NodeGraphGameTests.defaultTestData(environment, "empty");
        NodeGraphGameTests.registerFunctionTest(event, VARIABLE_SECTION_PATH, NodeGraphGameTests.functionKey(VARIABLE_SECTION_PATH), testData);
        NodeGraphGameTests.registerFunctionTest(event, NESTED_GROUP_PATH, NodeGraphGameTests.functionKey(NESTED_GROUP_PATH), testData);
        NodeGraphGameTests.registerFunctionTest(event, DOUBLE_ROUND_TRIP_PATH, NodeGraphGameTests.functionKey(DOUBLE_ROUND_TRIP_PATH), testData);
    }

    private static void variableSectionHierarchy(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var graph = new TestGraph();
        var graphModel = graph.graphModel;

        var var1 = graphModel.createVariable("alpha", float.class, 1.0f, VariableKind.LOCAL);
        var var2 = graphModel.createVariable("beta", float.class, 2.0f, VariableKind.LOCAL);
        var var3 = graphModel.createVariable("gamma", float.class, 3.0f, VariableKind.LOCAL);

        var defaultSection = graphModel.getSectionModel(GraphModel.DEFAULT_SECTION_NAME);
        assertNotNull(helper, "default section exists", defaultSection);
        assertEq(helper, "section item count before serialize", 3, defaultSection.getItems().size());

        CompoundTag serialized = serializeGraph(graphModel, provider);

        var graph2 = new TestGraph();
        var graphModel2 = graph2.graphModel;
        deserializeGraph(graphModel2, serialized, provider);

        assertEq(helper, "variable count", 3, graphModel2.getGraphVariableModels().size());

        var section2 = graphModel2.getSectionModel(GraphModel.DEFAULT_SECTION_NAME);
        assertNotNull(helper, "default section exists after deserialize", section2);
        assertEq(helper, "section item count after deserialize", 3, section2.getItems().size());

        var itemNames = section2.getItems().stream().map(IGroupItemModel::getName).toList();
        assertEq(helper, "first item name", "alpha", itemNames.get(0));
        assertEq(helper, "second item name", "beta", itemNames.get(1));
        assertEq(helper, "third item name", "gamma", itemNames.get(2));

        for (var item : section2.getItems()) {
            if (item.getParentGroup() != section2) {
                helper.fail("Item '" + item.getName() + "' has wrong parentGroup after deserialize");
                return;
            }
        }

        helper.succeed();
    }

    private static void nestedGroupHierarchy(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var graph = new TestGraph();
        var graphModel = graph.graphModel;

        var var1 = (VariableDeclarationModelBase) graphModel.createVariable("x", float.class, 1.0f, VariableKind.LOCAL);
        var var2 = (VariableDeclarationModelBase) graphModel.createVariable("y", float.class, 2.0f, VariableKind.LOCAL);
        var var3 = (VariableDeclarationModelBase) graphModel.createVariable("z", float.class, 3.0f, VariableKind.LOCAL);

        var defaultSection = graphModel.getSectionModel(GraphModel.DEFAULT_SECTION_NAME);
        assertNotNull(helper, "default section", defaultSection);
        var group = graphModel.createGroup("MyGroup", List.of(var1, var2));
        defaultSection.insertItem(group, defaultSection.getItems().size());

        assertEq(helper, "section items (var3 + group)", 2, defaultSection.getItems().size());
        assertEq(helper, "group items (var1 + var2)", 2, group.getItems().size());

        CompoundTag serialized = serializeGraph(graphModel, provider);

        var graph2 = new TestGraph();
        var graphModel2 = graph2.graphModel;
        deserializeGraph(graphModel2, serialized, provider);

        var section2 = graphModel2.getSectionModel(GraphModel.DEFAULT_SECTION_NAME);
        assertNotNull(helper, "section after deserialize", section2);
        assertEq(helper, "section items after deserialize", 2, section2.getItems().size());

        GroupModel restoredGroup = null;
        for (var item : section2.getItems()) {
            if (item instanceof GroupModel g && "MyGroup".equals(g.getName())) {
                restoredGroup = g;
                break;
            }
        }
        assertNotNull(helper, "restored group", restoredGroup);
        assertEq(helper, "group items after deserialize", 2, restoredGroup.getItems().size());

        var groupItemNames = restoredGroup.getItems().stream().map(IGroupItemModel::getName).toList();
        assertEq(helper, "group child 1", "x", groupItemNames.get(0));
        assertEq(helper, "group child 2", "y", groupItemNames.get(1));

        var topLevelVar = section2.getItems().stream()
                .filter(i -> !(i instanceof GroupModel))
                .findFirst().orElse(null);
        assertNotNull(helper, "top-level var3 in section", topLevelVar);
        assertEq(helper, "top-level var name", "z", topLevelVar.getName());

        helper.succeed();
    }

    private static void doubleRoundTripHierarchy(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var graph = new TestGraph();
        var graphModel = graph.graphModel;

        var var1 = (VariableDeclarationModelBase) graphModel.createVariable("a", float.class, 1.0f, VariableKind.LOCAL);
        var var2 = (VariableDeclarationModelBase) graphModel.createVariable("b", float.class, 2.0f, VariableKind.LOCAL);
        var group = graphModel.createGroup("G1", List.of(var1));
        var defaultSection = graphModel.getSectionModel(GraphModel.DEFAULT_SECTION_NAME);
        defaultSection.insertItem(group, defaultSection.getItems().size());

        CompoundTag tag1 = serializeGraph(graphModel, provider);
        var graph2 = new TestGraph();
        deserializeGraph(graph2.graphModel, tag1, provider);

        CompoundTag tag2 = serializeGraph(graph2.graphModel, provider);
        var graph3 = new TestGraph();
        deserializeGraph(graph3.graphModel, tag2, provider);

        var section = graph3.graphModel.getSectionModel(GraphModel.DEFAULT_SECTION_NAME);
        assertNotNull(helper, "section after double round-trip", section);
        assertEq(helper, "section items after double round-trip", 2, section.getItems().size());

        var restoredGroup = section.getItems().stream()
                .filter(i -> i instanceof GroupModel)
                .map(i -> (GroupModel) i)
                .findFirst().orElse(null);
        assertNotNull(helper, "group after double round-trip", restoredGroup);
        assertEq(helper, "group name", "G1", restoredGroup.getName());
        assertEq(helper, "group items", 1, restoredGroup.getItems().size());
        assertEq(helper, "group child name", "a", restoredGroup.getItems().get(0).getName());

        if (!tag1.equals(tag2)) {
            LDLib2.LOGGER.warn("Double round-trip produced different NBT");
        }

        helper.succeed();
    }

    // --- Serialization helpers ---

    private static CompoundTag serializeGraph(com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl graphModel, net.minecraft.core.HolderLookup.Provider provider) {
        var output = TagValueOutput.createWithContext(ProblemReporter.Collector.DISCARDING, provider);
        graphModel.serialize(output);
        return output.buildResult();
    }

    private static void deserializeGraph(com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl graphModel, CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        graphModel.deserialize(TagValueInput.create(ProblemReporter.Collector.DISCARDING, provider, tag));
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
