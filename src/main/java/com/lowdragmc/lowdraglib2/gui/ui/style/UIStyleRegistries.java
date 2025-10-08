package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.layout.YogaNodeStyleParser;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.*;
import lombok.experimental.UtilityClass;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@UtilityClass
public final class UIStyleRegistries {
    private static final Map<String, StyleHandler<?>> HANDLERS = new ConcurrentHashMap<>();

    public static <T> void registerHandler(StyleHandler<T> handler) {
        if (HANDLERS.containsKey(handler.getStyleName())) {
            throw new IllegalArgumentException("A style handler with the name " + handler.getStyleName() + " already exists");
        }
        HANDLERS.put(handler.getStyleName(), handler);
    }

    @SuppressWarnings("unchecked")
    public static <T> StyleHandler<T> getHandler(String name) {
        return (StyleHandler<T>) HANDLERS.get(name);
    }

    public static <T> StyleHandler<T> create(String name, Function<String, StyleValue<T>> creator) {
        var handler = StyleHandler.Simple.of(name, creator);
        registerHandler(handler);
        return handler;
    }

    public static final StyleHandler<Boolean> DRAW_BACKGROUND_WHEN_HOVER = UIStyleRegistries.create("draw-background-when-hover", BoolValue::new);
    public static final StyleHandler<IGuiTexture> BACKGROUND = UIStyleRegistries.create("background", TextureValue::new);
    public static final StyleHandler<IGuiTexture> BORDER = UIStyleRegistries.create("border", TextureValue::new);
    public static final StyleHandler<IGuiTexture> OVERLAY = UIStyleRegistries.create("overlay", TextureValue::new);
    public static final StyleHandler<List<Component>> TOOLTIPS = UIStyleRegistries.create("tooltips", TooltipsValue::new);
    public static final StyleHandler<Integer> Z_INDEX = UIStyleRegistries.create("z-index", IntValue::new);
    public static final StyleHandler<Transform2D> TRANSFORM_2D = UIStyleRegistries.create("transform", Transform2DValue::new);

    public static final StyleHandler<IGuiTexture> DEFAULT_BACKGROUND = UIStyleRegistries.create("default-background", TextureValue::new);
    public static final StyleHandler<IGuiTexture> HOVER_BACKGROUND = UIStyleRegistries.create("hover-background", TextureValue::new);
    public static final StyleHandler<IGuiTexture> PRESSED_BACKGROUND = UIStyleRegistries.create("pressed-background", TextureValue::new);

    public static final StyleHandler<Boolean> ALLOW_ZOOM = UIStyleRegistries.create("allow-zoom", BoolValue::new);
    public static final StyleHandler<Boolean> ALLOW_PAN = UIStyleRegistries.create("allow-pan", BoolValue::new);
    public static final StyleHandler<Float> MIN_SCALE = UIStyleRegistries.create("min-scale", FloatValue::new);
    public static final StyleHandler<Float> MAX_SCALE = UIStyleRegistries.create("max-scale", FloatValue::new);
    public static final StyleHandler<IGuiTexture> GRID_BACKGROUND = UIStyleRegistries.create("grid-background", TextureValue::new);
    public static final StyleHandler<Float> GRID_SIZE = UIStyleRegistries.create("grid-size", FloatValue::new);

    public static final StyleHandler<IGuiTexture> NODE = UIStyleRegistries.create("node-background", TextureValue::new);
    public static final StyleHandler<IGuiTexture> LEAF = UIStyleRegistries.create("leaf-background", TextureValue::new);
    public static final StyleHandler<IGuiTexture> NODE_HOVER = UIStyleRegistries.create("node-hover-background", TextureValue::new);
    public static final StyleHandler<IGuiTexture> LEAF_HOVER = UIStyleRegistries.create("leaf-hover-background", TextureValue::new);
    public static final StyleHandler<IGuiTexture> ARROW = UIStyleRegistries.create("menu-arrow", TextureValue::new);

    public static final StyleHandler<FillDirection> FILL_DIRECTION = UIStyleRegistries.create("fill-direction", EnumValue.of(FillDirection.class));
    public static final StyleHandler<Boolean> INTERPOLATE = UIStyleRegistries.create("interpolate", BoolValue::new);
    public static final StyleHandler<Float> INTERPOLATE_STEP = UIStyleRegistries.create("interpolate-step", FloatValue::new);

    public static final StyleHandler<Float> SCROLL_DELTA = UIStyleRegistries.create("scroll-delta", FloatValue::new);

    public static final StyleHandler<Float> SCROLLER_VIEW_MARGIN = UIStyleRegistries.create("scroller-view-margin", FloatValue::new);
    public static final StyleHandler<ScrollerMode> SCROLLER_VIEW_MODE = UIStyleRegistries.create("scroller-view-mode", EnumValue.of(ScrollerMode.class));
    public static final StyleHandler<ScrollDisplay> VERTICAL_DISPLAY = UIStyleRegistries.create("vertical-display", EnumValue.of(ScrollDisplay.class));
    public static final StyleHandler<ScrollDisplay> HORIZONTAL_DISPLAY = UIStyleRegistries.create("horizontal-display", EnumValue.of(ScrollDisplay.class));
    public static final StyleHandler<Boolean> ADAPTIVE_WIDTH = UIStyleRegistries.create("adaptive-width", BoolValue::new);
    public static final StyleHandler<Boolean> ADAPTIVE_HEIGHT = UIStyleRegistries.create("adaptive-height", BoolValue::new);
    public static final StyleHandler<Float> MIN_SCROLL_PIXEL = UIStyleRegistries.create("min-scroll", FloatValue::new);
    public static final StyleHandler<Float> MAX_SCROLL_PIXEL = UIStyleRegistries.create("max-scroll", FloatValue::new);

    public static final StyleHandler<IGuiTexture> FOCUS_OVERLAY = UIStyleRegistries.create("focus-overlay", TextureValue::new);
    public static final StyleHandler<Integer> MAX_ITEM = UIStyleRegistries.create("max-item", IntValue::new);
    public static final StyleHandler<Float> VIEW_HEIGHT = UIStyleRegistries.create("view-height", FloatValue::new);
    public static final StyleHandler<Boolean> SHOW_OVERLAY = UIStyleRegistries.create("show-overlay", BoolValue::new);
    public static final StyleHandler<Boolean> CLOSE_AFTER_SELECT = UIStyleRegistries.create("close-after_select", BoolValue::new);

    public static final StyleHandler<IGuiTexture> HOVER_OVERLAY = UIStyleRegistries.create("hover-overlay", TextureValue::new);
    public static final StyleHandler<Boolean> SHOW_FLUID_TOOLTIPS = UIStyleRegistries.create("show-fluid-tooltips", BoolValue::new);
    public static final StyleHandler<Boolean> SHOW_ITEM_TOOLTIPS = UIStyleRegistries.create("show-item-tooltips", BoolValue::new);
    public static final StyleHandler<Boolean> PLAYER_SLOT = UIStyleRegistries.create("player-slot", BoolValue::new);
    public static final StyleHandler<Boolean> ACCEPT_QUICK_MOVE = UIStyleRegistries.create("accept-quick-move", BoolValue::new);
    public static final StyleHandler<Integer> QUICK_MOVE_PRIORITY = UIStyleRegistries.create("quick-move-priority", IntValue::new);

    public static void init() {
        YogaNodeStyleParser.init();
    }
}
