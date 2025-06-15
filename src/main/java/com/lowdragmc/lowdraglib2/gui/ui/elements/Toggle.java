package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.editor_outdated.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.BindableUIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.style.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@RemapPrefixForJS("kjs$")
@Accessors(chain = true)
public class Toggle extends BindableUIElement<Boolean> {
    public static class ToggleGroup {
        @Setter
        @Accessors(chain = true)
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
    @Accessors(chain = true, fluent = true)
    public static class ToggleStyle extends Style {
        @Getter
        @Setter
        private IGuiTexture baseTexture = Sprites.RECT_DARK;
        @Getter
        @Setter
        private IGuiTexture hoverTexture = new GuiTextureGroup(Sprites.RECT_DARK, ColorPattern.WHITE.borderTexture(-1));
        @Getter
        @Setter
        private IGuiTexture unmarkTexture = IGuiTexture.EMPTY;
        @Getter
        @Setter
        private IGuiTexture markTexture = Icons.CHECK_SPRITE;

        public ToggleStyle(UIElement holder) {
            super(holder);
        }
    }
    public final Button toggleButton;
    public final UIElement markIcon;
    public final Label toggleLabel;
    @Getter
    private final ToggleStyle toggleStyle = new ToggleStyle(this);
    @Getter
    private boolean isOn = false;
    @Getter
    @Nullable
    private ToggleGroup toggleGroup;

    public Toggle() {
        getLayout().setFlexDirection(YogaFlexDirection.ROW);
        getLayout().setAlignItems(YogaAlign.CENTER);
        getLayout().setPadding(YogaEdge.ALL, 1);
        getLayout().setHeight(14);

        this.toggleButton = new Button();
        this.toggleButton
                .setOnClick(this::onToggleClick)
                .buttonStyle(style -> style
                        .defaultTexture(toggleStyle.baseTexture())
                        .hoverTexture(toggleStyle.hoverTexture())
                        .pressedTexture(toggleStyle.hoverTexture()))
                .noText()
                .layout(layout -> {
                    layout.setPadding(YogaEdge.ALL, 0);
                    layout.setWidth(12);
                    layout.setHeight(12);
                })
                .addChild(this.markIcon = new UIElement()
                        .layout(layout -> {
                            layout.setWidthPercent(100);
                            layout.setHeightPercent(100);
                        })
                        .style(style -> style.backgroundTexture(toggleStyle.unmarkTexture())));
        this.toggleLabel = new Label();
        this.toggleLabel
                .textStyle(style -> style
                        .textAlignHorizontal(Horizontal.LEFT)
                        .textAlignVertical(Vertical.CENTER))
                .layout(layout -> {
                    layout.setHeightPercent(100);
                    layout.setFlex(1);
                    layout.setMargin(YogaEdge.LEFT, 2);
                });
        this.toggleLabel.setText("Toggle");
        addChildren(toggleButton, toggleLabel);
    }

    public Toggle toggleStyle(Consumer<ToggleStyle> style) {
        style.accept(toggleStyle);
        onStyleChanged();
        this.markIcon.getStyle().backgroundTexture(isOn ? toggleStyle.markTexture() : toggleStyle.unmarkTexture());
        this.toggleButton.buttonStyle(buttonStyle -> buttonStyle
                .defaultTexture(toggleStyle.baseTexture())
                .hoverTexture(toggleStyle.hoverTexture())
                .pressedTexture(toggleStyle.hoverTexture()));
        return this;
    }

    @Override
    public void applyStyle(Map<String, StyleValue<?>> values) {
        super.applyStyle(values);
        toggleStyle.applyStyles(values);
    }

    @Override
    public boolean isInternalElement(UIElement child) {
        if (child == toggleButton || child == toggleLabel) {
            return true;
        }
        return super.isInternalElement(child);
    }

    protected void onToggleClick(UIEvent event) {
        setOn(!isOn, true);
    }

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
    public void onRemoved() {
        super.onRemoved();
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
    public Toggle setValue(Boolean value, boolean notify) {
        if (value == isOn) return this;
        isOn = value;
        if (toggleGroup != null) {
            if (value) {
                toggleGroup.setCurrentToggle(this);
            } else if (toggleGroup.currentToggle == this) {
                if (toggleGroup.allowEmpty) {
                    toggleGroup.clearCurrentToggle();
                }
            }
        }
        markIcon.getStyle().backgroundTexture(isOn ? toggleStyle.markTexture() : toggleStyle.unmarkTexture());
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

}
