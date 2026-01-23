package com.lowdragmc.lowdraglib2.gui.editor.view;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaDisplay;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaPositionType;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.OptionalInt;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ModularUIPreview extends UIElement {
    @Nullable
    public final UIEditorView editorView;
    public final SelectionBox selectionBox = new SelectionBox();
    @Getter @Setter
    private boolean showSelectionBox = true;

    // runtime;
    private OptionalInt previewWidth = OptionalInt.empty();
    private OptionalInt previewHeight = OptionalInt.empty();
    @Getter @Nullable
    private ModularUI previewModularUI;

    public ModularUIPreview(@Nullable UIEditorView editorView) {
        this.editorView = editorView;
        if (editorView != null) {
            editorView.graphView.addChild(selectionBox);
        }
    }

    public void loadUI(UI ui) {
        this.previewModularUI = new ModularUI(ui);
        this.previewModularUI.setDrawTooltips(false);
        this.previewModularUI.setDrawDrag(false);
        this.previewModularUI.setAllowDebugMode(false);
        if (previewWidth.isPresent() && previewHeight.isPresent()) {
            this.previewModularUI.init(previewWidth.getAsInt(), previewHeight.getAsInt());
        }
    }

    public void clear() {
        if (this.previewModularUI == null) return;
        this.previewModularUI.onRemoved();
        this.previewModularUI = null;
    }

    public void initPreviewSize(int previewWidth, int previewHeight) {
        this.previewWidth = OptionalInt.of(previewWidth);
        this.previewHeight = OptionalInt.of(previewHeight);
        if (this.previewModularUI == null) return;
        this.previewModularUI.init(previewWidth, previewHeight);
    }

    @Override
    protected void onRemoved() {
        super.onRemoved();
        clear();
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        updateSelectionBox();
        super.drawBackgroundAdditional(guiContext);
        if (this.previewModularUI == null) return;
        guiContext.pose.pushPose();
        var posX = getPositionX();
        var posY = getPositionY();

        guiContext.pose.translate(posX, posY, 0);

        this.previewModularUI.getWidget().render(guiContext.graphics, guiContext.mouseX, guiContext.mouseY, guiContext.partialTick);

        if (isShiftDown()) {
            var hovered = previewModularUI.getLastHoveredElement();
            if (hovered != null) {
                previewModularUI.getWidget().renderUISpacing(hovered, guiContext.graphics);
            }
        } else if (editorView != null && selectionBox.isDisplayed() && selectionBox.label.isSelfOrChildHover()) {
            var selectedOne = editorView.hierarchy.getSelectedOne();
            selectedOne.ifPresent(element -> previewModularUI.getWidget().renderUISpacing(element, guiContext.graphics));
        }
        guiContext.pose.popPose();
    }

    private void updateSelectionBox() {
        if (editorView == null) return;
        var selectedOne = editorView.hierarchy.getSelectedOne();
        if (showSelectionBox && selectedOne.isPresent()) {
            var selected = selectedOne.get();
            selectionBox.setDisplay(true);
            var posX = selected.getPositionX();
            var posY = selected.getPositionY();
            var sizeX = selected.getSizeWidth();
            var sizeY = selected.getSizeHeight();
            var marginTop = selected.getMarginTop();
            var marginBottom = selected.getMarginBottom();
            var marginLeft = selected.getMarginLeft();
            var marginRight = selected.getMarginRight();
            var scale = editorView.graphView.getScale();
            var offsetX = editorView.graphView.getOffsetX();
            var offsetY = editorView.graphView.getOffsetY();
            var width = (sizeX + marginLeft + marginRight) * scale;
            var height = (sizeY + marginTop + marginBottom) * scale;
            var x = (posX - marginLeft - offsetX) * scale;
            var y = (posY - marginTop - offsetY) * scale;

            this.selectionBox.layout(layout -> {
                layout.setPosition(YogaEdge.LEFT, x);
                layout.setPosition(YogaEdge.TOP, y);
                layout.setWidth(width);
                layout.setHeight(height);
            });
        } else {
            selectionBox.setDisplay(false);
        }
    }

    @Override
    protected boolean isInsideTheScissorView(GUIContext context) {
        return true;
    }

    public class SelectionBox extends UIElement {
        public final UIElement widgetsGroup;
        public final Label label;

        public SelectionBox() {
            getLayout().setPositionType(YogaPositionType.ABSOLUTE);
            getLayout().setWidth(0);
            getLayout().setHeight(0);
            setDisplay(false);

            getStyle().backgroundTexture(ColorPattern.BLUE.borderTexture(1));
            widgetsGroup = new UIElement();
            label = new Label();
            widgetsGroup.layout(layout -> {
                layout.setFlexDirection(YogaFlexDirection.ROW);
                layout.setPositionType(YogaPositionType.ABSOLUTE);
                layout.setPosition(YogaEdge.TOP, -15);
                layout.setPadding(YogaEdge.ALL, 2);
                layout.setHeight(14);
            });
            widgetsGroup.addChildren(
                    label.bindDataSource(SupplierDataSource.of(() ->
                            editorView == null ? Component.empty() : editorView.hierarchy.getSelectedOne().map(UIElement::getEditorName).orElseGet(Component::empty)))
                            .textStyle(textStyle -> textStyle.adaptiveWidth(true))
            );
            widgetsGroup.getStyle().backgroundTexture(ColorPattern.BLUE.rectTexture());
            addChild(widgetsGroup);
        }
    }

}
