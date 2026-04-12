package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.wiget;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.WindowDragHelper;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wiget.PlacematModel;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

public class PlacematElement extends GraphElement<PlacematModel> {
    public static final String PLACEMAT_LAYER = "Placemat";
    private static final float RESIZE_BORDER = 5f;
    private static final Vector2f MIN_SIZE = new Vector2f(80, 50);
    private static final Vector2f MAX_SIZE = new Vector2f(4000, 4000);

    private Label titleLabel;

    public PlacematElement(PlacematModel model) {
        super(model);
    }

    @Override
    public String getLayerName() {
        return PLACEMAT_LAYER;
    }

    @Override
    protected void buildUI() {
        var model = getModel();
        getLayout().positionType(TaffyPosition.ABSOLUTE)
                .left(model.getPosition().x)
                .top(model.getPosition().y)
                .width(model.getSize().x)
                .height(model.getSize().y);
        getStyle().background(SDFRectTexture.of(model.getElementColor()));

        titleLabel = new Label();
        titleLabel.setText(Component.literal(model.getName()));
        titleLabel.getLayout().widthPercent(100).height(14).marginAll(2);
        titleLabel.getTextStyle().textColor(0xFFFFFFFF);
        addChild(titleLabel);

        // Border resize support
        WindowDragHelper.setBorderResize(this, this, RESIZE_BORDER, MIN_SIZE, MAX_SIZE,
                // only resize on left-click
                event -> event.button == 0,
                // allow all resize drags
                null,
                // on finish: sync new layout back to model
                event -> {
                    model.setPosition(new Vector2f(getLayoutX(), getLayoutY()));
                    model.setSize(new Vector2f(getSizeWidth(), getSizeHeight()));
                });

        internalSetup();
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        var model = getModel();
        if (visitor.hasHint(ChangeHint.LAYOUT)) {
            getLayout().left(model.getPosition().x)
                    .top(model.getPosition().y)
                    .width(model.getSize().x)
                    .height(model.getSize().y);
        }
        if (visitor.hasHint(ChangeHint.STYLE)) {
            getStyle().background(SDFRectTexture.of(model.getElementColor()));
        }
        if (visitor.hasHint(ChangeHint.DATA)) {
            if (titleLabel != null) {
                titleLabel.setText(Component.literal(model.getName()));
            }
        }
    }

    public void drawBackgroundOverlay(@NotNull GUIContext guiContext) {
        if (isSelected()) {
            guiContext.drawTexture(ColorPattern.BLUE.borderTexture(1),
                    getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
        } else {
            var isHover = isSelfOrChildHover() || isUnderRegionSelection();
            if (isHover) {
                guiContext.drawTexture(ColorPattern.BLUE.borderTexture(1).setColor(0xaaffffff),
                        getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
            }
        }
        // Draw resize cursor hint
        if (isSelfOrChildHover()) {
            WindowDragHelper.drawResizeIcon(guiContext, this, RESIZE_BORDER);
        }
    }
}
