package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.gui.texture.RectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector4f;

@LDLRegisterClient(name = "render_operation", registry = "ldlib2:screen_test")
@NoArgsConstructor
public class TestRenderOperation implements IScreenTest {
    private int cornerSegments = 8;
    private float stroke = 4;
    private final Vector4f radius = new Vector4f(6, 20, 34, 10);

    @Override
    public ModularUI createUI(Player entityPlayer) {
        var root = new UIElement();
        root.layout(layout -> {
            layout.width(320);
            layout.height(220);
            layout.paddingAll(10);
            layout.gapAll(6);
        }).style(style -> style.backgroundTexture(new RectTexture()
                .setColor(0xFF1F1F1F)
                .setRadius(new Vector4f(8, 8, 8, 8))));

        var preview = new UIElement();
        preview.layout(layout -> {
            layout.width(180);
            layout.height(120);
        }).style(style -> style.backgroundTexture(createPreviewTexture()));

        Label status = new Label();
        status.setText(statusText());
        status.layout(layout -> layout.widthPercent(100));

        var increaseSegments = new Button().setText("segments +").setOnClick(e -> {
            cornerSegments = Math.min(cornerSegments + 1, 32);
            refreshPreview(preview, status);
        });

        var decreaseSegments = new Button().setText("segments -").setOnClick(e -> {
            cornerSegments = Math.max(cornerSegments - 1, 4);
            refreshPreview(preview, status);
        });

        var increaseStroke = new Button().setText("stroke +").setOnClick(e -> {
            stroke += 1;
            refreshPreview(preview, status);
        });

        var decreaseStroke = new Button().setText("stroke -").setOnClick(e -> {
            stroke = Math.max(0, stroke - 1);
            refreshPreview(preview, status);
        });

        root.addChildren(
                status,
                preview,
                new UIElement().layout(layout -> {
                    layout.flexDirection(FlexDirection.ROW);
                    layout.gapAll(4);
                }).addChildren(increaseSegments, decreaseSegments, increaseStroke, decreaseStroke)
        );

        return new ModularUI(UI.of(root));
    }

    private void refreshPreview(UIElement preview, Label status) {
        preview.style(style -> style.backgroundTexture(createPreviewTexture()));
        status.setText(statusText());
    }

    private RectTexture createPreviewTexture() {
        return new RectTexture()
                .setColor(0xFF58A6FF)
                .setBorderColor(0xFFF2C14E)
                .setStroke(stroke)
                .setRadius(new Vector4f(radius))
                .setCornerSegments(cornerSegments);
    }

    private String statusText() {
        return "RectTexture segments=" + cornerSegments + ", stroke=" + stroke +
                ", radius LT/RT/RB/LB=" + radius.x + "/" + radius.y + "/" + radius.z + "/" + radius.w;
    }
}
