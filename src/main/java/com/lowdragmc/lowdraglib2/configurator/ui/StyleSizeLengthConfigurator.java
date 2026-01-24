package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaUnit;
import org.appliedenergistics.yoga.YogaValue;
import org.appliedenergistics.yoga.style.StyleSizeLength;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class StyleSizeLengthConfigurator extends ValueConfigurator<StyleSizeLength> {
    public final TextField textField;
    public final Selector<YogaUnit> unitSelector;

    public StyleSizeLengthConfigurator(String name, Supplier<StyleSizeLength> supplier, Consumer<StyleSizeLength> onUpdate, @Nonnull StyleSizeLength defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        setCopiable(value -> value);

        if (value == null) {
            value = defaultValue;
        }

        inlineContainer.layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
        });
        inlineContainer.addChildren(textField = new TextField(), unitSelector = new Selector<>());

        unitSelector.buttonIcon.setDisplay(false);
        unitSelector.layout(layout -> {
            layout.flex(1);
        });
        unitSelector.setCandidates(List.of(YogaUnit.POINT, YogaUnit.PERCENT, YogaUnit.AUTO,
                YogaUnit.MAX_CONTENT, YogaUnit.FIT_CONTENT, YogaUnit.STRETCH, YogaUnit.UNDEFINED));
        updateSelector();

        unitSelector.setOnValueChanged(unit -> {
            switch (unit) {
                case UNDEFINED -> updateValueActively(StyleSizeLength.undefined());
                case AUTO -> updateValueActively(StyleSizeLength.ofAuto());
                case POINT -> updateValueActively(StyleSizeLength.points(0));
                case PERCENT -> updateValueActively(StyleSizeLength.percent(0));
                case MAX_CONTENT -> updateValueActively(StyleSizeLength.ofMaxContent());
                case FIT_CONTENT -> updateValueActively(StyleSizeLength.ofFitContent());
                case STRETCH -> updateValueActively(StyleSizeLength.ofStretch());
            }
            updateTextFieldValue();
        });
        unitSelector.setCandidateUIProvider(UIElementProvider.text(value -> switch (value) {
            case AUTO -> Component.translatable("auto");
            case POINT -> Component.literal("px");
            case PERCENT -> Component.literal("%");
            case UNDEFINED -> Component.translatable("initial");
            case MAX_CONTENT -> Component.translatable("max-content");
            case FIT_CONTENT -> Component.translatable("fit-content");
            case STRETCH -> Component.translatable("stretch");
        }));

        textField.layout(layout -> {
            layout.flex(2);
        });
        textField.setNumbersOnlyFloat(-Float.MAX_VALUE, Float.MAX_VALUE);
        textField.setWheelDur(1f);
        textField.setTextResponder(this::onNumberUpdate);
        updateTextFieldValue();
    }

    protected void updateTextFieldValue() {
        assert value != null;
        if (value.isPercent() || value.isPoints()) {
            textField.setText(String.valueOf(value.asYogaValue().value), false);
            textField.setDisplay(true);
        } else {
            textField.setDisplay(false);
        }
    }

    protected void updateSelector() {
        assert value != null;
        unitSelector.setValue(value.isUndefined() ? YogaUnit.UNDEFINED :
                value.isAuto() ? YogaUnit.AUTO :
                        value.isPoints() ? YogaUnit.POINT :
                                value.isPercent() ? YogaUnit.PERCENT :
                                        value.isMaxContent() ? YogaUnit.MAX_CONTENT :
                                                value.isFitContent() ? YogaUnit.FIT_CONTENT : YogaUnit.STRETCH, false);
    }

    @Override
    protected void onValueUpdatePassively(StyleSizeLength newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdatePassively(newValue);
        updateTextFieldValue();
        updateSelector();
    }

    private void onNumberUpdate(String s) {
        var number = Float.parseFloat(s);
        assert value != null;
        if (value.isPoints() || value.isPercent()) {
            updateValueActively(value.isPercent() ?
                    StyleSizeLength.fromYogaValue(YogaValue.percent(number)) :
                    value.isPoints() ? StyleSizeLength.fromYogaValue(YogaValue.point(number)) : value);
        }
    }
}
