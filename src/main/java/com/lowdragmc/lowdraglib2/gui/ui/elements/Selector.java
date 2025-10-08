package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.UIStyleRegistries;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.*;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.gui.widget.Widget;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.*;
import org.appliedenergistics.yoga.style.StyleSizeLength;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
@LDLRegister(name = "selector", registry = "ldlib2:ui_element")
public class Selector<T> extends BindableUIElement<T> {
    @Accessors(chain = true, fluent = true)
    public static class SelectorStyle extends Style {
        @Getter @Setter
        @Configurable(name = "focusOverlay")
        private IGuiTexture focusOverlay = Sprites.RECT_RD_T_SOLID;
        @Getter @Setter
        @Configurable(name = "maxItemCount")
        @ConfigNumber(range = {1, Integer.MAX_VALUE})
        private int maxItemCount = 5; // if more than this, use scroller view.
        @Getter @Setter
        @Configurable(name = "scrollerViewHeight", tips = "scrollerViewHeight.tips")
        @ConfigNumber(range = {0, Float.MAX_VALUE})
        private float scrollerViewHeight = 50;
        @Getter @Setter
        @Configurable(name = "showOverlay")
        private boolean showOverlay = true;
        @Getter @Setter
        @Configurable(name = "closeAfterSelect")
        private boolean closeAfterSelect = true;

        public SelectorStyle(UIElement holder) {
            super(holder);
        }

        @Override
        public void applyStyles(Map<String, StyleValue<?>> values) {
            super.applyStyles(values);

            UIStyleRegistries.FOCUS_OVERLAY.parse(values).ifPresent(this::focusOverlay);
            UIStyleRegistries.MAX_ITEM.parse(values).ifPresent(this::maxItemCount);
            UIStyleRegistries.VIEW_HEIGHT.parse(values).ifPresent(this::scrollerViewHeight);
            UIStyleRegistries.SHOW_OVERLAY.parse(values).ifPresent(this::showOverlay);
            UIStyleRegistries.CLOSE_AFTER_SELECT.parse(values).ifPresent(this::closeAfterSelect);

            if (holder instanceof Selector<?> selector) {
                selector.onSelectorStyleChanged();
            }
        }
    }
    public final UIElement display;
    public final UIElement preview;
    public final UIElement buttonIcon;
    public final UIElement dialog;
    public final UIElement listView;
    public final ScrollerView scrollerView;
    @Getter
    @Configurable(name = "selectorStyle", subConfigurable = true)
    private final SelectorStyle selectorStyle = new SelectorStyle(this);
    @Getter
    private List<T> candidates = List.of();
    private UIElementProvider<T> candidateUIProvider = UIElementProvider.text(value -> Component.translatable(value == null ? "---" : value.toString()));
    @Getter
    @Nullable
    private T value = null;

    // runtime
    protected final Map<T, Button> candidateButtons = new HashMap<>();

    public Selector() {
        getLayout().setHeight(14);
        getStyle().backgroundTexture(Sprites.RECT_RD_LIGHT);
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        this.preview = new UIElement().layout(layout -> {
            layout.setJustifyContent(YogaJustify.CENTER);
            layout.setHeightPercent(100);
            layout.setFlex(1);
        });

        this.buttonIcon = new UIElement();
        this.buttonIcon
                .layout(layout -> {
                    layout.setWidth(14);
                    layout.setHeight(14);
                    layout.setMargin(YogaEdge.LEFT, 2);
                })
                .style(style -> style.backgroundTexture(Icons.DOWN_ARROW_NO_BAR));
        this.display = new UIElement()
                .layout(layout -> {
                    layout.setFlexDirection(YogaFlexDirection.ROW);
                    layout.setAlignItems(YogaAlign.CENTER);
                    layout.setPadding(YogaEdge.ALL, 2);
                    layout.setPadding(YogaEdge.LEFT, 4);
                    layout.setHeightPercent(100);
                    layout.setWidthPercent(100);
                })
                .addChildren(preview, buttonIcon);

        this.dialog = new UIElement();
        this.dialog
                .setId("selector#dialog")
                .layout(layout -> {
                    layout.setHeight(StyleSizeLength.AUTO);
                    layout.setPositionType(YogaPositionType.ABSOLUTE);
                })
                .addChildren(listView = new UIElement().layout(layout -> layout.setPadding(YogaEdge.ALL, 2)), scrollerView = new ScrollerView())
                .style(style -> style.zIndex(1).backgroundTexture(Sprites.RECT_DARK))
                .setEnforceFocus(e -> {
                    if (e.target == this.dialog && this.isChildHover()) {
                        this.dialog.focus();
                        return;
                    }
                    hide();
                })
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

        scrollerView.verticalScroller.headButton.setDisplay(YogaDisplay.NONE);
        scrollerView.verticalScroller.tailButton.setDisplay(YogaDisplay.NONE);
        scrollerView.horizontalScroller.headButton.setDisplay(YogaDisplay.NONE);
        scrollerView.horizontalScroller.tailButton.setDisplay(YogaDisplay.NONE);
        scrollerView.viewPort.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        scrollerView.viewPort.layout(layout -> layout.setPadding(YogaEdge.ALL, 2));
        scrollerView.layout(layout -> layout.setFlexGrow(1));
        scrollerView.setDisplay(YogaDisplay.NONE);
        scrollerView.viewContainer.addEventListener(UIEvents.LAYOUT_CHANGED, this::onScrollViewLayoutChanged);
        addChildren(display);
        markAllChildrenAsInternal();
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
            scrollerView.setDisplay(YogaDisplay.NONE);
            listView.setDisplay(YogaDisplay.FLEX);
            for (T candidate : candidates) {
                listView.addChild(createItemUI(candidate));
            }
        } else {
            // scroller view
            listView.setDisplay(YogaDisplay.NONE);
            scrollerView.setDisplay(YogaDisplay.FLEX);
            scrollerView.layout(layout -> layout.setHeight(selectorStyle.scrollerViewHeight()));
            for (T candidate : candidates) {
                scrollerView.addScrollViewChild(createItemUI(candidate));
            }
        }
        setSelected(this.value, false, true);
    }

    private UIElement createItemUI(T candidate) {
        var candidateUI = new UIElement().layout(layout -> layout.setWidthPercent(100));
        var overlayButton = new Button();
        overlayButton.buttonStyle(style -> style.defaultTexture(IGuiTexture.EMPTY)
                        .hoverTexture(selectorStyle.showOverlay ? ColorPattern.T_GRAY.rectTexture() : IGuiTexture.EMPTY)
                        .pressedTexture(selectorStyle.showOverlay ? ColorPattern.T_GRAY.rectTexture() : IGuiTexture.EMPTY))
                .setOnClick(e -> {
                    setSelected(candidate);
                    if (selectorStyle.closeAfterSelect) {
                        hide();
                    }
                })
                .noText()
                .layout(layout -> {
                    layout.setPositionType(YogaPositionType.ABSOLUTE);
                    layout.setHeightPercent(100);
                    layout.setWidthPercent(100);
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
            currentValue.buttonStyle(style -> style.defaultTexture(IGuiTexture.EMPTY));
        }
        this.value = value;
        var button = candidateButtons.get(value);
        if (button != null) {
            button.buttonStyle(style -> style.defaultTexture(selectorStyle.showOverlay ? ColorPattern.T_GRAY.rectTexture() : IGuiTexture.EMPTY));
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
            Widget.playButtonClickSound();
        }
    }

    protected void onScrollViewLayoutChanged(UIEvent event) {

    }

    public Selector<T> selectorStyle(Consumer<SelectorStyle> style) {
        style.accept(getSelectorStyle());
        onStyleChanged();
        return this;
    }

    protected void onSelectorStyleChanged() {
        setupDialog();
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
        if (isChildHover() || isFocused()) {
            guiContext.drawTexture(getSelectorStyle().focusOverlay, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
        }
        super.drawBackgroundOverlay(guiContext);
    }
}
