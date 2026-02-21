package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutProperties;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.style.animation.Transition;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.math.interpolate.Eases;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import com.lowdragmc.lowdraglib2.utils.animation.Animation;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDirection;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.appliedenergistics.yoga.YogaEdge;
import org.w3c.dom.Element;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@RemapPrefixForJS("kjs$")
@Accessors(chain = true)
@KJSBindings
@LDLRegister(name = "switch", group = "basic", registry = "ldlib2:ui_element")
public class Switch extends BindableUIElement<Boolean> {
    @Configurable(name = "SwitchStyle")
    public class SwitchStyle extends Style {
        private static final Property<?>[] PROPERTIES = new Property[] {
                PropertyRegistry.BASE_BACKGROUND,
                PropertyRegistry.PRESSED_BACKGROUND,
                PropertyRegistry.UNMARK_BACKGROUND,
                PropertyRegistry.MARK_BACKGROUND,
        };

        public SwitchStyle() {
            super(Switch.this);
            setDefault(PropertyRegistry.BASE_BACKGROUND, Sprites.RECT_RD_DARK);
            setDefault(PropertyRegistry.PRESSED_BACKGROUND, Sprites.RECT_RD_T);
            setDefault(PropertyRegistry.UNMARK_BACKGROUND, Sprites.RECT_RD);
            setDefault(PropertyRegistry.MARK_BACKGROUND, Sprites.RECT_RD);
        }

        public static void init() {
            PropertyRegistry.UNMARK_BACKGROUND.addListener(SwitchStyle::onPropertyChanged);
            PropertyRegistry.MARK_BACKGROUND.addListener(SwitchStyle::onPropertyChanged);
            PropertyRegistry.BASE_BACKGROUND.addListener(SwitchStyle::onPropertyChanged);
            PropertyRegistry.PRESSED_BACKGROUND.addListener(SwitchStyle::onPropertyChanged);
        }

        private static <T> void onPropertyChanged(UIElement element, Property<T> property, @Nullable T oldValue, @Nullable T newValue) {
            if (element instanceof Switch _switch) {
                _switch.updateSwitchStyle();
            }
        }

        @Override
        protected Property<?>[] getProperties() {
            return PROPERTIES;
        }

        public SwitchStyle baseTexture(IGuiTexture texture) {
            set(PropertyRegistry.BASE_BACKGROUND, texture);
            return this;
        }

        public IGuiTexture baseTexture() {
            return getValueSave(PropertyRegistry.BASE_BACKGROUND);
        }


        public SwitchStyle pressedTexture(IGuiTexture texture) {
            set(PropertyRegistry.PRESSED_BACKGROUND, texture);
            return this;
        }

        public IGuiTexture pressedTexture() {
            return getValueSave(PropertyRegistry.PRESSED_BACKGROUND);
        }

        public SwitchStyle unmarkTexture(IGuiTexture texture) {
            set(PropertyRegistry.UNMARK_BACKGROUND, texture);
            return this;
        }

        public IGuiTexture unmarkTexture() {
            return getValueSave(PropertyRegistry.UNMARK_BACKGROUND);
        }

        public SwitchStyle markTexture(IGuiTexture texture) {
            set(PropertyRegistry.MARK_BACKGROUND, texture);
            return this;
        }

        public IGuiTexture markTexture() {
            return getValueSave(PropertyRegistry.MARK_BACKGROUND);
        }
    }

    public final UIElement placeholder;
    public final UIElement markIcon;
    @Getter
    private final SwitchStyle switchStyle = new SwitchStyle();
    @Getter
    @Configurable(name = "isOn")
    private boolean isOn = false;

    public Switch() {
        getLayout().flexDirection(FlexDirection.ROW);
        getLayout().alignItems(AlignItems.CENTER);
        getLayout().paddingAll(2);
        getLayout().height(14);
        getLayout().width(26);
        Style.importantPipeline(getStyle(), style -> style.backgroundTexture(Sprites.RECT_RD_DARK));
        addEventListener(UIEvents.MOUSE_DOWN, this::onSwitchClick);

        this.placeholder = new UIElement();
        Style.importantPipeline(placeholder.getLayout(), layout -> layout.flex(0));
        this.placeholder.getStyle().transition(new Transition(Map.of(LayoutProperties.FLEX, new Animation(0.1f, 0, Eases.LINEAR))));

        this.markIcon = new UIElement();
        this.markIcon.layout(layout -> {
                    layout.heightPercent(100);
                    layout.setAspectRatio(1);
                })
                .addClass("__switch_mark-icon__");
        Style.importantPipeline(markIcon.getStyle(), style -> style.backgroundTexture(Sprites.RECT_RD));
        addChildren(placeholder, markIcon);
        internalSetup();
    }

    public Switch switchStyle(Consumer<SwitchStyle> style) {
        style.accept(switchStyle);
        return this;
    }

    protected void onSwitchClick(UIEvent event) {
        if (!isActive()) return;
        setOn(!isOn, true);
    }

    @ConfigSetter(field = "isOn")
    public Switch setOn(boolean on) {
        return setOn(on, true);
    }

    public Switch setOn(boolean on, boolean notifyChange) {
        return setValue(on, notifyChange);
    }

    @Override
    protected void onRemoved() {
        super.onRemoved();
    }

    @Override
    public Boolean getValue() {
        return isOn;
    }

    @Override
    public Switch setValue(@Nullable Boolean value, boolean notify) {
        if (value == null) value = false;
        if (value == isOn) return this;
        isOn = value;
        updateSwitchStyle();
        if (notify) {
            notifyListeners();
        }
        return this;
    }

    protected void updateSwitchStyle() {
        Style.importantPipeline(getStyle(), style -> style.backgroundTexture(isOn ? switchStyle.pressedTexture() : switchStyle.baseTexture()));
        Style.importantPipeline(placeholder.getLayout(), layout -> layout.flex(isOn ? 1 : 0));
        Style.importantPipeline(markIcon.getStyle(), style -> style.backgroundTexture(isOn ? switchStyle.markTexture() : switchStyle.unmarkTexture()));
    }

    public Switch setOnSwitchChanged(BooleanConsumer onSwitchChanged) {
        registerValueListener(v -> onSwitchChanged.accept(v.booleanValue()));
        return this;
    }

    /// Editor + Xml
    @Override
    public void loadXml(Element element) {
        super.loadXml(element);
        // is on
        if (element.hasAttribute("is-on")) {
            setOn(XmlUtils.getAsBoolean(element, "is-on", isOn));
        }
    }
}
