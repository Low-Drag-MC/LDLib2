package com.lowdragmc.lowdraglib2.gui.ui;

import com.google.common.collect.ImmutableList;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.event.*;
import com.lowdragmc.lowdraglib2.gui.ui.style.BasicStyle;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib2.gui.widget.Widget;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.*;
import org.appliedenergistics.yoga.config.MutableYogaConfig;
import org.appliedenergistics.yoga.config.YogaConfig;
import org.appliedenergistics.yoga.config.YogaLogger;
import org.appliedenergistics.yoga.numeric.FloatOptional;
import org.joml.Vector4f;
import oshi.util.tuples.Pair;

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
    public static final YogaConfig DEFAULT_YOGA_CONFIG;
    static {
        MutableYogaConfig config = YogaConfig.create(YogaLogger.getDefaultLogger());
        config.setPointScaleFactor(0);
        DEFAULT_YOGA_CONFIG = config;
    }

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
        layoutNode = new YogaNode(DEFAULT_YOGA_CONFIG);
        layoutNode.setContext(this);
    }

    /**
     * Set the Modular UI for this element. In general, this method should only be called automatically.
     * You should not call this method manually.
     */
    protected void _setModularUIInternal(@Nullable ModularUI gui) {
        if (this.modularUI == gui) return;
        this.modularUI = gui;
        for (var child : children) {
            child._setModularUIInternal(gui);
        }
    }

    /**
     * This method is called when the screen is initialized with new width and height.
     */
    public void initScreen(int screenWidth, int screenHeight) {
        positionXCache = FloatOptional.of();
        positionYCache = FloatOptional.of();
        for (var child : children) {
            child.initScreen(screenWidth, screenHeight);
        }
    }

    /**
     * This method is called when the element is removed from the ui structure.
     * You can override this method to do something when the element is removed. e.g. clean up resources, stop animations, etc.
     */
    public void onRemoved() {
        for (var child : children) {
            child.onRemoved();
        }
        if (bubbleListeners.containsKey(UIEvents.REMOVED) || captureListeners.containsKey(UIEvents.REMOVED)) {
            var event = UIEvent.create(UIEvents.REMOVED);
            event.target = this;
            event.hasBubblePhase = false;
            event.hasCapturePhase = false;
            UIEventDispatcher.dispatchEvent(event);
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
        clearLayoutCache();
        if (bubbleListeners.containsKey(UIEvents.LAYOUT_CHANGED) || captureListeners.containsKey(UIEvents.LAYOUT_CHANGED)) {
            var event = UIEvent.create(UIEvents.LAYOUT_CHANGED);
            event.target = this;
            event.hasBubblePhase = false;
            event.hasCapturePhase = false;
            UIEventDispatcher.dispatchEvent(event);
        }
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
     * Clear the layout cache of the element and its children.
     */
    public final void clearLayoutCache() {
        if (!positionXCache.isDefined() && !positionYCache.isDefined()) return;
        positionXCache = FloatOptional.of();
        positionYCache = FloatOptional.of();
        for (var child : children) {
            child.clearLayoutCache();
        }
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

    /**
     * Adapt the position of the element to be within the screen.
     */
    public void adaptPositionToScreen() {
        var mui = getModularUI();
        if (mui != null) {
            // if outside the screen, move it back to the screen
            var screenWidth = mui.getScreenWidth();
            var screenHeight = mui.getScreenHeight();
            var x = getPositionX();
            var y = getPositionY();
            var width = getSizeWidth();
            var height = getSizeHeight();
            // check head out of screen
            if (y < 0) {
                layout(layout -> layout.setPosition(YogaEdge.TOP, getLayoutY() - y));
            } else if (y + height > screenHeight) {
                layout(layout -> layout.setPosition(YogaEdge.TOP, getLayoutY() + screenHeight - (y + height)));
            }
            if (x < 0) {
                layout(layout -> layout.setPosition(YogaEdge.LEFT, getLayoutX() - x));
            } else if (x + width > screenWidth) {
                if (x > width) {
                    // move to the left first
                    layout(layout -> layout.setPosition(YogaEdge.LEFT, 0 - width));
                } else {
                    layout(layout -> layout.setPosition(YogaEdge.LEFT, getLayoutX() + screenWidth - (x + width)));
                }
            }
        }
    }

    /**
     * Set the focus enforcement for the element.
     * This will ensure that the element will own the focus when it's children lose focus.
     * It will lose focus when the element itself loses focus or when the focus is moved to another non child element.
     * @param lostFocusHandler the handler to call when the element loses focus.
     */
    public UIElement setEnforceFocus(Consumer<UIEvent> lostFocusHandler) {
        setFocusable(true);
        addEventListener(UIEvents.BLUR, event -> {
            if (event.relatedTarget != null && this.isAncestorOf(event.relatedTarget)) { // focus on children
                return;
            }

            if (event.target == this) { // lose focus
                if (this.isChildHover()) {
                    this.focus();
                } else {
                    lostFocusHandler.accept(event);
                }
            } else { // child lose focus
                if (event.relatedTarget == null && isChildHover()) {
                    this.focus();
                } else {
                    lostFocusHandler.accept(event);
                }
            }
        }, true);
        return this;
    }

    public void adaptPositionToElement(UIElement element) {
        var elementX = element.getContentX();
        var elementY = element.getContentY();
        var elementWidth = element.getContentWidth();
        var elementHeight = element.getContentHeight();
        var x = getPositionX();
        var y = getPositionY();
        // check head out of parent
        if (y < elementY) {
            layout(layout -> layout.setPosition(YogaEdge.TOP, getLayoutY() - (y - elementY)));
        } else if (y + getSizeHeight() > elementY + elementHeight) {
            layout(layout -> layout.setPosition(YogaEdge.TOP, getLayoutY() + (elementY + elementHeight - (y + getSizeHeight()))));
        }
        if (x < elementX) {
            layout(layout -> layout.setPosition(YogaEdge.LEFT, getLayoutX() - (x - elementX)));
        } else if (x + getSizeWidth() > elementX + elementWidth) {
            layout(layout -> layout.setPosition(YogaEdge.LEFT, getLayoutX() + (elementX + elementWidth - (x + getSizeWidth()))));
        }
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
        child._setModularUIInternal(this.modularUI);
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
        // TODO
        return false;
    }

    public boolean removeChild(@Nullable UIElement child) {
        if (child == null) {
            return false;
        }
        if (!hasChild(child)) {
            return false;
        }
        children.remove(child);
        child.onRemoved();
        child._setModularUIInternal(null);
        layoutNode.removeChildAndInvalidate(child.layoutNode);
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

    public boolean isAncestorOf(@Nullable UIElement element) {
        if (element == null) {
            return false;
        }
        if (element == this) {
            return true;
        }
        var parent = element.getParent();
        while (parent != null) {
            if (parent == this) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
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

    /**
     * Return true if the element is focused by the mouse.
     */
    public boolean isFocused() {
        return getModularUI() != null && getModularUI().getFocusedElement() == this;
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
     * Start dragging the element. This will call the {@link com.lowdragmc.lowdraglib2.gui.ui.event.DragHandler#startDrag} method.
     */
    public DragHandler startDrag(@Nullable Object draggingObject, @Nullable IGuiTexture dragTexture) {
        var ui = getModularUI();
        if (ui != null) {
            ui.getDragHandler().startDrag(draggingObject, dragTexture, this);
            return ui.getDragHandler();
        }
        return new DragHandler();
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
     * @return the element that is hovered and its z-index, or null if no element is hovered
     */
    @Nullable
    public Pair<UIElement, Integer> getHoverElement(double mouseX, double mouseY) {
        if (!isDisplayed() || !isVisible()) return null;

        Pair<UIElement, Integer> hover = null;
        var hidden = layoutNode.getOverflow() == YogaOverflow.HIDDEN || layoutNode.getOverflow() == YogaOverflow.SCROLL;

        if (!hidden || isMouseOverContent(mouseX, mouseY)) {
            for (var child : getSortedChildren()) {
                var result = child.getHoverElement(mouseX, mouseY);
                if (result != null && (hover == null || hover.getB() < result.getB())) {
                    hover = result;
                }
            }
        }

        if (isMouseOver(mouseX, mouseY) && hover == null) {
            return new Pair<>(this, style.zIndex());
        }
        if (hover == null) return null;
        return new Pair<>(hover.getA(), hover.getB() + style.zIndex());
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return isMouseOver(getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), mouseX, mouseY);
    }

    public boolean isMouseOverContent(double mouseX, double mouseY) {
        return isMouseOver(getContentX(), getContentY(), getContentWidth(), getContentHeight(), mouseX, mouseY);
    }

    public static boolean isMouseOver(float x, float y, float width, float height, double mouseX, double mouseY) {
        return mouseX >= x && mouseY >= y && x + width > mouseX && y + height > mouseY;
    }

    /// Logic
    public void screenTick() {
        var safeChildren = new ArrayList<>(children);
        for (var child : safeChildren) {
            if (child.isActive() && child.isDisplayed()) {
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
        if (bubbleListeners.containsKey(UIEvents.TICK) || captureListeners.containsKey(UIEvents.TICK)) {
            var event = UIEvent.create(UIEvents.TICK);
            event.target = this;
            event.hasBubblePhase = false;
            event.hasCapturePhase = false;
            UIEventDispatcher.dispatchEvent(event);
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
            captureListeners.computeIfAbsent(eventType, k -> new ArrayList<>()).addFirst(listener);
        } else {
            bubbleListeners.computeIfAbsent(eventType, k -> new ArrayList<>()).addFirst(listener);
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
            drawContents(guiGraphics, mouseX, mouseY, partialTick);
            drawBackgroundOverlay(guiGraphics, mouseX, mouseY, partialTick);
        } else { // draw contents only
            drawContents(guiGraphics, mouseX, mouseY, partialTick);
        }
        if (zIndex != 0) {
            guiGraphics.pose().popPose();
        }
    }

    /**
     * Renders the background texture of the GUI element.
     */
    public void drawBackgroundTexture(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        var background = style.backgroundTexture();
        if (background != null && background != IGuiTexture.EMPTY) {
            background.draw(graphics, mouseX, mouseY, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), partialTicks);
        }
        var border = style.borderTexture();
        if (border != null && border != IGuiTexture.EMPTY) {
            border.draw(graphics, mouseX, mouseY, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), partialTicks);
        }
    }

    /**
     * Renders the contents of the GUI element. includes additional background and children
     */
    public void drawContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        var hidden = layoutNode.getOverflow() == YogaOverflow.HIDDEN || layoutNode.getOverflow() == YogaOverflow.SCROLL;
        if (hidden) {
            var trans = graphics.pose().last().pose();
            var x = getContentX();
            var y = getContentY();
            var width = getContentWidth();
            var height = getContentHeight();
            var realPos = trans.transform(new Vector4f(x, y, 0, 1));
            var realPos2 = trans.transform(new Vector4f(x + width, y + height, 0, 1));
            graphics.enableScissor((int) realPos.x, (int) realPos.y, (int) realPos2.x, (int) realPos2.y);
        }
        drawBackgroundAdditional(graphics, mouseX, mouseY, partialTicks);
        children.forEach(child -> child.drawInBackground(graphics, mouseX, mouseY, partialTicks));
        if (hidden) {
            graphics.disableScissor();
        }
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
        var overlay = style.overlayTexture();
        if (overlay != null && overlay != IGuiTexture.EMPTY) {
            overlay.draw(graphics, mouseX, mouseY, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), partialTicks);
        }
    }

    /**
     * Renders the graphical user interface (GUI) element in Foreground. In general, this method is used to render the element in the foreground.
     * You can do tooltips here.
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
                getElementName(), getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), children.size())).withColor(0xFFFF00FF));
        info.add(Component.literal("[id: %s, class: \"%s\"]".formatted(getId().isEmpty() ? "empty" : getId(), String.join(" ", classes))).withColor(0xFF00FFFF));
        var path = getStructurePath();
        for (int i = 0; i < path.size(); i++) {
            var element = path.get(i);
            var data =Component.empty();
            for (int i1 = 0; i1 < i; i1++) {
                data = data.append(Component.literal("  "));
            }
            data = data.append("└").append(element.toString());
            info.add(data.withColor(0xFF00FF00));
        }
        return info;
    }
}
