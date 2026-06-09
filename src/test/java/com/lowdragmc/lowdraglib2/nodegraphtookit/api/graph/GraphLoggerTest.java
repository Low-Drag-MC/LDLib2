package com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphLoggerTest {

    @Test
    void sortsEntriesByLevelAndKeepsInsertionOrderWithinSameLevel() {
        var logger = new GraphLogger();

        logger.info(Component.literal("info 1"));
        logger.warning(Component.literal("warning 1"));
        logger.error(Component.literal("error 1"));
        logger.warning(Component.literal("warning 2"));
        logger.info(Component.literal("info 2"));

        var sorted = logger.getSortedEntries();

        assertLevels(sorted,
                GraphLogger.Level.ERROR,
                GraphLogger.Level.WARNING,
                GraphLogger.Level.WARNING,
                GraphLogger.Level.INFO,
                GraphLogger.Level.INFO);
        assertEquals("error 1", sorted.get(0).message().getString());
        assertEquals("warning 1", sorted.get(1).message().getString());
        assertEquals("warning 2", sorted.get(2).message().getString());
        assertEquals("info 1", sorted.get(3).message().getString());
        assertEquals("info 2", sorted.get(4).message().getString());
    }

    @Test
    void storesOptionalContextOnEntries() {
        var logger = new GraphLogger();
        var context = new Object();

        logger.error(Component.literal("error"), context);

        assertEquals(context, logger.getSortedEntries().getFirst().context());
    }

    @Test
    void clearRemovesEntries() {
        var logger = new GraphLogger();
        logger.warning(Component.literal("warning"));

        logger.clear();

        assertTrue(logger.isEmpty());
        assertTrue(logger.getEntries().isEmpty());
        assertTrue(logger.getSortedEntries().isEmpty());
    }

    private static void assertLevels(List<GraphLogger.Entry> entries, GraphLogger.Level... levels) {
        assertEquals(levels.length, entries.size());
        for (int i = 0; i < levels.length; i++) {
            assertEquals(levels[i], entries.get(i).level());
        }
    }
}
