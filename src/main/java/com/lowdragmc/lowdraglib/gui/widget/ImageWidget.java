package com.lowdragmc.lowdraglib.gui.widget;


import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberColor;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

@Configurable(name = "ldlib.gui.editor.register.widget.image", collapse = false)
@LDLRegister(name = "image", group = "widget.basic")
public class ImageWidget extends Widget implements IConfigurableWidget {

    @Configurable(name = "ldlib.gui.editor.name.border")
    @NumberRange(range = {-100, 100})
    @Getter
    private int border;
    @Configurable(name = "ldlib.gui.editor.name.border_color")
    @NumberColor
    @Getter
    private int borderColor = -1;

    private Supplier<IGuiTexture> textureSupplier;

    public ImageWidget() {
        this(0, 0, 50, 50, new ResourceTexture());
    }


    public ImageWidget(int xPosition, int yPosition, int width, int height, IGuiTexture area) {
        super(xPosition, yPosition, width, height);
        setImage(area);
    }

    public ImageWidget(int xPosition, int yPosition, int width, int height, Supplier<IGuiTexture> textureSupplier) {
        super(xPosition, yPosition, width, height);
        setImage(textureSupplier);
    }

    public ImageWidget setImage(IGuiTexture area) {
        setBackground(area);
        return this;
    }

    public ImageWidget setImage(Supplier<IGuiTexture> textureSupplier) {
        this.textureSupplier = textureSupplier;
        if (textureSupplier != null) {
            setBackground(textureSupplier.get());
        }
        return this;
    }

    public IGuiTexture getImage() {
        return backgroundTexture;
    }

    public ImageWidget setBorder(int border, int color) {
        this.border = border;
        this.borderColor = color;
        return this;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void updateScreen() {
        super.updateScreen();
        if (textureSupplier != null) {
            setBackground(textureSupplier.get());
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        Position position = getPosition();
        Size size = getSize();
        if (border > 0) {
            DrawerHelper.drawBorder(graphics, position.x, position.y, size.width, size.height, borderColor, border);
        }
        drawOverlay(graphics, mouseX, mouseY, partialTicks);
    }
}

