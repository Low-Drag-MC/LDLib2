package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.google.common.util.concurrent.Runnables;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaEdge;
import org.w3c.dom.Element;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
@KJSBindings
@LDLRegister(name = "tab", group = "utils", registry = "ldlib2:ui_element")
public class Tab extends UIElement {
    @Configurable(name = "TabStyle")
    public class TabStyle extends Style {
        private static final Property<?>[] PROPERTIES = new Property[] {
                PropertyRegistry.BASE_BACKGROUND,
                PropertyRegistry.HOVER_BACKGROUND,
                PropertyRegistry.PRESSED_BACKGROUND,
        };
        public TabStyle() {
            super(Tab.this);
            setDefault(PropertyRegistry.BASE_BACKGROUND, Sprites.TAB_DARK);
            setDefault(PropertyRegistry.HOVER_BACKGROUND, Sprites.TAB_WHITE);
            setDefault(PropertyRegistry.PRESSED_BACKGROUND, Sprites.TAB);
        }

        @Override
        protected Property<?>[] getProperties() {
            return PROPERTIES;
        }

        public IGuiTexture baseTexture() {
            return getValueSave(PropertyRegistry.BASE_BACKGROUND);
        }

        public TabStyle baseTexture(IGuiTexture texture) {
            set(PropertyRegistry.BASE_BACKGROUND, texture);
            return this;
        }

        public IGuiTexture hoverTexture() {
            return getValueSave(PropertyRegistry.HOVER_BACKGROUND);
        }

        public TabStyle hoverTexture(IGuiTexture texture) {
            set(PropertyRegistry.HOVER_BACKGROUND, texture);
            return this;
        }

        public IGuiTexture selectedTexture() {
            return getValueSave(PropertyRegistry.PRESSED_BACKGROUND);
        }

        public TabStyle selectedTexture(IGuiTexture texture) {
            set(PropertyRegistry.PRESSED_BACKGROUND, texture);
            return this;
        }
    }

    public final Label text = new Label();
    @Getter
    private final TabStyle tabStyle = new TabStyle();
    @Setter
    private Runnable onTabSelected = Runnables.doNothing();
    @Setter
    private Runnable onTabUnselected = Runnables.doNothing();
    // runtime
    private boolean isSelected = false;
    private boolean isHovered = false;
    @Nullable
    private TabView tabView;

    public Tab() {
        getLayout().height(16);
        getLayout().paddingAll(3);
        getLayout().flexDirection(FlexDirection.ROW);

        text.setText(Component.empty());
        text.layout(layout -> layout.heightPercent(100));
        text.textStyle(textStyle -> {
            textStyle.textAlignHorizontal(Horizontal.CENTER);
            textStyle.textAlignVertical(Vertical.CENTER);
            textStyle.adaptiveWidth(true);
        });

        addEventListener(UIEvents.MOUSE_ENTER, this::onMouseEnter, true);
        addEventListener(UIEvents.MOUSE_LEAVE, this::onMouseLeave, true);
        addChild(text);

        this.text.addClass("__tab_text__");

        internalSetup();
    }

    public Tab tabStyle(Consumer<TabStyle> tabStyle) {
        tabStyle.accept(this.tabStyle);
        return this;
    }

    protected void setTabView(@Nullable TabView tabView) {
        this.tabView = tabView;
    }

    @Override
    public void initEditorTemplate() {
        setText("Tab");
    }

    @Nullable
    public TabView getTabView() {
        if (tabView != null) {
            if (tabView.getTabContents().containsKey(this)) {
                return tabView;
            }
        }
        return null;
    }

    @Nullable
    public UIElement getContent() {
        if (tabView == null) return null;
        return tabView.getTabContents().get(this);
    }

    @Override
    public boolean removeSelf() {
        if (getTabView() != null) {
            getTabView().removeTab(this);
            return true;
        } else {
            return super.removeSelf();
        }
    }

    public Tab setText(String text, boolean translate) {
        this.text.setText(text, translate);
        return this;
    }

    public Tab setText(String text) {
        return setText(text, false);
    }

    public Tab setText(Component text) {
        this.text.setText(text);
        return this;
    }

    public Tab setDynamicText(Supplier<Component> text) {
        this.text.bindDataSource(SupplierDataSource.of(text));
        return this;
    }

    public Tab textStyle(Consumer<TextElement.TextStyle> style) {
        text.textStyle(style);
        return this;
    }

    public void setSelected(boolean selected) {
        if (isSelected == selected) {
            return;
        }
        this.isSelected = selected;
        if (selected) {
            addClass("__tab_selected__");
            addClass("__selected__");
            onTabSelected.run();
        } else {
            removeClass("_tab_selected_");
            removeClass("__selected__");
            onTabUnselected.run();
        }
    }

    /// events
    protected void onMouseEnter(UIEvent event) {
        isHovered = true;
    }

    protected void onMouseLeave(UIEvent event) {
        isHovered = false;
    }

    /// rendering
    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        // draw button texture
        var texture = tabStyle.baseTexture();
        if (isSelected) {
            texture = tabStyle.selectedTexture();
        } else if (isHovered) {
            texture = tabStyle.hoverTexture();
        }
        guiContext.drawTexture(texture, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
        super.drawBackgroundAdditional(guiContext);
    }

    @Override
    public void loadXml(Element element) {
        // text
        if (element.hasAttribute("text")) {
            setText(element.getAttribute("text"));
        }
        super.loadXml(element);
    }
}
