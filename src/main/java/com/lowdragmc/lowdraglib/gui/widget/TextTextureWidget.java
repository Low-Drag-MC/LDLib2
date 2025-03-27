package com.lowdragmc.lowdraglib.gui.widget;

import com.lowdragmc.lowdraglib.editor.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.math.Size;
import dev.latvian.mods.rhino.util.RemapForJS;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegister(name = "text_texture", group = "widget.basic")
public class TextTextureWidget extends Widget implements IConfigurableWidget {
    @Configurable(name = "ldlib.gui.editor.name.text")
    @Getter
    private Component lastComponent;
    @Configurable(name = "ldlib.gui.editor.name.text", subConfigurable = true)
    @Getter
    private final TextTexture textTexture = new TextTexture();
    private Supplier<Component> textSupplier;

    public TextTextureWidget() {
        this(0, 0, 50, 50, "Text");
    }

    public TextTextureWidget(int xPosition, int yPosition, int width, int height) {
        this(xPosition, yPosition, width, height, "");
    }

    public TextTextureWidget(int xPosition, int yPosition, int width, int height, String text) {
        super(xPosition, yPosition, width, height);
        textTexture.setDropShadow(true);
        textTexture.setWidth(width);
        textTexture.setSupplier(() -> lastComponent == null ? "" : lastComponent.getString());
        if (isRemote()) {
            lastComponent = Component.translatable(text);
        }
        setText(text);
    }

    @ConfigSetter(field = "lastComponent")
    public void setLastComponent(Component component) {
        this.lastComponent = component;
        setText(component);
    }

    @Override
    @ConfigSetter(field = "size")
    public void setSize(Size size) {
        super.setSize(size);
        textTexture.setWidth(size.width);
    }

    public TextTextureWidget textureStyle(Consumer<TextTexture> consumer) {
        consumer.accept(textTexture);
        return this;
    }

    public TextTextureWidget setText(String text) {
        textSupplier = () -> Component.translatable(text);
        return this;
    }

    @RemapForJS("setComponent")
    public TextTextureWidget setText(Component text) {
        textSupplier = () -> text;
        return this;
    }

    public TextTextureWidget setText(Supplier<Component> textSupplier) {
        this.textSupplier = textSupplier;
        return this;
    }

    @Override
    public void writeInitialData(RegistryFriendlyByteBuf buffer) {
        super.writeInitialData(buffer);
        if (!isClientSideWidget) {
            buffer.writeBoolean(true);
            this.lastComponent = textSupplier.get();
            ComponentSerialization.STREAM_CODEC.encode(buffer, lastComponent);
        } else {
            buffer.writeBoolean(false);
        }
    }

    @Override
    public void readInitialData(RegistryFriendlyByteBuf buffer) {
        super.readInitialData(buffer);
        if (buffer.readBoolean()) {
            this.lastComponent = ComponentSerialization.STREAM_CODEC.decode(buffer);
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (!isClientSideWidget) {
            var latest = textSupplier.get();
            if (!latest.equals(lastComponent)) {
                this.lastComponent = latest;
                writeUpdateInfo(-1, buffer -> ComponentSerialization.STREAM_CODEC.encode(buffer, this.lastComponent));
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void readUpdateInfo(int id, RegistryFriendlyByteBuf buffer) {
        if (id == -1) {
            this.lastComponent = ComponentSerialization.STREAM_CODEC.decode(buffer);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void updateScreen() {
        super.updateScreen();
        if (isClientSideWidget) {
            var latest = textSupplier.get();
            if (!latest.equals(lastComponent)) {
                this.lastComponent = latest;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        var position = getPosition();
        var size = getSize();
        textTexture.draw(graphics, mouseX, mouseY, position.x, position.y, size.width, size.height);
        drawOverlay(graphics, mouseX, mouseY, partialTicks);
    }
}

