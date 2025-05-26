package com.lowdragmc.lowdraglib.editor_outdated;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.texture.*;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * @author KilaBash
 * @date 2022/12/1
 * @implNote Icons
 */
public class Icons {
    private static final ResourceLocation GDP_ICONS = LDLib.id("textures/gui/icon/gdp_icons.png");
    private static final BiFunction<String, String, ResourceTexture> CACHE = Util.memoize((modID, name) -> new ResourceTexture("%s:textures/gui/icon/%s.png".formatted(modID, name)));
    private static final Map<String, ResourceTexture> FILE_ICONS = new HashMap<>();
    public static ResourceTexture LEFT = new ResourceTexture("ldlib:textures/gui/left.png");
    public static ResourceTexture UP = new ResourceTexture("ldlib:textures/gui/up.png");
    public static ResourceTexture DOWN = new ResourceTexture("ldlib:textures/gui/down.png");
    public static ResourceTexture RIGHT = new ResourceTexture("ldlib:textures/gui/right.png");
    public static ResourceTexture ROTATION = icon("rotation");
    public static ResourceTexture REPLAY = icon("replay");
    public static ResourceTexture PLAY_PAUSE = icon("play_pause");
    public static ResourceTexture RESOURCE = icon("resource");
    public static ResourceTexture PALETTE = icon("palette");
    public static ResourceTexture RESOURCE_SETTING = icon("resource_setting");
    public static ResourceTexture WIDGET_SETTING = icon("widget_setting");
    public static ResourceTexture WIDGET_BASIC = icon("widget_basic");
    public static ResourceTexture WIDGET_GROUP = icon("widget_group");
    public static ResourceTexture WIDGET_CONTAINER = icon("widget_container");
    public static ResourceTexture WIDGET_CUSTOM = icon("widget_custom");
    public static ResourceTexture CURSOR = icon("cursor");
    public static ResourceTexture MOVE = icon("move");
    public static ResourceTexture LINK = icon("link");
    public static ResourceTexture GRID = icon("grid");
    public static ResourceTexture ADD = icon("add");
    public static ResourceTexture SAVE = icon("save");
    public static ResourceTexture HELP = icon("help");
    public static ResourceTexture COPY = icon("copy");
    public static ResourceTexture PASTE = icon("paste");
    public static ResourceTexture CUT = icon("cut");
    public static ResourceTexture REMOVE = icon("remove");
    public static ResourceTexture DELETE = icon("delete");
    public static ResourceTexture EXPORT = icon("export");
    public static ResourceTexture IMPORT = icon("import");
    public static ResourceTexture OPEN_FILE = icon("open_file");
    public static ResourceTexture ADD_FILE = icon("add_file");
    public static ResourceTexture EDIT_FILE = icon("edit_file");
    public static ResourceTexture REMOVE_FILE = icon("remove_file");
    public static ResourceTexture EDIT_ON = icon("edit_on");
    public static ResourceTexture EDIT_OFF = icon("edit_off");
    public static ResourceTexture CHECK = icon("check");
    public static ResourceTexture HISTORY = icon("history");
    public static ResourceTexture INFORMATION = icon("information");
    public static ResourceTexture MESH = icon("mesh");
    public static ResourceTexture EYE = icon("eye");
    public static ResourceTexture EYE_OFF = icon("eye_off");
    public static ResourceTexture FOLDER = icon("folder");
    public static ResourceTexture FILE = icon("file");
    public static ResourceTexture IMAGE = icon("image");
    public static ResourceTexture JSON = icon("json");
    public static ResourceTexture RADIOBOX_BLANK = icon("radiobox_blank");
    public static ResourceTexture RADIOBOX_MARKED = icon("radiobox_marked");
    public static ResourceTexture CHECKBOX_BLANK = icon("checkbox_blank");
    public static ResourceTexture CHECKBOX_MARKED = icon("checkbox_marked");
    public static ResourceTexture TRANSFORM_TRANSLATE = icon("transform_translate");
    public static ResourceTexture TRANSFORM_ROTATE = icon("transform_rotate");
    public static ResourceTexture TRANSFORM_SCALE = icon("transform_scale");
    public static ResourceTexture GLOBAL = icon("global");
    public static ResourceTexture LOCAL = icon("local");
    //align
    public static ResourceTexture ALIGN_H_C = icon("align_horizontal_center");
    public static ResourceTexture ALIGN_H_D = icon("align_horizontal_distribute");
    public static ResourceTexture ALIGN_H_L = icon("align_horizontal_left");
    public static ResourceTexture ALIGN_H_R = icon("align_horizontal_right");
    public static ResourceTexture ALIGN_V_C = icon("align_vertical_center");
    public static ResourceTexture ALIGN_V_D = icon("align_vertical_distribute");
    public static ResourceTexture ALIGN_V_T = icon("align_vertical_top");
    public static ResourceTexture ALIGN_V_B = icon("align_vertical_bottom");
    public static ResourceTexture COLOR = icon("color");
    public static ResourceTexture PICTURE = icon("picture");
    public static ResourceTexture MATERIAL = icon("material");
    public static ResourceTexture MODEL = icon("model");
    public static ResourceTexture TRANSLATE = icon("translate");



    public static SpriteTexture CHECK_SPRITE = SpriteTexture.of(GDP_ICONS).setSprite(72, 0, 12, 12);

    public static SpriteTexture DOWN_ARROW_NO_BAR = SpriteTexture.of(GDP_ICONS).setSprite(36, 24, 12, 12);
    public static SpriteTexture DOWN_ARROW_NO_BAR_S = SpriteTexture.of(GDP_ICONS).setSprite(114, 230, 5, 5);
    public static SpriteTexture DOWN_ARROW_NO_BAR_S_LIGHT = SpriteTexture.of(GDP_ICONS).setSprite(126, 230, 5, 5);
    public static SpriteTexture DOWN_ARROW_NO_BAR_S_WHITE= SpriteTexture.of(GDP_ICONS).setSprite(132, 230, 5, 5);
    public static SpriteTexture UP_ARROW_NO_BAR = SpriteTexture.of(GDP_ICONS).setSprite(48, 24, 12, 12);
    public static SpriteTexture UP_ARROW_NO_BAR_S = SpriteTexture.of(GDP_ICONS).setSprite(114, 226, 5, 5);
    public static SpriteTexture UP_ARROW_NO_BAR_S_LIGHT = SpriteTexture.of(GDP_ICONS).setSprite(126, 226, 5, 5);
    public static SpriteTexture UP_ARROW_NO_BAR_S_WHITE= SpriteTexture.of(GDP_ICONS).setSprite(132, 226, 5, 5);
    public static SpriteTexture LEFT_ARROW_NO_BAR = SpriteTexture.of(GDP_ICONS).setSprite(0, 24, 12, 12);
    public static SpriteTexture LEFT_ARROW_NO_BAR_S = SpriteTexture.of(GDP_ICONS).setSprite(154, 240, 5, 5);
    public static SpriteTexture LEFT_ARROW_NO_BAR_S_LIGHT = SpriteTexture.of(GDP_ICONS).setSprite(174, 240, 5, 5);
    public static SpriteTexture LEFT_ARROW_NO_BAR_S_WHITE= SpriteTexture.of(GDP_ICONS).setSprite(184, 240, 5, 5);
    public static SpriteTexture RIGHT_ARROW_NO_BAR = SpriteTexture.of(GDP_ICONS).setSprite(12, 24, 12, 12);
    public static SpriteTexture RIGHT_ARROW_NO_BAR_S = SpriteTexture.of(GDP_ICONS).setSprite(158, 240, 5, 5);
    public static SpriteTexture RIGHT_ARROW_NO_BAR_S_LIGHT = SpriteTexture.of(GDP_ICONS).setSprite(178, 240, 5, 5);
    public static SpriteTexture RIGHT_ARROW_NO_BAR_S_WHITE = SpriteTexture.of(GDP_ICONS).setSprite(188, 240, 5, 5);

    public static SpriteTexture ARROW_LEFT_RIGHT = SpriteTexture.of(GDP_ICONS).setSprite(214, 191, 13, 7);
    public static SpriteTexture ARROW_UP_DOWN = SpriteTexture.of(GDP_ICONS).setSprite(219, 233, 7, 11);

    static {
        registerFileIcon(IMAGE, "png", "jpg", "jpeg");
        registerFileIcon(JSON, "json", "nbt");
    }

    public static ResourceTexture icon(String name) {
        return icon("ldlib", name);
    }

    public static ResourceTexture icon(String modId, String name) {
        return CACHE.apply(modId, name);
    }

    public static IGuiTexture borderText(int border, String text, int color) {
        return new GuiTextureGroup(new ColorBorderTexture(border, color), new TextTexture(text, color).transform(1, 1));
    }

    public static IGuiTexture borderText(String text) {
        return borderText(1, text, -1);
    }

    public static void registerFileIcon(ResourceTexture icon, String... suffixes) {
        for (String suffix : suffixes) {
            FILE_ICONS.put(suffix.toLowerCase(), icon);
        }
    }

    public static ResourceTexture getIcon(String suffix) {
        return FILE_ICONS.getOrDefault(suffix.toLowerCase(), FILE);
    }

}
