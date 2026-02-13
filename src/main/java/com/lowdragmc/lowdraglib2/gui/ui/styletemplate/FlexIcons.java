package com.lowdragmc.lowdraglib2.gui.ui.styletemplate;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import lombok.experimental.UtilityClass;
import net.minecraft.resources.ResourceLocation;

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

    public static IGuiTexture getFlexWrapIcon(FlexWrap wrap) {
        return switch (wrap) {
            case FlexWrap.NO_WRAP -> Icons.NOWRAP;
            case FlexWrap.WRAP -> Icons.WRAP;
            case FlexWrap.WRAP_REVERSE -> Icons.WRAP_REVERSE;
        };
    }

    public static SpriteTexture getFlexDirectionIcon(FlexDirection flexDirection) {
        return switch (flexDirection) {
            case ROW -> FLEX_DIRECTION_ROW;
            case COLUMN -> FLEX_DIRECTION_COLUMN;
            case ROW_REVERSE -> FLEX_DIRECTION_ROW_REVERSE;
            case COLUMN_REVERSE -> FLEX_DIRECTION_COLUMN_REVERSE;
        };
    }

    public static SpriteTexture getAlignContentIcon(FlexDirection flexDirection, AlignContent alignContent) {
        var isRow = flexDirection == FlexDirection.ROW || flexDirection == FlexDirection.ROW_REVERSE;
        return isRow ? switch (alignContent) {
            case FLEX_START, START -> ALIGN_CONTENTS_FLEX_START_ROW;
            case FLEX_END, END -> ALIGN_CONTENTS_FLEX_END_ROW;
            case CENTER -> ALIGN_CONTENTS_CENTER_ROW;
            case STRETCH -> ALIGN_CONTENTS_STRETCH_ROW;
            case SPACE_BETWEEN-> JUSTIFY_CONTENTS_SPACE_BETWEEN_COLUMN;
            case SPACE_AROUND -> JUSTIFY_CONTENTS_SPACE_AROUND_COLUMN;
            case SPACE_EVENLY -> JUSTIFY_CONTENTS_SPACE_EVENLY_COLUMN;
            case AUTO -> AUTO_ROW;
        } : switch (alignContent) {
            case FLEX_START, START -> ALIGN_CONTENTS_FLEX_START_COLUMN;
            case FLEX_END, END -> ALIGN_CONTENTS_FLEX_END_COLUMN;
            case CENTER -> ALIGN_CONTENTS_CENTER_COLUMN;
            case STRETCH -> ALIGN_CONTENTS_STRETCH_COLUMN;
            case SPACE_BETWEEN-> JUSTIFY_CONTENTS_SPACE_BETWEEN_ROW;
            case SPACE_AROUND -> JUSTIFY_CONTENTS_SPACE_AROUND_ROW;
            case SPACE_EVENLY -> JUSTIFY_CONTENTS_SPACE_EVENLY_ROW;
            case AUTO -> AUTO_COLUMN;
        };
    }

    public static SpriteTexture getJustifyContentIcon(FlexDirection flexDirection, AlignContent alignContent) {
        return switch (flexDirection) {
            case COLUMN -> switch (alignContent) {
                case FLEX_START, START   -> JUSTIFY_CONTENTS_FLEX_START_COLUMN;
                case FLEX_END, END     -> JUSTIFY_CONTENTS_FLEX_END_COLUMN;
                case CENTER       -> JUSTIFY_CONTENTS_CENTER_COLUMN;
                case SPACE_BETWEEN-> JUSTIFY_CONTENTS_SPACE_BETWEEN_COLUMN;
                case SPACE_AROUND -> JUSTIFY_CONTENTS_SPACE_AROUND_COLUMN;
                case SPACE_EVENLY -> JUSTIFY_CONTENTS_SPACE_EVENLY_COLUMN;
                case STRETCH -> ALIGN_CONTENTS_STRETCH_COLUMN;
                case AUTO -> AUTO_COLUMN;
            };
            case COLUMN_REVERSE -> switch (alignContent) {
                case FLEX_START, START   -> JUSTIFY_CONTENTS_FLEX_START_COLUMN_REVERSE;
                case FLEX_END, END     -> JUSTIFY_CONTENTS_FLEX_END_COLUMN_REVERSE;
                case CENTER       -> JUSTIFY_CONTENTS_CENTER_COLUMN_REVERSE;
                case SPACE_BETWEEN-> JUSTIFY_CONTENTS_SPACE_BETWEEN_COLUMN_REVERSE;
                case SPACE_AROUND -> JUSTIFY_CONTENTS_SPACE_AROUND_COLUMN_REVERSE;
                case SPACE_EVENLY -> JUSTIFY_CONTENTS_SPACE_EVENLY_COLUMN_REVERSE;
                case STRETCH -> ALIGN_CONTENTS_STRETCH_COLUMN;
                case AUTO -> AUTO_COLUMN;
            };
            case ROW -> switch (alignContent) {
                case FLEX_START, START   -> JUSTIFY_CONTENTS_FLEX_START_ROW;
                case FLEX_END, END     -> JUSTIFY_CONTENTS_FLEX_END_ROW;
                case CENTER       -> JUSTIFY_CONTENTS_CENTER_ROW;
                case SPACE_BETWEEN-> JUSTIFY_CONTENTS_SPACE_BETWEEN_ROW;
                case SPACE_AROUND -> JUSTIFY_CONTENTS_SPACE_AROUND_ROW;
                case SPACE_EVENLY -> JUSTIFY_CONTENTS_SPACE_EVENLY_ROW;
                case STRETCH -> ALIGN_CONTENTS_STRETCH_ROW;
                case AUTO -> AUTO_ROW;
            };
            case ROW_REVERSE -> switch (alignContent) {
                case FLEX_START, START   -> JUSTIFY_CONTENTS_FLEX_START_ROW_REVERSE;
                case FLEX_END, END     -> JUSTIFY_CONTENTS_FLEX_END_ROW_REVERSE;
                case CENTER       -> JUSTIFY_CONTENTS_CENTER_ROW_REVERSE;
                case SPACE_BETWEEN-> JUSTIFY_CONTENTS_SPACE_BETWEEN_ROW_REVERSE;
                case SPACE_AROUND -> JUSTIFY_CONTENTS_SPACE_AROUND_ROW_REVERSE;
                case SPACE_EVENLY -> JUSTIFY_CONTENTS_SPACE_EVENLY_ROW_REVERSE;
                case STRETCH -> ALIGN_CONTENTS_STRETCH_ROW;
                case AUTO -> AUTO_ROW;
            };
        };
    }

    // TODO Baseline icon?
    public static SpriteTexture getAlignItemIcon(FlexDirection flexDirection, AlignItems alignItems) {
        return switch (flexDirection) {
            case COLUMN -> switch (alignItems) {
                case FLEX_START, START -> ALIGN_ITEMS_FLEX_START_COLUMN;
                case FLEX_END, END -> ALIGN_ITEMS_FLEX_END_COLUMN;
                case CENTER -> ALIGN_ITEMS_CENTER_COLUMN;
                case STRETCH -> ALIGN_ITEMS_STRETCH_COLUMN;
                case AUTO, BASELINE -> AUTO_COLUMN;
            };
            case COLUMN_REVERSE -> switch (alignItems) {
                case FLEX_START, START -> ALIGN_ITEMS_FLEX_START_COLUMN_REVERSE;
                case FLEX_END, END -> ALIGN_ITEMS_FLEX_END_COLUMN_REVERSE;
                case CENTER -> ALIGN_ITEMS_CENTER_COLUMN_REVERSE;
                case STRETCH -> ALIGN_ITEMS_STRETCH_COLUMN_REVERSE;
                case AUTO, BASELINE -> AUTO_COLUMN;
            };
            case ROW -> switch (alignItems) {
                case FLEX_START, START -> ALIGN_ITEMS_FLEX_START_ROW;
                case FLEX_END, END -> ALIGN_ITEMS_FLEX_END_ROW;
                case CENTER -> ALIGN_ITEMS_CENTER_ROW;
                case STRETCH -> ALIGN_ITEMS_STRETCH_ROW;
                case AUTO, BASELINE -> AUTO_ROW;
            };
            case ROW_REVERSE -> switch (alignItems) {
                case FLEX_START, START -> ALIGN_ITEMS_FLEX_START_ROW_REVERSE;
                case FLEX_END, END -> ALIGN_ITEMS_FLEX_END_ROW_REVERSE;
                case CENTER -> ALIGN_ITEMS_CENTER_ROW_REVERSE;
                case STRETCH -> ALIGN_ITEMS_STRETCH_ROW_REVERSE;
                case AUTO, BASELINE -> AUTO_ROW;
            };
        };
    }

    public static SpriteTexture getAlignSelfIcon(FlexDirection flexDirection, AlignItems alignSelf) {
        var isRow = flexDirection == FlexDirection.ROW || flexDirection == FlexDirection.ROW_REVERSE;
        return isRow ? switch (alignSelf) {
            case FLEX_START, START -> ALIGN_SELF_FLEX_START_ROW;
            case FLEX_END, END -> ALIGN_SELF_FLEX_END_ROW;
            case CENTER -> ALIGN_SELF_CENTER_ROW;
            case STRETCH -> ALIGN_SELF_STRETCH_ROW;
            case null, default -> AUTO_ROW;
        } : switch (alignSelf) {
            case FLEX_START, START -> ALIGN_SELF_FLEX_START_COLUMN;
            case FLEX_END, END -> ALIGN_SELF_FLEX_END_COLUMN;
            case CENTER -> ALIGN_SELF_CENTER_COLUMN;
            case STRETCH -> ALIGN_SELF_STRETCH_COLUMN;
            case AUTO, BASELINE -> AUTO_COLUMN;
        };
    }
}
