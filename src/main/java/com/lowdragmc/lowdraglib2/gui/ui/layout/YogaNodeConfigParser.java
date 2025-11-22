package com.lowdragmc.lowdraglib2.gui.ui.layout;

import com.lowdragmc.lowdraglib2.configurator.accessors.EnumAccessor;
import com.lowdragmc.lowdraglib2.configurator.ui.*;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.style.LayoutStyle;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.FlexIcons;
import lombok.experimental.UtilityClass;
import org.appliedenergistics.yoga.*;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

@UtilityClass
public final class YogaNodeConfigParser {

    public static void buildConfigurator(LayoutStyle style, ConfiguratorGroup father) {
        var uiElement = style.holder;
        var yogaNode = uiElement.getLayoutNode();

        father.addConfigurators(
                createConfigurator(YogaProperties.DISPLAY, style),
                createConfigurator(YogaProperties.LAYOUT_DIRECTION, style),
                // flex
                new ConfiguratorGroup("property.flex.group").addConfigurators(
                        createConfigurator(YogaProperties.FLEX, style),
                        createConfigurator(YogaProperties.FLEX_BASIS, style),
                        createConfigurator(YogaProperties.FLEX_GROW, style),
                        createConfigurator(YogaProperties.FLEX_SHRINK, style),
                        createConfigurator(YogaProperties.FLEX_DIRECTION, style),
                        createConfigurator(YogaProperties.FLEX_WRAP, style)
                ),
                // position
                new ConfiguratorGroup("property.position.group").addConfigurators(
                        createConfigurator(YogaProperties.POSITION, style)
                ).addConfigurators(
                        createConfigurators(YogaProperties.POSITIONS, style)
                ),
                // spacing
                new ConfiguratorGroup("property.spacing.group").addConfigurators(
                        // move all to outer for convenient
                        createConfigurator(YogaProperties.MARGINS[YogaProperties.MARGINS.length - 1], style).setLabel("margin-all"),
                        createConfigurator(YogaProperties.PADDINGS[YogaProperties.PADDINGS.length - 1], style).setLabel("padding-all"),
                        createConfigurator(YogaProperties.GAPS[YogaProperties.GAPS.length - 1], style).setLabel("gap-all"),
                        new ConfiguratorGroup("property.spacing.margin.group").addConfigurators(
                                createConfigurators(YogaProperties.MARGINS, style)
                        ),
                        new ConfiguratorGroup("property.spacing.padding.group").addConfigurators(
                                createConfigurators(YogaProperties.PADDINGS, style)
                        ),
                        new ConfiguratorGroup("property.spacing.gap.group").addConfigurators(
                                createConfigurators(YogaProperties.GAPS, style)
                        )
                ),
                // size
                new ConfiguratorGroup("property.size.group").addConfigurators(
                        createConfigurator(YogaProperties.WIDTH, style),
                        createConfigurator(YogaProperties.HEIGHT, style),
                        new ConfiguratorGroup("property.size.min.group").addConfigurators(
                                createConfigurators(YogaProperties.MIN, style)
                        ),
                        new ConfiguratorGroup("property.size.max.group").addConfigurators(
                                createConfigurators(YogaProperties.MAX, style)
                        ),
                        createConfigurator(YogaProperties.ASPECT_RATE, style),
                        createConfigurator(YogaProperties.OVERFLOW, style)
                ),
                // align
                new ConfiguratorGroup("property.align.group").addConfigurators(
                        createToggleConfigurator(YogaProperties.ALIGN_ITEMS, style, EnumAccessor::getEnumName,
                                v -> DynamicTexture.of(() -> FlexIcons.getAlignItemIcon(yogaNode.getFlexDirection(), v))),
                        createToggleConfigurator(YogaProperties.JUSTIFY_CONTENT, style, EnumAccessor::getEnumName,
                                v -> DynamicTexture.of(() -> FlexIcons.getJustifyContentIcon(yogaNode.getFlexDirection(), v))),
                        createToggleConfigurator(YogaProperties.ALIGN_SELF, style, EnumAccessor::getEnumName,
                                v -> DynamicTexture.of(() -> FlexIcons.getAlignSelfIcon(yogaNode.getFlexDirection(), v))),
                        createToggleConfigurator(YogaProperties.ALIGN_CONTENT, style, EnumAccessor::getEnumName,
                                v -> DynamicTexture.of(() -> FlexIcons.getAlignContentIcon(yogaNode.getFlexDirection(), v)))
                )
        );
    }

    private static <T> Configurator createConfigurator(Property<T> property, LayoutStyle style) {
        return property.createConfigurator(
                style.valueGetter(property),
                style.valueSetter(property),
                Optional.ofNullable(style.getDefault(property)).orElse(property.initialValue)
        );
    }

    private static <T> Configurator[] createConfigurators(Property<T>[] properties, LayoutStyle style) {
        return Arrays.stream(properties).map(property -> createConfigurator(property, style)).toArray(Configurator[]::new);
    }

    @SuppressWarnings("unchecked")
    private static <T> Configurator createToggleConfigurator(Property<T> property, LayoutStyle style, Function<T, String> nameMapper, Function<T, IGuiTexture> iconProvider) {
        var configurator = createConfigurator(property, style);
        if (configurator instanceof ToggleSelectorConfigurator toggleSelectorConfigurator) {
            toggleSelectorConfigurator.initToggles(nameMapper, iconProvider);
        }
        return configurator;
    }
}
