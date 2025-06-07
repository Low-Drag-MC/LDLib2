package com.lowdragmc.lowdraglib2.gui.widget;


import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.editor_outdated.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.math.Position;
import com.lowdragmc.lowdraglib2.math.Size;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

@Configurable(name = "widget.image", collapse = false)
@LDLRegister(name = "image", group = "widget.basic", registry = "ldlib2:widget")
public class ImageWidget extends Widget implements IConfigurableWidget {

    @Configurable(name = "ldlib.gui.editor.name.border")
    @ConfigNumber(range = {-100, 100})
    @Getter
    private int border;
    @Configurable(name = "ldlib.gui.editor.name.border_color")
    @ConfigColor
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

