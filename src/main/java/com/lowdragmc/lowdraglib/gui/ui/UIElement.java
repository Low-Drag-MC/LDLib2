package com.lowdragmc.lowdraglib.gui.ui;

import com.google.common.collect.ImmutableList;
import com.lowdragmc.lowdraglib.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEventDispatcher;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEventListener;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib.gui.ui.style.BasicStyle;
import com.lowdragmc.lowdraglib.gui.ui.style.StyleContext;
import com.lowdragmc.lowdraglib.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.*;
import org.appliedenergistics.yoga.numeric.FloatOptional;
import org.joml.Vector4f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Consumer;

/**
 * The base class for all UI elements.
 * <br>
 * LDLib uses Yoga for layout. please refer to the see <a href="https://www.yogalayout.dev/">Yoga Documentation</a> for more information.
 *
 */
@RemapPrefixForJS("kjs$")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class UIElement {
    // core ui
    @Getter
    protected final YogaNode layoutNode;
    @Getter
    @Nullable
    private ModularUI modularUI;
    // structure
    @Nullable
    private UIElement parent;
    @Getter
    private final List<UIElement> children = new ArrayList<>();
    @Getter
    private final List<UIElement> waitToRemoved = new ArrayList<>();
    @Getter
    private final List<UIElement> waitToAdded = new ArrayList<>();
    // style
    @Getter @Setter
    @Accessors(chain = true)
    private String id = "";
    @Getter
    private final List<String> classes = new ArrayList<>();
    @Getter
    private final StyleContext styleContext = createStyleContext();
    @Getter
    private final BasicStyle style = new BasicStyle(this);
    // internal properties
    @Getter @Setter
    private boolean isVisible = true;
    @Getter @Setter
    private boolean isActive = true;
    @Getter @Setter
    private boolean focusable = false;
    // event
    private final Map<String, List<UIEventListener>> captureListeners = new HashMap<>();
    private final Map<String, List<UIEventListener>> bubbleListeners = new HashMap<>();
    // runtime
    @Nullable
    private List<UIElement> sortedChildrenCache = null;
    private ImmutableList<UIElement> structurePathCache = null;
    private FloatOptional positionXCache = FloatOptional.of();
    private FloatOptional positionYCache = FloatOptional.of();

    @Getter
    @Configurable(name = "ldlib.gui.editor.name.hover_tips", tips = "ldlib.gui.editor.tips.hover_tips")
    protected final List<Component> tooltipTexts = new ArrayList<>();

    public UIElement() {
        layoutNode = new YogaNode();
        layoutNode.setContext(this);
    }

    /**
     * Set the Modular UI for this element. In general, this method should only be called automatically.
     * You should not call this method manually.
     */
    protected void _setModularUIInternal(ModularUI gui) {
        if (this.modularUI == gui) return;
        this.modularUI = gui;
        for (var child : children) {
            child._setModularUIInternal(gui);
        }
    }

    /// Layout
    public YogaProps getLayout() {
        return layoutNode;
    }

    public UIElement layout(Consumer<YogaProps> layout) {
        layout.accept(layoutNode);
        return this;
    }

    public UIElement node(Consumer<YogaNode> node) {
        node.accept(layoutNode);
        return this;
    }

    public UIElement setDisplay(YogaDisplay display) {
        layoutNode.setDisplay(display);
        return this;
    }

    public UIElement setOverflow(YogaOverflow overflow) {
        layoutNode.setOverflow(overflow);
        return this;
    }

    /**
     * Calculate the layout of the element and its children.
     */
    public void calculateLayout() {
        layoutNode.calculateLayout(YogaConstants.UNDEFINED, YogaConstants.UNDEFINED);
        applyLayout();
    }

    protected void applyLayout() {
        if (!layoutNode.hasNewLayout()) {
            return;
        }
        // Reset the flag
        layoutNode.markLayoutSeen();

        // Do the real work
        onLayoutChanged();

        for (var child : children) {
            child.applyLayout();
        }
    }

    /**
     * This method is called when the layout of the element has changed.
     * You can override this method to do something when the layout changes.
     */
    protected void onLayoutChanged() {
        positionXCache = FloatOptional.of();
        positionYCache = FloatOptional.of();
        for (var child : children) {
            child.positionXCache = FloatOptional.of();
            child.positionYCache = FloatOptional.of();
        }
        var event = UIEvent.create(UIEvents.LAYOUT_CHANGED);
        event.target = this;
        event.hasBubblePhase = false;
        event.hasCapturePhase = false;
        UIEventDispatcher.dispatchEvent(event);
    }

    /**
     * The X offset relative to the border box of the node's parent, along with dimensions, and the resolved values for margin, border, and padding for each physical edge.
     */
    public final float getLayoutX() {
        return parent == null ? modularUI == null ? 0 : modularUI.getLeftPos() : layoutNode.getLayoutX();
    }

    /**
     * The Y offset relative to the border box of the node's parent, along with dimensions, and the resolved values for margin, border, and padding for each physical edge.
     */
    public final float getLayoutY() {
        return parent == null ? modularUI == null ? 0 : modularUI.getTopPos() : layoutNode.getLayoutY();
    }

    /**
     * The absolute X offset relative to the screen.
     */
    public final float getPositionX() {
        if (positionXCache.isUndefined()) {
            positionXCache = FloatOptional.of(getLayoutX() + (parent == null ? 0 : parent.getPositionX()));
        }
        return positionXCache.getValue();
    }

    /**
     * The absolute Y offset relative to the screen.
     */
    public final float getPositionY() {
        if (positionYCache.isUndefined()) {
            positionYCache = FloatOptional.of(getLayoutY() + (parent == null ? 0 : parent.getPositionY()));
        }
        return positionYCache.getValue();
    }

    public final float getSizeWidth() {
        return layoutNode.getLayoutWidth();
    }

    public final float getSizeHeight() {
        return layoutNode.getLayoutHeight();
    }

    /**
     * Get the x position of the element excluding the border.
     */
    public final float getPaddingX() {
        return getPositionX() + layoutNode.getLayoutBorder(YogaEdge.LEFT);
    }

    /**
     * Get the X position of the content area in the element.
     */
    public final float getContentX() {
        return getPaddingX() + layoutNode.getLayoutPadding(YogaEdge.LEFT);
    }

    /**
     * Get the y position of the element excluding the border.
     */
    public final float getPaddingY() {
        return getPositionY() + layoutNode.getLayoutBorder(YogaEdge.TOP);
    }

    /**
     * Get the Y position of the content area in the element.
     */
    public final float getContentY() {
        return getPaddingY() + layoutNode.getLayoutPadding(YogaEdge.TOP);
    }

    public final float getPaddingWidth() {
        return getSizeWidth() - layoutNode.getLayoutBorder(YogaEdge.LEFT) - layoutNode.getLayoutBorder(YogaEdge.RIGHT);
    }

    public final float getContentWidth() {
        return getPaddingWidth() - layoutNode.getLayoutPadding(YogaEdge.LEFT) - layoutNode.getLayoutPadding(YogaEdge.RIGHT);
    }

    public final float getPaddingHeight() {
        return getSizeHeight() - layoutNode.getLayoutBorder(YogaEdge.TOP) - layoutNode.getLayoutBorder(YogaEdge.BOTTOM);
    }

    public final float getContentHeight() {
        return getPaddingHeight() - layoutNode.getLayoutPadding(YogaEdge.TOP) - layoutNode.getLayoutPadding(YogaEdge.BOTTOM);
    }

    /// Structure
    @Nullable
    public UIElement getParent() {
        return parent;
    }

    public boolean hasParent() {
        return parent != null;
    }

    public boolean hasChild(UIElement child) {
        return children.contains(child);
    }

    public UIElement addChildAt(@Nullable UIElement child, int index) {
        if (child == null) {
            return this;
        }
        if (child == this) {
            throw new IllegalArgumentException("Cannot add self as a child");
        }
        if (hasChild(child)) {
            throw new IllegalArgumentException("Cannot add the same child twice");
        }
        if (child.hasParent()) {
            assert child.getParent() != null;
            child.getParent().removeChild(child);
        }
        child.parent = this;
        if (this.modularUI != null) {
            child.modularUI = modularUI;
        }
        children.add(index, child);
        layoutNode.addChildAt(child.layoutNode, index);
        clearSortedChildrenCache();
        child.clearStructurePathCache();
        return this;
    }

    public UIElement addChild(@Nullable UIElement child) {
        return addChildAt(child, children.size());
    }

    public UIElement addChildren(UIElement... children) {
        Arrays.stream(children).forEach(this::addChild);
        return this;
    }

    /**
     * Internal elements are elements that can not be removed.
     */
    public boolean isInternalElement(UIElement child) {
        return false;
    }

    public boolean removeChild(@Nullable UIElement child) {
        if (child == null) {
            return false;
        }
        if (isInternalElement(child) || !hasChild(child)) {
            return false;
        }
        children.remove(child);
        layoutNode.removeChild(child.layoutNode);
        child.parent = null;
        clearSortedChildrenCache();
        child.clearStructurePathCache();
        return true;
    }

    public void clearAllChildren() {
        for (var element : new ArrayList<>(this.children)) {
            removeChild(element);
        }
        synchronized (waitToRemoved) {
            if (!waitToRemoved.isEmpty()) {
                waitToRemoved.clear();
            }
        }
        synchronized (waitToAdded) {
            if (!waitToAdded.isEmpty()) {
                waitToAdded.clear();
            }
        }
    }

    public void init(int screenWidth, int screenHeight) {
        positionXCache = FloatOptional.of();
        positionYCache = FloatOptional.of();
        for (var child : children) {
            child.init(screenWidth, screenHeight);
        }
    }

    /// Style
    public boolean hasClass(String identifier) {
        return classes.contains(identifier);
    }

    public UIElement removeClass(String identifier) {
        if (!classes.contains(identifier)) {
            return this;
        }
        classes.remove(identifier);
        styleContext.loadStyleRules();
        onStyleChanged();
        return this;
    }

    public UIElement addClass(String identifier) {
        if (classes.contains(identifier)) {
            return this;
        }
        classes.add(identifier);
        styleContext.loadStyleRules();
        onStyleChanged();
        return this;
    }

    public String getElementName() {
        // TODO use LDLRegister instead
        return getClass().getSimpleName();
    }

    protected StyleContext createStyleContext() {
        return new StyleContext(this, getInlineStyleValues());
    }

    protected Map<String, StyleValue<?>> getInlineStyleValues() {
        return new HashMap<>();
    }

    public boolean supportStyle(String name) {
        return true;
    }

    /**
     * Apply a style to the element. it will be triggered by the {@link StyleContext}.
     * Apply the actual logic of the style to the element.
     */
    public void applyStyle(Map<String, StyleValue<?>> values) {
        style.applyStyles(values);
    }

    public UIElement style(Consumer<BasicStyle> style) {
        style.accept(this.style);
        onStyleChanged();
        return this;
    }

    /**
     * This method is called when the style of the element has changed.
     * It will only be called when the style is changed by the {@link #style(Consumer)} or {@link #styleContext}.
     */
    protected void onStyleChanged() {
    }

    /// Focus
    public void focus() {
        var ui = getModularUI();
        if (ui != null) {
            ui.requestFocus(this);
        }
    }

    public void blur() {
        var ui = getModularUI();
        if (ui != null && ui.getFocusedElement() == this) {
            ui.clearFocus();
        }
    }

    public boolean hasFocus() {
        var ui = getModularUI();
        return ui != null && ui.getFocusedElement() == this;
    }

    /// Tooltip
    @HideFromJS
    public UIElement setHoverTooltips(String... tooltipText) {
        tooltipTexts.clear();
        appendHoverTooltips(tooltipText);
        return this;
    }

    @HideFromJS
    public UIElement setHoverTooltips(Component... tooltipText) {
        tooltipTexts.clear();
        appendHoverTooltips(tooltipText);
        return this;
    }

    @HideFromJS
    public UIElement setHoverTooltips(List<Component> tooltipText) {
        tooltipTexts.clear();
        appendHoverTooltips(tooltipText);
        return this;
    }

    public UIElement appendHoverTooltips(String... tooltipText) {
        Arrays.stream(tooltipText).filter(Objects::nonNull).filter(s->!s.isEmpty()).map(
                Component::translatable).forEach(tooltipTexts::add);
        return this;
    }

    public UIElement appendHoverTooltips(Component... tooltipText) {
        Arrays.stream(tooltipText).filter(Objects::nonNull).forEach(tooltipTexts::add);
        return this;
    }

    public UIElement appendHoverTooltips(List<Component> tooltipText) {
        tooltipTexts.addAll(tooltipText);
        return this;
    }

    public UIElement kjs$setHoverTooltips(Component... tooltipText) {
        tooltipTexts.clear();
        Arrays.stream(tooltipText).filter(Objects::nonNull).forEach(tooltipTexts::add);
        return this;
    }

    /// Interaction
    public boolean isMouseOverElement(double mouseX, double mouseY) {
        return isMouseOver(getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), mouseX, mouseY);
    }

    /**
     * Return true if the top most element is hovered by the mouse.
     */
    public boolean isHover() {
        return getModularUI() != null && getModularUI().getLastHoveredElement() == this;
    }

    /**
     * Return true if the child element is hovered by the mouse.
     */
    public boolean isChildHover() {
        var hovered = getModularUI() != null ? getModularUI().getLastHoveredElement() : null;
        while (hovered != null) {
            if (hovered == this) {
                return true;
            }
            hovered = hovered.getParent();
        }
        return false;
    }

    /**
     * Return true if the element is focused by the mouse.
     */
    public boolean isFocused() {
        return getModularUI() != null && getModularUI().getFocusedElement() == this;
    }

    /**
     * Start dragging the element. This will call the {@link com.lowdragmc.lowdraglib.gui.ui.event.DragHandler#startDrag} method.
     */
    public void startDrag(@Nullable Object draggingObject, @Nullable IGuiTexture dragTexture) {
        var ui = getModularUI();
        if (ui != null) {
            ui.getDragHandler().startDrag(draggingObject, dragTexture, this);
        }
    }

    /**
     * Get the sorted children of this element. The children are sorted by their zIndex and their order in the structure.
     */
    public List<UIElement> getSortedChildren() {
        if (sortedChildrenCache == null) {
            // sorted by zIndex
            sortedChildrenCache = new ArrayList<>(children);
            sortedChildrenCache.sort((a, b) -> {
                int zCompare = Integer.compare(b.style.zIndex(), a.style.zIndex());
                if (zCompare != 0) return zCompare;
                // if z-index is the same, sort by order in the list
                return children.indexOf(b) - children.indexOf(a);
            });
        }
        return sortedChildrenCache;
    }

    public void clearSortedChildrenCache() {
        sortedChildrenCache = null;
    }

    /**
     * Get the path to the target element. The path is a list of elements from the root to the target element.
     */
    public ImmutableList<UIElement> getStructurePath() {
        if (structurePathCache == null) {
            var builder = ImmutableList.<UIElement>builder();
            if (parent != null) {
                builder.addAll(parent.getStructurePath());
            }
            builder.add(this);
            structurePathCache = builder.build();
        }
        return structurePathCache;
    }

    public void clearStructurePathCache() {
        if (structurePathCache == null) return;
        structurePathCache = null;
        for (var child : children) {
            child.clearStructurePathCache();
        }
    }

    /**
     * Get the element that is hovered by the mouse.
     * @return the element that is hovered, or null if no element is hovered
     */
    @Nullable
    public UIElement getHoverElement(double mouseX, double mouseY) {
        if (!isDisplayed() || !isVisible()) return null;

        UIElement hover = null;
        for (var child : getSortedChildren()) {
            var result = child.getHoverElement(mouseX, mouseY);
            if (result != null && (hover == null || hover.style.zIndex() < result.style.zIndex())) {
                hover = result;
            }
        }

        if (isMouseOver(mouseX, mouseY) && (hover == null || hover.style.zIndex() < style.zIndex())) {
            return this;
        }
        return hover;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return isMouseOver(getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), mouseX, mouseY);
    }

    public static boolean isMouseOver(float x, float y, float width, float height, double mouseX, double mouseY) {
        return mouseX >= x && mouseY >= y && x + width > mouseX && y + height > mouseY;
    }

    /// Logic
    public void screenTick() {
        for (var child : children) {
            if (child.isActive()) {
                child.screenTick();
            }
        }
        synchronized (waitToRemoved) {
            if (!waitToRemoved.isEmpty()) {
                waitToRemoved.forEach(this::removeChild);
                waitToRemoved.clear();
            }
        }
        synchronized (waitToAdded) {
            if (!waitToAdded.isEmpty()) {
                waitToAdded.forEach(this::addChild);
                waitToAdded.clear();
            }
        }
    }

    /// Event
    /**
     * Adds an event listener to the element.
     * @param eventType the type of the event to listen for
     * @param listener the listener to add
     * @param useCapture if true, the listener will be called during the capture phase, otherwise it will be called during the bubble phase
     */
    public UIElement addEventListener(String eventType, UIEventListener listener, boolean useCapture) {
        if (useCapture) {
            captureListeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
        } else {
            bubbleListeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
        }
        return this;
    }

    public UIElement addEventListener(String eventType, UIEventListener listener) {
        return addEventListener(eventType, listener, false);
    }


    /**
     * Block the propagation of the event for the interaction.
     */
    public UIElement stopInteractionEventsPropagation() {
        this.addEventListener(UIEvents.MOUSE_DOWN, UIEvent::stopPropagation);
        this.addEventListener(UIEvents.MOUSE_UP, UIEvent::stopPropagation);
        this.addEventListener(UIEvents.CLICK, UIEvent::stopPropagation);
        this.addEventListener(UIEvents.DOUBLE_CLICK, UIEvent::stopPropagation);
        this.addEventListener(UIEvents.MOUSE_MOVE, UIEvent::stopPropagation);
        this.addEventListener(UIEvents.MOUSE_WHEEL, UIEvent::stopPropagation);
        this.addEventListener(UIEvents.DRAG_UPDATE, UIEvent::stopPropagation);
        this.addEventListener(UIEvents.DRAG_PERFORM, UIEvent::stopPropagation);
        return this;
    }

    /**
     * Removes an event listener from the element.
     * @param eventType the type of the event to stop listening for
     * @param listener the listener to remove
     * @param useCapture if true, the listener was added during the capture phase, otherwise it was added during the bubble phase
     */
    public void removeEventListener(String eventType, UIEventListener listener, boolean useCapture) {
        List<UIEventListener> listeners;
        if (useCapture) {
            listeners = captureListeners.get(eventType);
        } else {
            listeners = bubbleListeners.get(eventType);
        }
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    public void removeEventListener(String eventType, UIEventListener listener) {
        removeEventListener(eventType, listener, false);
    }

    public List<UIEventListener> getCaptureListeners(String eventType) {
        var listeners = captureListeners.get(eventType);
        if (listeners == null) {
            return Collections.emptyList();
        }
        return listeners;
    }

    public List<UIEventListener> getBubbleListeners(String eventType) {
        var listeners = bubbleListeners.get(eventType);
        if (listeners == null) {
            return Collections.emptyList();
        }
        return listeners;
    }

    public static boolean isShiftDown() {
        return Widget.isShiftDown();
    }

    public static boolean isCtrlDown() {
        return Widget.isCtrlDown();
    }

    public static boolean isAltDown() {
        return Widget.isAltDown();
    }

    public static boolean isKeyDown(int keyCode) {
        return Widget.isKeyDown(keyCode);
    }

    public boolean isMouseDown(int button) {
        return getModularUI() != null && getModularUI().getLastMouseDownButton() == button;
    }

    /// Rendering
    public boolean isDisplayed() {
        return layoutNode.getDisplay() != YogaDisplay.NONE;
    }

    /**
     * Renders the graphical user interface (GUI) element in Background.
     * Render phases are:
     * <li> 1. Background
     * <li> 2. Background Additional
     * <li> 3. Overlay
     * <li> 4. Children
     */
    public void drawInBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        var display = layoutNode.getDisplay();
        if (display == YogaDisplay.NONE || !isVisible()) {
            return;
        }
        var zIndex = style.zIndex();
        if (zIndex != 0) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, zIndex);
        }
        if (display == YogaDisplay.FLEX) {
            drawBackgroundTexture(guiGraphics, mouseX, mouseY, partialTick);
            var hidden = layoutNode.getOverflow() == YogaOverflow.HIDDEN;
            if (hidden) {
                var trans = guiGraphics.pose().last().pose();
                var x = getContentX();
                var y = getContentY();
                var width = getContentWidth();
                var height = getContentHeight();
                var realPos = trans.transform(new Vector4f(x, y, 0, 1));
                var realPos2 = trans.transform(new Vector4f(x + width, y + height, 0, 1));
                guiGraphics.enableScissor((int) realPos.x, (int) realPos.y, (int) realPos2.x, (int) realPos2.y);
            }
            drawBackgroundAdditional(guiGraphics, mouseX, mouseY, partialTick);
            if (hidden) {
                guiGraphics.disableScissor();
            }
            drawBackgroundOverlay(guiGraphics, mouseX, mouseY, partialTick);
        }
        children.forEach(child -> child.drawInBackground(guiGraphics, mouseX, mouseY, partialTick));
        if (zIndex != 0) {
            guiGraphics.pose().popPose();
        }
    }

    /**
     * Renders the background texture of the GUI element.
     */
    public void drawBackgroundTexture(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        style.backgroundTexture().draw(graphics, mouseX, mouseY, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), partialTicks);
        style.borderTexture().draw(graphics, mouseX, mouseY, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), partialTicks);
    }

    /**
     * Renders the additional background of the GUI element.
     */
    public void drawBackgroundAdditional(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {

    }

    /**
     * Renders the overlay texture of the GUI element.
     */
    public void drawBackgroundOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {

    }

    /**
     * Renders the graphical user interface (GUI) element in Foreground.
     */
    public void drawInForeground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
    }

    @Override
    public String toString() {
        return getElementName() + "{" + id + "}";
    }

    public List<Component> getDebugInfo() {
        var info = new ArrayList<Component>();
        info.add(Component.literal("[type: %s, pos: (%.1f %.1f), size: (%.1f, %.1f), children: %d]".formatted(
                getElementName(), getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), children.size())));
        info.add(Component.literal("[id: %s, class: \"%s\"]".formatted(getId().isEmpty() ? "empty" : getId(), String.join(" ", classes))));
        return info;
    }
}
