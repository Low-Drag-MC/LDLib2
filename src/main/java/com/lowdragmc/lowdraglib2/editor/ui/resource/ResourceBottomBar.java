package com.lowdragmc.lowdraglib2.editor.ui.resource;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Slider;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Setter;

import org.jetbrains.annotations.Nullable;
import java.util.function.IntConsumer;

/**
 * The status strip under a resource grid, in the spirit of Unity's Project window footer: the full
 * identifier of whatever is selected on the left, a preview size slider on the right.
 * <p>
 * The slider is continuous. It stays in sync with the fixed sizes the context menu offers — those just
 * jump it to a preset — so the two ways of resizing never disagree.
 */
public class ResourceBottomBar extends UIElement {
    private static final int SLIDER_WIDTH = 44;

    public final Label pathLabel = new Label();
    public final Slider.Horizontal slider = new Slider.Horizontal();

    /** Fired while dragging, for a live preview. */
    @Setter
    @Nullable
    private IntConsumer onValueChanged;
    /** Fired once the drag ends, for callers that want to persist the value. */
    @Setter
    @Nullable
    private Runnable onValueCommitted;

    public ResourceBottomBar(int minValue, int maxValue) {
        getLayout().widthPercent(100);
        getLayout().height(11);
        getLayout().flexDirection(FlexDirection.ROW);
        getLayout().alignItems(AlignItems.CENTER);
        getLayout().gapAll(4);
        getLayout().paddingHorizontal(3);

        pathLabel.textStyle(style -> style
                        .textAlignHorizontal(Horizontal.LEFT)
                        .textAlignVertical(Vertical.CENTER)
                        .textWrap(TextWrap.HOVER_ROLL)
                        .fontSize(6))
                .setText("")
                .setOverflowVisible(false)
                .layout(layout -> {
                    layout.flex(1);
                    layout.heightPercent(100);
                });
        pathLabel.addClass("__resource-bottom-bar_path__").moveInlineAsDefault();

        slider.setRange(minValue, maxValue);
        slider.setOnValueChanged(value -> {
            if (onValueChanged != null) {
                onValueChanged.accept(Math.round(value));
            }
        });
        // the value is only worth persisting once the user lets go of the handle
        slider.handle.addEventListener(UIEvents.DRAG_END, e -> {
            if (onValueCommitted != null) {
                onValueCommitted.run();
            }
        });
        slider.layout(layout -> layout.width(SLIDER_WIDTH));
        slider.style(style -> style.tooltips("editor.resource.preview_size"));
        slider.addClass("__resource-bottom-bar_slider__").moveInlineAsDefault();

        addChildren(pathLabel, slider);
        addClass("__resource-bottom-bar__");
        moveInlineAsDefault();
    }

    /** Moves the handle without notifying, so the context menu presets can push their value in. */
    public ResourceBottomBar setValue(int value) {
        slider.setValue((float) value, false);
        return this;
    }

    public ResourceBottomBar setText(@Nullable String text) {
        pathLabel.setText(text == null ? "" : text);
        return this;
    }
}
