package com.lowdragmc.lowdraglib2.gui.ui.layout;

import com.lowdragmc.lowdraglib2.configurator.accessors.EnumAccessor;
import com.lowdragmc.lowdraglib2.configurator.ui.*;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.style.LayoutStyle;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.FlexIcons;
import lombok.experimental.UtilityClass;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

@UtilityClass
public final class LayoutConfigParser {

    public static void buildConfigurator(LayoutStyle style, ConfiguratorGroup father) {
        father.addConfigurators(
                createConfigurator(LayoutProperties.DISPLAY, style),
                createConfigurator(LayoutProperties.LAYOUT_DIRECTION, style),
                // flex
                new ConfiguratorGroup("property.flex.group").addConfigurators(
                        createConfigurator(LayoutProperties.FLEX, style),
                        createConfigurator(LayoutProperties.FLEX_BASIS, style),
                        createConfigurator(LayoutProperties.FLEX_GROW, style),
                        createConfigurator(LayoutProperties.FLEX_SHRINK, style),
                        createConfigurator(LayoutProperties.FLEX_DIRECTION, style),
                        createConfigurator(LayoutProperties.FLEX_WRAP, style)
                ),
                // grid
                new ConfiguratorGroup("property.grid.group").addConfigurators(
                        createConfigurator(LayoutProperties.GRID_TEMPLATE_ROWS, style),
                        createConfigurator(LayoutProperties.GRID_TEMPLATE_COLUMNS, style),
                        createConfigurator(LayoutProperties.GRID_TEMPLATE_AREAS, style),
                        createConfigurator(LayoutProperties.GRID_AUTO_ROWS, style),
                        createConfigurator(LayoutProperties.GRID_AUTO_COLUMNS, style),
                        createConfigurator(LayoutProperties.GRID_AUTO_FLOW, style),
                        createConfigurator(LayoutProperties.GRID_ROW, style),
                        createConfigurator(LayoutProperties.GRID_COLUMN, style)
                ),
                // position
                new ConfiguratorGroup("property.position.group").addConfigurators(
                        createConfigurator(LayoutProperties.POSITION, style)
                ).addConfigurators(
                        createConfigurators(LayoutProperties.POSITIONS, style)
                ),
                // spacing
                new ConfiguratorGroup("property.spacing.group").addConfigurators(
                        // move all to outer for convenient
                        createConfigurator(LayoutProperties.MARGINS[LayoutProperties.MARGINS.length - 1], style)
                                .setLabel(
                                        Component.literal("margin-all").withStyle(
                                                style.valueGetter(LayoutProperties.MARGINS[LayoutProperties.MARGINS.length - 1]).get() == null ?
                                                        Style.EMPTY : Style.EMPTY.withColor(ColorPattern.ORANGE.color)
                                        )
                                ),
                        createConfigurator(LayoutProperties.PADDINGS[LayoutProperties.PADDINGS.length - 1], style)
                                .setLabel(
                                        Component.literal("padding-all").withStyle(
                                                style.valueGetter(LayoutProperties.PADDINGS[LayoutProperties.PADDINGS.length - 1]).get() == null ?
                                                        Style.EMPTY : Style.EMPTY.withColor(ColorPattern.ORANGE.color)
                                        )
                                ),
                        createConfigurator(LayoutProperties.GAPS[LayoutProperties.GAPS.length - 1], style)
                                .setLabel(
                                        Component.literal("gap-all").withStyle(
                                                style.valueGetter(LayoutProperties.GAPS[LayoutProperties.GAPS.length - 1]).get() == null ?
                                                        Style.EMPTY : Style.EMPTY.withColor(ColorPattern.ORANGE.color)
                                        )
                                ),
                        new ConfiguratorGroup("property.spacing.margin.group").addConfigurators(
                                createConfigurators(LayoutProperties.MARGINS, style)
                        ),
                        new ConfiguratorGroup("property.spacing.padding.group").addConfigurators(
                                createConfigurators(LayoutProperties.PADDINGS, style)
                        ),
                        new ConfiguratorGroup("property.spacing.gap.group").addConfigurators(
                                createConfigurators(LayoutProperties.GAPS, style)
                        )
                ),
                // size
                new ConfiguratorGroup("property.size.group").addConfigurators(
                        createConfigurator(LayoutProperties.WIDTH, style),
                        createConfigurator(LayoutProperties.HEIGHT, style),
                        new ConfiguratorGroup("property.size.min.group").addConfigurators(
                                createConfigurators(LayoutProperties.MIN, style)
                        ),
                        new ConfiguratorGroup("property.size.max.group").addConfigurators(
                                createConfigurators(LayoutProperties.MAX, style)
                        ),
                        createConfigurator(LayoutProperties.ASPECT_RATE, style),
                        createConfigurator(LayoutProperties.OVERFLOW, style)
                ),
                // align
                new ConfiguratorGroup("property.align.group").addConfigurators(
                        createToggleConfigurator(LayoutProperties.ALIGN_ITEMS, style, EnumAccessor::getEnumName,
                                v -> DynamicTexture.of(() -> FlexIcons.getAlignItemIcon(style.getFlexDirection(), v))),
                        createToggleConfigurator(LayoutProperties.JUSTIFY_CONTENT, style, EnumAccessor::getEnumName,
                                v -> DynamicTexture.of(() -> FlexIcons.getJustifyContentIcon(style.getFlexDirection(), v))),
                        createToggleConfigurator(LayoutProperties.ALIGN_SELF, style, EnumAccessor::getEnumName,
                                v -> DynamicTexture.of(() -> FlexIcons.getAlignSelfIcon(style.getFlexDirection(), v))),
                        createToggleConfigurator(LayoutProperties.ALIGN_CONTENT, style, EnumAccessor::getEnumName,
                                v -> DynamicTexture.of(() -> FlexIcons.getAlignContentIcon(style.getFlexDirection(), v)))
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
