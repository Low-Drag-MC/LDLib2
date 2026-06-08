package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.editor.ui.SplittableWindow;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.data.*;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutProperties;
import com.lowdragmc.lowdraglib2.gui.ui.style.animation.Transition;
import com.lowdragmc.lowdraglib2.gui.ui.style.properties.*;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.*;
import com.mojang.serialization.Codec;
import lombok.experimental.UtilityClass;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceArray;

@UtilityClass
public final class PropertyRegistry {
    private static final Map<String, Property<?>> PROPERTIES_BY_NAME = new ConcurrentHashMap<>();
    private static volatile AtomicReferenceArray<Property<?>> PROPERTIES_BY_ID = new AtomicReferenceArray<>(256);

    public static <T> void register(Property<T> property) {
        var prev = PROPERTIES_BY_NAME.putIfAbsent(property.name, property);
        if (prev != null) {
            throw new IllegalArgumentException("A style property named '" + property.name + "' already exists (id="
                    + prev.id + ")");
        }
        ensureCapacity(property.id);
        var existing = PROPERTIES_BY_ID.get(property.id);
        if (existing != null) {
            PROPERTIES_BY_NAME.remove(property.name, property);
            throw new IllegalArgumentException("A style property with id " + property.id +
                    " already exists: name='" + existing.name + "'");
        }
        PROPERTIES_BY_ID.set(property.id, property);
    }

    public static synchronized void ensureCapacity(int id) {
        int oldLen = PROPERTIES_BY_ID.length();
        if (id < oldLen) return;
        int newLen = oldLen;
        while (newLen <= id) newLen <<= 1;

        var newArr = new AtomicReferenceArray<Property<?>>(newLen);
        for (int i = 0; i < oldLen; i++) {
            newArr.set(i, PROPERTIES_BY_ID.get(i));
        }
        PROPERTIES_BY_ID = newArr;
    }

    public static Collection<Property<?>> all() {
        return PROPERTIES_BY_NAME.values();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> Property<T> byName(String name) {
        return (Property<T>) PROPERTIES_BY_NAME.get(name);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> Property<T> byId(int id) {
        if (id < 0 || id >= PROPERTIES_BY_ID.length()) return null;
        return (Property<T>) PROPERTIES_BY_ID.get(id);
    }

    public static <T> Property<T> create(String name, Codec<T> codec, T initialValue, ValueParser<T> valueParser) {
        var handler = Property.of(name, codec, initialValue, valueParser);
        register(handler);
        return handler;
    }

    public static <T> Property<T> create(String name, Class<T> clazz, Codec<T> codec, T initialValue, ValueParser<T> valueParser) {
        var handler = Property.of(name, clazz, codec, initialValue, valueParser);
        register(handler);
        return handler;
    }

    public static <T, P extends Property<T>> P create(P property) {
        register(property);
        return property;
    }

    public static TextureProperty create(String name, IGuiTexture initialValue) {
        return create(new TextureProperty(name, initialValue));
    }

    public static Property<Boolean> create(String name, boolean initialValue) {
        return create(name, Boolean.class, Codec.BOOL, initialValue, BoolValue::new);
    }

    public static TooltipsProperty create(String name, Tooltips initialValue) {
        return create(new TooltipsProperty(name, initialValue));
    }

    public static TransitionProperty create(String name, Transition initialValue) {
        return create(new TransitionProperty(name, initialValue));
    }

    public static Transform2DProperty create(String name, Transform2D initialValue) {
        return create(new Transform2DProperty(name, initialValue));
    }

    public static IntProperty create(String name, int initialValue) {
        return create(new IntProperty(name, initialValue));
    }

    public static ColorProperty createColor(String name, int initialValue) {
        return create(new ColorProperty(name, initialValue));
    }

    public static FloatProperty create(String name, float initialValue) {
        return create(new FloatProperty(name, initialValue));
    }

    public static Property<String> create(String name, String initialValue) {
        return create(name, String.class, Codec.STRING, initialValue, StringValue::new);
    }

    public static Property<Component> create(String name, Component initialValue) {
        return create(name, Component.class, ComponentSerialization.CODEC, initialValue, ComponentValue::new);
    }

    public static Property<ResourceLocation> create(String name, ResourceLocation initialValue) {
        return create(name, ResourceLocation.class, ResourceLocation.CODEC, initialValue, ResourceLocationValue::new);
    }

    public static <T extends Enum<T>> EnumProperty<T> create(String name, Class<T> clazz, T initialValue) {
        return create(new EnumProperty<>(name, clazz, initialValue));
    }

    public static <T extends Enum<T>> EnumProperty<T> create(String name, Class<T> clazz, T initialValue, List<T> candidates) {
        return create(new EnumProperty<>(name, clazz, initialValue, candidates));
    }

    public static final Property<IGuiTexture> BACKGROUND = create("background", IGuiTexture.EMPTY);
    public static final Property<IGuiTexture> OVERLAY = create("overlay", IGuiTexture.EMPTY);
    public static final Property<Tooltips> TOOLTIPS = create("tooltips", Tooltips.empty());
    public static final Property<Integer> Z_INDEX = create("z-index", 0);
    public static final Property<Transform2D> TRANSFORM_2D = create("transform", Transform2D.identity());
    public static final Property<Float> OPACITY = create("opacity", 1f).setRange(0f, 1f);
    public static final Property<IGuiTexture> OVERFLOW_CLIP = create("overflow-clip", IGuiTexture.EMPTY);
    public static final Property<Transition> TRANSITION = create("transition", Transition.EMPTY);
    public static final Property<Integer> COLOR = create(new ColorProperty("color", -1));

    public static final Property<IGuiTexture> BASE_BACKGROUND = create("base-background", IGuiTexture.EMPTY);
    public static final Property<IGuiTexture> HOVER_BACKGROUND = create("hover-background", IGuiTexture.EMPTY);
    public static final Property<IGuiTexture> PRESSED_BACKGROUND = create("pressed-background", IGuiTexture.EMPTY);
    public static final Property<IGuiTexture> UNMARK_BACKGROUND = create("unmark-background", IGuiTexture.EMPTY);
    public static final Property<IGuiTexture> MARK_BACKGROUND = create("mark-background", IGuiTexture.EMPTY);
    public static final Property<IGuiTexture> COLLAPSE_ICON = create("collapse-icon", IGuiTexture.EMPTY);
    public static final Property<IGuiTexture> EXPAND_ICON = create("expand-icon", IGuiTexture.EMPTY);

    public static final Property<Boolean> ALLOW_ZOOM = create("allow-zoom", true);
    public static final Property<Boolean> ALLOW_PAN = create("allow-pan", true);
    public static final Property<Float> MIN_SCALE = create("min-scale", 0.1f).setMin(0.001f);
    public static final Property<Float> MAX_SCALE = create("max-scale", 10f).setMin(0.001f);
    public static final Property<IGuiTexture> GRID_BACKGROUND = create("grid-background", IGuiTexture.EMPTY);
    public static final Property<Float> GRID_SIZE = create("grid-size", 64f).setMin(1f);

    public static final Property<IGuiTexture> NODE_BACKGROUND = create("node-background", IGuiTexture.EMPTY);
    public static final Property<IGuiTexture> NODE_HOVER_BACKGROUND = create("node-hover-background", ColorPattern.BLUE.rectTexture());
    public static final Property<IGuiTexture> LEAF_BACKGROUND = create("leaf-background", IGuiTexture.EMPTY);
    public static final Property<IGuiTexture> LEAF_HOVER_BACKGROUND = create("leaf-hover-background", ColorPattern.BLUE.rectTexture());
    public static final Property<IGuiTexture> ARROW = create("arrow", Icons.RIGHT_ARROW_NO_BAR_S_WHITE);

    public static final Property<FillDirection> FILL_DIRECTION = create("fill-direction", FillDirection.class, FillDirection.ALWAYS_FULL);
    public static final Property<Boolean> INTERPOLATE = create("interpolate", true);
    public static final Property<Float> INTERPOLATE_STEP = create("interpolate-step", 0.1f).setRange(0f, 1f);

    public static final Property<Float> SCROLL_DELTA = create("scroll-delta", 0.1f).setRange(0f, 1f);
    public static final Property<Float> SCROLL_BAR_SIZE = create("scroll-bar-size", 20f).setRange(0f, 100f);

    public static final Property<Float> SCROLLER_VIEW_MARGIN = create("scroller-view-margin", 5f);
    public static final Property<ScrollerMode> SCROLLER_VIEW_MODE = create("scroller-view-mode", ScrollerMode.class, ScrollerMode.BOTH);
    public static final Property<ScrollDisplay> SCROLLER_VERTICAL_DISPLAY = create("scroller-vertical-display", ScrollDisplay.class, ScrollDisplay.AUTO);
    public static final Property<ScrollDisplay> SCROLLER_HORIZONTAL_DISPLAY = create("scroller-horizontal-display", ScrollDisplay.class, ScrollDisplay.AUTO);
    public static final Property<Boolean> ADAPTIVE_WIDTH = create("adaptive-width", false);
    public static final Property<Boolean> ADAPTIVE_HEIGHT = create("adaptive-height", false);
    public static final Property<Float> MIN_SCROLL_PIXEL = create("min-scroll", 5f).setMin(0f);
    public static final Property<Float> MAX_SCROLL_PIXEL = create("max-scroll", 7f).setMin(0f);
    public static final Property<VirtualItemHeightMode> VIRTUAL_ITEM_HEIGHT_MODE = create("virtual-item-height-mode", VirtualItemHeightMode.class, VirtualItemHeightMode.VARIABLE);
    public static final Property<Float> VIRTUAL_ESTIMATED_ITEM_HEIGHT = create("virtual-estimated-item-height", 10f).setMin(0.001f);
    public static final Property<Float> VIRTUAL_OVERSCAN_PIXELS = create("virtual-overscan-pixels", 40f).setMin(0f);

    public static final Property<IGuiTexture> FOCUS_OVERLAY = create("focus-overlay", IGuiTexture.EMPTY);
    public static final Property<Integer> MAX_ITEM = create("max-item", 5).setMin(1);
    public static final Property<Float> VIEW_HEIGHT = create("view-height", 50f).setMin(1f);
    public static final Property<Boolean> SHOW_OVERLAY = create("show-overlay", true);
    public static final Property<Boolean> CLOSE_AFTER_SELECT = create("close-after-select", true);

    public static final Property<IGuiTexture> HOVER_OVERLAY = create("hover-overlay", IGuiTexture.EMPTY);
    public static final Property<IGuiTexture> SLOT_OVERLAY = create("slot-overlay", IGuiTexture.EMPTY);
    public static final Property<Boolean> SHOW_SLOT_OVERLAY_ONLY_EMPTY = create("show-slot-overlay-only-empty", true);
    public static final Property<Boolean> SHOW_FLUID_TOOLTIPS = create("show-fluid-tooltips", true);
    public static final Property<Boolean> SHOW_ITEM_TOOLTIPS = create("show-item-tooltips", true);
    public static final Property<Boolean> IS_PLAYER_SLOT = create("is-player-slot", false);
    public static final Property<Boolean> ACCEPT_QUICK_MOVE = create("accept-quick-move", true);
    public static final Property<Integer> QUICK_MOVE_PRIORITY = create("quick-move-priority", 0);

    public static final Property<Float> PERCENTAGE = create("percentage", 50f).setRange(0f, 100f);
    public static final Property<Float> MIN_PERCENTAGE = create("min-percentage", 5f).setRange(0f, 100f);
    public static final Property<Float> MAX_PERCENTAGE = create("max-percentage", 95f).setRange(0f, 100f);

    public static final Property<Float> FONT_SIZE = create("font-size", 9f).setMin(0f);
    public static final Property<ResourceLocation> FONT = create("font", Style.DEFAULT_FONT);
    public static final Property<Integer> TEXT_COLOR = createColor("text-color", -1);
    public static final Property<Integer> ERROR_COLOR = createColor("error-color", 0xffff0000);
    public static final Property<Integer> CURSOR_COLOR = createColor("cursor-color", 0xffeeeeee);
    public static final Property<Boolean> TEXT_SHADOW = create("text-shadow", true);
    public static final Property<Component> PLACEHOLDER = create("placeholder", Component.translatable("text_field.empty"));
    public static final Property<Float> LINE_SPACING = create("line-spacing", 1f).setMin(0f);
    public static final Property<Float> ROLL_SPEED = create("roll-speed", 1f).setMin(0f);
    public static final Property<Horizontal> HORIZONTAL_ALIGN = create("horizontal-align", Horizontal.class, Horizontal.LEFT);
    public static final Property<Vertical> VERTICAL_ALIGN = create("vertical-align", Vertical.class, Vertical.TOP);
    public static final Property<TextWrap> TEXT_WRAP = create("text-wrap", TextWrap.class, TextWrap.NONE);

    public static void init() {
        LayoutProperties.init();
        BasicStyle.init();
        ProgressBar.ProgressBarStyle.init();
        ScrollerView.ScrollerViewStyle.init();
        VirtualScrollerView.VirtualScrollerViewStyle.init();
        SearchComponent.SearchStyle.init();
        Selector.SelectorStyle.init();
        SplittableWindow.SplitStyle.init();
        TextArea.TextAreaStyle.init();
        TextField.TextFieldStyle.init();
        TextElement.TextStyle.init();
        Toggle.ToggleStyle.init();
        Switch.SwitchStyle.init();
        Scroller.ScrollerStyle.init();
    }
}
