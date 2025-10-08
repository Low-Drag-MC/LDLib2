package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.google.common.util.concurrent.Runnables;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
@LDLRegister(name = "tab", registry = "ldlib2:ui_element")
public class Tab extends UIElement {
    @Accessors(chain = true, fluent = true)
    public static class TabStyle extends Style {
        @Getter @Setter
        private IGuiTexture baseTexture = Sprites.TAB_DARK;
        @Getter @Setter
        private IGuiTexture hoverTexture = Sprites.TAB_WHITE;
        @Getter @Setter
        private IGuiTexture selectedTexture = Sprites.TAB;

        public TabStyle(UIElement holder) {
            super(holder);
        }
    }
    public final Label text = new Label();
    @Getter
    private final TabStyle tabStyle = new TabStyle(this);
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
        getLayout().setHeight(16);
        getLayout().setPadding(YogaEdge.ALL, 3);
        getLayout().setFlexDirection(YogaFlexDirection.ROW);

        text.setText(Component.empty());
        text.layout(layout -> layout.setHeightPercent(100));
        text.textStyle(textStyle -> {
            textStyle.textAlignHorizontal(Horizontal.CENTER);
            textStyle.textAlignVertical(Vertical.CENTER);
            textStyle.adaptiveWidth(true);
        });

        addEventListener(UIEvents.MOUSE_ENTER, this::onMouseEnter, true);
        addEventListener(UIEvents.MOUSE_LEAVE, this::onMouseLeave, true);
        addChild(text);
        markAllChildrenAsInternal();
    }

    public Tab tabStyle(Consumer<TabStyle> tabStyle) {
        tabStyle.accept(this.tabStyle);
        onStyleChanged();
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
            onTabSelected.run();
        } else {
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
        var texture = tabStyle.baseTexture;
        if (isSelected) {
            texture = tabStyle.selectedTexture;
        } else if (isHovered) {
            texture = tabStyle.hoverTexture;
        }
        guiContext.drawTexture(texture, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
        super.drawBackgroundAdditional(guiContext);
    }

}
