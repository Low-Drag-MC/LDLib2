package com.lowdragmc.lowdraglib2.gui.editor.view;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import lombok.Getter;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class UICanvas extends UIElement {
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
        this.canvasModularUI.setAllowDebugMode(false);
    }

    public void stopSimulation() {
        if (this.canvasModularUI == null) return;
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
        ModularUIClientAccess.getWidget(this.canvasModularUI).mouseMoved(event.x, event.y);
        event.stopPropagation();
    }

    protected void onMouseDown(UIEvent event) {
        if (this.canvasModularUI == null) return;
        ModularUIClientAccess.getWidget(this.canvasModularUI).mouseClicked(
                new MouseButtonEvent(event.x, event.y, new MouseButtonInfo(event.button, event.modifiers)), false
        );
        // trigger dragging event as well
        startDrag(null, null);
        event.stopPropagation();
    }

    protected void onMouseUp(UIEvent event) {
        if (this.canvasModularUI == null) return;
        ModularUIClientAccess.getWidget(this.canvasModularUI).mouseReleased(new MouseButtonEvent(event.x, event.y, new MouseButtonInfo(event.button, event.modifiers)));
        event.stopPropagation();
    }

    protected void onMouseDrag(UIEvent event) {
        if (this.canvasModularUI == null) return;
        ModularUIClientAccess.getWidget(this.canvasModularUI).mouseDragged(new MouseButtonEvent(event.x, event.y, new MouseButtonInfo(event.button, event.modifiers)), event.deltaX, event.deltaY);
        event.stopPropagation();
    }

    protected void onMouseWheel(UIEvent event) {
        if (this.canvasModularUI == null) return;
        ModularUIClientAccess.getWidget(this.canvasModularUI).mouseScrolled(event.x, event.y, event.deltaX, event.deltaY);
        event.stopPropagation();
    }

    protected void onKeyDown(UIEvent event) {
        if (this.canvasModularUI == null) return;
        ModularUIClientAccess.getWidget(this.canvasModularUI).keyPressed(new KeyEvent(event.keyCode, event.scanCode, event.modifiers));
        event.stopPropagation();
    }

    protected void onKeyUp(UIEvent event) {
        if (this.canvasModularUI == null) return;
        ModularUIClientAccess.getWidget(this.canvasModularUI).keyReleased(new KeyEvent(event.keyCode, event.scanCode, event.modifiers));
        event.stopPropagation();
    }

    protected void onCharTyped(UIEvent event) {
        if (this.canvasModularUI == null) return;
        ModularUIClientAccess.getWidget(this.canvasModularUI).charTyped(new CharacterEvent(event.codePoint, event.modifiers));
        event.stopPropagation();
    }

    @Override
    public void screenTick() {
        super.screenTick();
        if (this.canvasModularUI == null) return;
        this.canvasModularUI.tick();
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        super.drawBackgroundAdditional(context);
        if (this.canvasModularUI == null) return;

        var contentWidth = (int) getContentWidth();
        var contentHeight = (int) getContentHeight();
        if (this.canvasModularUI.getScreenWidth() != contentWidth || this.canvasModularUI.getScreenHeight() != contentHeight) {
            this.canvasModularUI.init(contentWidth, contentHeight);
        }

        context.pose.pushPose();
        var posX = getContentX();
        var posY = getContentY();

        context.pose.translate(posX, posY);

        ModularUIClientAccess.getWidget(this.canvasModularUI).render(context.graphics, context.mouseX, context.mouseY, context.partialTick);

        context.pose.popPose();
    }
}
