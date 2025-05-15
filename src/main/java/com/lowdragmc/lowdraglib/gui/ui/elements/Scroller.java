package com.lowdragmc.lowdraglib.gui.ui.elements;

import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaFlexDirection;

public abstract class Scroller extends UIElement {
    public final Button lowerButton;
    public final Button upperButton;
    public final UIElement scrollContainer;
    public final Button scrollBar;

    public Scroller() {
        getLayout().setAlignItems(YogaAlign.CENTER);
        this.lowerButton = new Button();
        this.upperButton = new Button();
        this.scrollContainer = new UIElement();
        this.scrollBar = new Button();

        this.lowerButton.setText("").layout(layout -> {
            layout.setWidth(5);
            layout.setHeight(5);
        });

        this.upperButton.setText("").layout(layout -> {
            layout.setWidth(5);
            layout.setHeight(5);
        });

        this.scrollContainer.layout(layout -> {
            layout.setAlignSelf(YogaAlign.STRETCH);
            layout.setFlexGrow(1);
        }).addChild(scrollBar);
        scrollBar.setText("").layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
        });
        addChildren(lowerButton, scrollContainer, upperButton);
    }

    public static class Vertical extends Scroller {
        public Vertical() {
            getLayout().setFlexDirection(YogaFlexDirection.COLUMN);
            getLayout().setWidth(5);
            scrollContainer.style(style -> style.backgroundTexture(Sprites.SCROLL_CONTAINER_V));
            scrollBar.buttonStyle(style -> style
                    .defaultTexture(Sprites.SCROLL_BAR_V)
                    .hoverTexture(Sprites.SCROLL_BAR_LIGHT_V)
                    .pressedTexture(Sprites.SCROLL_BAR_WHITE_V)
            );
            scrollBar.layout(layout -> {
                layout.setWidthPercent(100);
                layout.setHeightPercent(30);
            });
        }
    }

    public static class Horizontal extends Scroller {
        public Horizontal() {
            getLayout().setFlexDirection(YogaFlexDirection.ROW);
            getLayout().setHeight(5);
            scrollContainer.style(style -> style.backgroundTexture(Sprites.SCROLL_CONTAINER_H));
            scrollBar.buttonStyle(style -> style
                    .defaultTexture(Sprites.SCROLL_BAR_H)
                    .hoverTexture(Sprites.SCROLL_BAR_LIGHT_H)
                    .pressedTexture(Sprites.SCROLL_BAR_WHITE_H)
            );
            scrollBar.layout(layout -> {
                layout.setWidthPercent(30);
                layout.setHeightPercent(100);
            });
        }
    }
}
