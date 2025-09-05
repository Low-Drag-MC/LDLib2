package com.lowdragmc.lowdraglib2.gui.ui;

import com.google.common.collect.ImmutableList;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.sync.SyncValue;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEvent;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEventBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.event.*;
import com.lowdragmc.lowdraglib2.gui.ui.layout.YogaStyleConfigParser;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.BasicStyle;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib2.gui.widget.Widget;
import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.appliedenergistics.yoga.*;
import org.appliedenergistics.yoga.config.MutableYogaConfig;
import org.appliedenergistics.yoga.config.YogaConfig;
import org.appliedenergistics.yoga.config.YogaLogger;
import org.appliedenergistics.yoga.numeric.FloatOptional;
import oshi.util.tuples.Pair;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The base class for all UI elements.
 * <br>
 * LDLib uses Yoga for layout. please refer to the see <a href="https://www.yogalayout.dev/">Yoga Documentation</a> for more information.
 *
 */
@RemapPrefixForJS("kjs$")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@LDLRegister(name = "element", registry = "ldlib2:ui_element")
public class UIElement implements IConfigurable, ILDLRegister<UIElement, Supplier<UIElement>> {
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
    // style
    @Getter @Setter
    @Accessors(chain = true)
    @Configurable
    private String id = "";
    @Getter
    private final List<String> classes = new ArrayList<>();
    @Getter
    private final StyleContext styleContext = createStyleContext();
    @Getter
    @Configurable(name = "UIElement.basicStyle", subConfigurable = true)
    private final BasicStyle style = new BasicStyle(this);
    // internal properties
    @Getter @Setter
    @Configurable(name = "UIElement.isVisible", tips = "UIElement.isVisible.tips")
    private boolean isVisible = true;
    @Getter @Setter
    @Configurable(name = "UIElement.isActive", tips = "UIElement.isActive.tips")
    private boolean isActive = true;
    @Getter @Setter
    @Configurable(name = "UIElement.focusable", tips = {"UIElement.focusable.tips.0", "UIElement.focusable.tips.1"})
    private boolean focusable = false;
    // event
    private final Map<String, List<UIEventListener>> captureListeners = new HashMap<>();
    private final Map<String, List<UIEventListener>> bubbleListeners = new HashMap<>();
    // sync
    private final List<SyncValue<?>> syncValues = new ArrayList<>();
    private final List<RPCEvent> rpcEvents = new ArrayList<>();
    private final Map<String, Pair<RPCEvent, List<UIEventListener>>> serverCaptureEventListeners = new HashMap<>();
    private final Map<String, Pair<RPCEvent, List<UIEventListener>>> serverBaubleEventListeners = new HashMap<>();
    // runtime
    @Nullable
    private List<UIElement> sortedChildrenCache = null;
    private ImmutableList<UIElement> structurePathCache = null;
    private FloatOptional positionXCache = FloatOptional.of();
    private FloatOptional positionYCache = FloatOptional.of();
    @Getter
    private boolean isInternalUI = false;

    public UIElement() {
        layoutNode = new YogaNode(DEFAULT_YOGA_CONFIG);
        layoutNode.setContext(this);
    }

    /**
     * Set the Modular UI for this element. In general, this method should only be called automatically.
     * You should not call this method manually.
     */
    protected void _setModularUIInternal(@Nullable ModularUI mui) {
        if (this.modularUI == mui) return;
        if (this.modularUI != null) {
            this.modularUI.unregisterElement(this);
            if (this.modularUI.syncManager != null) {
                syncValues.forEach(this.modularUI.syncManager::unregisterSyncValue);
                rpcEvents.forEach(this.modularUI.syncManager::unregisterRPCEvent);
            }
        }
        this.modularUI = mui;
        if (mui != null) {
            mui.registerElement(this);
            if (mui.syncManager != null) {
                syncValues.forEach(mui.syncManager::registerSyncValue);
                rpcEvents.forEach(mui.syncManager::registerRPCEvent);
            }
        }
        for (var child : children) {
            child._setModularUIInternal(mui);
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

    protected void onAdded() {
        for (var child : children) {
            child.onAdded();
        }
        if (bubbleListeners.containsKey(UIEvents.ADDED) || captureListeners.containsKey(UIEvents.ADDED)) {
            var event = UIEvent.create(UIEvents.ADDED);
            event.target = this;
            event.hasBubblePhase = false;
            event.hasCapturePhase = false;
            UIEventDispatcher.dispatchEvent(event, false, false, false);
        }
    }

    /**
     * This method is called when the element is removed from the ui structure.
     * You can override this method to do something when the element is removed. e.g. clean up resources, stop animations, etc.
     */
    protected void onRemoved() {
        for (var child : new ArrayList<>(children)) {
            child.onRemoved();
        }
        if (bubbleListeners.containsKey(UIEvents.REMOVED) || captureListeners.containsKey(UIEvents.REMOVED)) {
            var event = UIEvent.create(UIEvents.REMOVED);
            event.target = this;
            event.hasBubblePhase = false;
            event.hasCapturePhase = false;
            UIEventDispatcher.dispatchEvent(event, false, false, false);
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
            UIEventDispatcher.dispatchEvent(event, false, false, false);
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
                layout(layout -> layout.setPosition(YogaEdge.LEFT, getLayoutX() + screenWidth - (x + width)));
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
    public UIElement selfCall(Consumer<UIElement> consumer) {
        consumer.accept(this);
        return this;
    }

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
        child.onAdded();
        return this;
    }

    public UIElement addChild(@Nullable UIElement child) {
        return addChildAt(child, children.size());
    }

    public UIElement addChildren(UIElement... children) {
        Arrays.stream(children).forEach(this::addChild);
        return this;
    }

    public boolean removeSelf() {
        if (getParent() != null) {
            return getParent().removeChild(this);
        }
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

    public UIElement transform(Consumer<Transform2D> transform) {
        transform.accept(getStyle().transform2D());
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

    /// Interaction
    public boolean isMouseOverElement(double mouseX, double mouseY) {
        return isDisplayed() && isMouseOver(getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), mouseX, mouseY);
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

    public int getSiblingIndex() {
        if (parent == null) return -1;
        return parent.children.indexOf(this);
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
     * Do hit-testing here. Get the element which is hovered by the mouse.
     * @return the element that is hovered and its z-index, or null if no element is hovered
     */
    @Nullable
    public Pair<UIElement, Integer> getHoverElement(double mouseX, double mouseY) {
        if (!isDisplayed() || !isVisible()) return null;

        var transform2D = style.transform2D();
        double[] pt = new double[]{mouseX, mouseY};
        if (!transform2D.isIdentity()) {
            transform2D.inversePoint(this, pt);
        }
        double localMouseX = pt[0];
        double localMouseY = pt[1];

        Pair<UIElement, Integer> hover = null;
        var hidden = layoutNode.getOverflow() == YogaOverflow.HIDDEN || layoutNode.getOverflow() == YogaOverflow.SCROLL;

        if (!hidden || isMouseOverContent(localMouseX, localMouseY)) {
            for (var child : getSortedChildren()) {
                var result = child.getHoverElement(localMouseX, localMouseY);
                if (result != null && (hover == null || hover.getB() < result.getB())) {
                    hover = result;
                }
            }
        }

        if (isMouseOver(localMouseX, localMouseY) && hover == null) {
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
        if (bubbleListeners.containsKey(UIEvents.TICK) || captureListeners.containsKey(UIEvents.TICK)) {
            var event = UIEvent.create(UIEvents.TICK);
            event.target = this;
            event.hasBubblePhase = false;
            event.hasCapturePhase = false;
            UIEventDispatcher.dispatchEvent(event, false, false, false);
        }
    }

    public void serverTick() {
        var safeChildren = new ArrayList<>(children);
        for (var child : safeChildren) {
            if (child.isActive() && child.isDisplayed()) {
                child.serverTick();
            }
        }
        if (serverCaptureEventListeners.containsKey(UIEvents.TICK) || serverBaubleEventListeners.containsKey(UIEvents.TICK)) {
            var tickEvent = UIEvent.create(UIEvents.TICK);
            for (var uiEventListener : serverCaptureEventListeners.get(UIEvents.TICK).getB()) {
                uiEventListener.handleEvent(tickEvent);
                if (tickEvent.immediatePropagationStopped) break;
            }
            for (var uiEventListener : serverBaubleEventListeners.get(UIEvents.TICK).getB()) {
                uiEventListener.handleEvent(tickEvent);
                if (tickEvent.immediatePropagationStopped) break;
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
        return new ArrayList<>(listeners);
    }

    public List<UIEventListener> getBubbleListeners(String eventType) {
        var listeners = bubbleListeners.get(eventType);
        if (listeners == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(listeners);
    }

    /// Sync
    public UIElement addSyncValue(SyncValue<?> syncValue) {
        this.syncValues.add(syncValue);
        var mui = getModularUI();
        if (mui != null && mui.syncManager != null) {
            mui.syncManager.registerSyncValue(syncValue);
        }
        return this;
    }

    public UIElement addSyncValue(Function<UIElement, SyncValue<?>> creator) {
        return addSyncValue(creator.apply(this));
    }

    public UIElement removeSyncValue(SyncValue<?> syncValue) {
        this.syncValues.remove(syncValue);
        var mui = getModularUI();
        if (mui != null && mui.syncManager != null) {
            mui.syncManager.unregisterSyncValue(syncValue);
        }
        return this;
    }

    public UIElement addRPCEvent(RPCEvent event) {
        this.rpcEvents.add(event);
        var mui = getModularUI();
        if (mui != null && mui.syncManager != null) {
            mui.syncManager.registerRPCEvent(event);
        }
        return this;
    }

    public UIElement addRPCEvent(Function<UIElement, RPCEvent> creator) {
        return addRPCEvent(creator.apply(this));
    }

    public UIElement removeRPCEvent(RPCEvent event) {
        this.rpcEvents.remove(event);
        var mui = getModularUI();
        if (mui != null && mui.syncManager != null) {
            mui.syncManager.unregisterRPCEvent(event);
        }
        return this;
    }

    public UIElement addServerEventListener(String eventType, UIEventListener listener) {
        return addEventListener(eventType, listener, false);
    }

    public UIElement addServerEventListener(String eventType, UIEventListener listener, boolean useCapture) {
        var eventListeners = useCapture ? serverCaptureEventListeners : serverBaubleEventListeners;
        eventListeners.computeIfAbsent(eventType, type -> {
            var listeners = new ArrayList<UIEventListener>();
            var rpcEvent = RPCEventBuilder.simple(UIEvent.class, event -> listeners.forEach(e -> e.handleEvent(event)));
            addRPCEvent(rpcEvent);
            return new Pair<>(rpcEvent, listeners);
        }).getB().add(listener);
        return this;
    }

    public UIElement removeServerEventListener(String eventType, UIEventListener listener) {
        return removeServerEventListener(eventType, listener, false);
    }

    public UIElement removeServerEventListener(String eventType, UIEventListener listener, boolean useCapture) {
        var eventListeners = useCapture ? serverCaptureEventListeners : serverBaubleEventListeners;
        var pair = eventListeners.get(eventType);
        if (pair != null) {
            pair.getB().remove(listener);
            if (pair.getB().isEmpty()) {
                eventListeners.remove(eventType);
                removeRPCEvent(pair.getA());
            }
        }
        return this;
    }

    @Nullable
    public RPCEvent getCaptureServerEvent(String eventType) {
        var pair = serverCaptureEventListeners.get(eventType);
        if (pair != null) {
            return pair.getA();
        }
        return null;
    }

    @Nullable
    public RPCEvent getBaubleServerEvent(String eventType) {
        var pair = serverBaubleEventListeners.get(eventType);
        if (pair != null) {
            return pair.getA();
        }
        return null;
    }

    public void sendEvent(RPCEvent event, Object... args) {
        var mui = getModularUI();
        if (mui != null && mui.syncManager != null) {
            mui.syncManager.sendEvent(event, args);
        }
    }

    public <T> void sendEvent(RPCEvent event, Consumer<T> callback, Object... args) {
        var mui = getModularUI();
        if (mui != null && mui.syncManager != null) {
            mui.syncManager.sendEvent(event, callback, args);
        }
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
    public final void drawInBackground(GUIContext guiContext) {
        var display = layoutNode.getDisplay();
        if (display == YogaDisplay.NONE || !isVisible()) {
            return;
        }
        var zIndex = style.zIndex();
        if (zIndex != 0) {
            guiContext.pose.pushPose();
            guiContext.pose.translate(0, 0, zIndex);
        }

        var transform2D = style.transform2D();
        var pushedTransform = !transform2D.isIdentity();
        if (pushedTransform) {
            transform2D.pushToPose(guiContext, this);
        }

        drawInBackgroundInternal(guiContext);

        if (pushedTransform) {
            transform2D.popPose(guiContext);
        }

        if (zIndex != 0) {
            guiContext.pose.popPose();
        }
    }

    public final void drawInBackgroundInternal(GUIContext guiContext) {
        if (layoutNode.getDisplay() == YogaDisplay.FLEX) {
            drawBackgroundTexture(guiContext);
            drawContents(guiContext);
            drawBackgroundOverlay(guiContext);
        } else { // draw contents only
            drawContents(guiContext);
        }
    }

    /**
     * Renders the background texture of the GUI element.
     */
    public void drawBackgroundTexture(GUIContext guiContext) {
        var background = style.backgroundTexture();
        if (background != null && background != IGuiTexture.EMPTY) {
            guiContext.drawTexture(background, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
        }
        var border = style.borderTexture();
        if (border != null && border != IGuiTexture.EMPTY) {
            guiContext.drawTexture(border, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
        }
    }

    /**
     * Renders the contents of the GUI element. includes additional background and children
     */
    public void drawContents(GUIContext guiContext) {
        var hidden = layoutNode.getOverflow() == YogaOverflow.HIDDEN || layoutNode.getOverflow() == YogaOverflow.SCROLL;
        if (hidden) {
            guiContext.enableScissor(getContentX(), getContentY(), getContentWidth(), getContentHeight());
        }
        drawBackgroundAdditional(guiContext);
        children.forEach(child -> child.drawInBackground(guiContext));
        if (hidden) {
            guiContext.disableScissor();
        }
    }

    /**
     * Renders the additional background of the GUI element.
     */
    public void drawBackgroundAdditional(GUIContext guiContext) {

    }

    /**
     * Renders the overlay texture of the GUI element.
     */
    public void drawBackgroundOverlay(GUIContext guiContext) {
        var overlay = style.overlayTexture();
        if (overlay != null && overlay != IGuiTexture.EMPTY) {
            guiContext.drawTexture(overlay, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
        }
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


    /// Editor
    public UIElement markAsInternal() {
        setInternalUI(true);
        return this;
    }

    public void markAllChildrenAsInternal() {
        for (var child : children) {
            child.markAsInternal();
        }
    }

    protected void setInternalUI(boolean isInternal) {
        if (isInternalUI == isInternal) return;
        isInternalUI = isInternal;
        children.forEach(uiElement -> uiElement.setInternalUI(isInternal));
    }

    public Component getEditorName() {
        var name = Component.literal(getElementName());
        if (!id.isEmpty()) {
            name = name.append(Component.literal("#").append(Component.literal(id).withColor(0xFF00FFFF)));
        }
        return name;
    }

    public IGuiTexture getEditorIcon() {
        return Icons.WIDGET_CUSTOM;
    }

    public List<UIElement> getEditorVisibleChildren() {
        return new ArrayList<>(children);
    }

    public void addEditorChild(UIElement child, int index) {
        if (isInternalUI()) return;
        if (index == -1) {
            addChild(child);
        } else {
            addChildAt(child, index);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void buildConfigurator(ConfiguratorGroup father) {
        IConfigurable.super.buildConfigurator(father);
        YogaStyleConfigParser.buildConfigurator(this, father);
    }
}
