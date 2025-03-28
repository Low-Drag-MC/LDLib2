package com.lowdragmc.lowdraglib.gui.widget;

import com.lowdragmc.lowdraglib.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.editor.configurator.Configurator;
import com.lowdragmc.lowdraglib.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.editor.configurator.GuiTextureConfigurator;
import com.lowdragmc.lowdraglib.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.UIResourceTexture;
import com.lowdragmc.lowdraglib.math.Position;
import com.lowdragmc.lowdraglib.math.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

@LDLRegister(name = "progress", group = "widget.basic", registry = "ldlib:widget")
@Accessors(chain = true)
public class ProgressWidget extends Widget implements IConfigurableWidget {
    public final static DoubleSupplier JEIProgress = () -> Math.abs(System.currentTimeMillis() % 2000) / 2000.;
    @Setter
    public DoubleSupplier progressSupplier;
    @Setter
    private Function<Double, String> dynamicHoverTips;
    @Configurable(name = "ldlib.gui.editor.name.progressTexture")
    @Setter
    private IGuiTexture progressTexture;
    @Configurable(name = "ldlib.gui.editor.name.overlayTexture")
    @Setter
    private IGuiTexture overlayTexture;
    @Getter
    private double lastProgressValue;

    public ProgressWidget() {
        this(JEIProgress, 0, 0, 40, 40, new ProgressTexture());
    }

    public ProgressWidget(DoubleSupplier progressSupplier, int x, int y, int width, int height, ResourceTexture fullImage) {
        super(Position.of(x, y), Size.of(width, height));
        this.progressSupplier = progressSupplier;
        this.progressTexture = new ProgressTexture(fullImage.getSubTexture(0.0, 0.0, 1.0, 0.5), fullImage.getSubTexture(0.0, 0.5, 1.0, 0.5));
        this.lastProgressValue = -1;
    }

    public ProgressWidget(DoubleSupplier progressSupplier, int x, int y, int width, int height, ProgressTexture progressBar) {
        super(Position.of(x, y), Size.of(width, height));
        this.progressSupplier = progressSupplier;
        this.progressTexture = progressBar;
        this.lastProgressValue = -1;
    }

    public ProgressWidget(DoubleSupplier progressSupplier, int x, int y, int width, int height) {
        super(Position.of(x, y), Size.of(width, height));
        this.progressSupplier = progressSupplier;
    }

    public ProgressWidget setProgressTexture(IGuiTexture emptyBarArea, IGuiTexture filledBarArea) {
        this.progressTexture = new ProgressTexture(emptyBarArea, filledBarArea);
        return this;
    }

    public ProgressWidget setFillDirection(ProgressTexture.FillDirection fillDirection) {
        if (this.progressTexture instanceof ProgressTexture progressTexture) {
            progressTexture.setFillDirection(fillDirection);
        }
        return this;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.progressTexture != null) {
            this.progressTexture.updateTick();
        }
        if (this.overlayTexture != null) {
            this.overlayTexture.updateTick();
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInForeground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if ((!tooltipTexts.isEmpty() || dynamicHoverTips != null) && isMouseOverElement(mouseX, mouseY) && getHoverElement(mouseX, mouseY) == this && gui != null && gui.getModularUIGui() != null) {
            var tips = new ArrayList<>(tooltipTexts);
            if (dynamicHoverTips != null) {
                tips.add(Component.translatable(dynamicHoverTips.apply(lastProgressValue)));
            }
            gui.getModularUIGui().setHoverTooltip(tips, ItemStack.EMPTY, null, null);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        if (progressSupplier == JEIProgress || isClientSideWidget) {
            lastProgressValue = progressSupplier.getAsDouble();
        }
        if (progressTexture instanceof ProgressTexture texture) {
            texture.setProgress(lastProgressValue);
        }
        Position pos = getPosition();
        Size size = getSize();
        if (progressTexture != null) {
            progressTexture.draw(graphics, mouseX, mouseY, pos.x, pos.y, size.width, size.height);
        }
        if (overlayTexture != null) {
            overlayTexture.draw(graphics, mouseX, mouseY, pos.x, pos.y, size.width, size.height);
        }
        drawOverlay(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void initWidget() {
        super.initWidget();
        this.lastProgressValue = progressSupplier.getAsDouble();
    }

    @Override
    public void writeInitialData(RegistryFriendlyByteBuf buffer) {
        super.writeInitialData(buffer);
        buffer.writeDouble(lastProgressValue);
    }

    @Override
    public void readInitialData(RegistryFriendlyByteBuf buffer) {
        super.readInitialData(buffer);
        lastProgressValue = buffer.readDouble();
    }

    @Override
    public void detectAndSendChanges() {
        double actualValue = progressSupplier.getAsDouble();
        if (actualValue - lastProgressValue != 0) {
            this.lastProgressValue = actualValue;
            writeUpdateInfo(0, buffer -> buffer.writeDouble(actualValue));
        }
    }

    @Override
    public void readUpdateInfo(int id, RegistryFriendlyByteBuf buffer) {
        if (id == 0) {
            this.lastProgressValue = buffer.readDouble();
        }
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        IConfigurableWidget.super.buildConfigurator(father);
        for (Configurator configurator : father.getConfigurators()) {
            if (configurator instanceof GuiTextureConfigurator guiConfigurator && configurator.getName().equals("progressTexture")) {
                guiConfigurator.setOnUpdate(t -> {
                    if (t instanceof ProgressTexture || (t instanceof UIResourceTexture uiResourceTexture && uiResourceTexture.getTexture() instanceof ProgressTexture)) {
                        this.progressTexture = t;
                    }
                });
                guiConfigurator.setAvailable(t -> t instanceof ProgressTexture || (t instanceof UIResourceTexture uiResourceTexture && uiResourceTexture.getTexture() instanceof ProgressTexture));
                guiConfigurator.setTips("ldlib.gui.editor.tips.progress_texture");
            }
        }
    }

    @Override
    public boolean canDragIn(Object dragging) {
        if (dragging instanceof IGuiTexture) {
            return dragging instanceof ProgressTexture || (dragging instanceof UIResourceTexture uiResourceTexture && uiResourceTexture.getTexture() instanceof ProgressTexture);
        }
        return IConfigurableWidget.super.canDragIn(dragging);
    }

    @Override
    public boolean handleDragging(Object dragging) {
        if (dragging instanceof IGuiTexture) {
            if (dragging instanceof ProgressTexture || (dragging instanceof UIResourceTexture uiResourceTexture && uiResourceTexture.getTexture() instanceof ProgressTexture) ) {
                this.progressTexture = (IGuiTexture) dragging;
                return true;
            }
            return false;
        } else return IConfigurableWidget.super.handleDragging(dragging);
    }

}
