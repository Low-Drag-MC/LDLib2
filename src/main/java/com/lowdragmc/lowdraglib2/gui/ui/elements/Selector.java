package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
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
import com.lowdragmc.lowdraglib2.gui.util.UISoundUtils;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.SkipPersistedValue;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.*;
import org.joml.Vector2f;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
@KJSBindings
@LDLRegister(name = "selector", group = "basic", registry = "ldlib2:ui_element")
public class Selector<T> extends BindableUIElement<T> {
    @Configurable(name = "SelectorStyle")
    public class SelectorStyle extends Style {
        private static final Property<?>[] PROPERTIES = new Property[] {
                PropertyRegistry.FOCUS_OVERLAY,
                PropertyRegistry.MAX_ITEM,
                PropertyRegistry.VIEW_HEIGHT,
                PropertyRegistry.SHOW_OVERLAY,
                PropertyRegistry.CLOSE_AFTER_SELECT,
        };

        protected SelectorStyle() {
            super(Selector.this);
            setDefault(PropertyRegistry.FOCUS_OVERLAY, Sprites.RECT_RD_T_SOLID);
        }

        public static void init() {
            PropertyRegistry.MAX_ITEM.addListener(Selector.SelectorStyle::onPropertyChanged);
            PropertyRegistry.VIEW_HEIGHT.addListener(Selector.SelectorStyle::onPropertyChanged);
            PropertyRegistry.SHOW_OVERLAY.addListener(Selector.SelectorStyle::onPropertyChanged);
        }

        private static <T> void onPropertyChanged(UIElement element, Property<T> property, @Nullable T oldValue, @Nullable T newValue) {
            if (element instanceof Selector<?> selector) {
                selector.onSelectorStyleChanged();
            }
        }

        @Override
        protected Property<?>[] getProperties() {
            return PROPERTIES;
        }

        public IGuiTexture focusOverlay() {
            return getValueSave(PropertyRegistry.FOCUS_OVERLAY);
        }

        public SelectorStyle focusOverlay(IGuiTexture texture) {
            set(PropertyRegistry.FOCUS_OVERLAY, texture);
            return this;
        }

        public int maxItemCount() {
            return getValueSave(PropertyRegistry.MAX_ITEM);
        }

        public SelectorStyle maxItemCount(int maxItemCount) {
            set(PropertyRegistry.MAX_ITEM, maxItemCount);
            return this;
        }

        public float scrollerViewHeight() {
            return getValueSave(PropertyRegistry.VIEW_HEIGHT);
        }

        public SelectorStyle scrollerViewHeight(float height) {
            set(PropertyRegistry.VIEW_HEIGHT, height);
            return this;
        }

        public boolean showOverlay() {
            return getValueSave(PropertyRegistry.SHOW_OVERLAY);
        }

        public SelectorStyle showOverlay(boolean showOverlay) {
            set(PropertyRegistry.SHOW_OVERLAY, showOverlay);
            return this;
        }

        public boolean closeAfterSelect() {
            return getValueSave(PropertyRegistry.CLOSE_AFTER_SELECT);
        }

        public SelectorStyle closeAfterSelect(boolean closeAfterSelect) {
            set(PropertyRegistry.CLOSE_AFTER_SELECT, closeAfterSelect);
            return this;
        }
    }

    public final UIElement display;
    public final UIElement preview;
    public final UIElement buttonIcon;
    public final UIElement dialog;
    public final UIElement listView;
    public final ScrollerView scrollerView;
    @Getter
    private final SelectorStyle selectorStyle = new SelectorStyle();
    @Getter
    private List<T> candidates = List.of();
    private UIElementProvider<T> candidateUIProvider = UIElementProvider.text(value -> Component.translatable(value == null ? "---" : value.toString()));
    @Getter
    @Nullable
    private T value = null;

    // editor support
    @Configurable(name = "EditorCandidates")
    private final List<String> editorCandidates = new ArrayList<>();
    @Configurable(name = "DefaultValue")
    private String defaultValue = "";

    // runtime
    protected final Map<T, Button> candidateButtons = new HashMap<>();

    public Selector() {
        getLayout().height(14);
        getStyle().backgroundTexture(Sprites.RECT_RD_LIGHT);
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        this.preview = new UIElement().layout(layout -> {
            layout.justifyContent(AlignContent.CENTER);
            layout.heightPercent(100);
            layout.flex(1);
        });

        this.buttonIcon = new UIElement();
        this.buttonIcon
                .layout(layout -> {
                    layout.width(14);
                    layout.height(14);
                    layout.marginLeft(2);
                })
                .style(style -> style.backgroundTexture(Icons.DOWN_ARROW_NO_BAR));
        this.display = new UIElement()
                .layout(layout -> {
                    layout.flexDirection(FlexDirection.ROW);
                    layout.alignItems(AlignItems.CENTER);
                    layout.paddingAll(2);
                    layout.paddingLeft(4);
                    layout.heightPercent(100);
                    layout.widthPercent(100);
                })
                .addChildren(preview, buttonIcon);

        this.dialog = new UIElement();
        this.dialog
                .layout(layout -> {
                    layout.heightAuto();
                    layout.positionType(TaffyPosition.ABSOLUTE);
                })
                .addChildren(listView = new UIElement().layout(layout -> layout.paddingAll(2)), scrollerView = new ScrollerView())
                .style(style -> style.zIndex(1).backgroundTexture(Sprites.RECT_DARK))
                .setEnforceFocus(e -> {
                    if (e.target == this.dialog && this.isSelfOrChildHover()) {
                        this.dialog.focus();
                        return;
                    }
                    hide();
                })
                .addEventListener(UIEvents.LAYOUT_CHANGED, e -> {
                    this.updateDialogPosition();
                    e.currentElement.adaptPositionToScreen();
                })
                .stopInteractionEventsPropagation();

        scrollerView.verticalScroller.headButton.setDisplay(false);
        scrollerView.verticalScroller.tailButton.setDisplay(false);
        scrollerView.horizontalScroller.headButton.setDisplay(false);
        scrollerView.horizontalScroller.tailButton.setDisplay(false);
        scrollerView.viewPort.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        scrollerView.viewPort.layout(layout -> layout.paddingAll(2));
        scrollerView.layout(layout -> layout.setFlexGrow(1));
        scrollerView.setDisplay(false);
        scrollerView.viewContainer.addEventListener(UIEvents.LAYOUT_CHANGED, this::onScrollViewLayoutChanged);
        addChildren(display);

        this.dialog.addClass("__selector_dialog__");
        this.preview.addClass("__selector_preview__");
        this.buttonIcon.addClass("__selector_button-icon__");
        this.listView.addClass("__selector_list-view__");
        this.scrollerView.addClass("__selector_scroller-view__");

        internalSetup();
        this.dialog.markAsInternal();
    }

    public Selector<T> setCandidates(List<T> candidates) {
        this.candidates = candidates;
        setupDialog();
        return this;
    }

    public Selector<T> setCandidateUIProvider(UIElementProvider<T> candidateUIProvider) {
        this.candidateUIProvider = candidateUIProvider;
        setupDialog();
        return this;
    }

    private void setupDialog() {
        candidateButtons.clear();
        listView.clearAllChildren();
        scrollerView.clearAllScrollViewChildren();
        if (candidates.size() <= selectorStyle.maxItemCount()) {
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
            scrollerView.layout(layout -> layout.height(selectorStyle.scrollerViewHeight()));
            for (T candidate : candidates) {
                scrollerView.addScrollViewChild(createItemUI(candidate));
            }
        }
        setSelected(this.value, false, true);
    }

    private UIElement createItemUI(T candidate) {
        var candidateUI = new UIElement().layout(layout -> layout.widthPercent(100));
        var overlayButton = new Button();
        overlayButton.buttonStyle(style -> style.baseTexture(IGuiTexture.EMPTY)
                        .hoverTexture(selectorStyle.showOverlay() ? ColorPattern.T_GRAY.rectTexture() : IGuiTexture.EMPTY)
                        .pressedTexture(selectorStyle.showOverlay() ? ColorPattern.T_GRAY.rectTexture() : IGuiTexture.EMPTY))
                .setOnClick(e -> {
                    setSelected(candidate);
                    if (selectorStyle.closeAfterSelect()) {
                        hide();
                    }
                })
                .noText()
                .layout(layout -> {
                    layout.positionType(TaffyPosition.ABSOLUTE);
                    layout.heightPercent(100);
                    layout.widthPercent(100);
                })
                .setId("selector#overlayButton");
        candidateUI.addChild(candidateUIProvider.apply(candidate).addChild(overlayButton));
        candidateButtons.put(candidate, overlayButton);
        return candidateUI;
    }

    public Selector<T> setSelected(T value) {
        return setSelected(value, true);
    }

    public Selector<T> setSelected(T value, boolean notify) {
        return setSelected(value, notify, false);
    }

    private Selector<T> setSelected(@Nullable T value, boolean notify, boolean force) {
        if (!force && this.value == value) return this;
        return setValue(value, notify);
    }

    @Override
    public Selector<T> setValue(@Nullable T value, boolean notify) {
        // update overlay button style
        var currentValue = candidateButtons.get(this.value);
        if (currentValue != null) {
            currentValue.buttonStyle(style -> style.baseTexture(IGuiTexture.EMPTY));
        }
        this.value = value;
        var button = candidateButtons.get(value);
        if (button != null) {
            button.buttonStyle(style -> style.baseTexture(selectorStyle.showOverlay() ? ColorPattern.T_GRAY.rectTexture() : IGuiTexture.EMPTY));
        }
        // update preview
        this.preview.clearAllChildren();
        var candidateUI = candidateUIProvider.apply(value);
        this.preview.addChild(candidateUI);

        // notify
        if (notify) {
            notifyListeners();
        }
        return this;
    }

    public Selector<T> setOnValueChanged(Consumer<T> onValueChanged) {
        registerValueListener(onValueChanged);
        return this;
    }

    ///  events
    protected void onMouseDown(UIEvent event) {
        if (event.button == 0) {
            if (isOpen()) {
                hide();
            } else {
                show();
            }
            UISoundUtils.playButtonClickSound();
        }
    }

    protected void onScrollViewLayoutChanged(UIEvent event) {

    }

    public Selector<T> selectorStyle(Consumer<SelectorStyle> style) {
        style.accept(getSelectorStyle());
        return this;
    }

    protected void onSelectorStyleChanged() {
        setupDialog();
    }

    /// Logic
    public boolean isOpen() {
        return this.dialog.getParent() != null;
    }

    protected void updateDialogPosition() {
        var mui = getModularUI();
        if (mui != null) {
            var root = mui.ui.rootElement;
            var worldPos = this.localToWorld(new Vector2f(getPositionX(), getPositionY() + getSizeHeight()));
            var pos = root.worldToLocalLayoutOffset(worldPos);
            this.dialog.layout(layout -> {
                layout.left(pos.x);
                layout.top(pos.y);
                layout.width(Math.max(this.getSizeWidth(), 50));
            });
        }
    }

    public void show() {
        if (this.isOpen()) {
            return;
        }
        var mui = getModularUI();
        if (mui != null) {
            var root = mui.ui.rootElement;
            root.addChild(dialog);
            this.updateDialogPosition();
            this.dialog.focus();
        }
    }

    public void hide() {
        var parent = this.dialog.getParent();
        if (parent != null) {
            this.dialog.blur();
            parent.removeChild(this.dialog);
        }
    }

    /// rendering
    @Override
    public void drawBackgroundOverlay(GUIContext guiContext) {
        if (isSelfOrChildHover() || isFocused()) {
            guiContext.drawTexture(getSelectorStyle().focusOverlay(), getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
        }
        super.drawBackgroundOverlay(guiContext);
    }

    /// Editor + Xml
    @Override
    public void beforeDeserialize() {
        super.beforeDeserialize();
        this.editorCandidates.clear();
        this.defaultValue = "";
    }

    @SkipPersistedValue(field = "editorCandidates")
    private boolean skipEditorCandidates(List<String> editorCandidates) {
        return editorCandidates.isEmpty();
    }

    @SkipPersistedValue(field = "defaultValue")
    private boolean skipDefaultValue(String defaultValue) {
        return defaultValue.isEmpty();
    }

    @Override
    public void afterDeserialize() {
        super.afterDeserialize();
        if (!editorCandidates.isEmpty()) {
            setCandidates((List) editorCandidates);
        }
        if (!defaultValue.isEmpty()) {
            setValue((T) defaultValue, false);
        }
    }

    @ConfigSetter(field = "defaultValue")
    private void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        setValue((T) defaultValue, false);
    }

    @Override
    public void loadXml(Element element) {
        super.loadXml(element);
        // candidates
        var nodes = element.getChildNodes();
        var candidates = new ArrayList<String>();
        for (int i = 0; i < nodes.getLength(); i++) {
            var node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && node instanceof Element childElement &&
                    childElement.getTagName().equals("candidate")) {
                candidates.add(XmlUtils.getContent(childElement, true));
            }
        }
        if (!candidates.isEmpty()) {
            setCandidates((List)candidates);
        }
        // default value
        if (element.hasAttribute("default-value")) {
            setDefaultValue(element.getAttribute("default-value"));
        }
    }
}
