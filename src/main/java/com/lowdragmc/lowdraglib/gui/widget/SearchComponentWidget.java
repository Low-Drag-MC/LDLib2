package com.lowdragmc.lowdraglib.gui.widget;

import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.utils.search.ISearch;
import com.lowdragmc.lowdraglib.math.Position;
import com.lowdragmc.lowdraglib.utils.search.SearchEngine;
import com.lowdragmc.lowdraglib.math.Size;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

import javax.annotation.Nonnull;

/**
 * @author KilaBash
 * @date 2022/8/24
 * @implNote SearchComponentWidget
 */
public class SearchComponentWidget<T> extends WidgetGroup {
    public final SearchEngine<T> engine;
    public final IWidgetSearch<T> search;
    public final DraggableScrollableWidgetGroup popUp;
    public final TextFieldWidget textFieldWidget;
    private int capacity = 10;
    protected boolean isShow;
    @Setter
    protected boolean showUp = false;

    public SearchComponentWidget(int x, int y, int width, int height, IWidgetSearch<T> search) {
        this(x, y, width, height, search, false);
    }

    public  SearchComponentWidget(int x, int y, int width, int height, IWidgetSearch<T> search, boolean isServer) {
        super(x, y, width, height);
        if (!isServer) {
            setClientSideWidget();
        }
        this.addWidget(textFieldWidget = new TextFieldWidget(0, 0, width, height, null, null){
            @Override
            public void onFocusChanged(@Nullable Widget lastFocus, Widget focus) {
                if (lastFocus != null && focus != null && lastFocus.parent == focus.parent) {
                    return;
                }
                super.onFocusChanged(lastFocus, focus);
                setShow(isFocus());
            }
        });
        this.addWidget(popUp = new DraggableScrollableWidgetGroup(0, height, width, 0) {
            @Override
            public void onFocusChanged(@Nullable Widget lastFocus, Widget focus) {
                if (lastFocus != null && focus != null && lastFocus.parent == focus.parent) {
                    return;
                }
                super.onFocusChanged(lastFocus, focus);
                setShow(isFocus());
            }
        });
        popUp.setBackground(new ColorRectTexture(0xAA000000));
        popUp.setVisible(false);
        popUp.setActive(true);
        this.search = search;
        this.engine = new SearchEngine<>(search, (r) -> {
            int size = popUp.getAllWidgetSize();
            popUp.setSize(Size.of(getSize().width, Math.min(size + 1, capacity) * 15));
            if (showUp) {
                popUp.setSelfPosition(Position.of(0, -Math.min(size + 1, capacity) * 15));
            } else {
                popUp.setSelfPosition(Position.of(0, height));
            }
            popUp.waitToAdded(new ButtonWidget(0, size * 15, width,
                    15, new TextTexture(search.resultDisplay(r)).setWidth(width).setType(TextTexture.TextType.ROLL),
                    cd -> {
                        search.selectResult(r);
                        setShow(false);
                        textFieldWidget.setCurrentString(search.resultDisplay(r));
                    }).setHoverBorderTexture(-1, -1));
            if (isServer) {
                writeUpdateInfo(-2, buf -> search.serialize(r, buf));
            }
        });

        textFieldWidget.setTextResponder(s -> {
            popUp.clearAllWidgets();
            popUp.setSize(Size.of(getSize().width, 0));
            if (showUp) {
                popUp.setSelfPosition(Position.of(0, 0));
            } else {
                popUp.setSelfPosition(Position.of(0, height));
            }
            setShow(true);
            this.engine.searchWord(s);
            if (isServer) {
                writeUpdateInfo(-1, buffer -> {});
            }
        });
    }

    @Override
    public void readUpdateInfo(int id, RegistryFriendlyByteBuf buffer) {
        if (id == -1) {
            popUp.clearAllWidgets();
            popUp.setSize(Size.of(getSize().width, 0));
            if (showUp) {
                popUp.setSelfPosition(Position.of(0, 0));
            } else {
                popUp.setSelfPosition(Position.of(0, getSize().height));
            }
        } else if (id == -2) {
            T r = search.deserialize(buffer);
            int size = popUp.getAllWidgetSize();
            int width = getSize().width;
            popUp.setSize(Size.of(getSize().width, Math.min(size + 1, capacity) * 15));
            if (showUp) {
                popUp.setSelfPosition(Position.of(0, -Math.min(size + 1, capacity) * 15));
            } else {
                popUp.setSelfPosition(Position.of(0, getSize().height));
            }
            popUp.addWidget(new ButtonWidget(0, size * 15, width,
                    15, new TextTexture(search.resultDisplay(r)).setWidth(width).setType(TextTexture.TextType.ROLL),
                    cd -> {
                        search.selectResult(r);
                        setShow(false);
                        textFieldWidget.setCurrentString(search.resultDisplay(r));
                    }).setHoverBorderTexture(-1, -1));
        } else {
            super.readUpdateInfo(id, buffer);
        }
    }

    public SearchComponentWidget<T> setCapacity(int capacity) {
        this.capacity = capacity;
        popUp.setSize(Size.of(getSize().width, Math.min(popUp.getAllWidgetSize(), capacity) * 15));
        if (showUp) {
            popUp.setSelfPosition(Position.of(0, -Math.min(popUp.getAllWidgetSize(), capacity) * 15));
        } else {
            popUp.setSelfPosition(Position.of(0, getSize().height));
        }
        return this;
    }

    public SearchComponentWidget<T> setCurrentString(String currentString) {
        textFieldWidget.setCurrentString(currentString);
        return this;
    }

    public String getCurrentString() {
        return textFieldWidget.getCurrentString();
    }

    public void setShow(boolean isShow) {
        this.isShow = isShow;
        popUp.setVisible(isShow);
        popUp.setActive(isShow);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInForeground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        boolean lastVisible = popUp.isVisible();
        popUp.setVisible(false);
        super.drawInForeground(graphics, mouseX, mouseY, partialTicks);
        popUp.setVisible(lastVisible);

        if(isShow) {
            graphics.pose().translate(0, 0, 200);
            popUp.drawInBackground(graphics, mouseX, mouseY, partialTicks);
            popUp.drawInForeground(graphics, mouseX, mouseY, partialTicks);
            graphics.pose().translate(0, 0, -200);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        boolean lastVisible = popUp.isVisible();
        popUp.setVisible(false);
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        popUp.setVisible(lastVisible);
    }

    public interface IWidgetSearch<T> extends ISearch<T> {
        String resultDisplay(T value);

        void selectResult(T value);

        /**
         * just used for server side
         */
        default void serialize(T value, RegistryFriendlyByteBuf buf) {
            buf.writeUtf(resultDisplay(value));
        }

        /**
         * just used for server side
         */
        default T deserialize(RegistryFriendlyByteBuf buf) {
            return (T) buf.readUtf();
        }
    }
}
