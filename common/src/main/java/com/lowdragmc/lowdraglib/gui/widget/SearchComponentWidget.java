package com.lowdragmc.lowdraglib.gui.widget;

import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.utils.ISearch;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.SearchEngine;
import com.lowdragmc.lowdraglib.utils.Size;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import javax.annotation.Nullable;

import javax.annotation.Nonnull;
import java.util.function.Function;

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
    @Nullable
    protected Function<T, IGuiTexture> iconProvider;
    @Nullable
    @Getter
    private T current;

    public SearchComponentWidget(int x, int y, int width, int height, IWidgetSearch<T> search) {
        this(x, y, width, height, search, false);
    }

    public SearchComponentWidget(int x, int y, int width, int height, IWidgetSearch<T> search, boolean isServer) {
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
            popUp.setSize(new Size(getSize().width, Math.min(size + 1, capacity) * 15));
            if (showUp) {
                popUp.setSelfPosition(new Position(0, -Math.min(size + 1, capacity) * 15));
            } else {
                popUp.setSelfPosition(new Position(0, height));
            }
            popUp.waitToAdded(createCandidateWidget(search, r, size));
            if (isServer) {
                writeUpdateInfo(-2, buf -> search.serialize(r, buf));
            }
        });

        textFieldWidget.setTextResponder(s -> {
            popUp.clearAllWidgets();
            popUp.setSize(new Size(getSize().width, 0));
            if (showUp) {
                popUp.setSelfPosition(new Position(0, 0));
            } else {
                popUp.setSelfPosition(new Position(0, height));
            }
            setShow(true);
            this.engine.searchWord(s);
            if (isServer) {
                writeUpdateInfo(-1, buffer -> {});
            }
        });
    }

    public void setIconProvider(@Nonnull Function<T, IGuiTexture> iconProvider) {
        this.iconProvider = iconProvider;
        this.textFieldWidget.setSizeWidth(getSizeWidth() - 14);
        this.addWidget(0, new ImageWidget(getSizeWidth() - 14, 1, 12, 12,
                () -> current == null ? IGuiTexture.EMPTY : iconProvider.apply(current)));
    }

    private Widget createCandidateWidget(IWidgetSearch<T> search, T result, int row) {
        var width = getSizeWidth();
        var group = new WidgetGroup(0, row * 15, width, 15);
        var hasIcon = iconProvider != null;
        group.addWidget(new TextTextureWidget(0, 0, hasIcon ? width - 14 : width, 15, search.resultDisplay(result))
                .textureStyle(t -> t.setType(TextTexture.TextType.ROLL)));
        if (hasIcon) {
            group.addWidget(new ImageWidget(width - 14, 1, 12, 12, iconProvider.apply(result)));
        }
        group.addWidget(new ButtonWidget(0, row * 15, width, 15, IGuiTexture.EMPTY, cd -> {
            search.selectResult(result);
            setShow(false);
            setCurrent(result);
        }).setHoverBorderTexture(-1, -1));
        return group;
    }

    @Override
    public void readUpdateInfo(int id, FriendlyByteBuf buffer) {
        if (id == -1) {
            popUp.clearAllWidgets();
            popUp.setSize(new Size(getSize().width, 0));
            if (showUp) {
                popUp.setSelfPosition(new Position(0, 0));
            } else {
                popUp.setSelfPosition(new Position(0, getSize().height));
            }
        } else if (id == -2) {
            T r = search.deserialize(buffer);
            int size = popUp.getAllWidgetSize();
            popUp.setSize(new Size(getSize().width, Math.min(size + 1, capacity) * 15));
            if (showUp) {
                popUp.setSelfPosition(new Position(0, -Math.min(size + 1, capacity) * 15));
            } else {
                popUp.setSelfPosition(new Position(0, getSize().height));
            }
            popUp.addWidget(createCandidateWidget(search, r, size));
        } else {
            super.readUpdateInfo(id, buffer);
        }
    }

    public SearchComponentWidget<T> setCapacity(int capacity) {
        this.capacity = capacity;
        popUp.setSize(new Size(getSize().width, Math.min(popUp.getAllWidgetSize(), capacity) * 15));
        if (showUp) {
            popUp.setSelfPosition(new Position(0, -Math.min(popUp.getAllWidgetSize(), capacity) * 15));
        } else {
            popUp.setSelfPosition(new Position(0, getSize().height));
        }
        return this;
    }

    public SearchComponentWidget<T> setCurrent(@Nullable T content) {
        current = content;
        if (content != null) {
            textFieldWidget.setCurrentString(search.resultDisplay(content));
        } else {
            textFieldWidget.setCurrentString("");
        }
        return this;
    }

    public void setShow(boolean isShow) {
        this.isShow = isShow;
        popUp.setVisible(isShow);
        popUp.setActive(isShow);
    }

    @Override
    @Environment(EnvType.CLIENT)
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
    @Environment(EnvType.CLIENT)
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
        default void serialize(T value, FriendlyByteBuf buf) {
            buf.writeUtf(resultDisplay(value));
        }

        /**
         * just used for server side
         */
        default T deserialize(FriendlyByteBuf buf) {
            return (T) buf.readUtf();
        }
    }
}
