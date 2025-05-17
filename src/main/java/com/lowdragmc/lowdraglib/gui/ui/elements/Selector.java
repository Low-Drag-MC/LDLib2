package com.lowdragmc.lowdraglib.gui.ui.elements;

import com.lowdragmc.lowdraglib.editor.ColorPattern;
import com.lowdragmc.lowdraglib.editor.Icons;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib.gui.ui.style.Style;
import com.lowdragmc.lowdraglib.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import org.appliedenergistics.yoga.*;
import org.appliedenergistics.yoga.style.StyleSizeLength;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
public class Selector<T> extends UIElement {
    @Accessors(chain = true, fluent = true)
    public static class SelectorStyle extends Style {
        @Getter @Setter
        private IGuiTexture focusOverlay = Sprites.RECT_RD_T_SOLID;
        @Getter @Setter
        private int maxItemCount = 5; // if more than this, use scroller view.
        @Getter @Setter
        private int scrollerViewHeight = 50;
        @Getter @Setter
        private boolean showOverlay = true;
        @Getter @Setter
        private boolean closeAfterSelect = true;

        public SelectorStyle(UIElement holder) {
            super(holder);
        }
    }
    public final UIElement display;
    public final UIElement preview;
    public final UIElement buttonIcon;
    public final UIElement dialog;
    public final UIElement listView;
    public final ScrollerView scrollerView;
    @Getter
    private final SelectorStyle selectorStyle = new SelectorStyle(this);
    @Getter
    private List<T> candidates = List.of();
    private Function<T, UIElement> candidateUIProvider = candidate -> new Label()
            .textStyle(style -> style
                    .textAlignHorizontal(Horizontal.LEFT)
                    .textAlignVertical(Vertical.CENTER))
            .setText(candidate == null ? "---" : candidate.toString()).layout(layout -> layout.setHeight(10));
    @Getter
    @Nullable
    private T value = null;
    @Setter
    @Nullable
    private Consumer<T> onValueChanged = null;

    // runtime
    @Getter
    private boolean isOpen = false;
    protected final Map<T, Button> candidateButtons = new HashMap<>();

    public Selector() {
        getLayout().setHeight(14);
        getStyle().backgroundTexture(Sprites.RECT_RD_LIGHT);
        setFocusable(true);
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.BLUR, this::onBlur);
        this.preview = new UIElement().layout(layout -> {
                    layout.setHeightPercent(100);
                    layout.setFlexGrow(1);
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
                    layout.setWidthPercent(100);
                    layout.setHeight(StyleSizeLength.AUTO);
                    layout.setPositionType(YogaPositionType.ABSOLUTE);
                    layout.setPositionPercent(YogaEdge.TOP, 100);
                })
                .addChildren(listView = new UIElement().layout(layout -> layout.setPadding(YogaEdge.ALL, 2)), scrollerView = new ScrollerView())
                .setDisplay(YogaDisplay.NONE)
                .style(style -> style.zIndex(1).backgroundTexture(Sprites.RECT_DARK))
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
        addChildren(display, dialog);
    }

    public Selector<T> setCandidates(List<T> candidates) {
        this.candidates = candidates;
        setupDialog();
        return this;
    }

    public Selector<T> setCandidateUIProvider(Function<T, UIElement> candidateUIProvider) {
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
        setValue(this.value, false, true);
    }

    private UIElement createItemUI(T candidate) {
        var candidateUI = candidateUIProvider.apply(candidate);
        var overlayButton = new Button();
        overlayButton.buttonStyle(style -> style.defaultTexture(IGuiTexture.EMPTY)
                        .hoverTexture(selectorStyle.showOverlay ? ColorPattern.T_GRAY.rectTexture() : IGuiTexture.EMPTY)
                        .pressedTexture(selectorStyle.showOverlay ? ColorPattern.T_GRAY.rectTexture() : IGuiTexture.EMPTY))
                .setOnClick(e -> {
                    setValue(candidate);
                    if (selectorStyle.closeAfterSelect) {
                        hide();
                    }
                })
                .setText("")
                .layout(layout -> {
                    layout.setPositionType(YogaPositionType.ABSOLUTE);
                    layout.setHeightPercent(100);
                    layout.setWidthPercent(100);
                })
                .setId("selector#overlayButton");
        candidateUI.addChild(overlayButton);
        candidateButtons.put(candidate, overlayButton);
        return candidateUI;
    }

    public Selector<T> setValue(T value) {
        return setValue(value, true);
    }

    public Selector<T> setValue(T value, boolean notify) {
        return setValue(value, notify, false);
    }

    private Selector<T> setValue(@Nullable T value, boolean notify, boolean force) {
        if (!force && this.value == value) return this;

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
        if (notify && onValueChanged != null) {
            onValueChanged.accept(value);
        }
        return this;
    }

    ///  events
    protected void onMouseDown(UIEvent event) {
        if (event.button == 0) {
            if (isOpen) {
                hide();
                blur();
            } else {
                show();
                focus();
            }
            Widget.playButtonClickSound();
        }
    }

    protected void onBlur(UIEvent event) {
        if (isChildHover()) {
            focus();
        } else {
            hide();
        }
    }

    protected void onScrollViewLayoutChanged(UIEvent event) {

    }

    public Selector<T> selectorStyle(Consumer<SelectorStyle> style) {
        style.accept(getSelectorStyle());
        onStyleChanged();
        setupDialog();
        return this;
    }

    @Override
    public void applyStyle(Map<String, StyleValue<?>> values) {
        super.applyStyle(values);
        selectorStyle.applyStyles(values);
    }

    /// Logic
    public void show() {
        if (this.isOpen) {
            return;
        }
        this.isOpen = true;
        this.dialog.setDisplay(YogaDisplay.FLEX);
    }

    public void hide() {
        if (!this.isOpen) {
            return;
        }
        this.isOpen = false;
        this.dialog.setDisplay(YogaDisplay.NONE);
    }

    /// rendering
    @Override
    public void drawBackgroundOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawBackgroundOverlay(graphics, mouseX, mouseY, partialTicks);
        if (isChildHover() || isFocused()) {
            getSelectorStyle().focusOverlay().draw(graphics, mouseX, mouseY, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), partialTicks);
        }
    }
}
