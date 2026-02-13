package com.lowdragmc.lowdraglib2.gui.ui.layout;

import com.lowdragmc.lowdraglib2.utils.LDLibExtraCodecs;
import com.mojang.serialization.Codec;
import lombok.experimental.UtilityClass;
import net.minecraft.nbt.*;
import org.appliedenergistics.yoga.*;
import org.appliedenergistics.yoga.numeric.FloatOptional;
import org.appliedenergistics.yoga.style.StyleLength;
import org.appliedenergistics.yoga.style.StyleSizeLength;

@UtilityClass
@Deprecated
public final class YogaCodecs {
    public static final Codec<FloatOptional> FLOAT_OPTIONAL_CODEC = LDLibExtraCodecs.TAG.xmap(YogaCodecs::decodeFloatOptional, YogaCodecs::encodeFloatOptional);
    public static final Codec<StyleSizeLength> STYLE_SIZE_LENGTH_CODEC = LDLibExtraCodecs.TAG.xmap(YogaCodecs::decodeStyleSizeLength, YogaCodecs::encodeStyleSizeLength);
    public static final Codec<StyleLength> STYLE_LENGTH_CODEC = LDLibExtraCodecs.TAG.xmap(YogaCodecs::decodeStyleLength, YogaCodecs::encodeStyleLength);

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
}
