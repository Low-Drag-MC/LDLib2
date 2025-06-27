package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.client.shader.LDShaderInstance;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.TextWrap;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import lombok.NoArgsConstructor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.player.Player;
import org.appliedenergistics.yoga.YogaFlexDirection;

@LDLRegisterClient(name="ld_shader_instance", registry = "ui_test")
@NoArgsConstructor
public class TestLDShaderInstance implements IUITest {
    CompoundTag serialized = new CompoundTag();

    @Override
    public ModularUI createUI(Player entityPlayer) {
        var root = new UIElement();
        root.layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setWidth(350);
            layout.setHeight(300);
        }).setId("root");

        var group = new ConfiguratorGroup("root");
        group.setCollapse(false);
        var shaderInstance= LDShaderInstance.create(LDLib2.id("sprite_blit"), DefaultVertexFormat.POSITION_TEX_COLOR);
        assert shaderInstance != null;
        shaderInstance.buildConfigurator(group);
        var text = new TextElement();
        root.addChildren(
                new ScrollerView().addScrollViewChild(group).layout(layout -> {
                    layout.setFlex(1);
                    layout.setHeightPercent(100);
                }),
                new UIElement().layout(layout -> {
                    layout.setFlex(1);
                    layout.setHeightPercent(100);
                }).addChildren(
                        new Button().setText("serialize").setOnClick(e -> {
                            serialized = shaderInstance.serializeNBT(Platform.getFrozenRegistry());
                            text.setText(NbtUtils.toPrettyComponent(serialized));
                        }),
                        new Button().setText("deserialize").setOnClick(e -> {
                            shaderInstance.deserializeNBT(Platform.getFrozenRegistry(), serialized);
                        }),
                        new ScrollerView().addScrollViewChild(text.textStyle(style -> {
                            style.adaptiveHeight(true);
                            style.textWrap(TextWrap.WRAP);
                        }).layout(layout -> {
                            layout.setWidthPercent(100);
                        })).layout(layout -> {
                            layout.setFlex(1);
                            layout.setWidthPercent(100);
                        })))
                .addEventListener(UIEvents.REMOVED, e -> shaderInstance.close());

        return new ModularUI(UI.of(root));
    }
}
