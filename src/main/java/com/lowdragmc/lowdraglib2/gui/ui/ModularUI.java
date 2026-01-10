package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.style.HierarchicalStyleMatcher;
import com.lowdragmc.lowdraglib2.utils.animation.AnimationEngine;
import com.lowdragmc.lowdraglib2.gui.sync.UISyncManager;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.event.*;
import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolder;
import com.lowdragmc.lowdraglib2.gui.ui.layout.YogaProperties;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleEngine;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.math.Size;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
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
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.appliedenergistics.yoga.YogaConstants;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaPositionType;
import org.appliedenergistics.yoga.YogaUnit;
import org.appliedenergistics.yoga.style.StyleSizeLength;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@KJSBindings
public class ModularUI {
    public final UI ui;
    public final UISyncManager syncManager;
    @Nullable
    public final Player player;

    @Setter @Getter @Accessors(fluent = true, chain = true)
    private boolean shouldCloseOnEsc = true;
    @Setter @Getter @Accessors(fluent = true, chain = true)
    private boolean shouldCloseOnKeyInventory = true;

    // runtime
    @OnlyIn(Dist.CLIENT)
    @Nullable
    private ModularUIWidget widget;
    @Nullable
    @OnlyIn(Dist.CLIENT)
    @Getter(onMethod_ = {@OnlyIn(Dist.CLIENT)})
    private Screen screen;
    @Getter
    private final StyleEngine styleEngine = new StyleEngine(this);
    @Getter
    private final AnimationEngine animationEngine = new AnimationEngine();
    @Getter
    @Nullable
    private AbstractContainerMenu menu;
    @Getter
    private int screenWidth, screenHeight;
    @Getter
    private float layoutWidth = YogaConstants.UNDEFINED, layoutHeight = YogaConstants.UNDEFINED;
    @Getter
    private float leftPos, topPos, width, height;
    @Getter
    private final DragHandler dragHandler = new DragHandler();
    @Getter
    private long tickCounter = 0;
    // Element registry for fast retrieval
    private final List<UIElement> elements = new ArrayList<>();
    private final Map<String, List<UIElement>> elementsById = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<UIElement>> elementsByType = new ConcurrentHashMap<>();

    // UI state
    @Getter @Setter
    private boolean focused;
    @Getter @Setter
    private boolean drawTooltips = true;
    @Getter @Setter
    private boolean drawDrag = true;
    @Nullable
    @Getter
    private UIElement lastHoveredElement;
    @Getter
    private final List<UIElement> lastHoveredElements = new ArrayList<>();
    @Getter
    private UIElement lastMouseDownElement;
    @Getter
    private UIElement lastMouseMoveElement;
    @Getter
    private UIElement lastMouseClickElement;
    @Getter
    private UIElement lastMouseDragElement;
    @Getter
    private int lastMouseDownButton = -1, lastMouseClickButton = -1;
    @Getter
    private int lastPressedKeyCode = - 1, lastPressedScanCode = -1, lastPressedModifiers = -1;
    @Getter
    private long lastMouseClickTime;
    @Getter
    private float lastMouseX, lastMouseY, lastMouseDownX, lastMouseDownY;
    @Getter @Nullable
    private UIElement focusedElement = null;
    private final List<Rect2i> extraAreas = new ArrayList<>();

    // hover tips
    @Nullable
    @Getter
    private List<Component> tooltipTexts;
    @Nullable
    @Getter
    private TooltipComponent tooltipComponent;
    @Nullable
    @OnlyIn(Dist.CLIENT)
    private Font tooltipFont;
    @Getter
    private ItemStack tooltipStack = ItemStack.EMPTY;
    @Getter @Setter
    private boolean allowDebugMode = true;
    @Getter @Setter
    private boolean debugMode = false;

    public ModularUI(UI ui) {
        this(ui, null);
    }

    public ModularUI(UI ui, @Nullable Player player) {
        this.ui = ui;
        this.player = player;
        this.syncManager = new UISyncManager(this);
        this.styleEngine.addStylesheets(this.ui.getStylesheets());
        this.ui.rootElement.addClass("__root__");
    }

    public static ModularUI of(UI ui) {
        return new ModularUI(ui);
    }

    public static ModularUI of(UI ui, @Nullable Player player) {
        return new ModularUI(ui, player);
    }

    public boolean isHoverSlot(Slot slot) {
        return getLastHoveredElement() instanceof ItemSlot itemSlot && itemSlot.getSlot() == slot;
    }

    /**
     * Add an element to the registry for fast retrieval.
     * This method is automatically called when elements are added to the UI tree.
     * @param element the element to add to the registry
     */
    public void registerElement(@Nullable UIElement element) {
        if (element == null) return;
        elements.add(element);

        // Register by ID if present and not empty
        String id = element.getId();
        if (!id.isEmpty()) {
            elementsById.computeIfAbsent(id, k -> new ArrayList<>()).add(element);
        }

        // Register by type
        Class<?> elementType = element.getClass();
        elementsByType.computeIfAbsent(elementType, k -> new ArrayList<>()).add(element);

        // Enqueue StyleEngine
        styleEngine.onElementRegister(element);
    }

    /**
     * Remove an element from the registry.
     * This method is automatically called when elements are removed from the UI tree.
     * @param element the element to remove from the registry
     */
    public void unregisterElement(@Nullable UIElement element) {
        if (element == null) return;

        // Remove StyleEngine
        styleEngine.onElementUnregister(element);

        // Remove by ID if present and not empty
        String id = element.getId();
        if (!id.isEmpty()) {
            List<UIElement> idList = elementsById.get(id);
            if (idList != null) {
                idList.remove(element);
                // Clean up empty lists to avoid memory leaks
                if (idList.isEmpty()) {
                    elementsById.remove(id);
                }
            }
        }

        // Remove by type
        Class<?> elementType = element.getClass();
        List<UIElement> typeList = elementsByType.get(elementType);
        if (typeList != null) {
            typeList.remove(element);
            // Clean up empty lists to avoid memory leaks
            if (typeList.isEmpty()) {
                elementsByType.remove(elementType);
            }
        }

        elements.remove(element);
    }

    public List<UIElement> getAllElements() {
        return Collections.unmodifiableList(elements);
    }

    /**
     * Selects a stream of {@link UIElement} instances that match a given CSS-like selector.
     * The selector is parsed and used to filter the {@link UIElement} objects within the current UI structure.
     *
     * @param selector the CSS-like selector used to match elements
     * @return a stream of {@link UIElement} instances that match the provided selector
     */
    public Stream<UIElement> select(String selector) {
        var match = HierarchicalStyleMatcher.parse(selector);
        return getAllElements().stream().filter(match::matches);
    }

    public <T> Stream<T> select(String selector, Class<T> type) {
        return select(selector).filter(type::isInstance).map(type::cast);
    }

    /**
     * Find the first element by its ID.
     * @param id the ID of the element to find
     * @return the first element with the given ID, or null if not found
     */
    @Nullable
    public UIElement getElementById(@Nullable String id) {
        if (id == null || id.isEmpty()) return null;
        List<UIElement> elements = elementsById.get(id);
        return elements != null && !elements.isEmpty() ? elements.getFirst() : null;
    }

    /**
     * Find all elements by their ID.
     * @param id the ID of the elements to find
     * @return a list of all elements with the given ID (never null, but may be empty)
     */
    public List<UIElement> getElementsById(@Nullable String id) {
        if (id == null || id.isEmpty()) return new ArrayList<>();
        List<UIElement> elements = elementsById.get(id);
        return elements != null ? new ArrayList<>(elements) : new ArrayList<>();
    }

    /**
     * Find the first element by its ID using regex pattern.
     * @param pattern the regex pattern to match element IDs
     * @return the first element with an ID matching the pattern, or null if not found
     */
    @Nullable
    public UIElement getElementByIdRegex(@Nullable String pattern) {
        return getElementByIdPattern(pattern != null ? Pattern.compile(pattern) : null);
    }

    /**
     * Find all elements by their ID using regex pattern.
     * @param pattern the regex pattern to match element IDs
     * @return a list of all elements with IDs matching the pattern (never null, but may be empty)
     */
    public List<UIElement> getElementsByIdRegex(@Nullable String pattern) {
        return getElementsByIdPattern(pattern != null ? Pattern.compile(pattern) : null);
    }

    /**
     * Find the first element by its ID using regex pattern with compiled Pattern.
     * This is more efficient when using the same pattern multiple times.
     * @param pattern the compiled regex pattern to match element IDs
     * @return the first element with an ID matching the pattern, or null if not found
     */
    @Nullable
    public UIElement getElementByIdPattern(@Nullable Pattern pattern) {
        if (pattern == null) return null;
        return elementsById.entrySet().stream()
                .filter(entry -> pattern.matcher(entry.getKey()).matches())
                .flatMap(entry -> entry.getValue().stream())
                .findFirst()
                .orElse(null);
    }

    /**
     * Find all elements by their ID using regex pattern with compiled Pattern.
     * This is more efficient when using the same pattern multiple times.
     * @param pattern the compiled regex pattern to match element IDs
     * @return a list of all elements with IDs matching the pattern (never null, but may be empty)
     */
    public List<UIElement> getElementsByIdPattern(@Nullable Pattern pattern) {
        if (pattern == null) return new ArrayList<>();
        return elementsById.entrySet().stream()
                .filter(entry -> pattern.matcher(entry.getKey()).matches())
                .flatMap(entry -> entry.getValue().stream())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Find elements by ID using partial matching (contains).
     * @param substring the substring to search for in element IDs
     * @return a list of all elements with IDs containing the substring (never null, but may be empty)
     */
    public List<UIElement> getElementsByIdContains(@Nullable String substring) {
        if (substring == null || substring.isEmpty()) return new ArrayList<>();
        return elementsById.entrySet().stream()
                .filter(entry -> entry.getKey().contains(substring))
                .flatMap(entry -> entry.getValue().stream())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Find elements by ID using prefix matching (starts with).
     * @param prefix the prefix to search for in element IDs
     * @return a list of all elements with IDs starting with the prefix (never null, but may be empty)
     */
    public List<UIElement> getElementsByIdStartsWith(@Nullable String prefix) {
        if (prefix == null || prefix.isEmpty()) return new ArrayList<>();
        return elementsById.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(prefix))
                .flatMap(entry -> entry.getValue().stream())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Find elements by ID using suffix matching (ends with).
     * @param suffix the suffix to search for in element IDs
     * @return a list of all elements with IDs ending with the suffix (never null, but may be empty)
     */
    public List<UIElement> getElementsByIdEndsWith(@Nullable String suffix) {
        if (suffix == null || suffix.isEmpty()) return new ArrayList<>();
        return elementsById.entrySet().stream()
                .filter(entry -> entry.getKey().endsWith(suffix))
                .flatMap(entry -> entry.getValue().stream())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Find all elements of a specific type.
     * @param type the type of elements to find
     * @return a list of all elements of the given type (never null, but may be empty)
     */
    public <T extends UIElement> List<T> getElementsByType(Class<T> type) {
        List<UIElement> elements = elementsByType.get(type);
        if (elements == null) {
            return new ArrayList<>();
        }
        // Safe cast since we store elements by their actual type
        @SuppressWarnings("unchecked")
        List<T> result = new ArrayList<>((List<T>) elements);
        return result;
    }

    /**
     * Get all registered elements by ID.
     * @return a copy of the ID-to-elements mapping
     */
    public Map<String, List<UIElement>> getAllElementsById() {
        Map<String, List<UIElement>> result = new HashMap<>();
        elementsById.forEach((id, elements) ->
                result.put(id, new ArrayList<>(elements)));
        return result;
    }

    /**
     * Get all registered elements by type.
     * @return a copy of the type-to-elements mapping
     */
    public Map<Class<?>, List<UIElement>> getAllElementsByType() {
        Map<Class<?>, List<UIElement>> result = new HashMap<>();
        elementsByType.forEach((type, elements) ->
                result.put(type, new ArrayList<>(elements)));
        return result;
    }

    /**
     * Check if an element with the given ID exists.
     * @param id the ID to check
     * @return true if at least one element with the given ID exists
     */
    public boolean hasElementWithId(@Nullable String id) {
        if (id == null || id.isEmpty()) return false;
        List<UIElement> elements = elementsById.get(id);
        return elements != null && !elements.isEmpty();
    }

    /**
     * Get the count of elements with the given ID.
     * @param id the ID to count
     * @return the number of elements with the given ID
     */
    public int getElementCountById(@Nullable String id) {
        if (id == null || id.isEmpty()) return 0;
        List<UIElement> elements = elementsById.get(id);
        return elements != null ? elements.size() : 0;
    }

    public void setMenu(@Nullable AbstractContainerMenu menu) {
        this.menu = menu;
        this.ui.rootElement.setFocusable(true);
        this.ui.rootElement._setModularUIInternal(this);
    }

    /// screen only
    @OnlyIn(Dist.CLIENT)
    public void setScreenAndInit(Screen screen) {
        setScreen(screen);
        init(screen.width, screen.height);
    }

    @OnlyIn(Dist.CLIENT)
    public void setScreen(@Nullable Screen screen) {
        this.screen = screen;
    }

    @OnlyIn(Dist.CLIENT)
    public void init(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        if (ui.dynamicSize != null) {
            var size = ui.dynamicSize.apply(Size.of(screenWidth, screenHeight));
            ui.rootElement.layout(layout -> {
                layout.setWidth(size.getWidth());
                layout.setHeight(size.getHeight());
            });
        }
        var isRelative = Optional.ofNullable(ui.rootElement.getStyleBag().computeCandidate(YogaProperties.POSITION))
                .orElse(YogaPositionType.RELATIVE) != YogaPositionType.ABSOLUTE;
        var width = Optional.ofNullable(ui.rootElement.getStyleBag().computeCandidate(YogaProperties.WIDTH))
                .orElseGet(StyleSizeLength::ofAuto)
                .asYogaValue();
        var height = Optional.ofNullable(ui.rootElement.getStyleBag().computeCandidate(YogaProperties.HEIGHT))
                .orElseGet(StyleSizeLength::ofAuto)
                .asYogaValue();
        this.width = switch (width.unit) {
            case PERCENT -> width.value * screenWidth * 0.01f;
            case POINT -> width.value;
            default -> 0;
        };
        this.height = switch (height.unit) {
            case PERCENT -> height.value * screenHeight * 0.01f;
            case POINT -> height.value;
            default -> 0;
        };

        if (width.unit == YogaUnit.PERCENT) {
            this.layoutWidth = screenWidth;
        } else {
            this.layoutWidth = YogaConstants.UNDEFINED;
        }
        if (height.unit == YogaUnit.PERCENT) {
            this.layoutHeight = screenHeight;
        } else {
            this.layoutHeight = YogaConstants.UNDEFINED;
        }

        // we'd better align it to the integer position to avoid a floating point error
        if (this.menu != null && this.ui.rootElement.getModularUI() == this) {
            // have already set
        } else {
            this.ui.rootElement._setModularUIInternal(this);
        }
        this.ui.rootElement.initScreen(screenWidth, screenHeight);
        this.ui.rootElement.getLayoutNode().markDirtyAndPropagate();
        calculateStyleAndLayout();

        // if dimension is auto, update real sizes after layout calculation
        if (width.unit == YogaUnit.AUTO) {
            this.width = ui.rootElement.getLayoutNode().getLayoutWidth();
            this.leftPos = isRelative ? (screenWidth - this.width) / 2 : ui.rootElement.layoutNode.getLayoutX();
        } else {
            this.leftPos = isRelative ? (screenWidth - this.width) / 2 : ui.rootElement.layoutNode.getLayoutX();
        }
        if (height.unit == YogaUnit.AUTO) {
            this.height = ui.rootElement.getSizeHeight();
            this.topPos = isRelative ? (screenHeight - this.height) / 2 : ui.rootElement.layoutNode.getLayoutY();
        } else {
            this.topPos = isRelative ? (screenHeight - this.height) / 2 : ui.rootElement.layoutNode.getLayoutY();
        }

        this.leftPos = Math.round(this.leftPos);
        this.topPos = Math.round(this.topPos);
        this.ui.rootElement.clearLayoutCache();
    }

    private void calculateStyleAndLayout() {
        int dirtyCount = 0;
        while (styleEngine.requireCalculate() || ui.rootElement.getLayoutNode().isDirty()) {
            dirtyCount++;

            // calculate style
            while (styleEngine.requireCalculate()) {
                styleEngine.calculateStyle();
            }

            // calculate layout
            ui.rootElement.calculateLayout(layoutWidth, layoutHeight);
            extraAreas.clear();
            if (dirtyCount >= 10) {
                if (isDebugMode() || Platform.isDevEnv()) {
                    LDLib2.LOGGER.warn("UI layout is dirty for more than 10 times per frame, please check your style / layout code.");
                }
                break;
            }
        }
    }

    public void tick() {
        ui.rootElement.screenTick();
        tickCounter++;
    }

    public void tickServer() {
        ui.rootElement.serverTick();
        syncManager.tick();
        tickCounter++;
    }

    /**
     * Called when the UI is removed.
     * This method can be overridden to perform cleanup tasks.
     */
    public void onRemoved() {
        ui.rootElement.onRemoved();
    }

    /**
     * Request focus to the given element.
     * This will trigger FocusOut event on the old focused element and FocusIn event on the new focused element.
     * @param element the element to focus, or null to clear focus
     */
    @OnlyIn(Dist.CLIENT)
    public void requestFocus(@Nullable UIElement element) {
        if (focusedElement == element) return;

        if (focusedElement != null) {
            var focusOut = UIEvent.create(UIEvents.FOCUS_OUT);
            focusOut.target = focusedElement;
            focusOut.relatedTarget = element;
            UIEventDispatcher.dispatchEvent(focusOut, true, true, false);
        }

        if (element != null) {
            var focusIn = UIEvent.create(UIEvents.FOCUS_IN);
            focusIn.target = element;
            focusIn.relatedTarget = focusedElement;
            UIEventDispatcher.dispatchEvent(focusIn, true, true, false);
        }

        var lastFocusedElement = focusedElement;
        focusedElement = element;

        if (lastFocusedElement != null) {
            lastFocusedElement.removeClass("__focused__");
            var blur = UIEvent.create(UIEvents.BLUR);
            blur.target = lastFocusedElement;
            blur.relatedTarget = focusedElement;
            blur.hasBubblePhase = false;
            UIEventDispatcher.dispatchEvent(blur);
        }

        if (focusedElement != null) {
            focusedElement.addClass("__focused__");
            var focus = UIEvent.create(UIEvents.FOCUS);
            focus.target = focusedElement;
            focus.relatedTarget = lastFocusedElement;
            focus.hasBubblePhase = false;
            UIEventDispatcher.dispatchEvent(focus);
            if (screen != null) {
                screen.setFocused(getWidget());
            }
        }

    }

    @OnlyIn(Dist.CLIENT)
    public void clearFocus() {
        requestFocus(null);
    }

    @OnlyIn(Dist.CLIENT)
    public void setHoverTooltip(List<Component> tooltipTexts, ItemStack tooltipStack, @Nullable Font tooltipFont, @Nullable TooltipComponent tooltipComponent) {
        this.tooltipTexts = tooltipTexts;
        this.tooltipStack = tooltipStack;
        this.tooltipFont = tooltipFont;
        this.tooltipComponent = tooltipComponent;
    }

    @OnlyIn(Dist.CLIENT)
    public void cleanTooltip() {
        tooltipTexts = null;
        tooltipComponent = null;
        tooltipFont = null;
        tooltipStack = ItemStack.EMPTY;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public Font getTooltipFont() {
        return tooltipFont;
    }

    @OnlyIn(Dist.CLIENT)
    public ModularUIWidget getWidget() {
        if (widget == null) {
            widget = new ModularUIWidget();
        }
        return widget;
    }

    @OnlyIn(Dist.CLIENT)
    public List<Rect2i> getGuiExtraAreas() {
        if (extraAreas.isEmpty()) calculateExtraAreas();
        return extraAreas;
    }

    @OnlyIn(Dist.CLIENT)
    private void calculateExtraAreas() {
        extraAreas.clear();
        ui.rootElement.appendExtraAreas(extraAreas);
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    @OnlyIn(Dist.CLIENT)
    public class ModularUIWidget implements GuiEventListener, NarratableEntry, Renderable, IModularUIHolder {
        @Override
        public ModularUI getModularUI() {
            return ModularUI.this;
        }

        // narration
        @Override
        public NarrationPriority narrationPriority() {
            if (focused) {
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
            return new ScreenRectangle(Math.round(leftPos), Math.round(topPos), Math.round(width), Math.round(height));
        }

        /// event handling
        @Override
        public void setFocused(boolean focused) {
            ModularUI.this.setFocused(focused);
        }

        @Override
        public boolean isFocused() {
            return ModularUI.this.isFocused();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            lastMouseDownX = (float) mouseX;
            lastMouseDownY = (float) mouseY;
            lastMouseDownButton = button;
            lastMouseDownElement = getLastHoveredElement();
            if (lastMouseDownElement != null) {
                if (!lastMouseDownElement.isFocusable()) {
                    clearFocus();
                    var structurePath = lastMouseDownElement.getStructurePath();
                    for (int i = structurePath.size() - 1; i >= 0; i--) {
                        var element = structurePath.get(i);
                        if (element.isFocusable()) {
                            requestFocus(element);
                            break;
                        }
                    }
                } else if (lastMouseDownElement.isActive()) {
                    requestFocus(lastMouseDownElement);
                }
                var event = UIEvent.create(UIEvents.MOUSE_DOWN);
                event.x = (float) mouseX;
                event.y = (float) mouseY;
                event.button = button;
                event.target = lastMouseDownElement;
                UIEventDispatcher.dispatchEvent(event);
                return event.hasHandler;
            } else {
                clearFocus();
            }
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            lastMouseDownButton = -1;
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
                var hasHandler = event.hasHandler;
                if (releasedElement == lastMouseDownElement) {
                    var clickEvent = UIEvent.create(UIEvents.CLICK);
                    clickEvent.x = (float) mouseX;
                    clickEvent.y = (float) mouseY;
                    clickEvent.button = button;
                    clickEvent.target = releasedElement;
                    UIEventDispatcher.dispatchEvent(clickEvent);
                    hasHandler |= clickEvent.hasHandler;
                    if (lastMouseClickElement == releasedElement && button == lastMouseClickButton) {
                        if (System.currentTimeMillis() - lastMouseClickTime < 300) { // 300ms follow HTML5 spec
                            var doubleClickEvent = UIEvent.create(UIEvents.DOUBLE_CLICK);
                            doubleClickEvent.x = (float) mouseX;
                            doubleClickEvent.y = (float) mouseY;
                            doubleClickEvent.button = button;
                            doubleClickEvent.target = releasedElement;
                            UIEventDispatcher.dispatchEvent(doubleClickEvent);
                            hasHandler |= doubleClickEvent.hasHandler;
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
                return hasHandler;
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
                return event.hasHandler;
            }
            return false;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (dragHandler.isDragging()) {
                var hasHandler = false;
                var current = getLastHoveredElement();
                if (dragHandler.dragSource != null) {
                    var event = UIEvent.create(UIEvents.DRAG_SOURCE_UPDATE);
                    event.hasBubblePhase = false;
                    event.hasCapturePhase = true;
                    dispatchDragEvent(mouseX, mouseY, dragX, dragY, dragHandler.dragSource, event);
                    hasHandler = event.hasHandler;
                }
                if (current != null) {
                    if (lastMouseDragElement == current) {
                        var event = UIEvent.create(UIEvents.DRAG_UPDATE);
                        dispatchDragEvent(mouseX, mouseY, dragX, dragY, current, event);
                        hasHandler |= event.hasHandler;
                    } else {
                        if (lastMouseDragElement != null) {
                            var event = UIEvent.create(UIEvents.DRAG_LEAVE);
                            event.hasBubblePhase = false;
                            event.relatedTarget = current;
                            dispatchDragEvent(mouseX, mouseY, dragX, dragY, lastMouseDragElement, event);
                        }
                        lastMouseDragElement = current;
                        var event = UIEvent.create(UIEvents.DRAG_ENTER);
                        event.hasBubblePhase = false;
                        dispatchDragEvent(mouseX, mouseY, dragX, dragY, current, event);
                        hasHandler |= event.hasHandler;
                    }
                    return hasHandler;
                } else if (lastMouseDragElement != null) {
                    var event = UIEvent.create(UIEvents.DRAG_LEAVE);
                    event.hasBubblePhase = false;
                    dispatchDragEvent(mouseX, mouseY, dragX, dragY, lastMouseDragElement, event);
                    lastMouseDragElement = null;
                    hasHandler |= event.hasHandler;
                    return hasHandler;
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
            UIEventDispatcher.dispatchEvent(event, true, true, false);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (allowDebugMode && keyCode == GLFW.GLFW_KEY_F3) {
                debugMode = !debugMode;
            }
            lastPressedKeyCode = keyCode;
            lastPressedScanCode = scanCode;
            lastPressedModifiers = modifiers;
            var hasHandler = false;
            var command = getCommandType(keyCode);
            if (focusedElement != null) {
                var event = UIEvent.create(UIEvents.KEY_DOWN);
                event.keyCode = keyCode;
                event.scanCode = scanCode;
                event.modifiers = modifiers;
                event.target = focusedElement;
                UIEventDispatcher.dispatchEvent(event);
                hasHandler = event.hasHandler;
                if (command != null) {
                    event = createExecuteCommandEvent(command, keyCode, scanCode, modifiers);
                    event.target = focusedElement;
                    UIEventDispatcher.dispatchEvent(event);
                    hasHandler |= event.hasHandler;
                }
                return hasHandler;
            } else if (command != null){
                var event = createValidCommandEvent(command, keyCode, scanCode, modifiers);
                event.target = ui.rootElement;
                var handled = UIEventDispatcher.dispatchAllChildren(event);
                hasHandler |= event.hasHandler;
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
        protected String getCommandType(int keyCode) {
            if (Screen.isCopy(keyCode)) {
                return CommandEvents.COPY;
            } else if (Screen.isPaste(keyCode)) {
                return CommandEvents.PASTE;
            } else if (Screen.isCut(keyCode)) {
                return CommandEvents.CUT;
            } else if (Screen.isSelectAll(keyCode)) {
                return CommandEvents.SELECT_ALL;
            } else if (keyCode == GLFW.GLFW_KEY_Z && Screen.hasControlDown() && !Screen.hasShiftDown() && !Screen.hasAltDown()) {
                return CommandEvents.UNDO;
            } else if (keyCode == GLFW.GLFW_KEY_Z && Screen.hasControlDown() && Screen.hasShiftDown() && !Screen.hasAltDown()) {
                return CommandEvents.REDO;
            } else if (keyCode == GLFW.GLFW_KEY_Y && Screen.hasControlDown() && !Screen.hasShiftDown() && !Screen.hasAltDown()) {
                return CommandEvents.REDO;
            } else if (keyCode == GLFW.GLFW_KEY_F && Screen.hasControlDown() && !Screen.hasShiftDown() && !Screen.hasAltDown()) {
                return CommandEvents.FIND;
            } else if (keyCode == GLFW.GLFW_KEY_S && Screen.hasControlDown() && !Screen.hasShiftDown() && !Screen.hasAltDown()) {
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
        public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
            if (focusedElement != null) {
                var event = UIEvent.create(UIEvents.KEY_UP);
                event.keyCode = keyCode;
                event.scanCode = scanCode;
                event.modifiers = modifiers;
                event.target = focusedElement;
                UIEventDispatcher.dispatchEvent(event);
                return event.hasHandler;
            }
            return false;
        }

        @Override
        public boolean charTyped(char codePoint, int modifiers) {
            if (focusedElement != null) {
                var event = UIEvent.create(UIEvents.CHAR_TYPED);
                event.codePoint = codePoint;
                event.modifiers = modifiers;
                event.hasCapturePhase = false;
                event.hasBubblePhase = false;
                event.target = focusedElement;
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

        /// rendering
        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            animationEngine.updateFrame();

            calculateStyleAndLayout();

            cleanTooltip();

            // rendering
            var guiContext = GUIContext.of(ModularUI.this, guiGraphics, mouseX, mouseY, partialTick);

            lastMouseX = guiContext.localMouseX;
            lastMouseY = guiContext.localMouseY;

            var hoverElement = ui.rootElement.hitTest(lastMouseX, lastMouseY);
            var newHoveredElement = hoverElement == null ? null : hoverElement.getA();
            if (lastHoveredElements.isEmpty() ||
                    newHoveredElement != null && !newHoveredElement.getStructurePath().equals(lastHoveredElements)) {
                for (var element : lastHoveredElements) {
                    element.removeClass("__hovered__");
                }

                lastHoveredElements.clear();

                if (newHoveredElement != null) {
                    lastHoveredElements.addAll(newHoveredElement.getStructurePath());
                    for (var element : lastHoveredElements) {
                        element.addClass("__hovered__");
                    }
                }
            }

            lastHoveredElement = newHoveredElement;
            ui.rootElement.drawInBackground(guiContext);

            if (lastHoveredElement != null && tooltipTexts == null) {
                var element = lastHoveredElement;
                while (element != null) {
                    var event = UIEvent.create(UIEvents.HOVER_TOOLTIPS);
                    event.hasBubblePhase = false;
                    event.hasCapturePhase = false;
                    event.target = element;
                    UIEventDispatcher.dispatchDirectEvent(event, false);
                    if (event.hoverTooltips != null) {
                        setHoverTooltip(event.hoverTooltips.tooltipTexts(),
                                Optional.ofNullable(event.hoverTooltips.tooltipStack()).orElse(ItemStack.EMPTY),
                                event.hoverTooltips.tooltipFont(),
                                event.hoverTooltips.tooltipComponent());
                        break;
                    }
                    if (!element.getStyle().tooltips().isEmpty()) {
                        setHoverTooltip(element.getStyle().tooltips().asList(), ItemStack.EMPTY, null, null);
                        break;
                    }
                    element = element.getParent();
                }
            }

            guiContext.callPostRendering();

            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            if (screen instanceof AbstractContainerScreen<?> containerScreen && !containerScreen.getMenu().getCarried().isEmpty()) {
                return;
            }

            // Do not render tooltips if carried item is existing
            if (drawDrag && dragHandler.isDragging() && dragHandler.dragTexture != null) {
                dragHandler.dragTexture.draw(guiGraphics, (int) lastMouseX, (int) lastMouseY, lastMouseX + dragHandler.offsetX, lastMouseY + dragHandler.offsetY, dragHandler.width, dragHandler.height, partialTick);
            }

            if (drawTooltips && !dragHandler.isDragging() && tooltipTexts != null && !tooltipTexts.isEmpty()) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 200);
                DrawerHelper.drawTooltip(guiGraphics, (int) lastMouseX, (int) lastMouseY, tooltipTexts, tooltipStack, tooltipComponent, tooltipFont == null ? Minecraft.getInstance().font : tooltipFont);
                guiGraphics.pose().popPose();
            }

            if (debugMode) {
                renderDebugInfo(guiGraphics, mouseX, mouseY, partialTick);
            }

            guiGraphics.flush();
        }

        public void renderUISpacing(UIElement element, GuiGraphics graphics) {
            var posX = element.getPositionX();
            var posY = element.getPositionY();
            var sizeX = element.getSizeWidth();
            var sizeY = element.getSizeHeight();
            var marginTop = element.getMarginTop();
            var marginBottom = element.getMarginBottom();
            var marginLeft = element.getMarginLeft();
            var marginRight = element.getMarginRight();
            DrawerHelper.drawSolidRect(graphics, posX - marginLeft, posY - marginTop,
                    sizeX + marginLeft + marginRight, sizeY + marginTop + marginBottom, ColorPattern.T_ORANGE.color);
            DrawerHelper.drawSolidRect(graphics, posX, posY, sizeX, sizeY, 0x80ff0000);
            var paddingX = element.getPaddingX();
            var paddingY = element.getPaddingY();
            var paddingWidth = element.getPaddingWidth();
            var paddingHeight = element.getPaddingHeight();
            DrawerHelper.drawSolidRect(graphics, paddingX, paddingY, paddingWidth, paddingHeight, 0x8000ff00);
            var contentX = element.getContentX();
            var contentY = element.getContentY();
            var contentWidth = element.getContentWidth();
            var contentHeight = element.getContentHeight();
            DrawerHelper.drawSolidRect(graphics, contentX, contentY, contentWidth, contentHeight, 0x800000ff);
        }

        public void renderDebugInfo(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 500);

            var x = 2;
            var y = 2;
            var font = Minecraft.getInstance().font;
            // focus element
            if (focusedElement != null) {
                var posX = focusedElement.getPositionX();
                var posY = focusedElement.getPositionY();
                var sizeX = focusedElement.getSizeWidth();
                var sizeY = focusedElement.getSizeHeight();
                DrawerHelper.drawBorder(graphics, posX, posY, sizeX, sizeY, ColorPattern.PURPLE.color, -1);
            }
            // hover element
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

                renderUISpacing(hovered, graphics);

                ///  draw layout box
                // draw layout on the right
                var sw = 300;
                var sh = 300;
                var sx = screenWidth - sw - 2;
                var sy = 12;
                var dist = 30;

                drawLayoutBox(graphics, font, sx, sy, sw, sh, "margin", 0x80646669, new String[]{
                        String.valueOf(hovered.layoutNode.getLayoutMargin(YogaEdge.TOP)),
                        String.valueOf(hovered.layoutNode.getLayoutMargin(YogaEdge.BOTTOM)),
                        String.valueOf(hovered.layoutNode.getLayoutMargin(YogaEdge.LEFT)),
                        String.valueOf(hovered.layoutNode.getLayoutMargin(YogaEdge.RIGHT))
                });

                sx += dist;
                sy += dist;
                sw -= dist * 2;
                sh -= dist * 2;
                drawLayoutBox(graphics, font, sx, sy, sw, sh, "border", 0x80ff0000, new String[]{
                        String.valueOf(hovered.layoutNode.getLayoutBorder(YogaEdge.TOP)),
                        String.valueOf(hovered.layoutNode.getLayoutBorder(YogaEdge.BOTTOM)),
                        String.valueOf(hovered.layoutNode.getLayoutBorder(YogaEdge.LEFT)),
                        String.valueOf(hovered.layoutNode.getLayoutBorder(YogaEdge.RIGHT))
                });

                sx += dist;
                sy += dist;
                sw -= dist * 2;
                sh -= dist * 2;
                drawLayoutBox(graphics, font, sx, sy, sw, sh, "padding", 0x8000ff00, new String[]{
                        String.valueOf(hovered.layoutNode.getLayoutPadding(YogaEdge.TOP)),
                        String.valueOf(hovered.layoutNode.getLayoutPadding(YogaEdge.BOTTOM)),
                        String.valueOf(hovered.layoutNode.getLayoutPadding(YogaEdge.LEFT)),
                        String.valueOf(hovered.layoutNode.getLayoutPadding(YogaEdge.RIGHT)),
                });

                sx += dist;
                sy += dist;
                sw -= dist * 2;
                sh -= dist * 2;
                drawLayoutBox(graphics, font, sx, sy, sw, sh, "content", 0x800000ff, new String[]{
                        hovered.getContentWidth() + " x " + hovered.getContentHeight()
                });
            }

            // draw cursor
            DrawerHelper.drawSolidRect(graphics, 0, mouseY - 1, screenWidth, 1, 0xffff0000);
            DrawerHelper.drawSolidRect(graphics, mouseX - 1, 0, 1, screenHeight, 0xffff0000);
            graphics.drawString(font, "pos(%d, %d)".formatted(mouseX, mouseY), mouseX, Math.max(0, mouseY - 10), ColorPattern.YELLOW.color, true);
            graphics.pose().popPose();
        }

        private void drawLayoutBox(GuiGraphics graphics, Font font, int x, int y, int width, int height, String labels, int color, String[] value) {
            // draw layout box
            if (color != 0) {
                DrawerHelper.drawSolidRect(graphics, x, y, width, height, color);
            }
            // draw label
            if (!labels.isEmpty()) {
                graphics.drawString(font, labels, x, y, 0xFFFFFFFF, true);
            }
            // draw values (top, bottom, left, right)
            if (value.length == 4) {
                var topText = value[0].replace("undefined", "und").replace("NaN", "0");
                graphics.drawString(font, topText, x + (width - font.width(topText)) / 2, y, 0xFFFFFFFF, true);
                var bottomText = value[1].replace("undefined", "und").replace("NaN", "0");
                graphics.drawString(font, bottomText, x + (width - font.width(bottomText)) / 2, y + height - font.lineHeight, 0xFFFFFFFF, true);
                var leftText = value[2].replace("undefined", "und").replace("NaN", "0");
                graphics.drawString(font, leftText, x, y + (height - font.lineHeight) / 2, 0xFFFFFFFF, true);
                var rightText = value[3].replace("undefined", "und").replace("NaN", "0");
                graphics.drawString(font, rightText, x + width - font.width(rightText), y + (height - font.lineHeight) / 2, 0xFFFFFFFF, true);
            } else if (value.length == 1) {
                var centerText = value[0];
                graphics.drawString(font, centerText, x + (width - font.width(centerText)) / 2, y + (height - font.lineHeight) / 2, 0xFFFFFFFF, true);
            }

        }
    }

}
