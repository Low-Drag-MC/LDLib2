package com.lowdragmc.lowdraglib2.editor.ui.view.ui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.OptionalInt;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ModularUIPreview extends UIElement {
    public final UIEditorView editorView;

    // runtime;
    private OptionalInt previewWidth = OptionalInt.empty();
    private OptionalInt previewHeight = OptionalInt.empty();
    @Getter @Nullable
    private ModularUI modularUI;

    public ModularUIPreview(UIEditorView editorView) {
        this.editorView = editorView;
    }

    public void setModularUI(UI ui) {
        this.modularUI = new ModularUI(ui);
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
        }
        guiContext.pose.popPose();
    }

}
