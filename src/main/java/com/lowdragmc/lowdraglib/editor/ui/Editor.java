package com.lowdragmc.lowdraglib.editor.ui;

import com.lowdragmc.lowdraglib.editor.resource.ColorsResource;
import com.lowdragmc.lowdraglib.editor.resource.LangResource;
import com.lowdragmc.lowdraglib.editor.resource.IRendererResource;
import com.lowdragmc.lowdraglib.editor.resource.TexturesResource;
import com.lowdragmc.lowdraglib.editor.ui.menu.FileMenu;
import com.lowdragmc.lowdraglib.editor.ui.menu.ViewMenu;
import com.lowdragmc.lowdraglib.editor.ui.view.InspectorView;
import com.lowdragmc.lowdraglib.editor.ui.view.ResourceView;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.elements.Menu;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib.gui.util.TreeNode;
import com.lowdragmc.lowdraglib.test.ui.TestConfigurators;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaGutter;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Function;

@Getter
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Editor extends UIElement {
    public final UIElement top;
    public final UIElement icon;
    public final UIElement menuContainer;
    public final FileMenu fileMenu;
    public final ViewMenu viewMenu;

    public final Window left;
    public final Window right;
    public final Window center;
    public final Window bottom;

    public final InspectorView inspectorView;
    public final ResourceView resourceView;

    public Editor() {
        this.top = new UIElement();
        this.icon = new UIElement();
        this.menuContainer = new UIElement();
        this.resourceView = new ResourceView(this);

        this.left = new Window();
        this.right = new Window();
        this.center = new Window();
        this.bottom = new Window();
        this.fileMenu = new FileMenu(this);
        this.viewMenu = new ViewMenu(this);
        inspectorView = new InspectorView();

        left.layout(layout -> {
            layout.setWidthPercent(28);
            layout.setHeightPercent(100);
        });
        center.layout(layout -> {
            layout.setFlex(1);
            layout.setHeightPercent(100);
        });
        right.layout(layout -> {
            layout.setWidthPercent(20);
            layout.setHeightPercent(100);
        });
        bottom.layout(layout -> {
            layout.setHeightPercent(25);
            layout.setWidthPercent(100);
        });

        left.setOnRightBorderDragging(this::onDragLeftBorder);
        center.setOnLeftBorderDragging(this::onDragLeftBorder);

        right.setOnLeftBorderDragging(this::onDragRightBorder);
        bottom.setOnRightBorderDragging(this::onDragRightBorder);
        center.setOnRightBorderDragging(this::onDragRightBorder);

        bottom.setOnTopBorderDragging(this::onDragBottomBorder);
        left.setOnBottomBorderDragging(this::onDragBottomBorder);
        center.setOnBottomBorderDragging(this::onDragBottomBorder);


        addChildren(top.layout(layout -> {
            layout.setPadding(YogaEdge.ALL, 1);
            layout.setWidthPercent(100);
            layout.setHeight(15);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setGap(YogaGutter.ALL, 2);
        }).style(style -> style.backgroundTexture(Sprites.RECT_SOLID)).addChildren(icon.layout(layout -> {
            layout.setWidth(11);
            layout.setHeight(11);
            layout.setMargin(YogaEdge.ALL, 1);
            layout.setMargin(YogaEdge.HORIZONTAL, 5);
        }).style(style -> style.backgroundTexture(new ResourceTexture())), menuContainer.layout(layout -> {
            layout.setHeightPercent(100);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setGap(YogaGutter.ALL, 2);
        })), new UIElement().layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setWidthPercent(100);
            layout.setFlex(1);
        }).addChildren(new UIElement().layout(layout -> {
            layout.setFlex(1);
            layout.setHeightPercent(100);
        }).addChildren(new UIElement().layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setFlex(1);
        }).addChildren(left, center), bottom), right));

        ///  internal components
        initMenus();
        initInspectorView();
        initResourceView();
    }

    /**
     * Initialize the menus here.
     */
    protected void initMenus() {
        menuContainer.addChildren(fileMenu.createMenuTab(), viewMenu.createMenuTab());
    }

    /**
     * Initialize the inspector view here.
     */
    protected void initInspectorView() {
        right.addView(inspectorView);
        // TODO remove test
        inspectorView.inspect(new TestConfigurators());
    }

    /**
     * Initialize the resource view here.
     */
    protected void initResourceView() {
        bottom.addView(resourceView);
        resourceView.addResources(
                new ColorsResource(),
                new LangResource(),
                new IRendererResource(),
                new TexturesResource()
        );
    }


    public <T, C> Menu<T, C> openMenu(float posX, float posY, TreeNode<T, C> menuNode, Function<T, UIElement> uiProvider) {
        var menu = new Menu<>(menuNode, uiProvider);
        menu.layout(layout -> {
            layout.setPosition(YogaEdge.LEFT, posX - getPositionX());
            layout.setPosition(YogaEdge.TOP, posY - getContentY());
        });
        addChildren(menu);
        return menu;
    }

    public void openMenu(float posX, float posY, @Nullable TreeBuilder.Menu menuBuilder) {
        if (menuBuilder == null || menuBuilder.isEmpty()) return;
        openMenu(posX, posY, menuBuilder.build(), TreeBuilder.Menu::uiProvider).setOnNodeClicked(TreeBuilder.Menu::handle);
    }

    public void close() {
        // TODO close editor with save state checking
        if (getModularUI() != null && getModularUI().getScreen() != null) {
            getModularUI().getScreen().onClose();
        }
    }

    protected void onDragLeftBorder(UIEvent event) {
        var mui = getModularUI();
        if (mui != null) {
            var screenWidth = mui.getWidth();
            var percent = (event.x - mui.getLeftPos()) / (screenWidth - right.getSizeWidth());
            left.layout(layout -> layout.setWidthPercent(Mth.clamp(percent * 100, 0, 100)));
        }
    }

    protected void onDragRightBorder(UIEvent event) {
        var mui = getModularUI();
        if (mui != null) {
            var screenWidth = mui.getWidth();
            var leftWidth = left.getSizeWidth();
            var percent = 1 - (event.x - mui.getLeftPos()) / screenWidth;
            right.layout(layout -> layout.setWidthPercent(Mth.clamp(percent * 100, 0, 100)));
            var leftPercent = leftWidth / (event.x - mui.getLeftPos());
            left.layout(layout -> layout.setWidthPercent(Mth.clamp(leftPercent * 100, 0, 100)));
        }
    }

    protected void onDragBottomBorder(UIEvent event) {
        var mui = getModularUI();
        if (mui != null) {
            var screenHeight = mui.getHeight();
            var percent = 1- (event.y - mui.getTopPos() - top.getSizeHeight()) / (screenHeight - top.getSizeHeight());
            bottom.layout(layout -> layout.setHeightPercent(Mth.clamp(percent * 100, 0, 100)));
        }
    }
}
