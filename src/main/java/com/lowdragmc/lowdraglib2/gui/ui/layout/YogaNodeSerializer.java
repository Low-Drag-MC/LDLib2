package com.lowdragmc.lowdraglib2.gui.ui.layout;

import lombok.experimental.UtilityClass;
import net.minecraft.nbt.*;
import org.appliedenergistics.yoga.*;
import org.appliedenergistics.yoga.numeric.FloatOptional;
import org.appliedenergistics.yoga.style.StyleLength;
import org.appliedenergistics.yoga.style.StyleSizeLength;

import java.util.function.BiConsumer;
import java.util.function.Function;

@UtilityClass
public final class YogaNodeSerializer {
    public static CompoundTag serialize(YogaNode yogaNode) {
        var yogaStyle = yogaNode.getStyle();
        var tag = new CompoundTag();

        tag.putString("LayoutDirection", yogaStyle.getDirection().name());
        tag.putString("Display", yogaNode.getDisplay().name());

        // flex
        tag.put("FlexBasis", encodeStyleSizeLength(yogaStyle.getFlexBasis()));
        tag.put("Flex", encodeFloatOptional(yogaStyle.getFlex()));
        tag.put("FlexGrow", encodeFloatOptional(yogaStyle.getFlexGrow()));
        tag.put("FlexShrink", encodeFloatOptional(yogaStyle.getFlexShrink()));
        tag.putString("FlexDirection", yogaStyle.getFlexDirection().name());
        tag.putString("FlexWrap", yogaNode.getWrap().name());

        // position
        tag.putString("PositionMode", yogaStyle.getPositionType().name());
        tag.put("Position", encodeEdge(yogaStyle::getPosition));

        // spacing
        tag.put("Margin", encodeEdge(yogaStyle::getMargin));
        tag.put("Padding", encodeEdge(yogaStyle::getPadding));
        tag.put("Gap", encodeGutter(yogaStyle::getGap));

        // size
        tag.put("Dimension", encodeDimension(yogaStyle::getDimension));
        tag.put("Min", encodeDimension(yogaStyle::getMinDimension));
        tag.put("Max", encodeDimension(yogaStyle::getMaxDimension));
        tag.put("AspectRate", encodeFloatOptional(yogaStyle.getAspectRatio()));
        tag.putString("Overflow", yogaNode.getOverflow().name());

        // align
        tag.putString("AlignItems", yogaStyle.getAlignItems().name());
        tag.putString("JustifyContent", yogaStyle.getJustifyContent().name());
        tag.putString("AlignSelf", yogaStyle.getAlignSelf().name());
        tag.putString("AlignContent", yogaStyle.getAlignContent().name());
        return tag;
    }

    public static YogaNode deserialize(CompoundTag tag, YogaNode yogaNode) {
        var yogaStyle = yogaNode.getStyle();
        if (tag.contains("LayoutDirection")) {
            yogaNode.setDirection(YogaDirection.valueOf(tag.getString("LayoutDirection")));
        }
        if (tag.contains("Display")) {
            yogaNode.setDisplay(YogaDisplay.valueOf(tag.getString("Display")));
        }

        if (tag.contains("FlexBasis")) {
            yogaStyle.setFlexBasis(decodeStyleSizeLength(tag.get("FlexBasis")));
            yogaNode.markDirtyAndPropagate();
        }
        if (tag.contains("Flex")) {
            yogaStyle.setFlex(decodeFloatOptional(tag.get("Flex")));
            yogaNode.markDirtyAndPropagate();
        }
        if (tag.contains("FlexGrow")) {
            yogaStyle.setFlexGrow(decodeFloatOptional(tag.get("FlexGrow")));
            yogaNode.markDirtyAndPropagate();
        }
        if (tag.contains("FlexShrink")) {
            yogaStyle.setFlexShrink(decodeFloatOptional(tag.get("FlexShrink")));
            yogaNode.markDirtyAndPropagate();
        }
        if (tag.contains("FlexDirection")) {
            yogaNode.setFlexDirection(YogaFlexDirection.valueOf(tag.getString("FlexDirection")));
        }
        if (tag.contains("FlexWrap")) {
            yogaNode.setWrap(YogaWrap.valueOf(tag.getString("FlexWrap")));
        }

        if (tag.contains("PositionMode")) {
            yogaNode.setPositionType(YogaPositionType.valueOf(tag.getString("PositionMode")));
        }
        if (tag.contains("Position")) {
            decodeEdge(tag.getCompound("Position"), yogaNode::setPosition);
        }

        if (tag.contains("Margin")) {
            decodeEdge(tag.getCompound("Margin"), yogaNode::setMargin);
        }
        if (tag.contains("Padding")) {
            decodeEdge(tag.getCompound("Padding"), yogaNode::setPadding);
        }
        if (tag.contains("Gap")) {
            decodeGutter(tag.getCompound("Gap"), yogaNode::setGap);
        }

        if (tag.contains("Dimension")) {
            decodeDimension(tag.getCompound("Dimension"), (dim, value) -> {
                yogaStyle.setDimension(dim, value);
                yogaNode.markDirtyAndPropagate();
            });
        }
        if (tag.contains("Min")) {
            decodeDimension(tag.getCompound("Min"), yogaStyle::setMinDimension);
        }
        if (tag.contains("Max")) {
            decodeDimension(tag.getCompound("Max"), yogaStyle::setMaxDimension);
        }
        if (tag.contains("AspectRate")) {
            yogaStyle.setAspectRatio(decodeFloatOptional(tag.get("AspectRate")));
            yogaNode.markDirtyAndPropagate();
        }
        if (tag.contains("Overflow")) {
            yogaNode.setOverflow(YogaOverflow.valueOf(tag.getString("Overflow")));
        }

        if (tag.contains("AlignItems")) {
            yogaNode.setAlignItems(YogaAlign.valueOf(tag.getString("AlignItems")));
        }
        if (tag.contains("JustifyContent")) {
            yogaNode.setJustifyContent(YogaJustify.valueOf(tag.getString("JustifyContent")));
        }
        if (tag.contains("AlignSelf")) {
            yogaNode.setAlignSelf(YogaAlign.valueOf(tag.getString("AlignSelf")));
        }
        if (tag.contains("AlignContent")) {
            yogaNode.setAlignContent(YogaAlign.valueOf(tag.getString("AlignContent")));
        }
        return yogaNode;
    }

    public static Tag encodeFloatOptional(FloatOptional floatOptional) {
        if (floatOptional.isDefined()) {
            return FloatTag.valueOf(floatOptional.getValue());
        }
        return StringTag.valueOf("undefined");
    }

    public static FloatOptional decodeFloatOptional(Tag tag) {
        if (tag instanceof FloatTag floatTag) {
            return FloatOptional.of(floatTag.getAsFloat());
        }
        return FloatOptional.of();
    }

    public static CompoundTag encodeYogaValue(YogaValue yogaValue) {
        var tag = new CompoundTag();
        tag.putFloat("value", yogaValue.value);
        tag.putString("unit", yogaValue.unit.name());
        return tag;
    }

    public static YogaValue decodeYogaValue(CompoundTag tag) {
        return new YogaValue(tag.getFloat("value"), YogaUnit.valueOf(tag.getString("unit")));
    }

    public static Tag encodeStyleSizeLength(StyleSizeLength styleSizeLength) {
        if (styleSizeLength.isAuto()) {
            return StringTag.valueOf("auto");
        } else if (styleSizeLength.isUndefined()) {
            return StringTag.valueOf("undefined");
        } else if (styleSizeLength.isMaxContent()) {
            return StringTag.valueOf("max-content");
        } else if (styleSizeLength.isFitContent()) {
            return StringTag.valueOf("fit-content");
        } else if (styleSizeLength.isStretch()) {
            return StringTag.valueOf("stretch");
        } else {
            return encodeYogaValue(styleSizeLength.asYogaValue());
        }
    }

    public static StyleSizeLength decodeStyleSizeLength(Tag tag) {
        if (tag instanceof StringTag stringTag) {
            return switch (stringTag.getAsString()) {
                case "auto" -> StyleSizeLength.ofAuto();
                case "max-content" -> StyleSizeLength.ofMaxContent();
                case "fit-content" -> StyleSizeLength.ofFitContent();
                case "stretch" -> StyleSizeLength.ofStretch();
                default -> StyleSizeLength.undefined();
            };
        } else if (tag instanceof CompoundTag compoundTag) {
            return StyleSizeLength.fromYogaValue(decodeYogaValue(compoundTag));
        }
        return StyleSizeLength.undefined();
    }

    public static Tag encodeStyleLength(StyleLength styleSizeLength) {
        if (styleSizeLength.isAuto()) {
            return StringTag.valueOf("auto");
        } else if (styleSizeLength.isUndefined()) {
            return StringTag.valueOf("undefined");
        } else {
            return encodeYogaValue(styleSizeLength.asYogaValue());
        }
    }

    public static StyleLength decodeStyleLength(Tag tag) {
        if (tag instanceof StringTag stringTag) {
            return stringTag.getAsString().equals("auto") ? StyleLength.ofAuto() : StyleLength.undefined();
        } else if (tag instanceof CompoundTag compoundTag) {
            return StyleLength.fromYogaValue(decodeYogaValue(compoundTag));
        }
        return StyleLength.undefined();
    }

    public static CompoundTag encodeEdge(Function<YogaEdge, StyleLength> getter) {
        var tag = new CompoundTag();
        for (var edge : YogaEdge.values()) {
            tag.put(edge.name(), encodeStyleLength(getter.apply(edge)));
        }
        return tag;
    }

    public static void decodeEdge(CompoundTag tag, BiConsumer<YogaEdge, StyleLength> setter) {
        for (var edge : YogaEdge.values()) {
            setter.accept(edge, decodeStyleLength(tag.get(edge.name())));
        }
    }

    public static CompoundTag encodeDimension(Function<YogaDimension, StyleSizeLength> getter) {
        var tag = new CompoundTag();
        for (var dimension : YogaDimension.values()) {
            tag.put(dimension.name(), encodeStyleSizeLength(getter.apply(dimension)));
        }
        return tag;
    }

    public static void decodeDimension(CompoundTag tag, BiConsumer<YogaDimension, StyleSizeLength> setter) {
        for (var dimension : YogaDimension.values()) {
            var styleLength = decodeStyleSizeLength(tag.get(dimension.name()));
            setter.accept(dimension, styleLength);
        }
    }

    public static CompoundTag encodeGutter(Function<YogaGutter, StyleLength> getter) {
        var tag = new CompoundTag();
        for (var gutter : YogaGutter.values()) {
            tag.put(gutter.name(), encodeStyleLength(getter.apply(gutter)));
        }
        return tag;
    }

    public static void decodeGutter(CompoundTag tag, BiConsumer<YogaGutter, StyleLength> setter) {
        for (var gutter : YogaGutter.values()) {
            setter.accept(gutter, decodeStyleLength(tag.get(gutter.name())));
        }
    }

}
