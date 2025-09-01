package com.lowdragmc.lowdraglib2.gui.ui.layout;

import com.lowdragmc.lowdraglib2.configurator.accessors.EnumAccessor;
import com.lowdragmc.lowdraglib2.configurator.ui.*;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import org.appliedenergistics.yoga.*;
import org.appliedenergistics.yoga.numeric.FloatOptional;
import org.appliedenergistics.yoga.style.StyleLength;
import org.appliedenergistics.yoga.style.StyleSizeLength;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class YogaStyleConfigParser {

    public static void buildConfigurator(YogaNode yogaNode, ConfiguratorGroup father) {
        var style = yogaNode.getStyle();
        father.addConfigurator(EnumAccessor.create("LayoutDirection",
                Arrays.stream(YogaDirection.values()).toList(), style::getDirection, yogaNode::setDirection,
                YogaDirection.INHERIT, true).setTips("LayoutDirection.tips"));

        // flex
        var flexGroup = new ConfiguratorGroup("Flex.name");

        flexGroup.addConfigurator(new StyleSizeLengthConfigurator("FlexBasis", style::getFlexBasis, value -> {
            style.setFlexBasis(value);
            yogaNode.markDirtyAndPropagate();
        }, StyleSizeLength.AUTO, true).setTips("FlexBasis.tips"));

        flexGroup.addConfigurator(new FloatOptionalConfigurator("Flex", style::getFlex, value -> {
            style.setFlex(value);
            yogaNode.markDirtyAndPropagate();
        }, FloatOptional.of(), true).setTips("Flex.tips"));

        flexGroup.addConfigurator(new FloatOptionalConfigurator("FlexGrow", style::getFlexGrow, value -> {
            style.setFlexGrow(value);
            yogaNode.markDirtyAndPropagate();
        }, FloatOptional.of(), true).setTips("FlexGrow.tips"));

        flexGroup.addConfigurator(new FloatOptionalConfigurator("FlexShrink", style::getFlexShrink, value -> {
            style.setFlexShrink(value);
            yogaNode.markDirtyAndPropagate();
        }, FloatOptional.of(), true).setTips("FlexShrink.tips"));

        flexGroup.addConfigurator(EnumAccessor.create("FlexDirection",
                Arrays.stream(YogaFlexDirection.values()).toList(), style::getFlexDirection, yogaNode::setFlexDirection,
                YogaFlexDirection.COLUMN, true,
                v -> switch (v) {
                    case YogaFlexDirection.COLUMN -> Icons.COLUMN;
                    case YogaFlexDirection.COLUMN_REVERSE -> Icons.COLUMN_REVERSE;
                    case YogaFlexDirection.ROW -> Icons.ROW;
                    case YogaFlexDirection.ROW_REVERSE -> Icons.ROW_REVERSE;
                }).setTips("FlexDirection.tips"));

        flexGroup.addConfigurator(EnumAccessor.create("FlexWrap",
                Arrays.stream(YogaWrap.values()).toList(), yogaNode::getWrap, yogaNode::setWrap,
                YogaWrap.NO_WRAP, true,
                v -> switch (v) {
                    case YogaWrap.NO_WRAP -> Icons.NOWRAP;
                    case YogaWrap.WRAP -> Icons.WRAP;
                    case YogaWrap.WRAP_REVERSE -> Icons.WRAP_REVERSE;
                }).setTips("FlexWrap.tips"));

        father.addConfigurator(flexGroup);

        // position
        var positionGroup = new ConfiguratorGroup("Position.name");

        positionGroup.addConfigurator(EnumAccessor.create("PositionMode",
                Arrays.stream(YogaPositionType.values()).toList(), yogaNode::getPositionType, yogaNode::setPositionType,
                YogaPositionType.RELATIVE, true).setTips("PositionMode.tips"));

        var group = createEdgeConfigurator("",
                style::getPosition, yogaNode::setPosition,
                edge -> StyleLength.undefined(), true);
        var configurators = new ArrayList<>(group.getConfigurators());
        group.removeAllConfigurators();
        positionGroup.addConfigurators(configurators.toArray(new Configurator[0]));

        father.addConfigurator(positionGroup);

        // spacing
        var spacingGroup = new ConfiguratorGroup("Spacing.name");

        spacingGroup.addConfigurator(createEdgeConfigurator("Margin",
                style::getMargin, yogaNode::setMargin,
                edge -> StyleLength.undefined(), true));
        spacingGroup.addConfigurator(createEdgeConfigurator("Padding",
                style::getPadding, yogaNode::setPadding,
                edge -> StyleLength.undefined(), true));

        father.addConfigurator(spacingGroup);

        // size
        var sizeGroup = new ConfiguratorGroup("Size.name");

        group = createDimensionConfigurator("",
                style::getDimension, (dim, value) -> {
            style.setDimension(dim, value);
            yogaNode.markDirtyAndPropagate();
                },
                dim -> StyleSizeLength.undefined(), true);
        configurators = new ArrayList<>(group.getConfigurators());
        group.removeAllConfigurators();
        sizeGroup.addConfigurators(configurators.toArray(new Configurator[0]));

        sizeGroup.addConfigurator(createDimensionConfigurator("Min",
                style::getMinDimension, (dim, value) -> {
                    style.setMinDimension(dim, value);
                    yogaNode.markDirtyAndPropagate();
                },
                dim -> StyleSizeLength.undefined(), true));
        sizeGroup.addConfigurator(createDimensionConfigurator("Max",
                style::getMaxDimension, (dim, value) -> {
                    style.setMaxDimension(dim, value);
                    yogaNode.markDirtyAndPropagate();
                },
                dim -> StyleSizeLength.undefined(), true));

        father.addConfigurator(sizeGroup);

        // align
        var alignGroup = new ConfiguratorGroup("Align.name");

        alignGroup.addConfigurator(EnumAccessor.create("AlignItems",
                List.of(YogaAlign.AUTO,
                        YogaAlign.FLEX_START,
                        YogaAlign.FLEX_END,
                        YogaAlign.CENTER,
                        YogaAlign.STRETCH), yogaNode::getAlignItems, yogaNode::setAlignItems,
                YogaAlign.STRETCH, true,
                v -> switch (v) {
                    case YogaAlign.FLEX_START -> Icons.ALIGN_ITEMS_FLEX_START;
                    case YogaAlign.FLEX_END -> Icons.ALIGN_ITEMS_FLEX_END;
                    case YogaAlign.CENTER -> Icons.ALIGN_ITEMS_CENTER;
                    case YogaAlign.STRETCH -> Icons.ALIGN_ITEMS_STRETCH;
                    default -> Icons.AUTO;
                }).setTips("AlignItems.tips"));

        alignGroup.addConfigurator(EnumAccessor.create("JustifyContent",
                Arrays.stream(YogaJustify.values()).toList(), yogaNode::getJustifyContent, yogaNode::setJustifyContent,
                YogaJustify.FLEX_START, true,
                v -> switch (v) {
                    case YogaJustify.FLEX_START -> Icons.JUSTIFY_CONTENT_FLEX_START;
                    case YogaJustify.CENTER -> Icons.JUSTIFY_CONTENT_CENTER;
                    case YogaJustify.FLEX_END -> Icons.JUSTIFY_CONTENT_FLEX_END;
                    case YogaJustify.SPACE_BETWEEN -> Icons.JUSTIFY_CONTENT_SPACE_BETWEEN;
                    case YogaJustify.SPACE_AROUND -> Icons.JUSTIFY_CONTENT_SPACE_AROUND;
                    case YogaJustify.SPACE_EVENLY -> Icons.JUSTIFY_CONTENT_SPACE_EVENLY;
                }).setTips("JustifyContent.tips"));

        alignGroup.addConfigurator(EnumAccessor.create("AlignSelf",
                List.of(YogaAlign.AUTO,
                        YogaAlign.FLEX_START,
                        YogaAlign.FLEX_END,
                        YogaAlign.CENTER,
                        YogaAlign.STRETCH), yogaNode::getAlignSelf, yogaNode::setAlignSelf,
                YogaAlign.STRETCH, true,
                v -> switch (v) {
                    case YogaAlign.FLEX_START -> Icons.ALIGN_SELF_FLEX_START;
                    case YogaAlign.FLEX_END -> Icons.ALIGN_SELF_FLEX_END;
                    case YogaAlign.CENTER -> Icons.ALIGN_SELF_CENTER;
                    case YogaAlign.STRETCH -> Icons.ALIGN_SELF_STRETCH;
                    default -> Icons.AUTO;
                }).setTips("AlignSelf.tips"));

        alignGroup.addConfigurator(EnumAccessor.create("AlignContent",
                List.of(YogaAlign.AUTO,
                        YogaAlign.FLEX_START,
                        YogaAlign.FLEX_END,
                        YogaAlign.CENTER,
                        YogaAlign.STRETCH), yogaNode::getAlignContent, yogaNode::setAlignContent,
                YogaAlign.STRETCH, true,
                v -> switch (v) {
                    case YogaAlign.FLEX_START -> Icons.ALIGN_CONTENT_FLEX_START;
                    case YogaAlign.FLEX_END -> Icons.ALIGN_CONTENT_FLEX_END;
                    case YogaAlign.CENTER -> Icons.ALIGN_CONTENT_CENTER;
                    case YogaAlign.STRETCH -> Icons.ALIGN_CONTENT_STRETCH;
                    default -> Icons.AUTO;
                }).setTips("AlignContent.tips"));

        father.addConfigurator(alignGroup);
    }

    private static ConfiguratorGroup createEdgeConfigurator(String name,
                                                            Function<YogaEdge, StyleLength> getter,
                                                            BiConsumer<YogaEdge, StyleLength> setter,
                                                            Function<YogaEdge, StyleLength> defaultValue,
                                                            boolean forceUpdate) {
        var group = new ConfiguratorGroup(name);
        for (YogaEdge edge : YogaEdge.values()) {
            var configurator = new StyleLengthConfigurator(edge.name(),
                    () -> getter.apply(edge),
                    value -> setter.accept(edge, value),
                    defaultValue.apply(edge),
                    forceUpdate);
            group.addConfigurator(configurator);
        }
        return group;
    }

    private static ConfiguratorGroup createDimensionConfigurator(String name,
                                                                 Function<YogaDimension, StyleSizeLength> getter,
                                                                 BiConsumer<YogaDimension, StyleSizeLength> setter,
                                                                 Function<YogaDimension, StyleSizeLength> defaultValue,
                                                                 boolean forceUpdate) {
        var group = new ConfiguratorGroup(name);
        for (YogaDimension dimension : YogaDimension.values()) {
            var configurator = new StyleSizeLengthConfigurator(dimension.name(),
                    () -> getter.apply(dimension),
                    value -> setter.accept(dimension, value),
                    defaultValue.apply(dimension),
                    forceUpdate);
            group.addConfigurator(configurator);
        }
        return group;
    }
}
