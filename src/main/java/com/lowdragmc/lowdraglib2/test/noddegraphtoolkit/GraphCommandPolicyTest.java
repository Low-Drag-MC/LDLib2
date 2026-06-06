package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.IGraphCommand;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Verifies the command-policy plumbing: a {@link com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph}
 * override of {@code canExecuteCommand} / {@code onCommandExecuted} is reached through
 * {@code CustomGraphModelImpl}'s delegation. The actual veto/listener behavior at
 * {@code GraphView.dispatchCommand} is UI and validated manually.
 *
 * <p>Commands are passed as {@code null} so no real command object (which references the client-only
 * {@code GraphView} in its signatures) is constructed on the dedicated server — the overrides ignore
 * the argument and decide by flag/counter.</p>
 */
@GameTestHolder(LDLib2.MOD_ID)
public class GraphCommandPolicyTest {

    /** A TestGraph whose command policy is driven by a flag, recording post-execute calls. */
    private static class PolicyTestGraph extends TestGraph {
        boolean allowCommands = true;
        int executedCount = 0;

        @Override
        public boolean canExecuteCommand(IGraphCommand command) {
            return allowCommands;
        }

        @Override
        public void onCommandExecuted(IGraphCommand command) {
            executedCount++;
        }
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void canExecuteCommandDelegatesToGraph(GameTestHelper helper) {
        LDLib2.LOGGER.info("Start canExecuteCommandDelegatesToGraph");

        var graph = new PolicyTestGraph();

        // GraphModel must forward to the Graph override.
        if (!graph.graphModel.canExecuteCommand(null)) {
            helper.fail("expected canExecuteCommand to allow by default flag (true)"); return;
        }
        graph.allowCommands = false;
        if (graph.graphModel.canExecuteCommand(null)) {
            helper.fail("canExecuteCommand did not delegate the false flag through GraphModel"); return;
        }

        LDLib2.LOGGER.info("End canExecuteCommandDelegatesToGraph - PASSED");
        helper.succeed();
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void onCommandExecutedDelegatesToGraph(GameTestHelper helper) {
        LDLib2.LOGGER.info("Start onCommandExecutedDelegatesToGraph");

        var graph = new PolicyTestGraph();
        graph.graphModel.onCommandExecuted(null);
        graph.graphModel.onCommandExecuted(null);

        if (graph.executedCount != 2) {
            helper.fail("expected 2 post-execute delegations, got " + graph.executedCount); return;
        }

        LDLib2.LOGGER.info("End onCommandExecutedDelegatesToGraph - PASSED");
        helper.succeed();
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void defaultsArePermissive(GameTestHelper helper) {
        LDLib2.LOGGER.info("Start defaultsArePermissive");

        var graph = new TestGraph();
        // Default graph allows everything and the post-hook is a no-op that must not throw.
        if (!graph.graphModel.canExecuteCommand(null)) {
            helper.fail("default canExecuteCommand should allow"); return;
        }
        try {
            graph.graphModel.onCommandExecuted(null);
        } catch (Exception e) {
            helper.fail("default onCommandExecuted should be a no-op, threw: " + e.getMessage()); return;
        }

        LDLib2.LOGGER.info("End defaultsArePermissive - PASSED");
        helper.succeed();
    }
}
