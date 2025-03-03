package com.lowdragmc.lowdraglib.test.ui;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseGraph;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.parameter.ExposedParameter;
import com.lowdragmc.lowdraglib.gui.graphprocessor.widget.GraphViewWidget;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;

@LDLRegisterClient(name="node_graph", group = "ui_test")
@NoArgsConstructor
public class TestNodeGraphView implements IUITest {
    @Override
    public ModularUI createUI(IUIHolder holder, Player entityPlayer) {
        var parameters = new ArrayList<ExposedParameter<?>>();
        parameters.add(new LevelParameter("test_level_get").setAccessor(ExposedParameter.ParameterAccessor.Get));
        parameters.add(new ExposedParameter.Int("test_int_get").setAccessor(ExposedParameter.ParameterAccessor.Get));
        parameters.add(new ExposedParameter.Bool("test_bool_get").setAccessor(ExposedParameter.ParameterAccessor.Get));
        parameters.add(new ExposedParameter.Float("test_float_get").setAccessor(ExposedParameter.ParameterAccessor.Get));
        parameters.add(new ExposedParameter.String("test_string_get").setAccessor(ExposedParameter.ParameterAccessor.Get));
        parameters.add(new ExposedParameter.Int("test_int_set").setAccessor(ExposedParameter.ParameterAccessor.Set));
        parameters.add(new ExposedParameter.Bool("test_bool_set").setAccessor(ExposedParameter.ParameterAccessor.Set));
        parameters.add(new ExposedParameter.Float("test_float_set").setAccessor(ExposedParameter.ParameterAccessor.Set));
        parameters.add(new ExposedParameter.String("test_string_set").setAccessor(ExposedParameter.ParameterAccessor.Set));
        var graph = new BaseGraph(parameters);
        return IUITest.super.createUI(holder, entityPlayer)
                .widget(new GraphViewWidget(graph, 0, 0, getScreenWidth(), getScreenHeight(), supportNodeGroups -> {
                    if (LDLib.isModLoaded("kubejs")) {
                        supportNodeGroups.add("graph_processor.node.kjs");
                    }
                }));
    }

    public static class LevelParameter extends ExposedParameter<Level> {
        public LevelParameter(java.lang.String identifier) {
            super(identifier, Level.class);
        }
    }
}
