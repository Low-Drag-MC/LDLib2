package com.lowdragmc.lowdraglib2.gui.editor.view;

import com.lowdragmc.lowdraglib2.client.GameFrameCapture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class UICanvas extends UIElement {
    /** Simulated as a HUD: at the game screen's size, scaled down to fit, over the game's frame. */
    @Getter @Setter
    private boolean hud;
    // runtime
    @Getter @Nullable
    private ModularUI canvasModularUI;

    public UICanvas() {
        style(style -> style.backgroundTexture(Sprites.BORDER));
        this.addEventListener(UIEvents.MOUSE_MOVE, this::onMouseMove);
        this.addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        this.addEventListener(UIEvents.MOUSE_UP, this::onMouseUp);
        this.addEventListener(UIEvents.DRAG_UPDATE, this::onMouseDrag);
        this.addEventListener(UIEvents.MOUSE_WHEEL, this::onMouseWheel);
        this.addEventListener(UIEvents.KEY_DOWN, this::onKeyDown);
        this.addEventListener(UIEvents.KEY_UP, this::onKeyUp);
        this.addEventListener(UIEvents.CHAR_TYPED, this::onCharTyped);
        setFocusable(true);
    }


    public boolean isSimulating() {
        return this.canvasModularUI != null;
    }

    public void startSimulation(UI ui) {
        stopSimulation();
        this.canvasModularUI = new ModularUI(ui);
        this.canvasModularUI.embedIn(this, "Simulation");
        // so keys (and F3) reach the simulation without clicking into it first
        focus();
    }

    public void stopSimulation() {
        if (this.canvasModularUI == null) return;
        this.canvasModularUI.enableDebugger(false);
        this.canvasModularUI.onRemoved();
        this.canvasModularUI = null;
    }

    @Override
    protected void onRemoved() {
        super.onRemoved();
        stopSimulation();
    }

    protected void onMouseMove(UIEvent event) {
        if (this.canvasModularUI == null) return;
        this.canvasModularUI.getWidget().mouseMoved(event.x, event.y);
        event.stopPropagation();
    }

    protected void onMouseDown(UIEvent event) {
        if (this.canvasModularUI == null) return;
        this.canvasModularUI.getWidget().mouseClicked(event.x, event.y, event.button);
        // trigger dragging event as well
        startDrag(null, null);
        event.stopPropagation();
    }

    protected void onMouseUp(UIEvent event) {
        if (this.canvasModularUI == null) return;
        this.canvasModularUI.getWidget().mouseReleased(event.x, event.y, event.button);
        event.stopPropagation();
    }

    protected void onMouseDrag(UIEvent event) {
        if (this.canvasModularUI == null) return;
        this.canvasModularUI.getWidget().mouseDragged(event.x, event.y, event.button, event.deltaX, event.deltaY);
        event.stopPropagation();
    }

    protected void onMouseWheel(UIEvent event) {
        if (this.canvasModularUI == null) return;
        this.canvasModularUI.getWidget().mouseScrolled(event.x, event.y, event.deltaX, event.deltaY);
        event.stopPropagation();
    }

    protected void onKeyDown(UIEvent event) {
        if (this.canvasModularUI == null) return;
        this.canvasModularUI.getWidget().keyPressed(event.keyCode, event.scanCode, event.modifiers);
        event.stopPropagation();
    }

    protected void onKeyUp(UIEvent event) {
        if (this.canvasModularUI == null) return;
        this.canvasModularUI.getWidget().keyReleased(event.keyCode, event.scanCode, event.modifiers);
        event.stopPropagation();
    }

    protected void onCharTyped(UIEvent event) {
        if (this.canvasModularUI == null) return;
        this.canvasModularUI.getWidget().charTyped(event.codePoint, event.modifiers);
        event.stopPropagation();
    }

    @Override
    public void screenTick() {
        super.screenTick();
        if (this.canvasModularUI == null) return;
        this.canvasModularUI.tick();
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        super.drawBackgroundAdditional(guiContext);
        if (this.canvasModularUI == null) return;

        var contentWidth = (int) getContentWidth();
        var contentHeight = (int) getContentHeight();
        var width = contentWidth;
        var height = contentHeight;
        if (hud) {
            var window = Minecraft.getInstance().getWindow();
            // a minimized game window has no size, keep the last one
            width = window.getGuiScaledWidth() > 0 ? window.getGuiScaledWidth() : this.canvasModularUI.getScreenWidth();
            height = window.getGuiScaledHeight() > 0 ? window.getGuiScaledHeight() : this.canvasModularUI.getScreenHeight();
            if (width <= 0 || height <= 0 || contentWidth <= 0 || contentHeight <= 0) return;
        }
        if (this.canvasModularUI.getScreenWidth() != width || this.canvasModularUI.getScreenHeight() != height) {
            this.canvasModularUI.init(width, height);
        }

        guiContext.pose.pushPose();
        var posX = getContentX();
        var posY = getContentY();

        if (hud) {
            // the canvas is smaller than the screen, it is in the window with the editor round it
            var scale = Math.min(1f, Math.min(contentWidth / (float) width, contentHeight / (float) height));
            guiContext.pose.translate(posX + (contentWidth - width * scale) / 2, posY + (contentHeight - height * scale) / 2, 0);
            guiContext.pose.scale(scale, scale, 1);
            GameFrameCapture.draw(guiContext.graphics, 0, 0, width, height);
        } else {
            guiContext.pose.translate(posX, posY, 0);
        }

        this.canvasModularUI.getWidget().render(guiContext.graphics, guiContext.mouseX, guiContext.mouseY, guiContext.partialTick);

        // end batch
        guiContext.graphics.bufferSource().endBatch();

        guiContext.pose.popPose();
    }
}
