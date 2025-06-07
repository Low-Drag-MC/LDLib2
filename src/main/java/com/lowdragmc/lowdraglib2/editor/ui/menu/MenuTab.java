package com.lowdragmc.lowdraglib2.editor.ui.menu;

import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaEdge;

import java.util.*;
import java.util.function.BiConsumer;

public abstract class MenuTab {
    public final Editor editor;
    private final List<BiConsumer<MenuTab, TreeBuilder.Menu>> menuCreators = new ArrayList<>();

    protected MenuTab(Editor editor) {
        this.editor = editor;
    }

    /**
     * Append menu creator to attach additional leafs to the menu or remove existing ones.
     */
    public ISubscription registerMenuCreator(BiConsumer<MenuTab, TreeBuilder.Menu> menuCreator) {
        this.menuCreators.add(menuCreator);
        return () -> this.menuCreators.remove(menuCreator);
    }

    /**
     * Create the default menu for this tab. To append additional leafs, register menu creators via {@link #registerMenuCreator(BiConsumer)}.
     */
    protected abstract TreeBuilder.Menu createDefaultMenu();

    /**
     * Return the component name of this tab.
     */
    protected abstract Component getComponent();

    private TreeBuilder.Menu createMenu() {
        var menu = createDefaultMenu();
        for (var creator : menuCreators) {
            creator.accept(this, menu);
        }
        return menu;
    }

    public UIElement createMenuTab() {
        return new TextElement().textStyle(textStyle -> textStyle.adaptiveWidth(true)
                        .textAlignHorizontal(Horizontal.CENTER)
                        .textAlignVertical(Vertical.CENTER))
                .setText(getComponent())
                .layout(layout ->{
                    layout.setHeightPercent(100);
                    layout.setPadding(YogaEdge.HORIZONTAL, 2);
                })
                .style(style -> style.backgroundTexture(IGuiTexture.EMPTY))
                .addEventListener(UIEvents.MOUSE_ENTER, e -> e.currentElement.style(style -> style.backgroundTexture(ColorPattern.T_WHITE.rectTexture())), true)
                .addEventListener(UIEvents.MOUSE_LEAVE, e -> e.currentElement.style(style -> style.backgroundTexture(IGuiTexture.EMPTY)), true)
                .addEventListener(UIEvents.MOUSE_DOWN, e -> {
                    // click to show the menu
                    editor.openMenu(e.currentElement.getPositionX(), e.currentElement.getPositionY() + e.currentElement.getSizeHeight(), createMenu());
                });
    }


}
