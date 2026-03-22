package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandleHelpers;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.VariableKind;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ConstantNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.VariableNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.ModifierFlags;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableScope;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Vector2f;

import java.util.*;

/**
 * Fuzz test: performs random graph operations (create/delete nodes+wires, serialize/deserialize, undo/redo snapshots)
 * and validates that all wires always have valid (non-null) ports after each round-trip.
 */
@GameTestHolder(LDLib2.MOD_ID)
public class GraphFuzzTest {

    private static final int ITERATIONS = 50;
    private static final long SEED = 42L;

    @GameTest(template = "empty", timeoutTicks = 600)
    @PrefixGameTestTemplate(false)
    public static void graphFuzzCreateDeleteSerialize(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var rng = new Random(SEED);
        var graph = new TestGraph();
        var gm = graph.graphModel;

        // Snapshot stack (simulates undo/redo)
        var snapshots = new ArrayList<CompoundTag>();

        for (int i = 0; i < ITERATIONS; i++) {
            int action = rng.nextInt(12);
            try {
                switch (action) {
                    case 0, 1 -> createRandomNode(gm, rng);
                    case 2 -> createRandomWire(gm, rng);
                    case 3 -> deleteRandomNode(gm, rng);
                    case 4 -> deleteRandomWire(gm, rng);
                    case 5 -> { // save snapshot
                        snapshots.add(gm.serializeNBT(provider));
                    }
                    case 6 -> { // restore random snapshot (simulates undo/redo)
                        if (!snapshots.isEmpty()) {
                            var snap = snapshots.get(rng.nextInt(snapshots.size()));
                            gm.deserializeNBT(provider, snap);
                        }
                    }
                    case 7 -> createRandomVariable(gm, rng);
                    case 8 -> createVariableNodeForExisting(gm, rng);
                    case 9 -> deleteRandomVariable(gm, rng);
                    case 10 -> modifyRandomVariable(gm, rng);
                    case 11 -> wireVariableNode(gm, rng);
                }
            } catch (Exception e) {
                helper.fail("Iteration " + i + " action " + action + " threw: " + e.getMessage());
                return;
            }

            // Validate: every non-null wire must have valid ports
            String err = validateGraph(gm);
            if (err != null) {
                helper.fail("Iteration " + i + " (action=" + action + "): " + err);
                return;
            }

            // Serialize → deserialize → validate round-trip
            var tag = gm.serializeNBT(provider);
            var graph2 = new TestGraph();
            graph2.graphModel.deserializeNBT(provider, tag);
            String err2 = validateGraph(graph2.graphModel);
            if (err2 != null) {
                helper.fail("Iteration " + i + " round-trip: " + err2);
                return;
            }

            // Verify counts match after round-trip
            int origNodes = countNonNull(gm.getNodeModels());
            int origWires = countNonNull(gm.getWireModels());
            int origVars = countNonNull(gm.getGraphVariableModels());
            int newNodes = countNonNull(graph2.graphModel.getNodeModels());
            int newWires = countNonNull(graph2.graphModel.getWireModels());
            int newVars = countNonNull(graph2.graphModel.getGraphVariableModels());
            if (origNodes != newNodes) {
                helper.fail("Iteration " + i + " node count mismatch: " + origNodes + " vs " + newNodes);
                return;
            }
            if (origWires != newWires) {
                helper.fail("Iteration " + i + " wire count mismatch: " + origWires + " vs " + newWires);
                return;
            }
            if (origVars != newVars) {
                helper.fail("Iteration " + i + " variable count mismatch: " + origVars + " vs " + newVars);
                return;
            }
        }

        LDLib2.LOGGER.info("GraphFuzzTest passed {} iterations (seed={})", ITERATIONS, SEED);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    @PrefixGameTestTemplate(false)
    public static void graphFuzzUndoRedoIntegrity(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var rng = new Random(SEED + 1);
        var graph = new TestGraph();
        var gm = graph.graphModel;

        // Build up a non-trivial graph first
        for (int i = 0; i < 8; i++) createRandomNode(gm, rng);
        for (int i = 0; i < 4; i++) createRandomVariable(gm, rng);
        for (int i = 0; i < 6; i++) createRandomWire(gm, rng);
        for (int i = 0; i < 3; i++) createVariableNodeForExisting(gm, rng);

        // Now simulate undo/redo cycles:
        // Take snapshots, do operations, restore snapshots, validate
        var undoStack = new ArrayDeque<CompoundTag>();
        var redoStack = new ArrayDeque<CompoundTag>();

        for (int i = 0; i < ITERATIONS; i++) {
            int action = rng.nextInt(5);
            try {
                switch (action) {
                    case 0 -> { // do an operation (push undo)
                        undoStack.push(gm.serializeNBT(provider));
                        redoStack.clear();
                        int op = rng.nextInt(8);
                        switch (op) {
                            case 0 -> createRandomNode(gm, rng);
                            case 1 -> createRandomWire(gm, rng);
                            case 2 -> deleteRandomNode(gm, rng);
                            case 3 -> deleteRandomWire(gm, rng);
                            case 4 -> createRandomVariable(gm, rng);
                            case 5 -> deleteRandomVariable(gm, rng);
                            case 6 -> modifyRandomVariable(gm, rng);
                            case 7 -> wireVariableNode(gm, rng);
                        }
                    }
                    case 1, 2 -> { // undo
                        if (!undoStack.isEmpty()) {
                            redoStack.push(gm.serializeNBT(provider));
                            gm.deserializeNBT(provider, undoStack.pop());
                        }
                    }
                    case 3 -> { // redo
                        if (!redoStack.isEmpty()) {
                            undoStack.push(gm.serializeNBT(provider));
                            gm.deserializeNBT(provider, redoStack.pop());
                        }
                    }
                    case 4 -> { // delete multiple elements at once
                        deleteRandomElements(gm, rng);
                    }
                }
            } catch (Exception e) {
                helper.fail("UndoRedo iteration " + i + " action " + action + " threw: " + e.getMessage());
                return;
            }

            String err = validateGraph(gm);
            if (err != null) {
                helper.fail("UndoRedo iteration " + i + " (action=" + action + "): " + err);
                return;
            }

            // Round-trip check
            var tag = gm.serializeNBT(provider);
            var graph2 = new TestGraph();
            graph2.graphModel.deserializeNBT(provider, tag);
            String err2 = validateGraph(graph2.graphModel);
            if (err2 != null) {
                helper.fail("UndoRedo iteration " + i + " round-trip: " + err2);
                return;
            }
        }

        LDLib2.LOGGER.info("GraphFuzzUndoRedoIntegrity passed {} iterations", ITERATIONS);
        helper.succeed();
    }

    // --- Operations ---

    private static final List<Class<? extends Node>> NODE_TYPES = List.of(
            TestAddNode.class, TestStringConcatNode.class, TestColorBlendNode.class, TestConstantNode.class
    );

    private static void createRandomNode(CustomGraphModelImpl gm, Random rng) {
        var pos = new Vector2f(rng.nextFloat() * 1000, rng.nextFloat() * 1000);
        int typeIdx = rng.nextInt(NODE_TYPES.size() + 2); // +2 for constant and variable
        if (typeIdx < NODE_TYPES.size()) {
            try {
                var node = NODE_TYPES.get(typeIdx).getConstructor().newInstance();
                gm.createNodeModel(node, pos);
            } catch (Exception e) {
                LDLib2.LOGGER.warn("Failed to create node: {}", e.getMessage());
            }
        } else if (typeIdx == NODE_TYPES.size()) {
            // constant node
            var floatType = TypeHandleHelpers.fromType(Float.class);
            gm.createConstantNode("const_" + rng.nextInt(100), pos, floatType, rng.nextFloat() * 100);
        } else {
            // variable + variable node
            var variable = gm.createVariable("var_" + rng.nextInt(100), float.class, rng.nextFloat(), VariableKind.LOCAL);
            if (variable != null) {
                gm.createVariableNode(
                        (VariableDeclarationModel) variable,
                        new Vector2f(pos.x + 100, pos.y), null, null);
            }
        }
    }

    private static void createRandomWire(CustomGraphModelImpl gm, Random rng) {
        var outputs = new ArrayList<PortModel>();
        var inputs = new ArrayList<PortModel>();
        for (var node : gm.getNodeModels()) {
            if (node == null) continue;
            if (node instanceof CustomNodeModelImpl cn) {
                outputs.addAll(cn.getOutputsById().values());
                inputs.addAll(cn.getInputsById().values());
            } else if (node instanceof ConstantNodeModel cn) {
                outputs.add(cn.getOutputPort());
            } else if (node instanceof VariableNodeModel vn) {
                if (vn.getOutputPort() != null) outputs.add(vn.getOutputPort());
                if (vn.getInputPort() != null) inputs.add(vn.getInputPort());
            }
        }
        if (outputs.isEmpty() || inputs.isEmpty()) return;

        var fromPort = outputs.get(rng.nextInt(outputs.size()));
        var toPort = inputs.get(rng.nextInt(inputs.size()));
        if (fromPort != null && toPort != null
                && fromPort.getNodeModel() != toPort.getNodeModel()) {
            try {
                gm.createWire(toPort, fromPort);
            } catch (Exception e) {
                // Ignore incompatible connections
            }
        }
    }

    private static void deleteRandomNode(CustomGraphModelImpl gm, Random rng) {
        var nodes = gm.getNodeModels().stream().filter(Objects::nonNull).toList();
        if (nodes.isEmpty()) return;
        var node = nodes.get(rng.nextInt(nodes.size()));
        gm.deleteElements(List.of(node));
    }

    private static void deleteRandomWire(CustomGraphModelImpl gm, Random rng) {
        var wires = gm.getWireModels().stream().filter(Objects::nonNull).toList();
        if (wires.isEmpty()) return;
        var wire = wires.get(rng.nextInt(wires.size()));
        gm.deleteWires(List.of(wire));
    }

    private static void deleteRandomElements(CustomGraphModelImpl gm, Random rng) {
        var all = new ArrayList<com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel>();
        for (var n : gm.getNodeModels()) if (n != null) all.add(n);
        for (var w : gm.getWireModels()) if (w != null) all.add(w);
        for (var v : gm.getGraphVariableModels()) if (v != null) all.add(v);
        if (all.isEmpty()) return;

        // Delete 1-3 random elements at once
        int count = Math.min(rng.nextInt(3) + 1, all.size());
        Collections.shuffle(all, rng);
        gm.deleteElements(all.subList(0, count));
    }

    // --- Variable types and kinds for fuzz ---

    private static final List<Class<?>> VAR_TYPES = List.of(
            float.class, int.class, boolean.class, String.class, Float.class, Integer.class
    );

    private static final VariableKind[] VAR_KINDS = VariableKind.values();
    private static final ModifierFlags[] MODIFIER_FLAGS = ModifierFlags.values();
    private static final VariableScope[] VAR_SCOPES = { VariableScope.LOCAL, VariableScope.EXPOSED };

    private static void createRandomVariable(CustomGraphModelImpl gm, Random rng) {
        var type = VAR_TYPES.get(rng.nextInt(VAR_TYPES.size()));
        var kind = VAR_KINDS[rng.nextInt(VAR_KINDS.length)];
        var name = "fuzzVar_" + rng.nextInt(200);
        Object defaultValue = switch (type.getSimpleName()) {
            case "float", "Float" -> rng.nextFloat() * 100;
            case "int", "Integer" -> rng.nextInt(1000);
            case "boolean" -> rng.nextBoolean();
            case "String" -> "str_" + rng.nextInt(50);
            default -> null;
        };
        gm.createVariable(name, type, defaultValue, kind);
    }

    private static void createVariableNodeForExisting(CustomGraphModelImpl gm, Random rng) {
        var vars = gm.getGraphVariableModels().stream().filter(Objects::nonNull).toList();
        if (vars.isEmpty()) return;
        var variable = vars.get(rng.nextInt(vars.size()));
        var pos = new Vector2f(rng.nextFloat() * 1000, rng.nextFloat() * 1000);
        gm.createVariableNode(variable, pos, null, null);
    }

    private static void deleteRandomVariable(CustomGraphModelImpl gm, Random rng) {
        var vars = gm.getGraphVariableModels().stream().filter(Objects::nonNull).toList();
        if (vars.isEmpty()) return;
        var variable = vars.get(rng.nextInt(vars.size()));
        boolean deleteUsages = rng.nextBoolean();
        gm.deleteVariableDeclaration(variable, deleteUsages);
    }

    private static void modifyRandomVariable(CustomGraphModelImpl gm, Random rng) {
        var vars = gm.getGraphVariableModels().stream().filter(Objects::nonNull).toList();
        if (vars.isEmpty()) return;
        var variable = vars.get(rng.nextInt(vars.size()));
        if (!(variable instanceof VariableDeclarationModel vdm)) return;

        int op = rng.nextInt(3);
        switch (op) {
            case 0 -> { // change modifiers
                vdm.setModifiers(MODIFIER_FLAGS[rng.nextInt(MODIFIER_FLAGS.length)]);
            }
            case 1 -> { // change scope
                vdm.setScope(VAR_SCOPES[rng.nextInt(VAR_SCOPES.length)]);
            }
            case 2 -> { // change type
                var newType = VAR_TYPES.get(rng.nextInt(VAR_TYPES.size()));
                vdm.setDataTypeHandle(TypeHandleHelpers.fromType(newType));
            }
        }
    }

    private static void wireVariableNode(CustomGraphModelImpl gm, Random rng) {
        // Find variable nodes and try to wire them to compatible ports
        var varOutputs = new ArrayList<PortModel>();
        var varInputs = new ArrayList<PortModel>();
        var otherOutputs = new ArrayList<PortModel>();
        var otherInputs = new ArrayList<PortModel>();

        for (var node : gm.getNodeModels()) {
            if (node == null) continue;
            if (node instanceof VariableNodeModel vn) {
                if (vn.getOutputPort() != null) varOutputs.add(vn.getOutputPort());
                if (vn.getInputPort() != null) varInputs.add(vn.getInputPort());
            } else if (node instanceof CustomNodeModelImpl cn) {
                otherOutputs.addAll(cn.getOutputsById().values());
                otherInputs.addAll(cn.getInputsById().values());
            } else if (node instanceof ConstantNodeModel cn) {
                otherOutputs.add(cn.getOutputPort());
            }
        }

        // Try to connect variable output -> other input, or other output -> variable input
        if (!varOutputs.isEmpty() && !otherInputs.isEmpty() && rng.nextBoolean()) {
            var from = varOutputs.get(rng.nextInt(varOutputs.size()));
            var to = otherInputs.get(rng.nextInt(otherInputs.size()));
            if (from != null && to != null && from.getNodeModel() != to.getNodeModel()) {
                try { gm.createWire(to, from); } catch (Exception ignored) {}
            }
        } else if (!otherOutputs.isEmpty() && !varInputs.isEmpty()) {
            var from = otherOutputs.get(rng.nextInt(otherOutputs.size()));
            var to = varInputs.get(rng.nextInt(varInputs.size()));
            if (from != null && to != null && from.getNodeModel() != to.getNodeModel()) {
                try { gm.createWire(to, from); } catch (Exception ignored) {}
            }
        }
    }

    // --- Validation ---

    private static String validateGraph(CustomGraphModelImpl gm) {
        for (var wire : gm.getWireModels()) {
            if (wire == null) continue;
            if (wire.getFromPort() == null) {
                return "Wire " + wire.getUid() + " has null fromPort";
            }
            if (wire.getToPort() == null) {
                return "Wire " + wire.getUid() + " has null toPort";
            }
            // Verify ports are registered in the graph
            if (gm.getModel(wire.getFromPort().getUid()) == null) {
                return "Wire " + wire.getUid() + " fromPort not registered in graph";
            }
            if (gm.getModel(wire.getToPort().getUid()) == null) {
                return "Wire " + wire.getUid() + " toPort not registered in graph";
            }
        }
        // Verify all nodes are consistent
        for (var node : gm.getNodeModels()) {
            if (node == null) continue;
            if (gm.getModel(node.getUid()) == null) {
                return "Node " + node.getUid() + " not registered in elementsByUID";
            }
            // Verify variable nodes reference valid declarations
            if (node instanceof VariableNodeModel vn) {
                var declUid = vn.getDeclarationModelUid();
                if (declUid != null) {
                    // The declaration should either exist in graphVariableModels or be resolvable
                    boolean found = gm.getGraphVariableModels().stream()
                            .anyMatch(v -> v != null && v.getUid().equals(declUid));
                    if (!found && gm.getModel(declUid) == null) {
                        return "VariableNode " + vn.getUid() + " references missing declaration " + declUid;
                    }
                }
            }
        }
        // Verify all variables are consistent
        for (var variable : gm.getGraphVariableModels()) {
            if (variable == null) continue;
            if (gm.getModel(variable.getUid()) == null) {
                return "Variable " + variable.getName() + " (" + variable.getUid() + ") not registered in elementsByUID";
            }
        }
        return null; // OK
    }

    private static int countNonNull(List<?> list) {
        return (int) list.stream().filter(Objects::nonNull).count();
    }
}
