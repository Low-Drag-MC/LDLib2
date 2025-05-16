package com.lowdragmc.lowdraglib.gui.ui;

import com.lowdragmc.lowdraglib.gui.ui.event.DragHandler;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEventDispatcher;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ModularUI implements GuiEventListener, NarratableEntry, Renderable {
    public final UI ui;
    // runtime
    @Getter
    @Nullable
    private Screen screen;
    @Getter
    private int screenWidth, screenHeight;
    @Getter
    private float leftPos, topPos, width, height;
    @Getter
    private final DragHandler dragHandler = new DragHandler();

    // UI state
    @Getter @Setter
    private boolean focused;
    @Nullable
    @Getter
    private UIElement lastHoveredElement;
    @Getter
    private UIElement lastMouseDownElement;
    @Getter
    private UIElement lastMouseMoveElement;
    @Getter
    private UIElement lastMouseClickElement;
    @Getter
    private UIElement lastMouseDragElement;
    @Getter
    private int lastMouseDownButton, lastMouseClickButton;
    @Getter
    private int lastPressedKeyCode, lastPressedScanCode, lastPressedModifiers;
    @Getter
    private long lastMouseClickTime;
    @Getter
    private float lastMouseX, lastMouseY, lastMouseDownX, lastMouseDownY;
    @Getter
    private UIElement focusedElement = null;

    // hover tips
    @Nullable
    private List<Component> tooltipTexts;
    @Nullable
    private TooltipComponent tooltipComponent;
    @Nullable
    private Font tooltipFont;
    private ItemStack tooltipStack = ItemStack.EMPTY;

    public ModularUI(UI ui) {
        this.ui = ui;
    }

    public void setScreen(@Nullable ModularUIContainerScreen<?> screen) {
        this.screen = screen;
    }

    public void init(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        var width = ui.rootElement.getLayout().getWidth();
        var height = ui.rootElement.getLayout().getHeight();
        this.width = switch (width.unit) {
            case PERCENT -> width.value * screenWidth * 0.01f;
            case POINT -> width.value;
            default -> 0;
        };
        this.leftPos = (screenWidth - this.width) / 2;
        this.height = switch (height.unit) {
            case PERCENT -> height.value * screenHeight * 0.01f;
            case POINT -> height.value;
            default -> 0;
        };
        this.topPos = (screenHeight - this.height) / 2;

        ui.rootElement._setModularUIInternal(this);
        ui.rootElement.init(screenWidth, screenHeight);
        ui.rootElement.calculateLayout();
    }

    public void tick() {
        ui.rootElement.screenTick();
    }

    @Override
    public NarrationPriority narrationPriority() {
        if (this.focused) {
            return NarrationPriority.FOCUSED;
        } else {
            return isHovered() ? NarrationPriority.HOVERED : NarrationPriority.NONE;
        }
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {

    }

    public boolean isHovered() {
        return lastHoveredElement != null;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return isHovered();
    }

    @Override
    public ScreenRectangle getRectangle() {
        return new ScreenRectangle((int) leftPos, (int) topPos, (int) width, (int) height);
    }

    /// focus
    /**
     * Request focus to the given element.
     * This will trigger FocusOut event on the old focused element and FocusIn event on the new focused element.
     * @param element the element to focus, or null to clear focus
     */
    public void requestFocus(@Nullable UIElement element) {
        if (focusedElement == element) return;

        if (focusedElement != null) {
            var focusOut = UIEvent.create(UIEvents.FOCUS_OUT);
            focusOut.target = focusedElement;
            focusOut.relatedTarget = element;
            UIEventDispatcher.dispatchEvent(focusOut);
        }

        if (element != null) {
            var focusIn = UIEvent.create(UIEvents.FOCUS_IN);
            focusIn.target = element;
            focusIn.relatedTarget = focusedElement;
            UIEventDispatcher.dispatchEvent(focusIn);
        }

        var lastFocusedElement = focusedElement;
        focusedElement = element;

        if (lastFocusedElement != null) {
            var blur = UIEvent.create(UIEvents.BLUR);
            blur.target = lastFocusedElement;
            blur.relatedTarget = focusedElement;
            blur.hasBubblePhase = false;
            UIEventDispatcher.dispatchEvent(blur);
        }

        if (focusedElement != null) {
            var focus = UIEvent.create(UIEvents.FOCUS);
            focus.target = focusedElement;
            focus.relatedTarget = lastFocusedElement;
            focus.hasBubblePhase = false;
            UIEventDispatcher.dispatchEvent(focus);
            if (screen != null) {
                screen.setFocused(this);
            }
        }

    }

    public void clearFocus() {
        requestFocus(null);
    }

    /// event handling
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        lastMouseDownX = (float) mouseX;
        lastMouseDownY = (float) mouseY;
        lastMouseDownButton = button;
        lastMouseDownElement = getLastHoveredElement();
        if (lastMouseDownElement != null) {
            if (!lastMouseDownElement.isFocusable()) {
                clearFocus();
            } else {
                requestFocus(lastMouseDownElement);
            }
            var event = UIEvent.create(UIEvents.MOUSE_DOWN);
            event.x = (float) mouseX;
            event.y = (float) mouseY;
            event.button = button;
            event.target = lastMouseDownElement;
            UIEventDispatcher.dispatchEvent(event);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        var releasedElement = getLastHoveredElement();
        if (dragHandler.isDragging()) {
            if (releasedElement != null) {
                var event = UIEvent.create(UIEvents.DRAG_PERFORM);
                dispatchDragEvent(mouseX, mouseY, 0, 0, releasedElement, event);
            }
            dragHandler.stopDrag(releasedElement);
        }
        if (releasedElement != null) {
            var event = UIEvent.create(UIEvents.MOUSE_UP);
            event.x = (float) mouseX;
            event.y = (float) mouseY;
            event.button = button;
            event.target = releasedElement;
            UIEventDispatcher.dispatchEvent(event);
            if (releasedElement == lastMouseDownElement) {
                var clickEvent = UIEvent.create(UIEvents.CLICK);
                clickEvent.x = (float) mouseX;
                clickEvent.y = (float) mouseY;
                clickEvent.button = button;
                clickEvent.target = releasedElement;
                UIEventDispatcher. dispatchEvent(clickEvent);
                if (lastMouseClickElement == releasedElement && button == lastMouseClickButton) {
                    if (System.currentTimeMillis() - lastMouseClickTime < 300) { // 300ms follow HTML5 spec
                        var doubleClickEvent = UIEvent.create(UIEvents.DOUBLE_CLICK);
                        doubleClickEvent.x = (float) mouseX;
                        doubleClickEvent.y = (float) mouseY;
                        doubleClickEvent.button = button;
                        doubleClickEvent.target = releasedElement;
                        UIEventDispatcher.dispatchEvent(doubleClickEvent);
                        lastMouseClickElement = null;
                    } else {
                        lastMouseClickElement = releasedElement;
                    }
                } else {
                    lastMouseClickElement = releasedElement;
                }
            }
            lastMouseClickButton = button;
            lastMouseClickTime = System.currentTimeMillis();
            return true;
        }
        lastMouseClickButton = button;
        lastMouseClickTime = System.currentTimeMillis();
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        var current = getLastHoveredElement();
        if (current != null) {
            var event = UIEvent.create(UIEvents.MOUSE_MOVE);
            event.x = (float) mouseX;
            event.y = (float) mouseY;
            event.target = current;
            UIEventDispatcher.dispatchEvent(event);
        }
        if (lastMouseMoveElement == null && current != null) {
            lastMouseMoveElement = current;
            triggerMouseEnter(lastMouseMoveElement, mouseX, mouseY);
        } else if (lastMouseMoveElement != null && current == null) {
            triggerMouseLeave(lastMouseMoveElement, mouseX, mouseY);
            lastMouseMoveElement = null;
        } else if (lastMouseMoveElement != null && lastMouseMoveElement != current) {
            triggerMouseLeave(lastMouseMoveElement, mouseX, mouseY);
            triggerMouseEnter(current, mouseX, mouseY);
            lastMouseMoveElement = current;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        var current = getLastHoveredElement();
        if (current != null) {
            var event = UIEvent.create(UIEvents.MOUSE_WHEEL);
            event.x = (float) mouseX;
            event.y = (float) mouseY;
            event.deltaX = (float) scrollX;
            event.deltaY = (float) scrollY;
            event.target = current;
            UIEventDispatcher.dispatchEvent(event);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragHandler.isDragging()) {
            var current = getLastHoveredElement();
            if (dragHandler.dragSource != null) {
                var event = UIEvent.create(UIEvents.DRAG_SOURCE_UPDATE);
                dispatchDragEvent(mouseX, mouseY, dragX, dragY, dragHandler.dragSource, event);
            }
            if (current != null) {
                if (lastMouseDragElement == current) {
                    var event = UIEvent.create(UIEvents.DRAG_UPDATE);
                    dispatchDragEvent(mouseX, mouseY, dragX, dragY, current, event);
                } else {
                    if (lastMouseDragElement != null) {
                        var event = UIEvent.create(UIEvents.DRAG_LEAVE);
                        event.hasBubblePhase = false;
                        dispatchDragEvent(mouseX, mouseY, dragX, dragY, lastMouseDragElement, event);
                    }
                    lastMouseDragElement = current;
                    var event = UIEvent.create(UIEvents.DRAG_ENTER);
                    event.hasBubblePhase = false;
                    dispatchDragEvent(mouseX, mouseY, dragX, dragY, current, event);
                }
                return true;
            } else if (lastMouseDragElement != null) {
                var event = UIEvent.create(UIEvents.DRAG_LEAVE);
                event.hasBubblePhase = false;
                dispatchDragEvent(mouseX, mouseY, dragX, dragY, lastMouseDragElement, event);
                lastMouseDragElement = null;
                return true;
            }
        }
        return false;
    }

    private void dispatchDragEvent(double mouseX, double mouseY, double dragX, double dragY, UIElement current, UIEvent event) {
        event.x = (float) mouseX;
        event.y = (float) mouseY;
        event.deltaX = (float) dragX;
        event.deltaY = (float) dragY;
        event.dragStartX = lastMouseDownX;
        event.dragStartY = lastMouseDownY;
        event.dragHandler = dragHandler;
        event.target = current;
        UIEventDispatcher.dispatchEvent(event);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        lastPressedKeyCode = keyCode;
        lastPressedScanCode = scanCode;
        lastPressedModifiers = modifiers;
        if (focusedElement != null) {
            var event = UIEvent.create(UIEvents.KEY_DOWN);
            event.keyCode = keyCode;
            event.scanCode = scanCode;
            event.modifiers = modifiers;
            event.target = focusedElement;
            UIEventDispatcher.dispatchEvent(event);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (focusedElement != null) {
            var event = UIEvent.create(UIEvents.KEY_UP);
            event.keyCode = keyCode;
            event.scanCode = scanCode;
            event.modifiers = modifiers;
            event.target = focusedElement;
            UIEventDispatcher.dispatchEvent(event);
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (focusedElement != null) {
            var event = UIEvent.create(UIEvents.CHAR_TYPED);
            event.codePoint = codePoint;
            event.modifiers = modifiers;
            event.target = focusedElement;
            UIEventDispatcher.dispatchEvent(event);
            return true;
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

    public void setHoverTooltip(List<Component> tooltipTexts, ItemStack tooltipStack, @Nullable Font tooltipFont, @Nullable TooltipComponent tooltipComponent) {
        this.tooltipTexts = tooltipTexts;
        this.tooltipStack = tooltipStack;
        this.tooltipFont = tooltipFont;
        this.tooltipComponent = tooltipComponent;
    }

    public void cleanTooltip() {
        tooltipTexts = null;
        tooltipComponent = null;
        tooltipFont = null;
        tooltipStack = ItemStack.EMPTY;
    }

    /// rendering
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (ui.rootElement.layoutNode.isDirty()) {
            ui.rootElement.calculateLayout();
        }

        cleanTooltip();

        lastHoveredElement = ui.rootElement.getHoverElement(mouseX, mouseY);
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        ui.rootElement.drawInBackground(guiGraphics, mouseX, mouseY, partialTick);
        ui.rootElement.drawInForeground(guiGraphics, mouseX, mouseY, partialTick);

        if (lastHoveredElement != null && tooltipTexts == null && !lastHoveredElement.getStyle().tooltips().isEmpty()) {
            setHoverTooltip(lastHoveredElement.getStyle().tooltips(), ItemStack.EMPTY, null, null);
        }

        if (dragHandler.isDragging() && dragHandler.dragTexture != null) {
            dragHandler.dragTexture.draw(guiGraphics, mouseX, mouseY, mouseX - 20, mouseY - 20, 40, 40, partialTick);
        }

        if (!dragHandler.isDragging() && tooltipTexts != null && !tooltipTexts.isEmpty()) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 200);
            DrawerHelper.drawTooltip(guiGraphics, mouseX, mouseY, tooltipTexts, tooltipStack, tooltipComponent, tooltipFont == null ? Minecraft.getInstance().font : tooltipFont);
            guiGraphics.pose().popPose();
        }
    }

    public void renderDebugInfo(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        var x = 2;
        var y = 2;
        var font = Minecraft.getInstance().font;
        var hovered = getLastHoveredElement();
        if (hovered != null) {

            graphics.drawString(font, "hovered element:", x, y, 0xffff0000, true);
            x += 10;
            y += 10;
            for (var info : hovered.getDebugInfo()) {
                graphics.drawString(font, info, x, y, -1, true);
                y += 10;
            }
            x -= 10;

            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);
            // draw overlay
            if (Widget.isShiftDown()) {
                var posX = hovered.getPositionX();
                var posY = hovered.getPositionY();
                var sizeX = hovered.getSizeWidth();
                var sizeY = hovered.getSizeHeight();
                graphics.fill((int) posX, (int) posY, (int) (posX + sizeX), (int) (posY + sizeY), 0x80ff0000);
                var paddingX = hovered.getPaddingX();
                var paddingY = hovered.getPaddingY();
                var paddingWidth = hovered.getPaddingWidth();
                var paddingHeight = hovered.getPaddingHeight();
                graphics.fill((int) paddingX, (int) paddingY, (int) (paddingX + paddingWidth), (int) (paddingY + paddingHeight), 0x8000ff00);
                var contentX = hovered.getContentX();
                var contentY = hovered.getContentY();
                var contentWidth = hovered.getContentWidth();
                var contentHeight = hovered.getContentHeight();
                graphics.fill((int) contentX, (int) contentY, (int) (contentX + contentWidth), (int) (contentY + contentHeight), 0x800000ff);
            }

            // draw layout on the right
            var sw = 250;
            var sh = 250;
            var sx = screenWidth - sw - 2;
            var sy = 12;
            var dist = 25;

            String[] labels = {"margin", "border", "padding", "content"};
            int[] colors = {0x80646669, 0x80ff0000, 0x8000ff00, 0x800000ff};

            drawLayoutBox(graphics, font, sx, sy, sw, sh, dist, labels, colors);
            graphics.pose().popPose();
        }

    }

    private void drawLayoutBox(GuiGraphics graphics, Font font, int x, int y, int width, int height, int dist, String[] labels, int[] colors) {
        for (int i = 0; i < 4; i++) {
            int currentX = x + dist * i;
            int currentY = y + dist * i;
            int currentWidth = width - dist * i * 2;
            int currentHeight = height - dist * i * 2;

            // 绘制当前层边框
            graphics.fill(currentX, currentY, currentX + currentWidth, currentY + currentHeight, colors[i]);

            // 计算当前层的尺寸
            String sizeText = (currentWidth) + " × " + (currentHeight);
            int textWidth = font.width(sizeText);
            int textHeight = font.lineHeight;

            // 计算文字居中位置
            int textX = currentX + (currentWidth - textWidth) / 2;
            int textY = currentY + (currentHeight - textHeight) / 2;

            // 绘制尺寸文字（黑色，带半透明背景）
            graphics.drawString(font, sizeText, textX, textY, 0xFFFFFFFF, false);

            // 绘制标签（如 "margin", "border" 等）
            if (i < labels.length) {
                String label = labels[i];
                int labelWidth = font.width(label);
                int labelX = x + dist * i + 5; // 稍微偏移避免贴边
                int labelY = y + dist * i - textHeight - 2;

                // 确保标签不会超出画布
                if (labelY >= 0) {
                    graphics.fill(labelX - 2, labelY - 2, labelX + labelWidth + 2, labelY + textHeight + 2, 0x80000000);
                    graphics.drawString(font, label, labelX, labelY, 0xFFFFFFFF, false);
                }
            }
        }
    }

}
