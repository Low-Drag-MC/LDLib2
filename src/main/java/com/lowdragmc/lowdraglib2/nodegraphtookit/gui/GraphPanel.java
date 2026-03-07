package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutProperties;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.util.WindowDragHelper;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

public class GraphPanel extends UIElement {
    public final IGraphTool panel;
    public final GraphView graphView;
    public final UIElement titleBar = new UIElement();
    public final Toggle collapseToggle = new Toggle();
    public final Label title = new Label();
    public final UIElement content = new UIElement();
    // runtime
    @Getter
    private boolean isResizing;
    @Getter
    private boolean isCollapsed = false;

    public GraphPanel(GraphView graphView, IGraphTool panel) {
        this.panel = panel;
        this.graphView = graphView;

        getLayout().positionType(TaffyPosition.ABSOLUTE).width(150).height(150).paddingAll(2);
        getStyle().background(new ColorRectTexture(0xAA000000));

        collapseToggle.getLayout().height(9);
        collapseToggle.noText().setOnToggleChanged(this::setCollapsed);
        collapseToggle.toggleStyle(toggleStyle -> toggleStyle
                .baseTexture(IGuiTexture.EMPTY)
                .hoverTexture(IGuiTexture.EMPTY)
                .markTexture(Icons.RIGHT_ARROW_NO_BAR_S_WHITE)
                .unmarkTexture(Icons.DOWN_ARROW_NO_BAR_S_WHITE));

        title.getLayout().flexGrow(1);
        title.setText(panel.getTitle());
        title.setOverflowVisible(false);

        titleBar.getLayout().flexDirection(FlexDirection.ROW);
        titleBar.addChildren(collapseToggle, title);

        content.getLayout().flex(1);
        content.addChild(panel.getUIElement());

        addChildren(titleBar, content);
        WindowDragHelper.setDragMove(titleBar, this, e -> canDragMove(), null);
        WindowDragHelper.setBorderResize(this, this, 2, new Vector2f(20f), new Vector2f(Float.MAX_VALUE),
                e -> canResize(),
                (e, handle) -> {
                    isResizing = canResize();
                    return isResizing;
                }, e -> isResizing = false);

        setFocusable(true);
        internalSetup();
    }

    public void setCollapsed(boolean collapsed) {
        if (this.isCollapsed == collapsed) return;
        if (collapsed) {
            content.setDisplay(false);
            title.getTextStyle().adaptiveWidth(true);
            Style.importantPipeline(getLayout(), l -> l.widthAuto().heightAuto());
        } else {
            content.setDisplay(true);
            title.getLayout().widthAuto();
            title.getTextStyle().adaptiveWidth(false);
            getStyleBag().removeCandidates(LayoutProperties.WIDTH, slot -> slot.origin() == StyleOrigin.IMPORTANT);
            getStyleBag().removeCandidates(LayoutProperties.HEIGHT, slot -> slot.origin() == StyleOrigin.IMPORTANT);
        }

        this.isCollapsed = collapsed;
    }

    protected boolean canDragMove() {
        return true;
    }

    protected boolean canResize() {
        return !isCollapsed;
    }

    @Override
    protected void onLayoutChanged() {
        super.onLayoutChanged();
        if (canDragMove()) {
            adaptPositionToElement(graphView.canvas);
        }
    }

    @Override
    public void drawBackgroundAdditional(@NotNull GUIContext context) {
        super.drawBackgroundAdditional(context);
        if (canResize() && this.isSelfOrChildHover() && !isResizing) {
            WindowDragHelper.drawResizeIcon(context, this, 2);
        }
    }
}
