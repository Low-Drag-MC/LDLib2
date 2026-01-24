package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.texture.ShaderTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.NoArgsConstructor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.player.Player;

@LDLRegisterClient(name="ld_shader_instance", registry = "ldlib2:screen_test")
@NoArgsConstructor
public class TestLDShaderInstance implements IScreenTest {
    CompoundTag serialized = new CompoundTag();

    @Override
    public ModularUI createUI(Player entityPlayer) {
        var root = new UIElement();
        root.layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.width(350);
            layout.height(300);
        }).setId("root");

        var group = new ConfiguratorGroup("root");
        group.setCollapse(false);
        var shaderTexture= new ShaderTexture();
        shaderTexture.buildConfigurator(group);
        var text = new TextElement();
        root.addChildren(
                new ScrollerView().addScrollViewChild(group).layout(layout -> {
                    layout.flex(1);
                    layout.heightPercent(100);
                }),
                new UIElement().layout(layout -> {
                    layout.flex(1);
                    layout.heightPercent(100);
                }).addChildren(
                        new Button().setText("serialize").setOnClick(e -> {
                            serialized = shaderTexture.serializeNBT(Platform.getFrozenRegistry());
                            text.setText(NbtUtils.toPrettyComponent(serialized));
                        }),
                        new Button().setText("deserialize").setOnClick(e -> {
                            shaderTexture.deserializeNBT(Platform.getFrozenRegistry(), serialized);
                        }),
                        new ScrollerView().addScrollViewChild(text.textStyle(style -> {
                            style.adaptiveHeight(true);
                            style.textWrap(TextWrap.WRAP);
                        }).layout(layout -> {
                            layout.widthPercent(100);
                        })).layout(layout -> {
                            layout.flex(1);
                            layout.widthPercent(100);
                        })))
                .addEventListener(UIEvents.REMOVED, e -> shaderTexture.close());

        return new ModularUI(UI.of(root));
    }
}
