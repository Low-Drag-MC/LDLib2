package com.lowdragmc.lowdraglib.gui.widget;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

@LDLRegister(name = "button", group = "widget.basic")
@RemapPrefixForJS("kjs$")
public class ButtonWidget extends Widget implements IConfigurableWidget {

    @Configurable(name = "ldlib.gui.editor.name.clicked_texture")
    protected IGuiTexture clickedTexture;

    protected Consumer<ClickData> onPressCallback;
    @Getter
    protected boolean isClicked = false;

    public ButtonWidget() {
        this(0, 0, 40, 20, new GuiTextureGroup(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("Button")), null);
    }

    @Override
    public void initTemplate() {
        setHoverBorderTexture(1, -1);
    }

    public ButtonWidget(int xPosition, int yPosition, int width, int height, IGuiTexture buttonTexture, Consumer<ClickData> onPressed) {
        super(xPosition, yPosition, width, height);
        this.onPressCallback = onPressed;
        setBackground(buttonTexture);
    }

    public ButtonWidget(int xPosition, int yPosition, int width, int height, Consumer<ClickData> onPressed) {
        super(xPosition, yPosition, width, height);
        this.onPressCallback = onPressed;
    }

    public ButtonWidget setOnPressCallback(Consumer<ClickData> onPressCallback) {
        this.onPressCallback = onPressCallback;
        return this;
    }

    public ButtonWidget setButtonTexture(IGuiTexture... buttonTexture) {
        super.setBackground(buttonTexture);
        return this;
    }

    @HideFromJS
    public ButtonWidget setHoverTexture(IGuiTexture... hoverTexture) {
        super.setHoverTexture(hoverTexture);
        return this;
    }

    public ButtonWidget setClickedTexture(IGuiTexture... clickedTexture) {
        this.clickedTexture = clickedTexture.length > 1 ? new GuiTextureGroup(clickedTexture) : clickedTexture[0];
        return this;
    }

    public ButtonWidget kjs$setHoverTexture(IGuiTexture... hoverTexture) {
        super.setHoverTexture(hoverTexture);
        return this;
    }

    public ButtonWidget setHoverBorderTexture(int border, int color) {
        super.setHoverTexture(new ColorBorderTexture(border, color));
        return this;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOverElement(mouseX, mouseY) && button == 0) {
            isClicked = true;
            ClickData clickData = new ClickData();
            writeClientAction(1, clickData::writeToBuf);
            if (onPressCallback != null) {
                onPressCallback.accept(clickData);
            }
            playButtonClickSound();
            return true;
        }
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isClicked = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void handleClientAction(int id, RegistryFriendlyByteBuf buffer) {
        super.handleClientAction(id, buffer);
        if (id == 1) {
            ClickData clickData = ClickData.readFromBuf(buffer);
            if (onPressCallback != null) {
                onPressCallback.accept(clickData);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected void drawBackgroundTexture(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {
        var isHovered = isMouseOverElement(mouseX, mouseY);
        if (!isHovered || drawBackgroundWhenHover) {
            if (isClicked && clickedTexture != null) {
                Position pos = getPosition();
                Size size = getSize();
                clickedTexture.draw(graphics, mouseX, mouseY, pos.x, pos.y, size.width, size.height);
            } else if (backgroundTexture != null) {
                Position pos = getPosition();
                Size size = getSize();
                backgroundTexture.draw(graphics, mouseX, mouseY, pos.x, pos.y, size.width, size.height);
            }
        }
        if (hoverTexture != null && isHovered && isActive()) {
            Position pos = getPosition();
            Size size = getSize();
            hoverTexture.draw(graphics, mouseX, mouseY, pos.x, pos.y, size.width, size.height);
        }
    }
}
