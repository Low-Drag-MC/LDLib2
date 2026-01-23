package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEvent;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEventBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import com.lowdragmc.lowdraglib2.utils.search.ISearch;
import com.lowdragmc.lowdraglib2.utils.search.SearchEngine;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.*;
import org.appliedenergistics.yoga.style.StyleSizeLength;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
@KJSBindings
@LDLRegister(name = "search-component", group = "basic", registry = "ldlib2:ui_element")
public class SearchComponent<T> extends BindableUIElement<T> {
    @Configurable(name = "SearchStyle")
    public class SearchStyle extends Style {
        private static final Property<?>[] PROPERTIES = new Property[] {
                PropertyRegistry.FOCUS_OVERLAY,
                PropertyRegistry.MAX_ITEM,
                PropertyRegistry.VIEW_HEIGHT,
                PropertyRegistry.SHOW_OVERLAY,
                PropertyRegistry.CLOSE_AFTER_SELECT,
        };

        protected SearchStyle() {
            super(SearchComponent.this);
            setDefault(PropertyRegistry.FOCUS_OVERLAY, Sprites.RECT_RD_T_SOLID);
        }

        public static void init() {
            PropertyRegistry.MAX_ITEM.addListener(SearchComponent.SearchStyle::onPropertyChanged);
            PropertyRegistry.VIEW_HEIGHT.addListener(SearchComponent.SearchStyle::onPropertyChanged);
            PropertyRegistry.SHOW_OVERLAY.addListener(SearchComponent.SearchStyle::onPropertyChanged);
        }

        private static <T> void onPropertyChanged(UIElement element, Property<T> property, @Nullable T oldValue, @Nullable T newValue) {
            if (element instanceof SearchComponent<?> searchComponent) {
                searchComponent.onSearchStyleChanged();
            }
        }

        @Override
        protected Property<?>[] getProperties() {
            return PROPERTIES;
        }

        public IGuiTexture focusOverlay() {
            return getValueSave(PropertyRegistry.FOCUS_OVERLAY);
        }

        public SearchStyle focusOverlay(IGuiTexture texture) {
            set(PropertyRegistry.FOCUS_OVERLAY, texture);
            return this;
        }

        public int maxItemCount() {
            return getValueSave(PropertyRegistry.MAX_ITEM);
        }

        public SearchStyle maxItemCount(int maxItemCount) {
            set(PropertyRegistry.MAX_ITEM, maxItemCount);
            return this;
        }

        public float scrollerViewHeight() {
            return getValueSave(PropertyRegistry.VIEW_HEIGHT);
        }

        public SearchStyle scrollerViewHeight(float height) {
            set(PropertyRegistry.VIEW_HEIGHT, height);
            return this;
        }

        public boolean showOverlay() {
            return getValueSave(PropertyRegistry.SHOW_OVERLAY);
        }

        public SearchStyle showOverlay(boolean showOverlay) {
            set(PropertyRegistry.SHOW_OVERLAY, showOverlay);
            return this;
        }

        public boolean closeAfterSelect() {
            return getValueSave(PropertyRegistry.CLOSE_AFTER_SELECT);
        }

        public SearchStyle closeAfterSelect(boolean closeAfterSelect) {
            set(PropertyRegistry.CLOSE_AFTER_SELECT, closeAfterSelect);
            return this;
        }
    }

    public final TextField textField;
    public final UIElement preview;
    public final UIElement dialog;
    public final UIElement listView;
    public final ScrollerView scrollerView;
    @Getter
    private final SearchStyle searchStyle = new SearchStyle();
    private UIElementProvider<T> candidateUIProvider = UIElementProvider.text(value -> value == null ?
            Component.translatable("text_field.empty").withColor(ColorPattern.LIGHT_GRAY.color) :
            Component.translatable(value.toString()));
    @Getter
    private ISearchUI<T> searchUI = ISearchUI.empty();
    @Getter
    @Nullable
    private T value = null;
    @Getter
    private boolean searchOnServer;
    // editor support

    // runtime
    private SearchEngine<T> searchEngine;
    private final ConcurrentLinkedQueue<T> candidates = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isCandidatesDirty = new AtomicBoolean(false);
    protected final Map<T, Button> candidateButtons = new HashMap<>();
    @Nullable
    protected RPCEvent searchEvent;

    public SearchComponent(ISearchUI<T> searchUI) {
        this();
        setSearchUI(searchUI);
    }

    public SearchComponent() {
        getLayout().setHeight(14);
        getStyle().backgroundTexture(Sprites.RECT_RD_SOLID);
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);

        this.textField = new TextField();
        this.dialog = new UIElement();
        this.preview = new UIElement().layout(layout -> {
            layout.justifyContent(AlignContent.CENTER);
            layout.setHeightPercent(100);
            layout.setFlex(1);
            layout.setPadding(YogaEdge.ALL, 2);
        });

        this.textField.addClass("__search-component_text-field__");
        this.dialog.addClass("__search-component_dialog__");
        this.preview.addClass("__search-component_preview__");

        textField.layout(layout -> {
            layout.setHeightPercent(100);
            layout.setFlex(1);
        });
        textField.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        textField.textFieldStyle(textFieldStyle -> textFieldStyle.focusOverlay(IGuiTexture.EMPTY));
        textField.setDisplay(false);
        textField.addEventListener(UIEvents.FOCUS, event -> show());
        textField.addEventListener(UIEvents.BLUR, event -> {
            var mui = getModularUI();
            if (mui != null && dialog.isAncestorOf(mui.getLastHoveredElement())) return;
            hide();
        });
        textField.setTextResponder(this::onSearchWordChanged);

        this.dialog
                .setId("selector#dialog")
                .layout(layout -> {
                    layout.setHeight(StyleSizeLength.AUTO);
                    layout.positionType(TaffyPosition.ABSOLUTE);
                })
                .addChildren(listView = new UIElement().layout(layout -> layout.setPadding(YogaEdge.ALL, 2)), scrollerView = new ScrollerView())
                .style(style -> style.zIndex(1).backgroundTexture(Sprites.RECT_DARK))
                .addEventListener(UIEvents.LAYOUT_CHANGED, e -> {
                    var mui = getModularUI();
                    if (mui != null) {
                        var root = mui.ui.rootElement;
                        e.currentElement.layout(layout -> {
                            var x = this.getPositionX();
                            var y = this.getPositionY();
                            layout.setPosition(YogaEdge.LEFT, x - root.getLayoutX());
                            layout.setPosition(YogaEdge.TOP, y - root.getLayoutY() + this.getSizeHeight());
                            layout.setWidth(this.getSizeWidth());
                        });
                    }
                    e.currentElement.adaptPositionToScreen();
                })
                .stopInteractionEventsPropagation();

        listView.addClass("__search-component_list-view__");
        scrollerView.addClass("__search-component_scroller-view__");

        scrollerView.verticalScroller.headButton.setDisplay(false);
        scrollerView.verticalScroller.tailButton.setDisplay(false);
        scrollerView.horizontalScroller.headButton.setDisplay(false);
        scrollerView.horizontalScroller.tailButton.setDisplay(false);
        scrollerView.viewPort.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        scrollerView.viewPort.layout(layout -> layout.setPadding(YogaEdge.ALL, 2));
        scrollerView.layout(layout -> layout.setFlexGrow(1));
        scrollerView.setDisplay(false);
        scrollerView.viewContainer.addEventListener(UIEvents.LAYOUT_CHANGED, this::onScrollViewLayoutChanged);
        addChildren(preview, textField);

        searchEngine = new SearchEngine<>(searchUI, this::onResultFound);
        internalSetup();
        dialog.markAsInternal();
    }

    protected void onMouseDown(UIEvent event) {
        if (event.button == 0) {
            textField.focus();
        }
    }

    public SearchComponent<T> setCandidateUIProvider(UIElementProvider<T> candidateUIProvider) {
        this.candidateUIProvider = candidateUIProvider;
        refreshDialog();
        setSelected(this.value, false, true);
        return this;
    }

    public SearchComponent<T> setSearchUI(ISearchUI<T> searchUI) {
        this.searchUI = searchUI;
        this.searchEngine.dispose();
        this.searchEngine = new SearchEngine<>(searchUI, this::onResultFound);
        return this;
    }

    private void onResultFound(T t) {
        candidates.add(t);
        isCandidatesDirty.set(true);
    }

    public SearchComponent<T> setSearchOnServer(Class<T[]> clazz) {
        this.searchOnServer = true;
        this.searchEvent = RPCEventBuilder.simple(String.class, clazz, word -> {
            var result = new ArrayList<T>();
            searchUI.search(word, result::add);
            return result.toArray((T[]) Array.newInstance(clazz.getComponentType(), result.size()));
        });
        addRPCEvent(searchEvent);
        return this;
    }

    @Override
    public void screenTick() {
        super.screenTick();
        updateCandidatesUI();
    }

    protected void onSearchWordChanged(String word) {
        candidates.clear();
        isCandidatesDirty.set(true);
        if (searchOnServer) {
            if (searchEvent != null) {
                this.<T[]>sendEvent(searchEvent, values -> {
                    candidates.addAll(Arrays.asList(values));
                    isCandidatesDirty.set(true);
                }, word);
            }
        } else {
            searchEngine.searchWord(word);
        }
    }

    protected void updateCandidatesUI() {
        if (isCandidatesDirty.compareAndSet(true, false)) {
            refreshDialog();
        }
    }

    protected void refreshDialog() {
        var candidates = new ArrayList<>(this.candidates);
        candidateButtons.clear();
        listView.clearAllChildren();
        scrollerView.clearAllScrollViewChildren();
        if (candidates.size() <= searchStyle.maxItemCount()) {
            // list view
            scrollerView.setDisplay(false);
            listView.setDisplay(true);
            for (T candidate : candidates) {
                listView.addChild(createItemUI(candidate));
            }
        } else {
            // scroller view
            listView.setDisplay(false);
            scrollerView.setDisplay(true);
            scrollerView.layout(layout -> layout.setHeight(searchStyle.scrollerViewHeight()));
            for (T candidate : candidates) {
                scrollerView.addScrollViewChild(createItemUI(candidate));
            }
        }
    }

    private UIElement createItemUI(T candidate) {
        var candidateUI = new UIElement().layout(layout -> layout.setWidthPercent(100));
        var overlayButton = new Button();
        overlayButton.buttonStyle(style -> style.baseTexture(IGuiTexture.EMPTY)
                        .hoverTexture(searchStyle.showOverlay() ? ColorPattern.T_GRAY.rectTexture() : IGuiTexture.EMPTY)
                        .pressedTexture(searchStyle.showOverlay() ? ColorPattern.T_GRAY.rectTexture() : IGuiTexture.EMPTY))
                .setOnClick(e -> {
                    setSelected(candidate);
                    if (searchStyle.closeAfterSelect()) {
                        hide();
                    }
                })
                .noText()
                .layout(layout -> {
                    layout.positionType(TaffyPosition.ABSOLUTE);
                    layout.setHeightPercent(100);
                    layout.setWidthPercent(100);
                })
                .setId("selector#overlayButton");
        candidateUI.addChild(candidateUIProvider.apply(candidate).addChild(overlayButton));
        candidateButtons.put(candidate, overlayButton);
        return candidateUI;
    }

    public SearchComponent<T> setSelected(T value) {
        return setSelected(value, true);
    }

    public SearchComponent<T> setSelected(T value, boolean notify) {
        return setSelected(value, notify, false);
    }

    private SearchComponent<T> setSelected(@Nullable T value, boolean notify, boolean force) {
        if (!force && this.value == value) return this;
        return setValue(value, notify);
    }

    @Override
    public SearchComponent<T> setValue(@Nullable T value, boolean notify) {
        // update overlay button style
        var currentValue = candidateButtons.get(this.value);
        if (currentValue != null) {
            currentValue.buttonStyle(style -> style.baseTexture(IGuiTexture.EMPTY));
        }
        this.value = value;
        var button = candidateButtons.get(value);
        if (button != null) {
            button.buttonStyle(style -> style.baseTexture(searchStyle.showOverlay() ? ColorPattern.T_GRAY.rectTexture() : IGuiTexture.EMPTY));
        }

        // update preview
        this.preview.clearAllChildren();
        if (value != null) {
            var candidateUI = candidateUIProvider.apply(value);
            this.preview.addChild(candidateUI);
        }
        textField.setText(value == null ? "" : searchUI.resultText(value));

        // notify
        if (notify) {
            notifyListeners();
            searchUI.onResultSelected(value);
        }
        return this;
    }

    public SearchComponent<T> setOnValueChanged(Consumer<T> onValueChanged) {
        registerValueListener(onValueChanged);
        return this;
    }


    protected void onScrollViewLayoutChanged(UIEvent event) {

    }

    public SearchComponent<T> searchStyle(Consumer<SearchStyle> style) {
        style.accept(searchStyle);
        return this;
    }

    protected void onSearchStyleChanged() {
        refreshDialog();
        setSelected(this.value, false, true);
    }

    /// Logic
    public boolean isOpen() {
        return this.dialog.getParent() != null;
    }

    public void show() {
        if (this.isOpen()) {
            return;
        }
        var mui = getModularUI();
        if (mui != null) {
            var root = mui.ui.rootElement;
            root.addChild(dialog.layout(layout -> {
                var x = this.getPositionX();
                var y = this.getPositionY();
                layout.setPosition(YogaEdge.LEFT, x - root.getLayoutX());
                layout.setPosition(YogaEdge.TOP, y - root.getLayoutY() + this.getSizeHeight());
                layout.setWidth(this.getSizeWidth());
            }));
        }
        preview.setDisplay(false);
        textField.setDisplay(true);
    }

    public void hide() {
        var parent = this.dialog.getParent();
        if (parent != null) {
            this.dialog.blur();
            parent.removeChild(this.dialog);
        }
        textField.setText(value == null ? "" : searchUI.resultText(value));
        preview.setDisplay(true);
        textField.setDisplay(false);
    }

    /// rendering
    @Override
    public void drawBackgroundOverlay(GUIContext guiContext) {
        if (isSelfOrChildHover() || textField.isFocused()) {
            guiContext.drawTexture(getSearchStyle().focusOverlay(), getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
        }
        super.drawBackgroundOverlay(guiContext);
    }

    public interface ISearchUI<T> extends ISearch<T> {
        class Empty<T> implements ISearchUI<T> {
            @Override
            public String resultText(T value) {
                return value.toString();
            }

            @Override
            public void onResultSelected(@Nullable T value) {}

            @Override
            public void search(String word, IResultHandler<T> find) {}
        }

        Empty EMPTY = new Empty<>();

        static <T> ISearchUI<T> empty() {
            return EMPTY;
        }

        /**
         * Displays the result representation of a given value of type {@code T}.
         * This method is used to convert an object into its string representation
         * for display purposes in the UI.
         *
         * @param value the object of type {@code T} whose result representation is to be displayed.
         * @return a {@code String} representation of the given object.
         */
        String resultText(T value);

        /**
         * Invoked when a result is selected from the search or selection process.
         *
         * @param value the selected result of type {@code T}, or {@code null} if no result is selected*/
        void onResultSelected(@Nullable T value);
    }

    /// Editor Support
    // TODO add supports for editor quick actions
//    public enum EditorMode {
//        BLOCK,
//        ITEM,
//        FLUID,
//        ITEM_STACK,
//        FLUID_STACK,
//        BIOME,
//        ENTITY_TYPE,
//        POTION,
//    }
}
