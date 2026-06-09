package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphViewLoggerApiTest {

    @Test
    void exposesManualGraphLoggerRefreshApi() throws NoSuchMethodException {
        var method = GraphView.class.getMethod("refreshGraphLogger");

        assertTrue(Modifier.isPublic(method.getModifiers()));
    }
}
