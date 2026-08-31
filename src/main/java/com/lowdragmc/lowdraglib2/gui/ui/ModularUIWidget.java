package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.core.mixins.accessor.MinecraftAccessor;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.holder.DebugScreen;
import com.lowdragmc.lowdraglib2.gui.holder.IAbstractContainerScreenExt;
import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolder;
import com.lowdragmc.lowdraglib2.gui.ui.event.CommandEvents;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ModularUIWidget implements GuiEventListener, NarratableEntry, Renderable, IModularUIHolder {
    private final ModularUI modularUI;
    private long lastTick;

    ModularUIWidget(ModularUI modularUI) {
        this.modularUI = modularUI;
    }

    @Override
    public ModularUI getModularUI() {
        return modularUI;
    }

    @Override
    public NarrationPriority narrationPriority() {
        if (modularUI.focused) {
            return NarrationPriority.FOCUSED;
        }
        return isHovered() ? NarrationPriority.HOVERED : NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
    }

    public boolean isHovered() {
        return modularUI.getLastHoveredElement() != null;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return isHovered();
    }

    @Override
    public ScreenRectangle getRectangle() {
        return new ScreenRectangle(
                Math.round(modularUI.getLeftPos()),
                Math.round(modularUI.getTopPos()),
                Math.round(modularUI.getWidth()),
                Math.round(modularUI.getHeight())
        );
    }

    @Override
    public void setFocused(boolean focused) {
        modularUI.setFocused(focused);
    }

    @Override
    public boolean isFocused() {
        return modularUI.isFocused();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        // Picking comes first and swallows the press: in focus mode the pointer is an inspector,
        // and inspecting a button is not supposed to also press it. Handled here rather than in
        // whatever host is showing this UI, because since the debugger became a window of its own
        // it need not be the same host - and both paths arrive through this method.
        var picking = ModularUIClientAccess.pickingDebugger(modularUI);
        if (picking != null) {
            picking.pickHovered();
            return true;
        }
        modularUI.lastMouseDownX = (float) mouseButtonEvent.x();
        modularUI.lastMouseDownY = (float) mouseButtonEvent.y();
        modularUI.lastMouseDownButton = mouseButtonEvent.button();
        modularUI.lastMouseDownElement = modularUI.getLastHoveredElement();
        if (modularUI.lastMouseDownElement != null) {
            if (!modularUI.lastMouseDownElement.isFocusable()) {
                modularUI.clearFocus();
                var structurePath = modularUI.lastMouseDownElement.getStructurePath();
                for (int i = structurePath.size() - 1; i >= 0; i--) {
                    var element = structurePath.get(i);
                    if (element.isFocusable()) {
                        modularUI.requestFocus(element);
                        break;
                    }
                }
            } else if (modularUI.lastMouseDownElement.isActive()) {
                modularUI.requestFocus(modularUI.lastMouseDownElement);
            }
            var event = UIEvent.create(UIEvents.MOUSE_DOWN);
            event.x = modularUI.lastMouseDownX;
            event.y = modularUI.lastMouseDownY;
            event.button = modularUI.lastMouseDownButton;
            event.target = modularUI.lastMouseDownElement;
            UIEventDispatcher.dispatchEvent(event);
            return event.hasHandler;
        }
        modularUI.clearFocus();
        return false;
    }

    /**
     * Cause forge call mouseReleased twice, ContainerEventHandlerMixin will also be called twice.
     * We should cache the result and return it. see {@link com.lowdragmc.lowdraglib2.core.mixins.ui.ContainerEventHandlerMixin}
     * and {@link com.lowdragmc.lowdraglib2.core.mixins.ui.AbstractContainerMenuMixin}
     */
    private int lastMouseReleasedMark;
    private boolean lastMouseReleasedResult;

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        // The other half of the swallowed press. Without it the release still lands, and against
        // whatever element the last real click remembered — which fires a CLICK on it, so
        // inspecting a button you had already pressed once would press it again. Ahead of the
        // double-dispatch guard below, so both of the two calls answer the same way.
        if (ModularUIClientAccess.pickingDebugger(modularUI) != null) return true;
        if (ModularUIClientAccess.getScreen(getModularUI()) instanceof IAbstractContainerScreenExt ext) {
            if (ext.getLdlib2$mouseReleasedMark() == lastMouseReleasedMark) {
                return lastMouseReleasedResult;
            }
            lastMouseReleasedMark = ext.getLdlib2$mouseReleasedMark();
        }
        var mouseX = mouseButtonEvent.x();
        var mouseY = mouseButtonEvent.y();
        var button = mouseButtonEvent.button();
        modularUI.lastMouseDownButton = -1;
        var releasedElement = modularUI.getLastHoveredElement();
        if (modularUI.getDragHandler().isDragging()) {
            if (releasedElement != null) {
                var event = UIEvent.create(UIEvents.DRAG_PERFORM);
                dispatchDragEvent(mouseX, mouseY, button, 0, 0, releasedElement, event);
            }
            modularUI.getDragHandler().stopDrag(releasedElement);
        }
        if (releasedElement != null) {
            var event = UIEvent.create(UIEvents.MOUSE_UP);
            event.x = (float) mouseX;
            event.y = (float) mouseY;
            event.button = button;
            event.target = releasedElement;
            UIEventDispatcher.dispatchEvent(event);
            var hasHandler = event.hasHandler;
            if (releasedElement == modularUI.lastMouseDownElement) {
                var clickEvent = UIEvent.create(UIEvents.CLICK);
                clickEvent.x = (float) mouseX;
                clickEvent.y = (float) mouseY;
                clickEvent.button = button;
                clickEvent.target = releasedElement;
                UIEventDispatcher.dispatchEvent(clickEvent);
                hasHandler |= clickEvent.hasHandler;
                if (modularUI.lastMouseClickElement == releasedElement && button == modularUI.lastMouseClickButton) {
                    if (System.currentTimeMillis() - modularUI.lastMouseClickTime < 300) {
                        var doubleClickEvent = UIEvent.create(UIEvents.DOUBLE_CLICK);
                        doubleClickEvent.x = (float) mouseX;
                        doubleClickEvent.y = (float) mouseY;
                        doubleClickEvent.button = button;
                        doubleClickEvent.target = releasedElement;
                        UIEventDispatcher.dispatchEvent(doubleClickEvent);
                        hasHandler |= doubleClickEvent.hasHandler;
                        modularUI.lastMouseClickElement = null;
                    } else {
                        modularUI.lastMouseClickElement = releasedElement;
                    }
                } else {
                    modularUI.lastMouseClickElement = releasedElement;
                }
            }
            modularUI.lastMouseClickButton = button;
            modularUI.lastMouseClickTime = System.currentTimeMillis();
            return lastMouseReleasedResult = hasHandler;
        }
        modularUI.lastMouseClickButton = button;
        modularUI.lastMouseClickTime = System.currentTimeMillis();
        return lastMouseReleasedResult = false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        var current = modularUI.getLastHoveredElement();
        if (current != null) {
            var event = UIEvent.create(UIEvents.MOUSE_MOVE);
            event.x = (float) mouseX;
            event.y = (float) mouseY;
            event.target = current;
            UIEventDispatcher.dispatchEvent(event);
        }
        if (modularUI.lastMouseMoveElement == null && current != null) {
            modularUI.lastMouseMoveElement = current;
            triggerMouseEnter(modularUI.lastMouseMoveElement, mouseX, mouseY);
        } else if (modularUI.lastMouseMoveElement != null && current == null) {
            triggerMouseLeave(modularUI.lastMouseMoveElement, mouseX, mouseY);
            modularUI.lastMouseMoveElement = null;
        } else if (modularUI.lastMouseMoveElement != null && modularUI.lastMouseMoveElement != current) {
            triggerMouseLeave(modularUI.lastMouseMoveElement, mouseX, mouseY);
            triggerMouseEnter(current, mouseX, mouseY);
            modularUI.lastMouseMoveElement = current;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        var current = modularUI.getLastHoveredElement();
        if (current != null) {
            var event = UIEvent.create(UIEvents.MOUSE_WHEEL);
            event.x = (float) mouseX;
            event.y = (float) mouseY;
            event.deltaX = (float) scrollX;
            event.deltaY = (float) scrollY;
            event.target = current;
            UIEventDispatcher.dispatchEvent(event);
            return event.hasHandler;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
        var mouseX = mouseButtonEvent.x();
        var mouseY = mouseButtonEvent.y();
        var button = mouseButtonEvent.button();
        if (modularUI.getDragHandler().isDragging()) {
            var hasHandler = false;
            var current = modularUI.getLastHoveredElement();
            if (modularUI.getDragHandler().dragSource != null) {
                var event = UIEvent.create(UIEvents.DRAG_SOURCE_UPDATE);
                event.hasBubblePhase = false;
                event.hasCapturePhase = true;
                dispatchDragEvent(mouseX, mouseY, button, dragX, dragY, modularUI.getDragHandler().dragSource, event);
                hasHandler = event.hasHandler;
            }
            if (current != null) {
                if (modularUI.lastMouseDragElement == current) {
                    var event = UIEvent.create(UIEvents.DRAG_UPDATE);
                    dispatchDragEvent(mouseX, mouseY, button, dragX, dragY, current, event);
                    hasHandler |= event.hasHandler;
                } else {
                    if (modularUI.lastMouseDragElement != null) {
                        var event = UIEvent.create(UIEvents.DRAG_LEAVE);
                        event.hasBubblePhase = false;
                        event.relatedTarget = current;
                        dispatchDragEvent(mouseX, mouseY, button, dragX, dragY, modularUI.lastMouseDragElement, event);
                    }
                    modularUI.lastMouseDragElement = current;
                    var event = UIEvent.create(UIEvents.DRAG_ENTER);
                    event.hasBubblePhase = false;
                    dispatchDragEvent(mouseX, mouseY, button, dragX, dragY, current, event);
                    hasHandler |= event.hasHandler;
                }
                return hasHandler;
            } else if (modularUI.lastMouseDragElement != null) {
                var event = UIEvent.create(UIEvents.DRAG_LEAVE);
                event.hasBubblePhase = false;
                dispatchDragEvent(mouseX, mouseY, button, dragX, dragY, modularUI.lastMouseDragElement, event);
                modularUI.lastMouseDragElement = null;
                hasHandler |= event.hasHandler;
                return hasHandler;
            }
        }
        return false;
    }

    private void dispatchDragEvent(double mouseX, double mouseY, int button, double dragX, double dragY, UIElement current, UIEvent event) {
        event.x = (float) mouseX;
        event.y = (float) mouseY;
        event.button = button;
        event.deltaX = (float) dragX;
        event.deltaY = (float) dragY;
        event.dragStartX = modularUI.lastMouseDownX;
        event.dragStartY = modularUI.lastMouseDownY;
        event.dragHandler = modularUI.getDragHandler();
        event.target = current;
        UIEventDispatcher.dispatchEvent(event, true, true, false);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        var keyCode = keyEvent.key();
        var scanCode = keyEvent.scancode();
        var modifiers = keyEvent.modifiers();
        if (modularUI.isAllowDebugMode() && keyCode == GLFW.GLFW_KEY_F12) {
            ModularUIClientAccess.enableDebugger(modularUI, !modularUI.isDebugMode());
        }
        // The debugger's own chords, handled here so they work with the pointer over the UI being
        // inspected — which, now that the debugger is a window of its own, is not where its
        // keyboard is. Live only while it is open, so a UI's own F1 is untouched otherwise.
        var debugger = ModularUIClientAccess.activeDebugger(modularUI);
        if (debugger != null) {
            if (keyCode == GLFW.GLFW_KEY_F1) {
                debugger.setFocusMode(!debugger.isFocusMode());
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_F4) {
                debugger.setRenderUIShaping(!debugger.isRenderUIShaping());
                return true;
            }
        }
        modularUI.lastPressedKeyCode = keyCode;
        modularUI.lastPressedScanCode = scanCode;
        modularUI.lastPressedModifiers = modifiers;
        var command = getCommandType(keyEvent);
        if (modularUI.focusedElement != null) {
            var event = UIEvent.create(UIEvents.KEY_DOWN);
            event.keyCode = keyCode;
            event.scanCode = scanCode;
            event.modifiers = modifiers;
            event.target = modularUI.focusedElement;
            UIEventDispatcher.dispatchEvent(event);
            var hasHandler = event.hasHandler;
            if (command != null) {
                event = createExecuteCommandEvent(command, keyCode, scanCode, modifiers);
                event.target = modularUI.focusedElement;
                UIEventDispatcher.dispatchEvent(event);
                hasHandler |= event.hasHandler;
            }
            return hasHandler;
        } else if (command != null) {
            var event = createValidCommandEvent(command, keyCode, scanCode, modifiers);
            event.target = modularUI.ui.rootElement;
            var handled = UIEventDispatcher.dispatchAllChildren(event);
            var hasHandler = event.hasHandler;
            if (handled && event.currentElement != null) {
                var executeCommandEvent = createExecuteCommandEvent(command, keyCode, scanCode, modifiers);
                executeCommandEvent.target = event.currentElement;
                UIEventDispatcher.dispatchEvent(executeCommandEvent);
                hasHandler |= event.hasHandler;
            }
            return hasHandler;
        }
        return false;
    }

    @Nullable
    protected String getCommandType(KeyEvent keyEvent) {
        var keyCode = keyEvent.key();
        if (keyEvent.isCopy()) {
            return CommandEvents.COPY;
        } else if (keyEvent.isPaste()) {
            return CommandEvents.PASTE;
        } else if (keyEvent.isCut()) {
            return CommandEvents.CUT;
        } else if (keyEvent.isSelectAll()) {
            return CommandEvents.SELECT_ALL;
        } else if (keyCode == GLFW.GLFW_KEY_Z && UIElement.isCtrlOrCmdDown() && !UIElement.isShiftDown() && !UIElement.isAltDown()) {
            return CommandEvents.UNDO;
        } else if (keyCode == GLFW.GLFW_KEY_Z && UIElement.isCtrlOrCmdDown() && UIElement.isShiftDown() && !UIElement.isAltDown()) {
            return CommandEvents.REDO;
        } else if (keyCode == GLFW.GLFW_KEY_Y && UIElement.isCtrlOrCmdDown() && !UIElement.isShiftDown() && !UIElement.isAltDown()) {
            return CommandEvents.REDO;
        } else if (keyCode == GLFW.GLFW_KEY_F && UIElement.isCtrlOrCmdDown() && !UIElement.isShiftDown() && !UIElement.isAltDown()) {
            return CommandEvents.FIND;
        } else if (keyCode == GLFW.GLFW_KEY_S && UIElement.isCtrlOrCmdDown() && !UIElement.isShiftDown() && !UIElement.isAltDown()) {
            return CommandEvents.SAVE;
        }
        return null;
    }

    protected UIEvent createValidCommandEvent(String command, int keyCode, int scanCode, int modifiers) {
        var event = UIEvent.create(UIEvents.VALIDATE_COMMAND);
        event.hasBubblePhase = false;
        event.hasCapturePhase = false;
        event.keyCode = keyCode;
        event.scanCode = scanCode;
        event.modifiers = modifiers;
        event.command = command;
        return event;
    }

    protected UIEvent createExecuteCommandEvent(String command, int keyCode, int scanCode, int modifiers) {
        var event = UIEvent.create(UIEvents.EXECUTE_COMMAND);
        event.hasBubblePhase = false;
        event.hasCapturePhase = false;
        event.keyCode = keyCode;
        event.scanCode = scanCode;
        event.modifiers = modifiers;
        event.command = command;
        return event;
    }

    @Override
    public boolean keyReleased(KeyEvent keyEvent) {
        if (modularUI.focusedElement != null) {
            var event = UIEvent.create(UIEvents.KEY_UP);
            event.keyCode = keyEvent.key();
            event.scanCode = keyEvent.scancode();
            event.modifiers = keyEvent.modifiers();
            event.target = modularUI.focusedElement;
            UIEventDispatcher.dispatchEvent(event);
            return event.hasHandler;
        }
        return false;
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        if (modularUI.focusedElement != null) {
            var event = UIEvent.create(UIEvents.CHAR_TYPED);
            event.codePoint = (char) characterEvent.codepoint();
//            event.modifiers = characterEvent.modifiers();
            event.hasCapturePhase = false;
            event.hasBubblePhase = false;
            event.target = modularUI.focusedElement;
            UIEventDispatcher.dispatchEvent(event);
            return event.hasHandler;
        }
        return false;
    }

    private void triggerMouseEnter(UIElement element, double mouseX, double mouseY) {
        var event = UIEvent.create(UIEvents.MOUSE_ENTER);
        event.hasBubblePhase = false;
        event.x = (float) mouseX;
        event.y = (float) mouseY;
        event.target = element;
        UIEventDispatcher.dispatchEvent(event);
    }

    private void triggerMouseLeave(UIElement element, double mouseX, double mouseY) {
        var event = UIEvent.create(UIEvents.MOUSE_LEAVE);
        event.hasBubblePhase = false;
        event.x = (float) mouseX;
        event.y = (float) mouseY;
        event.target = element;
        UIEventDispatcher.dispatchEvent(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (modularUI.isTickWhileRending() && Minecraft.getInstance() instanceof MinecraftAccessor accessor) {
            var currentTick = accessor.ldlib2$getClientTickCount();
            if (currentTick != lastTick) {
                modularUI.tick();
                lastTick = currentTick;
            }
        }
        if (modularUI.isDebugMode() && mouseX == Integer.MAX_VALUE && mouseY == Integer.MAX_VALUE) {
            mouseX = DebugScreen.REAL_MOUSE_POS.x;
            mouseY = DebugScreen.REAL_MOUSE_POS.y;
        }
        modularUI.getAnimationEngine().updateFrame();
        modularUI.calculateStyleAndLayout();
        ModularUIClientAccess.cleanTooltip(modularUI);

        modularUI.lastDrawPose = new Matrix3x2f(guiGraphics.pose());
        var context = GUIContext.of(guiGraphics, mouseX, mouseY, partialTick);
        modularUI.refreshHoveredElement(context.localMouseX, context.localMouseY);
        modularUI.ui.rootElement.drawInBackground(context);

        if (modularUI.lastHoveredElement != null && ModularUIClientAccess.getHoverTooltips(modularUI) == null) {
            var element = modularUI.lastHoveredElement;
            while (element != null) {
                var event = UIEvent.create(UIEvents.HOVER_TOOLTIPS);
                event.hasBubblePhase = false;
                event.hasCapturePhase = false;
                event.target = element;
                UIEventDispatcher.dispatchDirectEvent(event, false);
                if (event.hoverTooltips != null) {
                    ModularUIClientAccess.setHoverTooltip(modularUI, event.hoverTooltips);
                    break;
                }
                if (!element.getStyle().tooltips().isEmpty()) {
                    ModularUIClientAccess.setHoverTooltip(modularUI, HoverTooltips.create(Arrays.stream(element.getStyle().tooltips().tooltips()).toArray()));
                    break;
                }
                element = element.getParent();
            }
        }

        context.callPostRendering();

        // Above the UI's own content, below its tooltips - and drawn here, in the inspected UI's
        // frame, because the debugger showing these outlines may well be in a different window.
        var debugger = ModularUIClientAccess.activeDebugger(modularUI);
        if (debugger != null) {
            debugger.renderHostOverlay(context, guiGraphics, mouseX, mouseY);
        }

        if (ModularUIClientAccess.getScreen(modularUI) instanceof AbstractContainerScreen<?> containerScreen
                && !containerScreen.getMenu().getCarried().isEmpty()) {
            return;
        }

        if (modularUI.isDrawDrag() && modularUI.getDragHandler().isDragging() && modularUI.getDragHandler().dragTexture != null) {
            context.drawTexture(
                    modularUI.getDragHandler().dragTexture,
                    modularUI.lastMouseX + modularUI.getDragHandler().offsetX,
                    modularUI.lastMouseY + modularUI.getDragHandler().offsetY,
                    modularUI.getDragHandler().width,
                    modularUI.getDragHandler().height
            );
        }

        var hoverTooltips = ModularUIClientAccess.getHoverTooltips(modularUI);
        if (modularUI.isDrawTooltips() && !modularUI.getDragHandler().isDragging() && hoverTooltips != null) {
            DrawerHelperClient.drawTooltip(context, hoverTooltips, true, true);
        }
    }

    public void renderUISpacing(GUIContext context, UIElement element, GuiGraphicsExtractor graphics) {
        var transform = element.getLocalToWorldPose();
        context.pose.pushPose();
        context.pose.setIdentity();
        context.pose.mulPose(transform);
        var posX = element.getPositionX();
        var posY = element.getPositionY();
        var sizeX = element.getSizeWidth();
        var sizeY = element.getSizeHeight();
        var marginTop = element.getMarginTop();
        var marginBottom = element.getMarginBottom();
        var marginLeft = element.getMarginLeft();
        var marginRight = element.getMarginRight();
        DrawerHelperClient.drawSolidRect(
                context,
                posX - marginLeft,
                posY - marginTop,
                sizeX + marginLeft + marginRight,
                sizeY + marginTop + marginBottom,
                ColorPattern.T_ORANGE.color
        );
        DrawerHelperClient.drawSolidRect(context, posX, posY, sizeX, sizeY, 0x80ff0000);
        var paddingX = element.getPaddingX();
        var paddingY = element.getPaddingY();
        var paddingWidth = element.getPaddingWidth();
        var paddingHeight = element.getPaddingHeight();
        DrawerHelperClient.drawSolidRect(context, paddingX, paddingY, paddingWidth, paddingHeight, 0x8000ff00);
        var contentX = element.getContentX();
        var contentY = element.getContentY();
        var contentWidth = element.getContentWidth();
        var contentHeight = element.getContentHeight();
        DrawerHelperClient.drawSolidRect(context, contentX, contentY, contentWidth, contentHeight, 0x800000ff);
        context.pose.popPose();
    }
}
