package com.lowdragmc.lowdraglib2.gui.ui.styletemplate;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import lombok.experimental.UtilityClass;
import net.minecraft.resources.ResourceLocation;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaJustify;
import org.appliedenergistics.yoga.YogaWrap;

@UtilityClass
public final class FlexIcons {
    public static ResourceLocation FLEX = LDLib2.id("textures/gui/flex.png");

    public static SpriteTexture create(int x, int y) {
        return SpriteTexture.of(FLEX).setSprite(x * 72, y * 72, 72, 72);
    }

    public static SpriteTexture FLEX_DIRECTION_ROW = create(0, 0);
    public static SpriteTexture FLEX_DIRECTION_COLUMN = create(1, 0);
    public static SpriteTexture FLEX_DIRECTION_ROW_REVERSE = create(2, 0);
    public static SpriteTexture FLEX_DIRECTION_COLUMN_REVERSE = create(3, 0);

    public static SpriteTexture ALIGN_CONTENTS_CENTER_ROW = create(0, 1);
    public static SpriteTexture ALIGN_CONTENTS_FLEX_START_ROW = create(1, 1);
    public static SpriteTexture ALIGN_CONTENTS_FLEX_END_ROW = create(2, 1);
    public static SpriteTexture ALIGN_CONTENTS_STRETCH_ROW = create(3, 1);

    public static SpriteTexture ALIGN_CONTENTS_CENTER_COLUMN = create(0, 2);
    public static SpriteTexture ALIGN_CONTENTS_FLEX_START_COLUMN = create(1, 2);
    public static SpriteTexture ALIGN_CONTENTS_FLEX_END_COLUMN = create(2, 2);
    public static SpriteTexture ALIGN_CONTENTS_STRETCH_COLUMN = create(3, 2);

    public static SpriteTexture JUSTIFY_CONTENTS_CENTER_ROW =  create(0, 3);
    public static SpriteTexture JUSTIFY_CONTENTS_FLEX_START_ROW =  create(1, 3);
    public static SpriteTexture JUSTIFY_CONTENTS_FLEX_END_ROW =  create(2, 3);
    public static SpriteTexture JUSTIFY_CONTENTS_SPACE_BETWEEN_ROW =  create(3, 3);
    public static SpriteTexture JUSTIFY_CONTENTS_SPACE_AROUND_ROW =  create(4, 3);
    public static SpriteTexture JUSTIFY_CONTENTS_SPACE_EVENLY_ROW =  create(5, 3);

    public static SpriteTexture JUSTIFY_CONTENTS_CENTER_ROW_REVERSE =  create(6, 3);
    public static SpriteTexture JUSTIFY_CONTENTS_FLEX_START_ROW_REVERSE =  create(7, 3);
    public static SpriteTexture JUSTIFY_CONTENTS_FLEX_END_ROW_REVERSE =  create(8, 3);
    public static SpriteTexture JUSTIFY_CONTENTS_SPACE_BETWEEN_ROW_REVERSE =  create(9, 3);
    public static SpriteTexture JUSTIFY_CONTENTS_SPACE_AROUND_ROW_REVERSE =  create(10, 3);
    public static SpriteTexture JUSTIFY_CONTENTS_SPACE_EVENLY_ROW_REVERSE =  create(11, 3);

    public static SpriteTexture JUSTIFY_CONTENTS_CENTER_COLUMN =  create(0, 4);
    public static SpriteTexture JUSTIFY_CONTENTS_FLEX_START_COLUMN =  create(1, 4);
    public static SpriteTexture JUSTIFY_CONTENTS_FLEX_END_COLUMN =  create(2, 4);
    public static SpriteTexture JUSTIFY_CONTENTS_SPACE_BETWEEN_COLUMN =  create(3, 4);
    public static SpriteTexture JUSTIFY_CONTENTS_SPACE_AROUND_COLUMN =  create(4, 4);
    public static SpriteTexture JUSTIFY_CONTENTS_SPACE_EVENLY_COLUMN =  create(5, 4);

    public static SpriteTexture JUSTIFY_CONTENTS_CENTER_COLUMN_REVERSE =  create(6, 4);
    public static SpriteTexture JUSTIFY_CONTENTS_FLEX_START_COLUMN_REVERSE =  create(7, 4);
    public static SpriteTexture JUSTIFY_CONTENTS_FLEX_END_COLUMN_REVERSE =  create(8, 4);
    public static SpriteTexture JUSTIFY_CONTENTS_SPACE_BETWEEN_COLUMN_REVERSE =  create(9, 4);
    public static SpriteTexture JUSTIFY_CONTENTS_SPACE_AROUND_COLUMN_REVERSE =  create(10, 4);
    public static SpriteTexture JUSTIFY_CONTENTS_SPACE_EVENLY_COLUMN_REVERSE =  create(11, 4);

    public static SpriteTexture ALIGN_ITEMS_CENTER_ROW = create(0, 5);
    public static SpriteTexture ALIGN_ITEMS_FLEX_START_ROW = create(1, 5);
    public static SpriteTexture ALIGN_ITEMS_FLEX_END_ROW = create(2, 5);
    public static SpriteTexture ALIGN_ITEMS_STRETCH_ROW = create(3, 5);
    public static SpriteTexture ALIGN_ITEMS_CENTER_ROW_REVERSE = create(5, 5);
    public static SpriteTexture ALIGN_ITEMS_FLEX_START_ROW_REVERSE = create(6, 5);
    public static SpriteTexture ALIGN_ITEMS_FLEX_END_ROW_REVERSE = create(7, 5);
    public static SpriteTexture ALIGN_ITEMS_STRETCH_ROW_REVERSE = create(8, 5);


    public static SpriteTexture ALIGN_ITEMS_CENTER_COLUMN = create(0, 6);
    public static SpriteTexture ALIGN_ITEMS_FLEX_START_COLUMN = create(1, 6);
    public static SpriteTexture ALIGN_ITEMS_FLEX_END_COLUMN = create(2, 6);
    public static SpriteTexture ALIGN_ITEMS_STRETCH_COLUMN = create(3, 6);
    public static SpriteTexture ALIGN_ITEMS_CENTER_COLUMN_REVERSE = create(5, 6);
    public static SpriteTexture ALIGN_ITEMS_FLEX_START_COLUMN_REVERSE = create(6, 6);
    public static SpriteTexture ALIGN_ITEMS_FLEX_END_COLUMN_REVERSE = create(7, 6);
    public static SpriteTexture ALIGN_ITEMS_STRETCH_COLUMN_REVERSE = create(8, 6);


    public static SpriteTexture AUTO_ROW = create(4, 5);
    public static SpriteTexture AUTO_COLUMN = create(4, 6);

    public static SpriteTexture ALIGN_SELF_CENTER_ROW = create(0, 7);
    public static SpriteTexture ALIGN_SELF_FLEX_START_ROW = create(1, 7);
    public static SpriteTexture ALIGN_SELF_FLEX_END_ROW = create(2, 7);
    public static SpriteTexture ALIGN_SELF_STRETCH_ROW = create(3, 7);

    public static SpriteTexture ALIGN_SELF_CENTER_COLUMN = create(0, 8);
    public static SpriteTexture ALIGN_SELF_FLEX_START_COLUMN = create(1, 8);
    public static SpriteTexture ALIGN_SELF_FLEX_END_COLUMN = create(2, 8);
    public static SpriteTexture ALIGN_SELF_STRETCH_COLUMN = create(3, 8);

    public static IGuiTexture getFlexWrapIcon(YogaWrap wrap) {
        return switch (wrap) {
            case YogaWrap.NO_WRAP -> Icons.NOWRAP;
            case YogaWrap.WRAP -> Icons.WRAP;
            case YogaWrap.WRAP_REVERSE -> Icons.WRAP_REVERSE;
        };
    }

    public static SpriteTexture getFlexDirectionIcon(YogaFlexDirection flexDirection) {
        return switch (flexDirection) {
            case ROW -> FLEX_DIRECTION_ROW;
            case COLUMN -> FLEX_DIRECTION_COLUMN;
            case ROW_REVERSE -> FLEX_DIRECTION_ROW_REVERSE;
            case COLUMN_REVERSE -> FLEX_DIRECTION_COLUMN_REVERSE;
        };
    }

    public static SpriteTexture getAlignContentIcon(YogaFlexDirection flexDirection, YogaAlign yogaAlign) {
        var isRow = flexDirection == YogaFlexDirection.ROW || flexDirection == YogaFlexDirection.ROW_REVERSE;
        return isRow ? switch (yogaAlign) {
            case YogaAlign.FLEX_START -> ALIGN_CONTENTS_FLEX_START_ROW;
            case YogaAlign.FLEX_END -> ALIGN_CONTENTS_FLEX_END_ROW;
            case YogaAlign.CENTER -> ALIGN_CONTENTS_CENTER_ROW;
            case YogaAlign.STRETCH -> ALIGN_CONTENTS_STRETCH_ROW;
            default -> AUTO_ROW;
        } : switch (yogaAlign) {
            case YogaAlign.FLEX_START -> ALIGN_CONTENTS_FLEX_START_COLUMN;
            case YogaAlign.FLEX_END -> ALIGN_CONTENTS_FLEX_END_COLUMN;
            case YogaAlign.CENTER -> ALIGN_CONTENTS_CENTER_COLUMN;
            case YogaAlign.STRETCH -> ALIGN_CONTENTS_STRETCH_COLUMN;
            default -> AUTO_COLUMN;
        };
    }

    public static SpriteTexture getJustifyContentIcon(YogaFlexDirection flexDirection, YogaJustify yogaJustify) {
        return switch (flexDirection) {
            case COLUMN -> switch (yogaJustify) {
                case FLEX_START   -> JUSTIFY_CONTENTS_FLEX_START_COLUMN;
                case FLEX_END     -> JUSTIFY_CONTENTS_FLEX_END_COLUMN;
                case CENTER       -> JUSTIFY_CONTENTS_CENTER_COLUMN;
                case SPACE_BETWEEN-> JUSTIFY_CONTENTS_SPACE_BETWEEN_COLUMN;
                case SPACE_AROUND -> JUSTIFY_CONTENTS_SPACE_AROUND_COLUMN;
                case SPACE_EVENLY -> JUSTIFY_CONTENTS_SPACE_EVENLY_COLUMN;
            };
            case COLUMN_REVERSE -> switch (yogaJustify) {
                case FLEX_START   -> JUSTIFY_CONTENTS_FLEX_START_COLUMN_REVERSE;
                case FLEX_END     -> JUSTIFY_CONTENTS_FLEX_END_COLUMN_REVERSE;
                case CENTER       -> JUSTIFY_CONTENTS_CENTER_COLUMN_REVERSE;
                case SPACE_BETWEEN-> JUSTIFY_CONTENTS_SPACE_BETWEEN_COLUMN_REVERSE;
                case SPACE_AROUND -> JUSTIFY_CONTENTS_SPACE_AROUND_COLUMN_REVERSE;
                case SPACE_EVENLY -> JUSTIFY_CONTENTS_SPACE_EVENLY_COLUMN_REVERSE;
            };
            case ROW -> switch (yogaJustify) {
                case FLEX_START   -> JUSTIFY_CONTENTS_FLEX_START_ROW;
                case FLEX_END     -> JUSTIFY_CONTENTS_FLEX_END_ROW;
                case CENTER       -> JUSTIFY_CONTENTS_CENTER_ROW;
                case SPACE_BETWEEN-> JUSTIFY_CONTENTS_SPACE_BETWEEN_ROW;
                case SPACE_AROUND -> JUSTIFY_CONTENTS_SPACE_AROUND_ROW;
                case SPACE_EVENLY -> JUSTIFY_CONTENTS_SPACE_EVENLY_ROW;
            };
            case ROW_REVERSE -> switch (yogaJustify) {
                case FLEX_START   -> JUSTIFY_CONTENTS_FLEX_START_ROW_REVERSE;
                case FLEX_END     -> JUSTIFY_CONTENTS_FLEX_END_ROW_REVERSE;
                case CENTER       -> JUSTIFY_CONTENTS_CENTER_ROW_REVERSE;
                case SPACE_BETWEEN-> JUSTIFY_CONTENTS_SPACE_BETWEEN_ROW_REVERSE;
                case SPACE_AROUND -> JUSTIFY_CONTENTS_SPACE_AROUND_ROW_REVERSE;
                case SPACE_EVENLY -> JUSTIFY_CONTENTS_SPACE_EVENLY_ROW_REVERSE;
            };
        };
    }

    public static SpriteTexture getAlignItemIcon(YogaFlexDirection flexDirection, YogaAlign yogaAlign) {
        return switch (flexDirection) {
            case COLUMN -> switch (yogaAlign) {
                case YogaAlign.FLEX_START -> ALIGN_ITEMS_FLEX_START_COLUMN;
                case YogaAlign.FLEX_END -> ALIGN_ITEMS_FLEX_END_COLUMN;
                case YogaAlign.CENTER -> ALIGN_ITEMS_CENTER_COLUMN;
                case YogaAlign.STRETCH -> ALIGN_ITEMS_STRETCH_COLUMN;
                default -> AUTO_COLUMN;
            };
            case COLUMN_REVERSE -> switch (yogaAlign) {
                case YogaAlign.FLEX_START -> ALIGN_ITEMS_FLEX_START_COLUMN_REVERSE;
                case YogaAlign.FLEX_END -> ALIGN_ITEMS_FLEX_END_COLUMN_REVERSE;
                case YogaAlign.CENTER -> ALIGN_ITEMS_CENTER_COLUMN_REVERSE;
                case YogaAlign.STRETCH -> ALIGN_ITEMS_STRETCH_COLUMN_REVERSE;
                default -> AUTO_COLUMN;
            };
            case ROW -> switch (yogaAlign) {
                case YogaAlign.FLEX_START -> ALIGN_ITEMS_FLEX_START_ROW;
                case YogaAlign.FLEX_END -> ALIGN_ITEMS_FLEX_END_ROW;
                case YogaAlign.CENTER -> ALIGN_ITEMS_CENTER_ROW;
                case YogaAlign.STRETCH -> ALIGN_ITEMS_STRETCH_ROW;
                default -> AUTO_ROW;
            };
            case ROW_REVERSE -> switch (yogaAlign) {
                case YogaAlign.FLEX_START -> ALIGN_ITEMS_FLEX_START_ROW_REVERSE;
                case YogaAlign.FLEX_END -> ALIGN_ITEMS_FLEX_END_ROW_REVERSE;
                case YogaAlign.CENTER -> ALIGN_ITEMS_CENTER_ROW_REVERSE;
                case YogaAlign.STRETCH -> ALIGN_ITEMS_STRETCH_ROW_REVERSE;
                default -> AUTO_ROW;
            };
        };
    }

    public static SpriteTexture getAlignSelfIcon(YogaFlexDirection flexDirection, YogaAlign yogaAlign) {
        var isRow = flexDirection == YogaFlexDirection.ROW || flexDirection == YogaFlexDirection.ROW_REVERSE;
        return isRow ? switch (yogaAlign) {
            case YogaAlign.FLEX_START -> ALIGN_SELF_FLEX_START_ROW;
            case YogaAlign.FLEX_END -> ALIGN_SELF_FLEX_END_ROW;
            case YogaAlign.CENTER -> ALIGN_SELF_CENTER_ROW;
            case YogaAlign.STRETCH -> ALIGN_SELF_STRETCH_ROW;
            default -> AUTO_ROW;
        } : switch (yogaAlign) {
            case YogaAlign.FLEX_START -> ALIGN_SELF_FLEX_START_COLUMN;
            case YogaAlign.FLEX_END -> ALIGN_SELF_FLEX_END_COLUMN;
            case YogaAlign.CENTER -> ALIGN_SELF_CENTER_COLUMN;
            case YogaAlign.STRETCH -> ALIGN_SELF_STRETCH_COLUMN;
            default -> AUTO_COLUMN;
        };
    }
}
