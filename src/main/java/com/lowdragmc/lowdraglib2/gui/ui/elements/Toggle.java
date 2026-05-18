package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaEdge;
import org.w3c.dom.Element;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@RemapPrefixForJS("kjs$")
@Accessors(chain = true)
@KJSBindings
@LDLRegister(name = "toggle", group = "basic", registry = "ldlib2:ui_element")
public class Toggle extends BindableUIElement<Boolean> {
    public static class ToggleGroup implements IPersistedSerializable, IConfigurable {
        @Setter
        @Accessors(chain = true)
        @Configurable(name = "allowEmpty")
        private boolean allowEmpty = false;
        @Getter
        private List<Toggle> toggles = new ArrayList<>();
        @Getter
        @Nullable
        private Toggle currentToggle;

        protected void registerToggle(Toggle toggle) {
            toggles.add(toggle);
            if (!allowEmpty && currentToggle == null || toggle.isOn()) {
                setCurrentToggle(toggle);
            }
        }

        protected void unregisterToggle(Toggle toggle) {
            toggles.remove(toggle);
            if (currentToggle == toggle) {
                clearCurrentToggle();
                if (!allowEmpty) {
                    toggles.stream().findAny().ifPresent(t -> t.setOn(true));
                }
            }
        }

        protected void clearCurrentToggle() {
            currentToggle = null;
        }

        protected void setCurrentToggle(Toggle toggle) {
            if (toggle == currentToggle) return;
            if (currentToggle != null) {
                currentToggle.setOn(false);
            }
            currentToggle = toggle;
        }
    }
    @Configurable(name = "ToggleStyle")
    public class ToggleStyle extends Style {
        private static final Property<?>[] PROPERTIES = new Property[] {
                PropertyRegistry.UNMARK_BACKGROUND,
                PropertyRegistry.MARK_BACKGROUND,
        };

        public ToggleStyle() {
            super(Toggle.this);
            Toggle.this.toggleButton.getButtonStyle().setDefault(PropertyRegistry.BASE_BACKGROUND, Sprites.RECT_DARK);
            Toggle.this.toggleButton.getButtonStyle().setDefault(PropertyRegistry.HOVER_BACKGROUND, new GuiTextureGroup(Sprites.RECT_DARK, ColorPattern.WHITE.borderTexture(-1)));
            Toggle.this.toggleButton.getButtonStyle().setDefault(PropertyRegistry.PRESSED_BACKGROUND, Sprites.RECT_DARK);
            setDefault(PropertyRegistry.MARK_BACKGROUND, Icons.CHECK_SPRITE);
        }

        public static void init() {
            PropertyRegistry.UNMARK_BACKGROUND.addListener(ToggleStyle::onPropertyChanged);
            PropertyRegistry.MARK_BACKGROUND.addListener(ToggleStyle::onPropertyChanged);
        }

        private static <T> void onPropertyChanged(UIElement element, Property<T> property, @Nullable T oldValue, @Nullable T newValue) {
            if (element instanceof Toggle toggle) {
                Style.importantPipeline(toggle.markIcon.getStyle(), style ->
                        style.backgroundTexture(toggle.isOn ? toggle.toggleStyle.markTexture() : toggle.toggleStyle.unmarkTexture())
                );
            }
        }

        @Override
        protected Property<?>[] getProperties() {
            return PROPERTIES;
        }

        public ToggleStyle baseTexture(IGuiTexture texture) {
            Toggle.this.toggleButton.getButtonStyle().baseTexture(texture);
            Toggle.this.toggleButton.getButtonStyle().pressedTexture(texture);
            return this;
        }

        public ToggleStyle hoverTexture(IGuiTexture texture) {
            Toggle.this.toggleButton.getButtonStyle().hoverTexture(texture);
            return this;
        }

        public IGuiTexture unmarkTexture() {
            return getValueSave(PropertyRegistry.UNMARK_BACKGROUND);
        }

        public ToggleStyle unmarkTexture(IGuiTexture texture) {
            set(PropertyRegistry.UNMARK_BACKGROUND, texture);
            return this;
        }

        public IGuiTexture markTexture() {
            return getValueSave(PropertyRegistry.MARK_BACKGROUND);
        }

        public ToggleStyle markTexture(IGuiTexture texture) {
            set(PropertyRegistry.MARK_BACKGROUND, texture);
            return this;
        }

        @Override
        public void setPipelineState(StyleOrigin pipelineState) {
            super.setPipelineState(pipelineState);
            Toggle.this.toggleButton.getButtonStyle().setPipelineState(pipelineState);
        }
    }

    public final Button toggleButton = new Button();
    public final UIElement markIcon;
    public final Label toggleLabel;
    @Getter
    private final ToggleStyle toggleStyle = new ToggleStyle();
    @Getter
    @Configurable(name = "isOn")
    private boolean isOn = false;
    @Getter
    @Nullable
    private ToggleGroup toggleGroup;

    public Toggle() {
        getLayout().flexDirection(FlexDirection.ROW);
        getLayout().alignItems(AlignItems.CENTER);
        getLayout().paddingAll(1);
        getLayout().height(14);

        this.toggleButton
                .setOnClick(this::onToggleClick)
                .noText()
                .layout(layout -> {
                    layout.paddingAll(0);
                    layout.heightPercent(100);
                    layout.setAspectRatio(1);
                })
                .addClass("__toggle_button__")
                .addChild(this.markIcon = new UIElement()
                        .layout(layout -> {
                            layout.widthPercent(100);
                            layout.heightPercent(100);
                        })
                        .style(style -> Style.importantPipeline(style, s -> s.backgroundTexture(toggleStyle.unmarkTexture())))
                        .addClass("__toggle_mark-icon__"));
        this.toggleLabel = new Label();
        this.toggleLabel
                .textStyle(style -> style
                        .textAlignHorizontal(Horizontal.LEFT)
                        .textAlignVertical(Vertical.CENTER))
                .addClass("__toggle_label__")
                .layout(layout -> {
                    layout.heightPercent(100);
                    layout.flex(1);
                    layout.marginLeft(2);
                });
        this.toggleLabel.setText("Toggle");
        addChildren(toggleButton, toggleLabel);
        internalSetup();
    }

    public Toggle toggleStyle(Consumer<ToggleStyle> style) {
        style.accept(toggleStyle);
        return this;
    }

    protected void onToggleClick(UIEvent event) {
        if (!isActive()) return;
        if (toggleGroup != null) {
            if (isOn && !toggleGroup.allowEmpty) return;
        }
        setOn(!isOn, true);
    }

    @ConfigSetter(field = "isOn")
    public Toggle setOn(boolean on) {
        return setOn(on, true);
    }

    public Toggle setOn(boolean on, boolean notifyChange) {
        return setValue(on, notifyChange);
    }

    /**
     * Sets the {@code ToggleGroup} for this {@code Toggle}. A {@code ToggleGroup} manages a collection of toggles
     * where only one toggle can be active at a time unless the group is configured to allow empty selection.
     *
     * If a new {@code ToggleGroup} is assigned, this {@code Toggle} will be registered to the new group.
     * If the group is set to {@code null}, this {@code Toggle} will be unregistered from its current group, if any.
     *
     * @param group the {@code ToggleGroup} to associate with this {@code Toggle}, or {@code null}
     *              to disassociate it from any group
     * @return the current {@code Toggle} instance for method chaining
     */
    public Toggle setToggleGroup(@Nullable ToggleGroup group) {
        if (group != null) {
            group.registerToggle(this);
        } else if (this.toggleGroup != null) {
            this.toggleGroup.unregisterToggle(this);
        }
        this.toggleGroup = group;
        return this;
    }

    @Override
    protected void onRemoved() {
        super.onRemoved();
    }

    public Toggle noText() {
        toggleLabel.setDisplay(false);
        return this;
    }

    public Toggle enableText() {
        toggleLabel.setDisplay(true);
        return this;
    }

    @HideFromJS
    public Toggle setText(String text) {
        toggleLabel.setText(text);
        return this;
    }

    @HideFromJS
    public Toggle setText(Component text) {
        toggleLabel.setText(text);
        return this;
    }

    public Toggle setText(String text, boolean translate) {
        return setText(translate ? Component.translatable(text) : Component.literal(text));
    }

    public Toggle kjs$setText(Component text) {
        return setText(text);
    }

    @Override
    public Boolean getValue() {
        return isOn;
    }

    @Override
    public Toggle setValue(@Nullable Boolean value, boolean notify) {
        if (value == null) value = false;
        if (value == isOn) return this;
        isOn = value;
        if (isOn) {
            addClass("__on__");
        } else {
            removeClass("__on__");
        }
        Style.importantPipeline(this.markIcon.getStyle(), style ->
            style.backgroundTexture(isOn ? toggleStyle.markTexture() : toggleStyle.unmarkTexture())
        );
        if (toggleGroup != null) {
            if (value) {
                toggleGroup.setCurrentToggle(this);
            } else if (toggleGroup.currentToggle == this) {
                if (toggleGroup.allowEmpty) {
                    toggleGroup.clearCurrentToggle();
                }
            }
        }
        if (notify) {
            notifyListeners();
        }

        return this;
    }

    public Toggle setOnToggleChanged(BooleanConsumer onToggleChanged) {
        registerValueListener(v -> onToggleChanged.accept(v.booleanValue()));
        return this;
    }

    public Toggle toggleButton(Consumer<Button> button) {
        button.accept(toggleButton);
        return this;
    }

    public Toggle toggleLabel(Consumer<Label> label) {
        label.accept(toggleLabel);
        return this;
    }

    public Toggle markIcon(Consumer<UIElement> icon) {
        icon.accept(markIcon);
        return this;
    }

    /// Editor + Xml
    @Override
    public void loadXml(Element element) {
        if (element.hasAttribute("text")) {
            var text = element.getAttribute("text");
            if (text.isEmpty()) {
                noText();
            } else {
                setText(text);
            }
        }
        super.loadXml(element);
        // is on
        if (element.hasAttribute("is-on")) {
            setOn(XmlUtils.getAsBoolean(element, "is-on", isOn));
        }
    }

    @Override
    public void afterDeserialize() {
        super.afterDeserialize();
        if (isOn) {
            addClass("__on__");
        } else {
            removeClass("__on__");
        }
    }
}
