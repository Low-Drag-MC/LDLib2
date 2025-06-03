package com.lowdragmc.lowdraglib.editor.ui;

import com.lowdragmc.lowdraglib.editor.resource.ColorsResource;
import com.lowdragmc.lowdraglib.editor.resource.LangResource;
import com.lowdragmc.lowdraglib.editor.resource.IRendererResource;
import com.lowdragmc.lowdraglib.editor.resource.TexturesResource;
import com.lowdragmc.lowdraglib.editor.ui.menu.FileMenu;
import com.lowdragmc.lowdraglib.editor.ui.menu.ViewMenu;
import com.lowdragmc.lowdraglib.editor.ui.util.SplitView;
import com.lowdragmc.lowdraglib.editor.ui.view.InspectorView;
import com.lowdragmc.lowdraglib.editor.ui.view.ResourceView;
import com.lowdragmc.lowdraglib.editor.ui.view.ui.UIEditorView;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.ui.UI;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib.gui.ui.elements.Menu;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib.gui.util.TreeNode;
import com.lowdragmc.lowdraglib.test.ui.TestConfigurators;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
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
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
        });
        center.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
        });
        right.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
        });
        bottom.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
        });

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
        })), new SplitView.Horizontal()
                .left(new SplitView.Vertical()
                        .top(new SplitView.Horizontal()
                                .left(left)
                                .right(center)
                                .setPercentage(28)
                                .setMinPercentage(1)
                                .setMaxPercentage(99))
                        .bottom(bottom)
                        .setPercentage(75)
                        .setMinPercentage(1)
                        .setMaxPercentage(99))
                .right(right)
                .setPercentage(80)
                .setMinPercentage(1)
                .setMaxPercentage(99));

        ///  internal components
        initMenus();
        initLeftWindow();
        initRightWindow();
        initBottomWindow();
        initCenterWindow();
    }

    /**
     * Initialize the menus here.
     */
    protected void initMenus() {
        menuContainer.addChildren(fileMenu.createMenuTab(), viewMenu.createMenuTab());
    }

    protected void initLeftWindow() {

    }

    protected void initRightWindow() {
        right.addView(inspectorView);
        // TODO remove test
        inspectorView.inspect(new TestConfigurators());
    }

    protected void initBottomWindow() {
        bottom.addView(resourceView);
        resourceView.addResources(
                new ColorsResource(),
                new LangResource(),
                new IRendererResource(),
                new TexturesResource()
        );
    }

    protected void initCenterWindow() {
        center.addView(new UIEditorView(UI.of(new UIElement().layout(layout -> {
            layout.setWidth(250);
            layout.setHeight(250);
            layout.setPadding(YogaEdge.ALL, 10);
        }).addChildren(new Button(), new Button(), new Label()).style(style -> style.backgroundTexture(Sprites.BORDER)))));
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

}
