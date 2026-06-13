package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.gui.ui.style.HierarchicalStyleMatcher;
import com.lowdragmc.lowdraglib2.utils.animation.AnimationEngine;
import com.lowdragmc.lowdraglib2.gui.sync.UISyncManager;
import com.lowdragmc.lowdraglib2.gui.ui.event.*;
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutProperties;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleEngine;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.math.Size;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;

import org.jetbrains.annotations.Nullable;
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
    @Getter
    private TaffyTree taffyTree;
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
    private float layoutWidth = Float.NaN, layoutHeight = Float.NaN;
    @Getter
    private float leftPos, topPos, width, height;
    @Getter
    private final DragHandler dragHandler = new DragHandler(this);
    @Getter
    private long tickCounter = 0;
    @Getter
    Matrix3x2f lastDrawPose = new Matrix3x2f();
    @Nullable
    Object clientState;
    // Element registry for fast retrieval
    private final ObjectArrayList<UIElement> elements = new ObjectArrayList<>();
    private final Map<String, List<UIElement>> elementsById = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<UIElement>> elementsByType = new ConcurrentHashMap<>();
    private final Map<NodeId, UIElement> elementByNode = new HashMap<>();
    private final Set<NodeId> nodesWithNewLayout = new HashSet<>();
    private final Set<NodeId> nodesWithNewGeometry = new HashSet<>();

    // UI state
    @Getter @Setter
    boolean tickWhileRending;
    @Getter @Setter
    boolean focused;
    @Getter @Setter
    boolean drawTooltips = true;
    @Getter @Setter
    boolean drawDrag = true;
    @Nullable
    @Getter
    UIElement lastHoveredElement;
    @Getter
    final List<UIElement> lastHoveredElements = new ArrayList<>();
    @Getter
    UIElement lastMouseDownElement;
    @Getter
    UIElement lastMouseMoveElement;
    @Getter
    UIElement lastMouseClickElement;
    @Getter
    UIElement lastMouseDragElement;
    @Getter
    int lastMouseDownButton = -1, lastMouseClickButton = -1;
    @Getter
    int lastPressedKeyCode = - 1, lastPressedScanCode = -1, lastPressedModifiers = -1;
    @Getter
    long lastMouseClickTime;
    @Getter
    float lastMouseX, lastMouseY, lastMouseDownX, lastMouseDownY;
    @Getter @Nullable
    UIElement focusedElement = null;

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
        this.taffyTree = new TaffyTree();
        this.taffyTree.disableRounding();
        this.taffyTree.setLayoutChangeListener((nodeId, oldLayout, newLayout) -> {
            nodesWithNewLayout.add(nodeId);
            if (Objects.equals(oldLayout, newLayout)) return;
            nodesWithNewGeometry.add(nodeId);
        });
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

    /**
     * Add an element to the registry for fast retrieval.
     * This method is automatically called when elements are added to the UI tree.
     * @param element the element to add to the registry
     */
    public void registerElement(@Nullable UIElement element) {
        if (element == null) return;
        elements.add(element);

        // Add Layout Node
        element.nodeId = taffyTree.newLeaf(element.getTaffyStyle().style);
        elementByNode.put(element.nodeId, element);
        if (element.getParent() != null) {
            var parentID = element.getParent().nodeId;
            // parent may belong to another tree is invalid
            if (taffyTree.containsNode(parentID)) {
                taffyTree.insertChildAtIndex(parentID, element.getSiblingIndex(), element.nodeId);
            }
        }

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

        // Remove Layout Node
        elementByNode.remove(element.nodeId);
        if (element.nodeId != null) {
            if (element.getParent() != null) {
                var parentID = element.getParent().nodeId;
                // parent may already belong to other tree.
                if (parentID != null && taffyTree.containsNode(parentID)) {
                    taffyTree.removeChild(parentID, element.nodeId);
                }
            }
            taffyTree.remove(element.nodeId);
            element.nodeId = null;
        }

        elements.remove(element);
    }

    public List<UIElement> getAllElements() {
        return ObjectLists.unmodifiable(elements);
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

    public void init(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        if (ui.dynamicSize != null) {
            var size = ui.dynamicSize.apply(Size.of(screenWidth, screenHeight));
            ui.rootElement.layout(layout -> {
                layout.width(size.getWidth());
                layout.height(size.getHeight());
            });
        }
        var isRelative = Optional.ofNullable(ui.rootElement.getStyleBag().computeCandidate(LayoutProperties.POSITION))
                .orElse(TaffyPosition.RELATIVE) != TaffyPosition.ABSOLUTE;
        var width = Optional.ofNullable(ui.rootElement.getStyleBag().computeCandidate(LayoutProperties.WIDTH))
                .orElseGet(TaffyDimension::auto);
        var height = Optional.ofNullable(ui.rootElement.getStyleBag().computeCandidate(LayoutProperties.HEIGHT))
                .orElseGet(TaffyDimension::auto);
        this.width = switch (width.getType()) {
            case PERCENT -> width.getValue() * screenWidth;
            case LENGTH -> width.getValue();
            default -> 0;
        };
        this.height = switch (height.getType()) {
            case PERCENT -> height.getValue() * screenHeight;
            case LENGTH -> height.getValue();
            default -> 0;
        };

        if (width.isPercent()) {
            this.layoutWidth = screenWidth;
        } else {
            this.layoutWidth = Float.NaN;
        }
        if (height.isPercent()) {
            this.layoutHeight = screenHeight;
        } else {
            this.layoutHeight = Float.NaN;
        }

        // we'd better align it to the integer position to avoid a floating point error
        if (this.menu != null && this.ui.rootElement.getModularUI() == this) {
            // have already set
        } else {
            this.ui.rootElement._setModularUIInternal(this);
        }
        this.ui.rootElement.initScreen(screenWidth, screenHeight);
        this.ui.rootElement.markTaffyStyleDirty();
        calculateStyleAndLayout();

        // if dimension is auto, update real sizes after layout calculation
        if (width.isAuto()) {
            this.width = ui.rootElement.getSizeWidth();
            this.leftPos = isRelative ? (screenWidth - this.width) / 2 : ui.rootElement.getTaffyLayout().location().x;
        } else {
            this.leftPos = isRelative ? (screenWidth - this.width) / 2 : ui.rootElement.getTaffyLayout().location().x;
        }
        if (height.isAuto()) {
            this.height = ui.rootElement.getSizeHeight();
            this.topPos = isRelative ? (screenHeight - this.height) / 2 : ui.rootElement.getTaffyLayout().location().y;
        } else {
            this.topPos = isRelative ? (screenHeight - this.height) / 2 : ui.rootElement.getTaffyLayout().location().y;
        }

        this.leftPos = Math.round(this.leftPos);
        this.topPos = Math.round(this.topPos);
        this.ui.rootElement.clearLayoutCache();
    }

    void calculateStyleAndLayout() {
        int dirtyCount = 0;
        while (styleEngine.requireCalculate() || taffyTree.isDirty(ui.rootElement.nodeId)) {
            dirtyCount++;

            // calculate style
            while (styleEngine.requireCalculate()) {
                styleEngine.calculateStyle();
            }

            if (taffyTree.isDirty(ui.rootElement.nodeId)) {
                taffyTree.computeLayout(ui.rootElement.nodeId, new TaffySize<>(
                        Float.isNaN(layoutWidth) ? AvailableSpace.MAX_CONTENT : AvailableSpace.definite(layoutWidth),
                        Float.isNaN(layoutHeight) ? AvailableSpace.MAX_CONTENT : AvailableSpace.definite(layoutHeight)
                ));

                for (var nodeId : nodesWithNewLayout) {
                    var element = elementByNode.get(nodeId);
                    if (element != null) {
                        element.onLayoutChanged(nodesWithNewGeometry.contains(nodeId));
                    }
                }
                nodesWithNewLayout.clear();
                nodesWithNewGeometry.clear();

            }

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
        styleEngine.dispose();
    }

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
        }

    }

    public void clearFocus() {
        requestFocus(null);
    }

    // Tooltip/widget/rendering methods are in ModularUIClientAccess and ModularUIWidget (extracted from here)

}
