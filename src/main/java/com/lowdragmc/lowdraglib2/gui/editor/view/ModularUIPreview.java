package com.lowdragmc.lowdraglib2.gui.editor.view;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
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
            var graphScale = editorView.graphView.getScale();
            var offsetX = editorView.graphView.getOffsetX();
            var offsetY = editorView.graphView.getOffsetY();

            // Corners of the element rect including margins
            float left = posX - marginLeft;
            float top = posY - marginTop;
            float right = posX + sizeX + marginRight;
            float bottom = posY + sizeY + marginBottom;

            // Build combined transform matrix (element + all ancestors)
            var matrix = buildTransformMatrix(selected);

            // Transform the 4 corners and compute AABB
            var corners = new Vector4f[]{
                    new Vector4f(left, top, 0, 1),
                    new Vector4f(right, top, 0, 1),
                    new Vector4f(left, bottom, 0, 1),
                    new Vector4f(right, bottom, 0, 1)
            };
            float minX = Float.POSITIVE_INFINITY;
            float minY = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY;
            float maxY = Float.NEGATIVE_INFINITY;
            for (var corner : corners) {
                matrix.transform(corner);
                float cx = corner.x / corner.w;
                float cy = corner.y / corner.w;
                minX = Math.min(minX, cx);
                minY = Math.min(minY, cy);
                maxX = Math.max(maxX, cx);
                maxY = Math.max(maxY, cy);
            }

            var x = (minX - offsetX) * graphScale;
            var y = (minY - offsetY) * graphScale;
            var width = (maxX - minX) * graphScale;
            var height = (maxY - minY) * graphScale;

            this.selectionBox.layout(layout -> {
                layout.left(x);
                layout.top(y);
                layout.width(width);
                layout.height(height);
            });
        } else {
            selectionBox.setDisplay(false);
        }
    }

    /**
     * Build the combined transform matrix for an element, including all ancestor transforms.
     * Transforms are applied from root to element (outermost first).
     */
    private Matrix4f buildTransformMatrix(UIElement element) {
        var chain = new ArrayList<UIElement>();
        var current = element;
        while (current != null) {
            if (!current.getStyle().transform2D().isIdentity()) {
                chain.add(current);
            }
            current = current.getParent();
        }
        var matrix = new Matrix4f();
        // Apply from root to element (reverse order)
        for (int i = chain.size() - 1; i >= 0; i--) {
            var elem = chain.get(i);
            elem.getStyle().transform2D().pushPose(matrix, elem);
        }
        return matrix;
    }

    @Override
    protected boolean isInsideTheScissorView(GUIContext context) {
        return true;
    }

    public class SelectionBox extends UIElement {
        public final UIElement widgetsGroup;
        public final Label label;

        public SelectionBox() {
            getLayout().positionType(TaffyPosition.ABSOLUTE);
            getLayout().width(0);
            getLayout().height(0);
            setDisplay(false);

            getStyle().backgroundTexture(ColorPattern.BLUE.borderTexture(1));
            widgetsGroup = new UIElement();
            label = new Label();
            widgetsGroup.layout(layout -> {
                layout.flexDirection(FlexDirection.ROW);
                layout.positionType(TaffyPosition.ABSOLUTE);
                layout.top(-15);
                layout.paddingAll(2);
                layout.height(14);
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
