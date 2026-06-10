package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.LDLib2;
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
    private static final ResourceLocation GDP_ICONS = LDLib2.id("textures/gui/icon/gdp_icons.png");
    private static final BiFunction<String, String, SpriteTexture> CACHE = Util.memoize((modID, name) -> 
            SpriteTexture.of("%s:textures/gui/icon/%s.png".formatted(modID, name)));
    private static final Map<String, SpriteTexture> FILE_ICONS = new HashMap<>();
    public static SpriteTexture ICON = SpriteTexture.of("ldlib2:textures/gui/icon.png");
    public static SpriteTexture LEFT = SpriteTexture.of("ldlib2:textures/gui/left.png");
    public static SpriteTexture UP = SpriteTexture.of("ldlib2:textures/gui/up.png");
    public static SpriteTexture DOWN = SpriteTexture.of("ldlib2:textures/gui/down.png");
    public static SpriteTexture RIGHT = SpriteTexture.of("ldlib2:textures/gui/right.png");
    public static SpriteTexture ROTATION = icon("rotation");
    public static SpriteTexture REPLAY = icon("replay");
    public static SpriteTexture PLAY_PAUSE = icon("play_pause");
    public static SpriteTexture PLAY = icon("play");
    public static SpriteTexture STOP = icon("stop");
    public static SpriteTexture RESOURCE = icon("resource");
    public static SpriteTexture PALETTE = icon("palette");
    public static SpriteTexture RESOURCE_SETTING = icon("resource_setting");
    public static SpriteTexture WIDGET_SETTING = icon("widget_setting");
    public static SpriteTexture WIDGET_BASIC = icon("widget_basic");
    public static SpriteTexture WIDGET_GROUP = icon("widget_group");
    public static SpriteTexture WIDGET_CONTAINER = icon("widget_container");
    public static SpriteTexture WIDGET_CUSTOM = icon("widget_custom");
    public static SpriteTexture CURSOR = icon("cursor");
    public static SpriteTexture MOVE = icon("move");
    public static SpriteTexture LINK = icon("link");
    public static SpriteTexture GRID = icon("grid");
    public static SpriteTexture ADD = icon("add");
    public static SpriteTexture SAVE = icon("save");
    public static SpriteTexture HELP = icon("help");
    public static SpriteTexture COPY = icon("copy");
    public static SpriteTexture PASTE = icon("paste");
    public static SpriteTexture CUT = icon("cut");
    public static SpriteTexture CLOSE = icon("close");
    public static SpriteTexture REMOVE = icon("remove");
    public static SpriteTexture DELETE = icon("delete");
    public static SpriteTexture EXPORT = icon("export");
    public static SpriteTexture IMPORT = icon("import");
    public static SpriteTexture OPEN_FILE = icon("open_file");
    public static SpriteTexture ADD_FILE = icon("add_file");
    public static SpriteTexture EDIT_FILE = icon("edit_file");
    public static SpriteTexture REMOVE_FILE = icon("remove_file");
    public static SpriteTexture EDIT_ON = icon("edit_on");
    public static SpriteTexture EDIT_OFF = icon("edit_off");
    public static SpriteTexture CHECK = icon("check");
    public static SpriteTexture HISTORY = icon("history");
    public static SpriteTexture INFORMATION = icon("information");
    public static SpriteTexture MESH = icon("mesh");
    public static SpriteTexture EYE = icon("eye");
    public static SpriteTexture EYE_OFF = icon("eye_off");
    public static SpriteTexture FOLDER = icon("folder");
    public static SpriteTexture FILE = icon("file");
    public static SpriteTexture IMAGE = icon("image");
    public static SpriteTexture JSON = icon("json");
    public static SpriteTexture LSS = icon("css");
    public static SpriteTexture XML = icon("xml");
    public static SpriteTexture RADIOBOX_BLANK = icon("radiobox_blank");
    public static SpriteTexture RADIOBOX_MARKED = icon("radiobox_marked");
    public static SpriteTexture PLAY_EMPTY = icon("play_empty");
    public static SpriteTexture PLAY_FILL = icon("play_fill");
    public static SpriteTexture CHECKBOX_BLANK = icon("checkbox_blank");
    public static SpriteTexture CHECKBOX_MARKED = icon("checkbox_marked");
    public static SpriteTexture TRANSFORM_TRANSLATE = icon("transform_translate");
    public static SpriteTexture TRANSFORM_ROTATE = icon("transform_rotate");
    public static SpriteTexture TRANSFORM_SCALE = icon("transform_scale");
    public static SpriteTexture GLOBAL = icon("global");
    public static SpriteTexture LOCAL = icon("local");
    public static SpriteTexture WINDOW_MINIMIZE = icon("window_minimize");
    public static SpriteTexture WINDOW_MAXIMIZE = icon("window_maximize");
    public static SpriteTexture WINDOW_RESTORE = icon("window_restore");
    public static SpriteTexture WINDOW_CLOSE = icon("window_close");
    //align
    public static SpriteTexture ALIGN_H_C = icon("align_horizontal_center");
    public static SpriteTexture ALIGN_H_D = icon("align_horizontal_distribute");
    public static SpriteTexture ALIGN_H_L = icon("align_horizontal_left");
    public static SpriteTexture ALIGN_H_R = icon("align_horizontal_right");
    public static SpriteTexture ALIGN_V_C = icon("align_vertical_center");
    public static SpriteTexture ALIGN_V_D = icon("align_vertical_distribute");
    public static SpriteTexture ALIGN_V_T = icon("align_vertical_top");
    public static SpriteTexture ALIGN_V_B = icon("align_vertical_bottom");
    public static SpriteTexture COLOR = icon("color");
    public static SpriteTexture PICTURE = icon("picture");
    public static SpriteTexture MATERIAL = icon("material");
    public static SpriteTexture MODEL = icon("model");
    public static SpriteTexture TRANSLATE = icon("translate");
    public static SpriteTexture GRADIENT = icon("gradient");
    public static SpriteTexture CURVE = icon("curve");
    public static SpriteTexture SETTINGS = icon("settings");
    public static SpriteTexture MAGNET = icon("magnet");
    public static SpriteTexture LEFT_CLICK = icon("left_click");
    public static SpriteTexture RIGHT_CLICK = icon("right_click");
    public static SpriteTexture SCREEN = icon("screen");
    public static SpriteTexture CAMERA = icon("camera");
    public static SpriteTexture PAGE_FIT = icon("page_fit");
    public static SpriteTexture COLLAPSE_VERTICAL = icon("collapse_vertical");
    public static SpriteTexture COLLAPSE_HORIZONTAL = icon("collapse_horizontal");
    public static SpriteTexture EXPAND_VERTICAL = icon("expand_vertical");
    public static SpriteTexture EXPAND_HORIZONTAL = icon("expand_horizontal");


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
    public static SpriteTexture ARROW_LT_RB = (SpriteTexture) SpriteTexture.of(GDP_ICONS).setSprite(214, 191, 13, 7).rotate(45);
    public static SpriteTexture ARROW_RT_LB = (SpriteTexture) SpriteTexture.of(GDP_ICONS).setSprite(214, 191, 13, 7).rotate(-45);


    public static SpriteTexture NOWRAP = icon("nowrap");
    public static SpriteTexture WRAP = icon("wrap");
    public static SpriteTexture WRAP_REVERSE = icon("wrap_reverse");

    public static SpriteTexture EDITING = icon("editing");
    public static SpriteTexture NON_EDITING = icon("non_editing");

    // graph view
    public static SpriteTexture RESIZE_BOTTOM_RIGHT = icon("resize_bottom_right");
    public static SpriteTexture NODE = icon("node");
    public static SpriteTexture BOOL = icon("bool");
    public static SpriteTexture FLOAT = icon("float");
    public static SpriteTexture INT = icon("int");
    public static SpriteTexture LONG = icon("long");
    public static SpriteTexture STRING = icon("string");

    static {
        registerFileIcon(IMAGE, "png", "jpg", "jpeg");
        registerFileIcon(JSON, "json", "nbt");
        registerFileIcon(LSS, "lss");
    }

    public static SpriteTexture icon(String name) {
        return icon(LDLib2.MOD_ID, name);
    }

    public static SpriteTexture icon(String modId, String name) {
        return CACHE.apply(modId, name);
    }

    public static void registerFileIcon(SpriteTexture icon, String... suffixes) {
        for (String suffix : suffixes) {
            FILE_ICONS.put(suffix.toLowerCase(), icon);
        }
    }

    public static SpriteTexture getIcon(String suffix) {
        return FILE_ICONS.getOrDefault(suffix.toLowerCase(), FILE);
    }

}
