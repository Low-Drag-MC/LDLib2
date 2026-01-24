package com.lowdragmc.lowdraglib2.gui.ui.layout;

import com.lowdragmc.lowdraglib2.configurator.ui.*;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.style.LayoutStyle;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.FlexIcons;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@UtilityClass
public final class LayoutConfigParser {
    private final static List<LPAConfigurator.Unit> LPA_VALUES = List.of(LPAConfigurator.Unit.AUTO, LPAConfigurator.Unit.LENGTH, LPAConfigurator.Unit.PERCENT);

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
                        createConfigurator(LayoutProperties.POSITION, style),
                        createLPAConfigurator(LayoutProperties.LEFT, style, LPA_VALUES),
                        createLPAConfigurator(LayoutProperties.RIGHT, style, LPA_VALUES),
                        createLPAConfigurator(LayoutProperties.TOP, style, LPA_VALUES),
                        createLPAConfigurator(LayoutProperties.BOTTOM, style, LPA_VALUES)
                ),
                // spacing
                new ConfiguratorGroup("property.spacing.group").addConfigurators(
                        // move all to outer for convenient
                        createLPAConfigurator(LayoutProperties.MARGIN_ALL, style, LPA_VALUES),
                        createLPAConfigurator(LayoutProperties.PADDING_ALL, style, LPA_VALUES),
                        createLPAConfigurator(LayoutProperties.GAP_ALL, style, LPA_VALUES),
                        new ConfiguratorGroup("property.spacing.margin.group").addConfigurators(
                                createLPAConfigurator(LayoutProperties.MARGIN_LEFT, style, LPA_VALUES),
                                createLPAConfigurator(LayoutProperties.MARGIN_RIGHT, style, LPA_VALUES),
                                createLPAConfigurator(LayoutProperties.MARGIN_TOP, style, LPA_VALUES),
                                createLPAConfigurator(LayoutProperties.MARGIN_BOTTOM, style, LPA_VALUES),
                                createLPAConfigurator(LayoutProperties.MARGIN_VERTICAL, style, LPA_VALUES),
                                createLPAConfigurator(LayoutProperties.MARGIN_HORIZONTAL, style, LPA_VALUES),
                                createLPAConfigurator(LayoutProperties.MARGIN_ALL, style, LPA_VALUES)
                        ),
                        new ConfiguratorGroup("property.spacing.padding.group").addConfigurators(
                                createLPAConfigurator(LayoutProperties.PADDING_LEFT, style, LPA_VALUES),
                                createLPAConfigurator(LayoutProperties.PADDING_RIGHT, style, LPA_VALUES),
                                createLPAConfigurator(LayoutProperties.PADDING_TOP, style, LPA_VALUES),
                                createLPAConfigurator(LayoutProperties.PADDING_BOTTOM, style, LPA_VALUES),
                                createLPAConfigurator(LayoutProperties.PADDING_VERTICAL, style, LPA_VALUES),
                                createLPAConfigurator(LayoutProperties.PADDING_HORIZONTAL, style, LPA_VALUES),
                                createLPAConfigurator(LayoutProperties.PADDING_ALL, style, LPA_VALUES)
                        ),
                        new ConfiguratorGroup("property.spacing.gap.group").addConfigurators(
                                createLPAConfigurator(LayoutProperties.GAP_ROW, style, LPA_VALUES),
                                createLPAConfigurator(LayoutProperties.GAP_COLUMN, style, LPA_VALUES),
                                createLPAConfigurator(LayoutProperties.GAP_ALL, style, LPA_VALUES)
                        )
                ),
                // size
                new ConfiguratorGroup("property.size.group").addConfigurators(
                        createConfigurator(LayoutProperties.WIDTH, style),
                        createConfigurator(LayoutProperties.HEIGHT, style),
                        new ConfiguratorGroup("property.size.min.group").addConfigurators(
                                createConfigurator(LayoutProperties.MIN_WIDTH, style),
                                createConfigurator(LayoutProperties.MIN_HEIGHT, style)
                        ),
                        new ConfiguratorGroup("property.size.max.group").addConfigurators(
                                createConfigurator(LayoutProperties.MAX_WIDTH, style),
                                createConfigurator(LayoutProperties.MAX_HEIGHT, style)
                        ),
                        createConfigurator(LayoutProperties.ASPECT_RATE, style),
                        createConfigurator(LayoutProperties.OVERFLOW, style)
                ),
                // align
                new ConfiguratorGroup("property.align.group").addConfigurators(
                        createToggleConfigurator(LayoutProperties.ALIGN_ITEMS, style, LayoutConfigParser::alignItemsNameMapper,
                                v -> DynamicTexture.of(() -> FlexIcons.getAlignItemIcon(style.getFlexDirection(), v))),
                        createToggleConfigurator(LayoutProperties.ALIGN_SELF, style, LayoutConfigParser::alignItemsNameMapper,
                                v -> DynamicTexture.of(() -> FlexIcons.getAlignSelfIcon(style.getFlexDirection(), v))),
                        createToggleConfigurator(LayoutProperties.ALIGN_CONTENT, style, LayoutConfigParser::alignContentNameMapper,
                                v -> DynamicTexture.of(() -> FlexIcons.getAlignContentIcon(style.getFlexDirection(), v))),
                        createToggleConfigurator(LayoutProperties.JUSTIFY_CONTENT, style, LayoutConfigParser::alignContentNameMapper,
                                v -> DynamicTexture.of(() -> FlexIcons.getJustifyContentIcon(style.getFlexDirection(), v))),
                        createToggleConfigurator(LayoutProperties.JUSTIFY_ITEMS, style, LayoutConfigParser::alignItemsNameMapper,
                                v -> DynamicTexture.of(() -> FlexIcons.getAlignSelfIcon(style.getFlexDirection(), v))),
                        createToggleConfigurator(LayoutProperties.JUSTIFY_SELF, style, LayoutConfigParser::alignItemsNameMapper,
                                v -> DynamicTexture.of(() -> FlexIcons.getAlignItemIcon(style.getFlexDirection(), v)))
                )
        );
    }

    public static String alignItemsNameMapper(AlignItems alignItems) {
        return switch (alignItems) {
            case START -> "start";
            case FLEX_START -> "flex-start";
            case CENTER -> "center";
            case END -> "end";
            case FLEX_END -> "flex-end";
            case STRETCH -> "stretch";
            case BASELINE -> "baseline";
            case AUTO -> "auto";
        };
    }

    public static String alignContentNameMapper(AlignContent alignContent) {
        return switch (alignContent) {
            case START -> "start";
            case FLEX_START -> "flex-start";
            case CENTER -> "center";
            case END -> "end";
            case FLEX_END -> "flex-end";
            case STRETCH -> "stretch";
            case SPACE_BETWEEN -> "space-between";
            case SPACE_EVENLY -> "space-evenly";
            case SPACE_AROUND -> "space-around";
            case AUTO -> "auto";
        };
    }

    private static <T> Configurator createConfigurator(Property<T> property, LayoutStyle style) {
        return property.createConfigurator(
                style.valueGetter(property),
                style.valueSetter(property),
                Optional.ofNullable(style.getDefault(property)).orElse(property.initialValue)
        );
    }

    private static Configurator createLPAConfigurator(Property<LengthPercentageAuto> property, LayoutStyle style, List<LPAConfigurator.Unit> candidates) {
        var configurator = createConfigurator(property, style);
        if (configurator instanceof LPAConfigurator lpaConfigurator) {
            lpaConfigurator.setCandidates(candidates);
        }
        return configurator;
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
