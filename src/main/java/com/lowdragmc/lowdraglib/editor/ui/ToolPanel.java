package com.lowdragmc.lowdraglib.editor.ui;

import com.lowdragmc.lowdraglib.gui.animation.Transform;
import com.lowdragmc.lowdraglib.editor.ColorPattern;
import com.lowdragmc.lowdraglib.editor.Icons;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.math.Size;
import com.lowdragmc.lowdraglib.math.interpolate.Eases;
import lombok.Getter;
import lombok.Setter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author KilaBash
 * @date 2022/12/2
 * @implNote ResourcePanel
 */
public class ToolPanel extends WidgetGroup {
    @Deprecated
    public static final int WIDTH = 100;

    @Getter
    protected final Editor editor;
    @Getter
    protected final List<Widget> toolBoxes = new ArrayList<>();
    protected ButtonWidget buttonHide;
    @Getter
    protected TabContainer tabContainer;
    protected ImageWidget tabsBackground;
    @Setter
    protected String title = "ldlib.gui.editor.group.tool_box";

    @Getter
    protected boolean isShow;

    public ToolPanel(Editor editor) {
        super(-WIDTH, 30, WIDTH, Math.max(100, editor.getSize().getHeight() - ResourcePanel.HEIGHT - 30));
        setClientSideWidget();
        this.editor = editor;
    }

    @Override
    public void setSize(Size size) {
        var width = getSizeWidth();
        var newWidth = size.width;
        super.setSize(size);
        if (width != newWidth) {
            if (tabContainer != null) tabContainer.setSize(Size.of(newWidth, size.height - 15));
            if (tabsBackground != null) tabsBackground.setSize(Size.of(20, toolBoxes.size() * 20));
            if (buttonHide != null) buttonHide.setSelfPosition(newWidth - 13, 3);
            if (isShow) {
                setSelfPosition(0, getSelfPositionY());
            } else {
                setSelfPosition(-newWidth, getSelfPositionY());
            }
        }
    }

    @Override
    public void initWidget() {
        Size size = getSize();
        this.setBackground(ColorPattern.BLACK.rectTexture());

        addWidget(new LabelWidget(3, 3, () -> title));
        addWidget(tabsBackground = new ImageWidget(size.width, 15, 20, 0, ColorPattern.BLACK.rectTexture().setRightRadius(8)));

        addWidget(tabContainer = new TabContainer(0, 15, size.width, size.height - 15));
        tabContainer.setBackground(ColorPattern.T_GRAY.borderTexture(-1));

        addWidget(buttonHide = new ButtonWidget(size.width - 13, 3, 10, 10, new GuiTextureGroup(
                ColorPattern.BLACK.rectTexture(),
                ColorPattern.T_GRAY.borderTexture(1),
                Icons.RIGHT
        ), cd -> {
            if (isShow()) {
                hide();
            } else {
                show();
            }
        }).setHoverBorderTexture(1, -1));

        super.initWidget();
    }

    @Override
    public void clearAllWidgets() {
        toolBoxes.clear();
        tabContainer.clearAllWidgets();
        tabsBackground.setSize(Size.of(20, 0));
    }

    @Deprecated
    public void addNewToolBox(String name, ResourceTexture texture, WidgetGroup toolBox) {
        addNewToolBox(name, texture, size -> {
            toolBox.setSize(size);
            return toolBox;
        });
    }

    public void addNewToolBox(String name, ResourceTexture texture, Function<Size, WidgetGroup> toolBoxSupplier) {
        var toolBox = toolBoxSupplier.apply(Size.of(getSizeWidth(), getSize().height - 15));
        tabContainer.addTab((TabButton) new TabButton(getSizeWidth() + 4, 4 + toolBoxes.size() * 20, 12, 12) {
            @Override
            @OnlyIn(Dist.CLIENT)
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if(isMouseOverElement(mouseX, mouseY)) {
                    show();
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }
        }.setTexture(texture, texture.copy().setColor(ColorPattern.T_GREEN.color)).setHoverTooltips(name), toolBox);
        toolBoxes.add(toolBox);
        tabsBackground.setSize(Size.of(20, toolBoxes.size() * 20));
    }

    public void hide() {
        hide(true);
    }

    public void hide(boolean animate) {
        if (isShow() && !inAnimate()) {
            isShow = !isShow;
            if (animate) {
                animation(new Transform()
                        .offset(-getSizeWidth(), 0)
                        .ease(Eases.EaseQuadOut)
                        .duration(500)
                        .onFinish(() -> {
                            addSelfPosition(-getSizeWidth(), 0);
                            buttonHide.setButtonTexture(ColorPattern.BLACK.rectTexture(), ColorPattern.T_GRAY.borderTexture(1), Icons.RIGHT);
                        }));
            } else {
                addSelfPosition(-getSizeWidth(), 0);
                buttonHide.setButtonTexture(ColorPattern.BLACK.rectTexture(), ColorPattern.T_GRAY.borderTexture(1), Icons.RIGHT);
            }
        }
    }

    public void show() {
        show(true);
    }

    public void show(boolean animate) {
        if (!isShow() && !inAnimate()) {
            isShow = !isShow;
            if (animate) {
                animation(new Transform()
                        .offset(getSizeWidth(), 0)
                        .ease(Eases.EaseQuadOut)
                        .duration(500)
                        .onFinish(() -> {
                            addSelfPosition(getSizeWidth(), 0);
                            buttonHide.setButtonTexture(ColorPattern.BLACK.rectTexture(), ColorPattern.T_GRAY.borderTexture(1), Icons.LEFT);
                        }));
            } else {
                addSelfPosition(getSizeWidth(), 0);
                buttonHide.setButtonTexture(ColorPattern.BLACK.rectTexture(), ColorPattern.T_GRAY.borderTexture(1), Icons.LEFT);
            }
        }
    }
}
