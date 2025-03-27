package com.lowdragmc.lowdraglib.editor.ui.view;

import com.lowdragmc.lowdraglib.editor.ColorPattern;
import com.lowdragmc.lowdraglib.editor.Icons;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import lombok.Getter;

/**
 * @author KilaBash
 * @date 2022/12/17
 * @implNote HistoryView
 */
@LDLRegister(name = "history", group = "editor")
@Getter
public class HistoryView extends FloatViewWidget {
    private WidgetGroup container;

    public HistoryView() {
        super(100, 100, 180, 120, false);
    }

    @Override
    public IGuiTexture getIcon() {
        return Icons.HISTORY.copy();
    }

    @Override
    public void initWidget() {
        super.initWidget();
        content.addWidget(new DraggableScrollableWidgetGroup(0, 0, content.getSizeWidth(), content.getSizeHeight())
                .setYScrollBarWidth(2).setYBarStyle(null, ColorPattern.T_WHITE.rectTexture().setRadius(1).transform(-0.5f, 0))
                .addWidget(container = new WidgetGroup(0, 0, content.getSizeWidth() - 2, 0)));
        loadList();
    }

    public void loadList() {
        container.clearAllWidgets();
        var width = container.getSizeWidth();
        var history = editor.getHistory();
        for (var historyItem : history) {
            var group = new WidgetGroup(0, container.widgets.size() * 10, width, 10);
            group.addWidget(new ButtonWidget(0, 0, width, 10, IGuiTexture.EMPTY, cd -> {
                editor.jumpToHistory(historyItem);
                if (!editor.getFloatView().widgets.contains(this)) {
                    editor.getFloatView().addWidget(this);
                }
                loadList();
            }).setHoverBorderTexture(-1, -1));
            group.addWidget(new TextTextureWidget(3, 0, width - 3, 10, historyItem.name())
                    .textureStyle(t -> t.setType(TextTexture.TextType.LEFT_ROLL)));
            group.addWidget(new ImageWidget(0, 0, width, 10,
                    () -> editor.getCurrentHistory() == historyItem ? ColorPattern.T_WHITE.rectTexture() : IGuiTexture.EMPTY));
            container.addWidget(group);
        }
        container.setSizeHeight(10 * history.size());
    }
}
