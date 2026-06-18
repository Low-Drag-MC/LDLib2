package com.lowdragmc.lowdraglib2.gui.ui;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.*;
import com.lowdragmc.lowdraglib2.gui.editor.view.UIHierarchy;
import com.lowdragmc.lowdraglib2.gui.sync.SyncValue;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEmitter;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEvent;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEventBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.event.*;
import com.lowdragmc.lowdraglib2.gui.ui.layout.TaffyLayoutStyle;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.UIVisualLayer;
import com.lowdragmc.lowdraglib2.gui.ui.style.*;
import com.lowdragmc.lowdraglib2.gui.ui.style.animation.StyleAnimation;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.math.Rect;
import com.lowdragmc.lowdraglib2.registry.AutoRegistry;
import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.SkipPersistedValue;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.lowdragmc.lowdraglib2.utils.TagBuilder;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import dev.vfyjxf.taffy.style.*;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.appliedenergistics.yoga.*;
import org.appliedenergistics.yoga.numeric.FloatOptional;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import oshi.util.tuples.Pair;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * The base class for all UI elements.
 * <br>
 * LDLib uses Taffy for layout.
 * please refer to the see <a href="https://github.com/vfyjxf/taffy-java">Taffy Documentation</a> for more information.
 *
 */
@RemapPrefixForJS("kjs$")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@KJSBindings
@LDLRegister(name = "element", registry = "ldlib2:ui_element", priority = -1)
public class UIElement implements IConfigurable, IPersistedSerializable, ILDLRegister<UIElement, Supplier<UIElement>> {
    public static Codec<UIElement> CODEC = LDLib2Registries.UI_ELEMENTS.optionalCodec().dispatch(ILDLRegister::getRegistryHolderOptional,
            optional -> optional.map(holder ->
                            PersistedParser.createCodec(holder.value()).optionalFieldOf("data").xmap(
                                    opt -> opt.orElseGet(holder.value()),
                                    Optional::ofNullable
                            ))
                    .orElseGet(() -> MapCodec.unit(UIElement::new)));

    public static final Layout EMPTY_LAYOUT = new Layout();

    // core ui
    @Getter
    protected final TaffyLayoutStyle taffyStyle;
    @Getter
    @Nullable
    protected NodeId nodeId;
    @Getter
    @Nullable
    private ModularUI modularUI;
    // structure
    @Nullable
    private UIElement parent;
    private final ObjectArrayList<UIElement> children = new ObjectArrayList<>();
    // style
    @Getter
    @Accessors(chain = true)
    @Configurable
    private String id = "";
    @Getter
    private final Set<String> classes = new LinkedHashSet<>();
    @Getter
    private final StyleBag styleBag = new StyleBag(this);
    @Getter
    private final ObjectArrayList<Style> styles = new ObjectArrayList<>();
    private final ObjectArrayList<Stylesheet> localStylesheets = new ObjectArrayList<>();
    @Getter
    private final LayoutStyle layoutStyle = new LayoutStyle(this);
    @Getter
    private final BasicStyle style = new BasicStyle(this);
    // internal properties
    @Getter @Setter @Accessors(chain = true)
    @Configurable(name = "UIElement.isVisible", tips = "UIElement.isVisible.tips")
    private boolean isVisible = true;
    @Getter @Accessors(chain = true)
    @Configurable(name = "UIElement.isActive", tips = "UIElement.isActive.tips")
    private boolean isActive = true;
    @Getter @Setter @Accessors(chain = true)
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
    private final Map<String, List<Consumer<CompoundTag>>> messageHandlers = new HashMap<>();
    @Nullable
    private RPCEvent messageRPC;
    // runtime
    private final Supplier<String> elementName = Suppliers.memoize(() -> {
        var name = name();
        return name.isEmpty() ? "Unknown" : name;
    });
    @Nullable
    private UIElement[] sortedChildrenCache = null;
    private ImmutableList<UIElement> structurePathCache = null;
    private FloatOptional positionXCache = FloatOptional.of();
    private FloatOptional positionYCache = FloatOptional.of();
    @Nullable
    private Matrix4f localToWorldCache = null;
    @Nullable
    private Matrix4f worldToLocalCache = null;
    @Getter
    private boolean isCulled;
    @Getter
    private boolean isInternalUI = false;
    @Getter @Setter @Accessors(chain = true)
    private boolean allowHitTest = true;
    @Nullable
    private UIVisualLayer visualLayer;

    public UIElement() {
        taffyStyle = new TaffyLayoutStyle(this);
    }

    @OnlyIn(Dist.CLIENT)
    private UIVisualLayer getOrCreateVisualLayer() {
        if (visualLayer == null) {
            visualLayer = new UIVisualLayer(this);
        }
        return visualLayer;
    }

    /**
     * Set the Modular UI for this element. In general, this method should only be called automatically.
     * You should not call this method manually.
     */
    protected final void _setModularUIInternal(@Nullable ModularUI mui) {
        var previous = modularUI;
        if (this.modularUI != mui) {
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
        }
        // always notify mui changes for menu and screen changes
        if (bubbleListeners.containsKey(UIEvents.MUI_CHANGED) || captureListeners.containsKey(UIEvents.MUI_CHANGED)) {
            var event = UIEvent.create(UIEvents.MUI_CHANGED);
            event.target = this;
            event.hasBubblePhase = false;
            event.hasCapturePhase = false;
            event.customData = previous;
            UIEventDispatcher.dispatchEvent(event, false, false, false);
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

    /**
     * Handles logic to be executed when this object is added to a parent or container.
     */
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
        for (var child : getSafeChildren()) {
            child.onRemoved();
        }
        if (visualLayer != null) {
            visualLayer.release();
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
    public LayoutStyle getLayout() {
        return layoutStyle;
    }

    public UIElement layout(Consumer<LayoutStyle> layout) {
        if (LDLib2.isServer()) return this;
        layout.accept(layoutStyle);
        return this;
    }

    @Nullable
    public TaffyTree getTaffyTree() {
        return modularUI == null ? null : modularUI.getTaffyTree();
    }

    public Layout getTaffyLayout() {
        var taffyTree = getTaffyTree();
        if (taffyTree != null) {
            return taffyTree.getLayout(nodeId);
        }
        return EMPTY_LAYOUT;
    }

    public void markTaffyStyleDirty() {
        var taffyTree = getTaffyTree();
        if (taffyTree != null) {
            taffyTree.markDirty(nodeId);
        }
    }

    @Deprecated(since = "26.1")
    public UIElement setDisplay(YogaDisplay display) {
        layoutStyle.display(display);
        return this;
    }

    public UIElement setDisplay(TaffyDisplay display) {
        layoutStyle.display(display);
        return this;
    }

    public UIElement setDisplay(boolean display) {
        layoutStyle.display(display ? TaffyDisplay.FLEX : TaffyDisplay.NONE);
        return this;
    }

    @Deprecated(since = "26.1")
    public UIElement setOverflow(YogaOverflow overflow) {
        layoutStyle.setOverflow(overflow);
        return this;
    }

    public UIElement setOverflowVisible(boolean overflow) {
        style.overflowVisible(overflow);
        return this;
    }

    /**
     * Called when the layout's geometry or bounds have changed.
     * This method notifies that there has been a change in the layout,
     * such as alterations in size, position, or other geometrical properties.
     *
     * @param hasGeometryChanged {@code true} if the geometry of the layout has changed,
     *                           {@code false} otherwise.
     */
    protected void onLayoutChanged(boolean hasGeometryChanged) {
        if (hasGeometryChanged) {
            onLayoutChanged();
        }
    }

    /**
     * This method is called when the layout of the element has changed.
     * You can override this method to do something when the layout changes.
     * It will be called if an only if the geometry of the layout has changed.
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
     * Clear the layout cache of the element and its children.
     */
    public final void clearLayoutCache() {
        clearPoseCache();
        if (!positionXCache.isDefined() && !positionYCache.isDefined()) return;
        positionXCache = FloatOptional.of();
        positionYCache = FloatOptional.of();
        for (var child : children) {
            child.clearLayoutCache();
        }
    }

    /**
     * Clear the pose cache of the element and its children.
     */
    public final void clearPoseCache() {
        if (localToWorldCache == null && worldToLocalCache == null) return;
        localToWorldCache = null;
        worldToLocalCache = null;
        for (var child : children) {
            child.clearPoseCache();
        }
    }

    /**
     * Retrieves the current pose matrix. The pose can transfer points from the local coordinate system to the world coordinate system.
     *
     * @return The current pose matrix as a {@link Matrix4f}.
     */
    public final Matrix4f getLocalToWorldPose() {
        if (localToWorldCache == null) {
            return computeLocalToWorldPose();
        }
        return localToWorldCache;
    }

    protected final Matrix4f computeLocalToWorldPose() {
        if (localToWorldCache != null) return localToWorldCache;
        var parent = getParent();
        if (parent == null) {
            if (modularUI == null) return new Matrix4f();
            return modularUI.getLastDrawPose();
        }
        var matrix = new Matrix4f(parent.getLocalToWorldPose());
        var transform2D = style.transform2D();
        var pushedTransform = !transform2D.isIdentity();
        if (pushedTransform) {
            transform2D.pushPose(matrix, this);
        }
        return matrix;
    }

    /**
     * Retrieves the current pose matrix. The pose can transfer points from the world coordinate system to the local coordinate system.
     *
     * @return The current pose matrix as a {@link Matrix4f}.
     */
    public final Matrix4f getWorldToLocalPose() {
        if (worldToLocalCache == null) {
            if (localToWorldCache == null) {
                return computeLocalToWorldPose().invert(new Matrix4f());
            }
            worldToLocalCache = localToWorldCache.invert(new Matrix4f());
        }
        return worldToLocalCache;
    }

    /**
     * The X offset relative to the border box of the node's parent, along with dimensions, and the resolved values for margin, border, and padding for each physical edge.
     */
    public final float getLayoutX() {
        return (parent == null ? modularUI == null ? 0 : modularUI.getLeftPos() : getTaffyLayout().location().x);
    }

    /**
     * The Y offset relative to the border box of the node's parent, along with dimensions, and the resolved values for margin, border, and padding for each physical edge.
     */
    public final float getLayoutY() {
        return (parent == null ? modularUI == null ? 0 : modularUI.getTopPos() : getTaffyLayout().location().y);
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
        return getTaffyLayout().size().width;
    }

    public final float getSizeHeight() {
        return getTaffyLayout().size().height;
    }

    public final float getMarginTop() {
        return getTaffyLayout().margin().top;
    }

    public final float getMarginBottom() {
        return getTaffyLayout().margin().bottom;
    }

    public final float getMarginLeft() {
        return getTaffyLayout().margin().left;
    }

    public final float getMarginRight() {
        return getTaffyLayout().margin().right;
    }

    /**
     * Get the x position of the element excluding the border.
     */
    public final float getPaddingX() {
        return (getPositionX() + getTaffyLayout().border().left);
    }

    /**
     * Get the X position of the content area in the element.
     */
    public final float getContentX() {
        return (getPaddingX() + getTaffyLayout().padding().left);
    }

    /**
     * Get the y position of the element excluding the border.
     */
    public final float getPaddingY() {
        return (getPositionY() + getTaffyLayout().border().top);
    }

    /**
     * Get the Y position of the content area in the element.
     */
    public final float getContentY() {
        return (getPaddingY() + getTaffyLayout().padding().top);
    }

    public final float getPaddingWidth() {
        return (getSizeWidth() - getTaffyLayout().border().left - getTaffyLayout().border().right);
    }

    public final float getContentWidth() {
        return getTaffyLayout().contentBoxWidth();
    }

    public final float getPaddingHeight() {
        return (getSizeHeight() - getTaffyLayout().border().top - getTaffyLayout().border().bottom);
    }

    public final float getContentHeight() {
        return getTaffyLayout().contentBoxHeight();
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
                layout(layout -> layout.top(getLayoutY() - y));
            } else if (y + height > screenHeight) {
                layout(layout -> layout.top(getLayoutY() + screenHeight - (y + height)));
            }
            if (x < 0) {
                layout(layout -> layout.left(getLayoutX() - x));
            } else if (x + width > screenWidth) {
                layout(layout -> layout.left(getLayoutX() + screenWidth - (x + width)));
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void appendExtraAreas(List<Rect2i> extraAreas) {
        if (!isDisplayed() || !isVisible()) return;
        var rect = new Rect2i(Math.round(getPositionX()), Math.round(getPositionY()),
                Math.round(getSizeWidth()), Math.round(getSizeHeight()));
        var contains = false;
        for (var extraArea : extraAreas) {
            if (extraArea.getX() <= rect.getX() &&
                    extraArea.getY() <= rect.getY() &&
                    extraArea.getX() + extraArea.getWidth() >= rect.getX() + rect.getWidth() &&
                    extraArea.getY() + extraArea.getHeight() >= rect.getY() + rect.getHeight()) {
                contains = true;
                break;
            }
        }
        if (!contains) {
            extraAreas.add(rect);
        }
        for (UIElement child : getChildren()) {
            child.appendExtraAreas(extraAreas);
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
                if (event.relatedTarget != null && !this.isAncestorOf(event.relatedTarget)) {
                    lostFocusHandler.accept(event);
                    return;
                }
                if (this.isSelfOrChildHover()) {
                    this.focus();
                } else {
                    lostFocusHandler.accept(event);
                }
            } else { // child lose focus
                if (event.relatedTarget == null && isSelfOrChildHover()) {
                    this.focus();
                } else {
                    lostFocusHandler.accept(event);
                }
            }
        }, true);
        return this;
    }

    /**
     * Adjusts the position of the current element to ensure it fits within the boundaries
     * of the specified {@link UIElement}. This method will check if the current element's
     * position exceeds the bounds of the given element and repositions it accordingly.
     *
     * @param element The {@link UIElement} to which the position of the current element
     *                should be adapted. This acts as the boundary for adjustment.
     */
    public void adaptPositionToElement(UIElement element) {
        var elementX = element.getContentX();
        var elementY = element.getContentY();
        var elementWidth = element.getContentWidth();
        var elementHeight = element.getContentHeight();
        var x = getPositionX();
        var y = getPositionY();
        // check head out of parent
        if (y < elementY) {
            layout(layout -> layout.top(getLayoutY() - (y - elementY)));
        } else if (y + getSizeHeight() > elementY + elementHeight) {
            layout(layout -> layout.top(getLayoutY() + (elementY + elementHeight - (y + getSizeHeight()))));
        }
        if (x < elementX) {
            layout(layout -> layout.left(getLayoutX() - (x - elementX)));
        } else if (x + getSizeWidth() > elementX + elementWidth) {
            layout(layout -> layout.left(getLayoutX() + (elementX + elementWidth - (x + getSizeWidth()))));
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

    @Nullable
    public <T extends UIElement> T getFirstAncestorOfType(Class<T> type) {
        if (type.isAssignableFrom(this.getClass())) return type.cast(this);
        if (parent == null) return null;
        return parent.getFirstAncestorOfType(type);
    }
    
    public List<UIElement> getChildren() {
        return Collections.unmodifiableList(children);
    }
    
    public List<UIElement> getSafeChildren() {
        return List.copyOf(children);
    }

    public Stream<UIElement> selfAndAllChildren() {
        return Stream.concat(
                Stream.of(this),
                children.stream().flatMap(UIElement::selfAndAllChildren)
        );
    }

    public Stream<UIElement> allChildrenStream() {
        return children.stream().flatMap(UIElement::selfAndAllChildren);
    }

    /**
     * Selects and retrieves a stream of {@link UIElement} objects that match the specified CSS-like selector.
     */
    public Stream<UIElement> select(String selector) {
        var match = HierarchicalStyleMatcher.parse(selector);
        return selfAndAllChildren().filter(match::matches);
    }

    public <T> Stream<T> select(String selector, Class<T> type) {
        return select(selector).filter(type::isInstance).map(type::cast);
    }

    /**
     * Selects and retrieves a stream of {@link UIElement} objects whose IDs match the specified regular expression.
     */
    public Stream<UIElement> selectRegex(String regex) {
        var pattern = java.util.regex.Pattern.compile(regex);
        return selfAndAllChildren().filter(element -> pattern.matcher(element.getId()).find());
    }

    public <T> Stream<T> selectRegex(String regex, Class<T> type) {
        return selectRegex(regex).filter(type::isInstance).map(type::cast);
    }

    /**
     * Selects and retrieves a stream of {@link UIElement} objects whose IDs match the specified string.
     */
    public Stream<UIElement> selectId(String id) {
        return selfAndAllChildren().filter(element -> id.equals(element.getId()));
    }

    public <T> Stream<T> selectId(String id, Class<T> type) {
        return selectId(id).filter(type::isInstance).map(type::cast);
    }

    public final List<UIElement> getFlattenChildren() {
        var list = new ArrayList<UIElement>();
        for (var child : children) {
            list.add(child);
            list.addAll(child.getFlattenChildren());
        }
        return Collections.unmodifiableList(list);
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
        children.add(index, child);
        child._setModularUIInternal(this.modularUI);
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

    /**
     * Attempts to remove this object from its parent's list of children.
     * The removal is performed by calling the {@code removeChild} method on the parent.
     * If the object has no parent, no action is taken.
     * <br>
     * It will be triggered while it was removed from {@link UIHierarchy} as well.
     *
     * @return {@code true} if the object was successfully removed from its parent;
     *         {@code false} if the object has no parent or the removal failed.
     */
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
        child.parent = null;
        clearSortedChildrenCache();
        child.clearStructurePathCache();
        return true;
    }

    public void clearAllChildren() {
        for (var element : getSafeChildren()) {
            removeChild(element);
        }
    }

    public void clearAllExternalChildren() {
        for (var child : getSafeChildren()) {
            if (child.isInternalUI) {
                child.clearAllExternalChildren();
            } else {
                removeChild(child);
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
        return element.getStructurePath().contains(this);
    }

    @ConfigSetter(field = "id")
    public UIElement setId(String id) {
        this.id = id;
        onClassIdChanged();
        return this;
    }

    @ConfigSetter(field = "isActive")
    public UIElement setActive(boolean active) {
        isActive = active;
        if (isActive) {
            removeClass("__disabled__");
        } else {
            addClass("__disabled__");
        }
        return this;
    }

    public UIElement disabled() {
        return setActive(false);
    }

    /// Style
    public boolean hasClass(String clazz) {
        return classes.contains(clazz);
    }

    public final UIElement removeClass(String clazz) {
        if (classes.remove(clazz)) {
            onClassIdChanged();
        }
        return this;
    }

    public final UIElement removeClasses(String... classes) {
        var doRemoved = false;
        for (String clazz : classes) {
            if (this.classes.remove(clazz)) {
                doRemoved = true;
            }
        }
        if (doRemoved) {
            onClassIdChanged();
        }
        return this;
    }

    public final UIElement addClass(String clazz) {
        if (classes.add(clazz)) {
            onClassIdChanged();
        }
        return this;
    }

    public final UIElement addClasses(String... classes) {
        var doAdded = false;
        for (String clazz : classes) {
            if (this.classes.add(clazz)) {
                doAdded = true;
            }
        }
        if (doAdded) {
            onClassIdChanged();
        }
        return this;
    }

    public final UIElement setClasses(String... classes) {
        this.classes.clear();
        this.classes.addAll(Arrays.asList(classes));
        onClassIdChanged();
        return this;
    }

    public final UIElement moveInlineAsDefault() {
        styleBag.moveInlineAsDefault();
        return this;
    }

    @Override
    public String name() {
        return isLDLRegister() ? ILDLRegister.super.name() : getClass().getSimpleName();
    }

    @Override
    public AutoRegistry.LDLibRegister<UIElement, Supplier<UIElement>> getRegistry() {
        return LDLib2Registries.UI_ELEMENTS;
    }

    public final String getElementName() {
        return elementName.get();
    }

    protected final void _addStyleInternal(Style style) {
        this.styles.add(style);
    }

    public void onClassIdChanged() {
        var mui = getModularUI();
        if (mui != null) {
            mui.getStyleEngine().scheduleReloadElementStyles(this);
        }
    }

    public void addStyleRules(List<StyleRule> rules) {
        for (var rule : rules) {
            styleBag.putCandidates(
                    rule.properties,
                    StyleOrigin.STYLESHEET,
                    rule.getSpecificity(),
                    rule.sourceOrder
            );
        }
    }

    public void removeStyleRules(List<StyleRule> rules) {
        for (StyleRule rule : rules) {
            styleBag.removeCandidates(slot ->
                    slot.origin() == StyleOrigin.STYLESHEET && slot.sourceOrder() == rule.sourceOrder);
        }
    }

    public void removeAllRules() {
        styleBag.removeCandidates(slot -> slot.origin() == StyleOrigin.STYLESHEET);
    }

    // region Local Stylesheets

    /**
     * Returns an unmodifiable view of the local stylesheets attached to this element.
     * Local stylesheets only affect this element and its descendants.
     */
    public List<Stylesheet> getLocalStylesheets() {
        return ObjectLists.unmodifiable(localStylesheets);
    }

    /**
     * Attach a local stylesheet to this element.
     * The stylesheet will only apply to this element and its descendants.
     *
     * @param stylesheet the stylesheet to attach
     * @return this element for chaining
     */
    public UIElement addLocalStylesheet(Stylesheet stylesheet) {
        if (!localStylesheets.contains(stylesheet)) {
            localStylesheets.add(stylesheet);
            if (!LDLib2.isServer()) {
                var mui = getModularUI();
                if (mui != null) {
                    mui.getStyleEngine().addLocalStylesheet(this, stylesheet);
                }
            }
        }
        return this;
    }

    /**
     * Parse and attach a local stylesheet from a CSS string.
     * The stylesheet will only apply to this element and its descendants.
     *
     * @param lss the LSS string to parse
     * @return this element for chaining
     */
    public UIElement addLocalStylesheet(String lss) {
        return addLocalStylesheet(Stylesheet.parse(lss));
    }

    /**
     * Remove a previously attached local stylesheet from this element.
     *
     * @param stylesheet the stylesheet to remove
     * @return this element for chaining
     */
    public UIElement removeLocalStylesheet(Stylesheet stylesheet) {
        if (localStylesheets.remove(stylesheet)) {
            if (!LDLib2.isServer()) {
                var mui = getModularUI();
                if (mui != null) {
                    mui.getStyleEngine().removeLocalStylesheet(this, stylesheet);
                }
            }
        }
        return this;
    }

    /**
     * Remove all local stylesheets from this element.
     *
     * @return this element for chaining
     */
    public UIElement clearLocalStylesheets() {
        if (!localStylesheets.isEmpty()) {
            final var sheets = localStylesheets.toArray(Stylesheet[]::new);
            localStylesheets.clear();
            if (!LDLib2.isServer()) {
                var mui = getModularUI();
                if (mui != null) {
                    for (var sheet : sheets) {
                        mui.getStyleEngine().removeLocalStylesheet(this, sheet);
                    }
                }
            }
        }
        return this;
    }

    // endregion

    /**
     * This method is called when the style of the element has changed.
     */
    public void onStyleChanged() {
        if (bubbleListeners.containsKey(UIEvents.STYLE_CHANGED) || captureListeners.containsKey(UIEvents.STYLE_CHANGED)) {
            var event = UIEvent.create(UIEvents.STYLE_CHANGED);
            event.target = this;
            event.hasBubblePhase = false;
            event.hasCapturePhase = false;
            UIEventDispatcher.dispatchEvent(event, false, false, false);
        }
    }

    public UIElement style(Consumer<BasicStyle> style) {
        if (LDLib2.isServer()) return this;
        style.accept(this.style);
        return this;
    }

    /**
     * Updates the style properties of the {@code UIElement} based on the
     * given property name and its corresponding raw value. If {@code rawValue} is
     * {@code null}, the method removes the associated style property from the
     * stylesheet. Otherwise, it sets or updates the property value.
     *
     * @param propertyName the name of the property to be updated; this must
     *                     correspond to a valid property in {@link PropertyRegistry}.
     * @param rawValue     the raw string value to be parsed and applied to the
     *                     specified property; if {@code null}, the property will
     *                     be removed from the stylesheet.
     * @param origin       the origin of the style property.
     * @return this {@code UIElement} instance for method chaining.
     */
    public UIElement lss(String propertyName, @Nullable Object rawValue, StyleOrigin origin) {
        var p = PropertyRegistry.byName(propertyName);
        if (p == null) {
            return this;
        }
        if (rawValue == null) {
            styleBag.removeCandidates(p, slot ->
                    slot.property() == p &&
                    slot.origin() == origin &&
                    slot.specificity() == 999 &&
                    slot.sourceOrder() == 999);
        } else {
            var value = p.valueParser.parse(rawValue.toString());
            styleBag.replaceOrPutCandidate(p, StyleSlot.of(p, origin, 999, 999, value.compute()));
        }
        return this;
    }

    public UIElement lss(String propertyName, @Nullable Object rawValue) {
        return lss(propertyName, rawValue, StyleOrigin.STYLESHEET);
    }

    public UIElement transform(Consumer<Transform2D> transform) {
        var t = style.transform2D().copy();
        transform.accept(t);
        style.transform2D(t);
        return this;
    }

    /**
     * Creates and returns a {@link StyleAnimation} object based on the current modular UI context.
     * This method selects the current instance as the target for the returned animation.
     * mui may not valid yet
     *
     * @return a {@link StyleAnimation} instance configured with the modular UI context and targeting this instance.
     */
    public StyleAnimation animation() {
        return StyleAnimation.of(getModularUI()).select(this);
    }

    public UIElement animation(Consumer<StyleAnimation> animation) {
        if (this.modularUI == null) {
            addEventListener(UIEvents.MUI_CHANGED, e -> {
                if (this.modularUI != null) {
                    animation.accept(this.animation());
                    removeEventListener(UIEvents.MUI_CHANGED, e.currentListener);
                }
            });
        } else {
            animation.accept(this.animation());
        }
        return this;
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

    public boolean isChildFocused() {
        var mui = getModularUI();
        return mui != null && mui.getFocusedElement() != null && this.isAncestorOf(mui.getFocusedElement());
    }

    /// Interaction
    public boolean isMouseOverElement(float mouseX, float mouseY) {
        return isDisplayed() &&
                isSelfOrChildHover() &&
                isMouseOver(getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), mouseX, mouseY);
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
    public boolean isSelfOrChildHover() {
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
        return new DragHandler(null);
    }

    /**
     * Get the sorted children of this element. The children are sorted by their zIndex and their order in the structure.
     */
    public List<UIElement> getSortedChildren() {
        // to keep compatibility
        return Arrays.asList(getSafeSortedChildren());
    }

    public UIElement[] getSafeSortedChildren() {
        if (sortedChildrenCache == null) {
            int n = children.size();
            var indexArrayBuffer = new int[n];

            for (int i = 0; i < n; i++) {
                indexArrayBuffer[i] = i;
            }

            IntArrays.quickSort(indexArrayBuffer, 0, n, (a, b) -> {
                int zA = children.get(a).style.zIndex();
                int zB = children.get(b).style.zIndex();

                if (zA != zB) {
                    return Integer.compare(zB, zA);
                }
                return Integer.compare(b, a);
            });

            sortedChildrenCache = new UIElement[n];
            for (int i = 0; i < n; i++) {
                sortedChildrenCache[i] = children.get(indexArrayBuffer[i]);
            }
        }

        return sortedChildrenCache;
    }

    public void clearSortedChildrenCache() {
        sortedChildrenCache = null;
    }

    public final int getSiblingIndex() {
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
     * The mouse here is already transformed.
     *
     * @return the element that is hovered and its z-index, or null if no element is hovered
     */
    @Nullable
    public final Pair<UIElement, Integer> hitTest(double mouseX, double mouseY) {
        // TODO do hit tree in the future?
        if (!isDisplayed() || !isVisible() || getStyle().opacity() <= 0) return null;

        var transform2D = style.transform2D();
        double[] pt = new double[]{mouseX, mouseY};
        if (!transform2D.isIdentity()) {
            transform2D.inversePoint(this, pt);
        }
        double localMouseX = pt[0];
        double localMouseY = pt[1];

        Pair<UIElement, Integer> hover = null;
        var hidden = !style.overflowVisible();

        if (!hidden || isMouseOverRect(getContentX(), getContentY(), getContentWidth(), getContentHeight(), mouseX, mouseY)) {
            for (var child : getSafeSortedChildren()) {
                var result = child.hitTest(localMouseX, localMouseY);
                if (result != null && (hover == null || hover.getB() < result.getB())) {
                    hover = result;
                }
            }
        }

        if (hover == null && isAllowHitTest() && isIntersectWithPoint(localMouseX, localMouseY)) {
            return new Pair<>(this, style.zIndex());
        }

        if (hover == null) return null;
        return new Pair<>(hover.getA(), hover.getB() + style.zIndex());
    }

    /**
     * Determines whether a given point, defined by its coordinates, intersects with the object.
     * <br>
     * This method will affect hit testing.
     *
     * @param localX The local x-coordinate of the point to check.
     * @param localY The local y-coordinate of the point to check.
     * @return {@code true} if the point intersects with the object; {@code false} otherwise.
     */
    public boolean isIntersectWithPoint(double localX, double localY) {
        return isMouseOverRect(getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), localX, localY);
    }

    public boolean isOverlapping(float localX, float localY, float localWidth, float localHeight) {
        var x = getPositionX();
        var y = getPositionY();
        var width = getSizeWidth();
        var height = getSizeHeight();
        return x < localX + localWidth &&
                x + width > localX &&
                y < localY + localHeight &&
                y + height > localY;
    }

    public final Vector2f getLocalMouse(float worldX, float worldY) {
        var worldToLocal = getWorldToLocalPose();
        var localMouse = worldToLocal.transform(new Vector4f(worldX, worldY, 0, 1));
        return new Vector2f(localMouse.x / localMouse.w, localMouse.y / localMouse.w);
    }

    public final Vector2f worldToLocal(Vector2f world) {
        return getLocalMouse(world.x, world.y);
    }

    public final Vector2f getWorldMouse(float localX, float localY) {
        var localToWorld = getLocalToWorldPose();
        var worldMouse = localToWorld.transform(new Vector4f(localX, localY, 0, 1));
        return new Vector2f(worldMouse.x / worldMouse.w, worldMouse.y / worldMouse.w);
    }

    public final Vector2f localToWorld(Vector2f local) {
        return getWorldMouse(local.x, local.y);
    }

    public final Vector2f getLocalMouseNormal(float dirX, float dirY) {
        var worldToLocal = getWorldToLocalPose();
        var localMouse = worldToLocal.transformDirection(new Vector3f(dirX, dirY, 0));
        return new Vector2f(localMouse.x, localMouse.y);
    }

    public final Vector2f worldToLocalNormal(Vector2f dir) {
        return getLocalMouseNormal(dir.x, dir.y);
    }

    public final Vector2f getWorldMouseNormal(float dirX, float dirY) {
        var localToWorld = getLocalToWorldPose();
        var worldMouse = localToWorld.transformDirection(new Vector3f(dirX, dirY, 0));
        return new Vector2f(worldMouse.x, worldMouse.y);
    }

    public final Vector2f localToWorldNormal(Vector2f dir) {
        return getWorldMouseNormal(dir.x, dir.y);
    }

    public final Vector2f worldToLocalLayoutOffset(Vector2f world) {
        var local = worldToLocal(world);
        return local.sub(getPositionX(), getPositionY());
    }

    public final boolean isMouseOver(float worldX, float worldY) {
        return isMouseOver(getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), worldX, worldY);
    }

    public final boolean isMouseOverContent(float worldX, float worldY) {
        return isMouseOver(getContentX(), getContentY(), getContentWidth(), getContentHeight(), worldX, worldY);
    }

    public final boolean isMouseOver(float x, float y, float width, float height, float worldX, float worldY) {
        var localMouse = getLocalMouse(worldX, worldY);
        return localMouse.x >= x && localMouse.y >= y && localMouse.x < x + width && localMouse.y < y + height;
    }

    public static boolean isMouseOverRect(float x, float y, float width, float height, double mouseX, double mouseY) {
        return mouseX >= x && mouseY >= y && x + width > mouseX && y + height > mouseY;
    }

    /// Logic
    public void screenTick() {
        var safeChildren = getSafeChildren();
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
        var safeChildren = getSafeChildren();
        for (var child : safeChildren) {
            if (child.isActive() && child.isDisplayed()) {
                child.serverTick();
            }
        }
        if (serverCaptureEventListeners.containsKey(UIEvents.TICK) || serverBaubleEventListeners.containsKey(UIEvents.TICK)) {
            var tickEvent = UIEvent.create(UIEvents.TICK);
            for (var uiEventListener : serverCaptureEventListeners.get(UIEvents.TICK).getB()) {
                uiEventListener.handleEvent(tickEvent);
                if (tickEvent.laterPropagationStopped) break;
            }
            for (var uiEventListener : serverBaubleEventListeners.get(UIEvents.TICK).getB()) {
                uiEventListener.handleEvent(tickEvent);
                if (tickEvent.laterPropagationStopped) break;
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

    public boolean hasEventListener(String eventType, UIEventListener listener) {
        return captureListeners.getOrDefault(eventType, Collections.emptyList()).contains(listener) ||
                bubbleListeners.getOrDefault(eventType, Collections.emptyList()).contains(listener);
    }

    public boolean hasEventListener(String eventType, UIEventListener listener, boolean useCapture) {
        return useCapture ? captureListeners.getOrDefault(eventType, Collections.emptyList()).contains(listener) :
                bubbleListeners.getOrDefault(eventType, Collections.emptyList()).contains(listener);
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

    public RPCEmitter addRPCEvent(RPCEvent event) {
        this.rpcEvents.add(event);
        var mui = getModularUI();
        if (mui != null && mui.syncManager != null) {
            mui.syncManager.registerRPCEvent(event);
        }
        return new RPCEmitter(event, this::getModularUI);
    }

    public RPCEmitter addRPCEvent(Function<UIElement, RPCEvent> creator) {
        return addRPCEvent(creator.apply(this));
    }

    /**
     * Retrieves the existing messageRPC instance or creates a new one if it is null.
     * The messageRPC is initialized as an RPC event with a String name and a CompoundTag data payload.
     * It handles registered message handlers by invoking them with the provided data.
     *
     * @return The existing or newly created RPCEvent instance for message handling.
     */
    private RPCEvent getOrCreateMessageRPC() {
        if (messageRPC == null) {
            messageRPC = RPCEventBuilder.simple(String.class, CompoundTag.class, (name, data) -> {
                var handlers = messageHandlers.get(name);
                if (handlers == null || handlers.isEmpty()) return;
                var payload = data == null ? new CompoundTag() : data;
                for (var handler : new ArrayList<>(handlers)) {
                    handler.accept(payload);
                }
            });
            addRPCEvent(messageRPC);
        }
        return messageRPC;
    }

    /**
     * Registers a message handler for the specified message name. The handler will be invoked
     * whenever a message with the given name is received.
     *
     * @param name the name of the message to listen for
     * @param handler the function to handle the message, which receives a CompoundTag as its input
     * @return the current instance of UIElement
     */
    @HideFromJS
    public UIElement onMessage(String name, Consumer<CompoundTag> handler) {
        getOrCreateMessageRPC();
        messageHandlers.computeIfAbsent(name, k -> new ArrayList<>()).add(handler);
        return this;
    }

    /**
     * Registers a message handler that gets triggered when a message with the specified
     * name is received. The handler processes the message payload and performs operations
     * on the current {@code UIElement}.
     *
     * @param name the unique name of the message to register the handler for
     * @param handler a {@code BiConsumer} accepting the current {@code UIElement}
     *                and the {@code CompoundTag} payload that represents the message data
     * @return the current {@code UIElement}, allowing for method chaining
     */
    @HideFromJS
    public UIElement onMessage(String name, BiConsumer<UIElement, CompoundTag> handler) {
        return onMessage(name, (payload) -> handler.accept(this, payload));
    }

    // fxxk kjs!
    public UIElement kjs$onMessage(String name, BiConsumer<UIElement, CompoundTag> handler) {
        return onMessage(name, (payload) -> handler.accept(this, payload));
    }

    /**
     * Removes the specified message handler for a given message name. If the message handler is successfully
     * removed and no handlers remain associated with the message name, the message name is also removed from
     * the internal registry.
     *
     * @param name the name of the message whose handler is to be removed
     * @param handler the handler to be removed for the specified message name
     * @return this UIElement instance for method chaining
     */
    public UIElement offMessage(String name, Consumer<CompoundTag> handler) {
        var handlers = messageHandlers.get(name);
        if (handlers != null) {
            handlers.remove(handler);
            if (handlers.isEmpty()) {
                messageHandlers.remove(name);
            }
        }
        return this;
    }

    /**
     * Sends a message event with the provided name and data.
     * If the data is null, an empty CompoundTag will be used.
     *
     * @param name the name of the message to send
     * @param data optional CompoundTag containing the message data; if null, an empty CompoundTag will be used
     */
    public void sendMessage(String name, @Nullable CompoundTag data) {
        sendEvent(getOrCreateMessageRPC(), name, data == null ? new CompoundTag() : data);
    }

    /**
     * Sends a message with the specified name and a default compound tag.
     *
     * @param name the name associated with the message to be sent
     */
    public void sendMessage(String name) {
        sendMessage(name, new CompoundTag());
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
        return addServerEventListener(eventType, listener, false);
    }

    public UIElement addServerEventListener(String eventType, UIEventListener listener, boolean useCapture) {
        var eventListeners = useCapture ? serverCaptureEventListeners : serverBaubleEventListeners;
        eventListeners.computeIfAbsent(eventType, type -> {
            var listeners = new ArrayList<UIEventListener>();
            var rpcEvent = RPCEventBuilder.simple(UIEvent.class, event -> {
                event.currentElement = this;
                listeners.forEach(e -> e.handleEvent(event));
            });
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
        long id = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(id, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(id, GLFW.GLFW_KEY_LEFT_SHIFT);
    }

    public static boolean isCtrlDown() {
        return Screen.hasControlDown();
    }

    public static boolean isAltDown() {
        long id = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(id, GLFW.GLFW_KEY_LEFT_ALT) || InputConstants.isKeyDown(id, GLFW.GLFW_KEY_RIGHT_ALT);
    }

    public static boolean isKeyDown(int keyCode) {
        long id = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(id, keyCode);
    }

    public boolean isMouseDown(int button) {
        return getModularUI() != null && getModularUI().getLastMouseDownButton() == button;
    }

    /// Rendering
    public boolean isDisplayed() {
        return taffyStyle.style.display != TaffyDisplay.NONE;
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
        var display = taffyStyle.style.display;
        var opacity = style.opacity();
        if (display == TaffyDisplay.NONE || !isVisible() || opacity == 0) {
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
            transform2D.pushPose(guiContext, this);
        }

        if (localToWorldCache == null) {
            localToWorldCache = new Matrix4f(guiContext.pose.last().pose());
        }

        isCulled = !isInsideTheScissorView(guiContext);
        var hasOverlayClip = !style.overflowVisible() && getStyle().overflowClip() != IGuiTexture.EMPTY;
        var hasVisualLayer = !isCulled && (hasOverlayClip || opacity < 1);

        if (hasVisualLayer) {
            guiContext.pushVisualLayer(getOrCreateVisualLayer());
        }

        drawInBackgroundInternal(guiContext);

        if (hasVisualLayer) {
            guiContext.popVisualLayer();
        }

        if (pushedTransform) {
            transform2D.popPose(guiContext);
        }

        if (zIndex != 0) {
            guiContext.pose.popPose();
        }
    }

    public final void drawInBackgroundInternal(GUIContext guiContext) {
        var elementColor = style.color();
        var hasColor = elementColor != -1;
        if (hasColor) {
            guiContext.graphics.flush();
            guiContext.setElementColor(elementColor);
        }
        if (!isCulled) {
            drawBackgroundTexture(guiContext);
            drawContents(guiContext);
            drawBackgroundOverlay(guiContext);
        } else { // draw contents only
            drawContents(guiContext);
        }
        if (hasColor) {
            guiContext.graphics.flush();
            guiContext.resetElementColor();
        }
    }

    protected boolean isInsideTheScissorView(GUIContext context) {
        if (!context.scissorStack.isEmpty()) {
            var trans = context.pose.last().pose();
            var x = getPositionX();
            var y = getPositionY();
            var width = getSizeWidth();
            var height = getSizeHeight();

            var corners = new Vector4f[]{
                    new Vector4f(x, y, 0, 1),
                    new Vector4f(x + width, y, 0, 1),
                    new Vector4f(x, y + height, 0, 1),
                    new Vector4f(x + width, y + height, 0, 1)
            };
            float minX = Float.POSITIVE_INFINITY;
            float minY = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY;
            float maxY = Float.NEGATIVE_INFINITY;
            for (var corner : corners) {
                trans.transform(corner);
                minX = Math.min(minX, corner.x / corner.w);
                minY = Math.min(minY, corner.y / corner.w);
                maxX = Math.max(maxX, corner.x / corner.w);
                maxY = Math.max(maxY, corner.y / corner.w);
            }
            var aabb = Rect.of(Mth.floor(minX), Mth.floor(minY), Mth.ceil(maxX), Mth.ceil(maxY));
            return aabb.isCollide(context.scissorStack.top());
        }
        return true;
    }

    /**
     * Renders the background texture of the GUI element.
     */
    public void drawBackgroundTexture(GUIContext guiContext) {
        var background = style.backgroundTexture();
        if (background != null && background != IGuiTexture.EMPTY) {
            guiContext.drawTexture(background, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
        }
    }

    /**
     * Renders the contents of the GUI element. includes additional background and children
     */
    public void drawContents(GUIContext guiContext) {
        // not need to use scissoring if overflow cip defined
        var hidden = !style.overflowVisible() && getStyle().overflowClip() == IGuiTexture.EMPTY ;
        if (hidden) {
            if (isCulled) return;
            guiContext.graphics.flush();
            guiContext.enableScissor(getContentX(), getContentY(), getContentWidth(), getContentHeight());
        }
        if(!isCulled) {
            drawBackgroundAdditional(guiContext);
        }
        if (!children.isEmpty()) {
            var currentColor = guiContext.elementColor;
            var hasColor = currentColor != -1;
            // we roll back first
            if (hasColor) {
                guiContext.graphics.flush();
                guiContext.resetElementColor();
            }

            var sortedChildren = getSafeSortedChildren();
            for (int i = sortedChildren.length - 1; i >= 0; i--) {
                sortedChildren[i].drawInBackground(guiContext);
            }

            if (hasColor) {
                guiContext.graphics.flush();
                guiContext.setElementColor(currentColor);
            }
        }
        if (hidden) {
            guiContext.graphics.flush();
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

    /// Editor + Serialization
    // region Editor
    @Override
    public String toString() {
        return getElementName() + "{" + id + "}";
    }

    public List<Component> getDebugInfo() {
        var info = new ArrayList<Component>();
        info.add(Component.literal("[type: %s, pos: (%.1f %.1f), size: (%.1f, %.1f), children: %d]".formatted(
                getElementName(), getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), children.size())).withColor(0xFFFF00FF));
        info.add(Component.literal("[id: %s, class: \"%s\"]".formatted(getId().isEmpty() ? "empty" : getId(), String.join(" ", classes))).withColor(0xFF00FFFF));
//        var path = getStructurePath();
//        for (int i = 0; i < path.size(); i++) {
//            var element = path.get(i);
//            var data =Component.empty();
//            for (int i1 = 0; i1 < i; i1++) {
//                data = data.append(Component.literal("  "));
//            }
//            var style =  element.getTaffyStyle().style;
//            data = data.append("└").append(element.toString()).append(
//                    Component.literal("[flex: %s, inset: (%s, %s, %s, %s), size: (%s, %s)]".formatted(
//                            style.flex,
//                            style.inset.left, style.inset.right,
//                            style.inset.top, style.inset.bottom,
//                            style.size.width, style.size.height
//                            )).withColor(0xFFFF00FF));
//            info.add(data.withColor(0xFF00FF00));
//        }
        return info;
    }

    /// Editor
    public UIElement markAsInternal() {
        if (isInternalUI()) return this;
        setInternalUI(true);
        moveInlineAsDefault();
        children.forEach(UIElement::markAsInternal);
        return this;
    }

    public void internalSetup() {
        moveInlineAsDefault();
        for (var child : children) {
            child.markAsInternal();
        }
    }

    protected void setInternalUI(boolean isInternal) {
        isInternalUI = isInternal;
    }

    public Component getEditorName() {
        var name = Component.literal(getElementName());
        if (!id.isEmpty()) {
            name = name.append(Component.literal("#").append(Component.literal(id).withColor(0xFF00FFFF)));
        }
        if (!isDisplayed()) {
            name.withStyle(style -> style.withStrikethrough(true));
        }
        return name;
    }

    public IGuiTexture getEditorIcon() {
        return Icons.WIDGET_CUSTOM;
    }

    public List<UIElement> getEditorVisibleChildren() {
        return children.stream().filter(UIElement::isEditorVisible).toList();
    }

    public boolean isEditorVisible() {
        return true;
    }

    /**
     * Initializes the editor template, setting up the necessary resources
     * and configurations required for the editor to function properly.
     * This method is typically called during the initialization phase
     * of the editor setup process.
     */
    public void initEditorTemplate() {
    }

    public boolean canAddEditorChild(AutoRegistry.Holder<LDLRegister, UIElement, Supplier<UIElement>> holder) {
        return true;
    }

    public void addEditorChild(UIElement child, int index) {
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
        additionalConfigurators(father);
        // class selector
        final var classList = new ArrayList<>(classes);
        var classConfigurator = new ArrayConfiguratorGroup<>("UIElement.class", true, () -> {
            if (modularUI != null && (modularUI.getTickCounter() & 20) ==0) return classList;
            var set = new HashSet<>(classList);
            if (!set.equals(classes)) {
                classList.removeIf(s -> !classes.contains(s));
                classes.stream().filter(s -> !classList.contains(s)).forEach(classList::add);
            }
            return classList;
        }, (getter, setter) -> {
            var value = getter.get();
            if (value.startsWith("__") && value.endsWith("__")) {
                return new Configurator(value).setCopiableDirect(value);
            }
            return new StringConfigurator("", getter, setter, "", true);
        }, true);
        classConfigurator.setAddDefault(() -> "");
        classConfigurator.setOnUpdate(list -> {
            classes.clear();
            classes.addAll(list);
            classList.clear();
            classList.addAll(list);
            onClassIdChanged();
        });
        classConfigurator.setCanRemove(clazz -> !clazz.startsWith("__") || !clazz.endsWith("__"));
        father.addConfigurators(classConfigurator);
        // style
        for (int i = getStyles().size() - 1; i >= 0; i--) {
            var style = getStyles().get(i);
            style.buildConfigurator(father);
        }
    }

    /**
     * Append additional configurators after auto-detected ones and before th class configurator.
     */
    protected void additionalConfigurators(ConfiguratorGroup father) {}

    // endregion

    // region Serialization
    public UIElement copy() {
        return CODEC.encodeStart(Platform.getFrozenRegistry().createSerializationContext(NbtOps.INSTANCE), this)
                .result()
                .map(tag -> CODEC.parse(Platform.getFrozenRegistry().createSerializationContext(NbtOps.INSTANCE), tag)
                        .result().orElseGet(UIElement::new))
                .orElseGet(UIElement::new);
    }

    @Override
    public void beforeDeserialize() {
        IPersistedSerializable.super.beforeDeserialize();
        clearAllExternalChildren();
        setFocusable(false);
        setVisible(true);
        setActive(true);
        setId("");
        classes.removeIf(s -> !s.startsWith("__") || !s.endsWith("__"));
    }

    @SkipPersistedValue(field = "focusable")
    private boolean skipFocusablePersisted(boolean focusable) {
        return !focusable;
    }

    @SkipPersistedValue(field = "isVisible")
    private boolean skipIsVisiblePersisted(boolean isVisible) {
        return isVisible;
    }

    @SkipPersistedValue(field = "isActive")
    private boolean skipIsActivePersisted(boolean isActive) {
        return isActive;
    }

    @SkipPersistedValue(field = "id")
    private boolean skipIdPersisted(String id) {
        return id.isEmpty();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        var tagBuilder = TagBuilder.compound(IPersistedSerializable.super.serializeNBT(provider));
        // serialize inline styles
        var inlineTag = new CompoundTag();
        for (Style style : getStyles()) {
            var styleTag = style.serializeNBT(provider);
            // quick merge without copy
            for (var key : styleTag.getAllKeys()) {
                var value = styleTag.get(key);
                if (value != null) {
                    inlineTag.put(key, value);
                }
            }
        }
        if (!inlineTag.isEmpty()) {
            tagBuilder.add("inline", inlineTag);
        }
        // serialize classes
        var classTag = new ListTag();
        for (var clazz : classes) {
            // skip internal classes
            if (clazz.startsWith("__") && clazz.endsWith("__")) continue;
            classTag.add(StringTag.valueOf(clazz));
        }
        if (!classTag.isEmpty()) {
            tagBuilder.add("classes", classTag);
        }
        // serialize internal children
        var internalTag = TagBuilder.list().add(getChildren().stream()
                .filter(UIElement::isInternalUI)
                .map(element -> element.serializeNBT(provider)).toList()
        ).build();
        if (!internalTag.isEmpty()) {
            tagBuilder.add("internal", internalTag);
        }
        // serialize external children
        var childrenTag = TagBuilder.list().add(getChildren().stream()
                .filter(uiElement -> !uiElement.isInternalUI())
                .map(child -> TagBuilder.compound()
                        .add("index", child.getSiblingIndex())
                        .add("data", child.serializeNBT(provider))
                        .add("type", child.name())
                        .build()).toList()
        ).build();
        if (!childrenTag.isEmpty()) {
            tagBuilder.add("children", childrenTag);
        }
        return tagBuilder.build();
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, CompoundTag tag) {
        IPersistedSerializable.super.deserializeNBT(provider, tag);
        if (!LDLib2.isServer()) {
            // only for client side
            // deserialize inline styles
            if (tag.contains("inline")) {
                var inlineTag = tag.getCompound("inline");
                getStyles().forEach(style -> style.deserializeNBT(provider, inlineTag));
            }
        }
        // deserialize classes
        if (tag.contains("classes")) {
            for (var clazz : tag.getList("classes", Tag.TAG_STRING)) {
                classes.add(clazz.getAsString());
            }
        }
        // deserialize internal children
        if (tag.contains("internal")) {
            var internalTag = tag.getList("internal", Tag.TAG_COMPOUND);
            var internalChildren = getChildren().stream().filter(UIElement::isInternalUI).toList();
            for (var i = 0; i < internalTag.size(); i++) {
                if (i < internalChildren.size()) {
                    internalChildren.get(i).deserializeNBT(provider, internalTag.getCompound(i));
                }
            }
        }
        // deserialize external children
        if (tag.contains("children")) {
            var externalTag = tag.getList("children", Tag.TAG_COMPOUND);
            for (var i = 0; i < externalTag.size(); i++) {
                var childTag = externalTag.getCompound(i);
                var index = childTag.getInt("index");
                var result = CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), childTag).result();
                if (result.isEmpty()) {
                    LDLib2.LOGGER.error("Failed to deserialize UI Element {}: {}", this, tag);
                }
                addChildAt(result.orElseGet(UIElement::new), index);
            }
        }
    }
    // endregion

    // region XML Support
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void loadXml(Element element) {
        // id
        if (element.hasAttribute("id")) {
            setId(element.getAttribute("id"));
        }
        // visible
        if (element.hasAttribute("visible")) {
            setVisible(XmlUtils.getAsBoolean(element, "visible", true));
        }
        // focusable
        if (element.hasAttribute("focusable")) {
            setFocusable(XmlUtils.getAsBoolean(element, "focusable", false));
        }
        // active
        if (element.hasAttribute("active")) {
            setActive(XmlUtils.getAsBoolean(element, "active", true));
        }

        if (!LDLib2.isServer()) {
            // load inline styles
            if (element.hasAttribute("style")) {
                for (var entry : Stylesheet.parseStyleValues(element.getAttribute("style")).entrySet()) {
                    Property p = entry.getKey();
                    StyleValue v = entry.getValue();
                    getStyleBag().replaceOrPutCandidate(p, StyleSlot.of(p,
                            StyleOrigin.INLINE,
                            0, 0, v.compute()
                    ));
                }
            }
        }

        // load classes
        if (element.hasAttribute("class")) {
            for (var clazz : element.getAttribute("class").split(" ")) {
                addClass(clazz);
            }
        }
        // deserialize external children
        var nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            var node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && node instanceof Element child) {
                var tagName = child.getTagName();
                if (tagName.equals("style")) {
                    // Inline CSS block — parsed as a local stylesheet scoped to this element
                    if (!LDLib2.isServer()) {
                        var content = child.getTextContent();
                        if (content != null && !content.isBlank()) {
                            addLocalStylesheet(Stylesheet.parse(content));
                        }
                    }
                } else if (tagName.equals("stylesheet")) {
                    // External stylesheet reference — loaded from resource location
                    if (!LDLib2.isServer()) {
                        var location = XmlUtils.getAsString(child, "location", "");
                        if (!location.isEmpty()) {
                            var rs = ResourceLocation.tryParse(location);
                            if (rs != null) {
                                var sheet = StylesheetManager.INSTANCE.getStylesheet(rs);
                                if (sheet != null) {
                                    addLocalStylesheet(sheet);
                                }
                            }
                        }
                    }
                } else if (tagName.equals("internal")) {
                    parseXmlInternalChild(child);
                } else {
                    parseXmlChildElement(child);
                }
            }
        }
    }

    protected void parseXmlInternalChild(Element childElement) {
        var index = XmlUtils.getAsInt(childElement, "index", 0);
        var cur = 0;
        for (var child : children) {
            if (child.isInternalUI() && cur++ >= index) {
                child.loadXml(childElement);
                return;
            }
        }
    }

    protected void parseXmlChildElement(Element childElement) {
        var tagName = childElement.getTagName();
        var holder = LDLib2Registries.UI_ELEMENTS.get(tagName);
        if (holder != null) {
            var child = holder.value().get();
            child.loadXml(childElement);
            addEditorChild(child, -1);
        }
    }

    private Element saveXml(Document document) {
        throw new UnsupportedOperationException();
    }
    // endregion
}
