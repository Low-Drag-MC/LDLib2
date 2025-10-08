package com.lowdragmc.lowdraglib2.editor.ui.view.ui;

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
    public final UIEditorView editorView;
    public final SelectionBox selectionBox = new SelectionBox();
    @Getter @Setter
    private boolean showSelectionBox = true;

    // runtime;
    private OptionalInt previewWidth = OptionalInt.empty();
    private OptionalInt previewHeight = OptionalInt.empty();
    @Getter @Nullable
    private ModularUI modularUI;

    public ModularUIPreview(UIEditorView editorView) {
        this.editorView = editorView;
        editorView.graphView.addChild(selectionBox);
    }

    public void setModularUI(UI ui) {
        this.modularUI = new ModularUI(ui);
        this.modularUI.setDrawTooltips(false);
        this.modularUI.setDrawDrag(false);
        this.modularUI.setAllowDebugMode(false);
        if (previewWidth.isPresent() && previewHeight.isPresent()) {
            this.modularUI.init(previewWidth.getAsInt(), previewHeight.getAsInt());
        }
    }

    public void clear() {
        if (this.modularUI == null) return;
        this.modularUI.onRemoved();
        this.modularUI = null;
    }

    public void initPreviewSize(int previewWidth, int previewHeight) {
        this.previewWidth = OptionalInt.of(previewWidth);
        this.previewHeight = OptionalInt.of(previewHeight);
        if (this.modularUI == null) return;
        this.modularUI.init(previewWidth, previewHeight);
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
        if (this.modularUI == null) return;
        guiContext.pose.pushPose();
        var posX = getPositionX();
        var posY = getPositionY();

        guiContext.pose.translate(posX, posY, 0);

        this.modularUI.getWidget().render(guiContext.graphics, guiContext.mouseX, guiContext.mouseY, guiContext.partialTick);

        if (isShiftDown()) {
            var hovered = modularUI.getLastHoveredElement();
            if (hovered != null) {
                modularUI.getWidget().renderUISpacing(hovered, guiContext.graphics);
            }
        } else if (selectionBox.isDisplayed() && selectionBox.label.isChildHover()) {
            var selectedOne = editorView.hierarchy.getSelectedOne();
            selectedOne.ifPresent(element -> modularUI.getWidget().renderUISpacing(element, guiContext.graphics));
        }
        guiContext.pose.popPose();
    }

    private void updateSelectionBox() {
        var selectedOne = editorView.hierarchy.getSelectedOne();
        if (showSelectionBox && selectedOne.isPresent()) {
            var selected = selectedOne.get();
            selectionBox.setDisplay(YogaDisplay.FLEX);
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

            selectionBox.layout(layout -> {
                layout.setPosition(YogaEdge.LEFT, x);
                layout.setPosition(YogaEdge.TOP, y);
                layout.setWidth(width);
                layout.setHeight(height);
            });
        } else {
            selectionBox.setDisplay(YogaDisplay.NONE);
        }
    }

    public class SelectionBox extends UIElement {
        public final UIElement widgetsGroup;
        public final Label label;

        public SelectionBox() {
            getLayout().setPositionType(YogaPositionType.ABSOLUTE);
            getLayout().setWidth(0);
            getLayout().setHeight(0);
            setDisplay(YogaDisplay.NONE);

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
