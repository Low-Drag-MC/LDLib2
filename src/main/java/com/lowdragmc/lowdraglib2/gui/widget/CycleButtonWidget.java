package com.lowdragmc.lowdraglib2.gui.widget;

import com.lowdragmc.lowdraglib2.gui.texture.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * @author KilaBash
 * @date 2023/2/26
 * @implNote CycleButtonWidget
 */
@Accessors(chain = true)
public class CycleButtonWidget extends Widget {
    @Setter
    protected Int2ObjectFunction<IGuiTexture> texture;
    @Setter
    protected IntConsumer onChanged;
    @Setter
    protected IntSupplier indexSupplier;
    protected int range, index;

    public CycleButtonWidget(int xPosition, int yPosition, int width, int height, int range, Int2ObjectFunction<IGuiTexture> texture, IntConsumer onChanged) {
        super(xPosition, yPosition, width, height);
        this.texture = texture;
        this.onChanged = onChanged;
        this.range = range;
        setBackground(texture.get(0));
    }

    public void setIndex(int index) {
        this.index = index;
        setBackground(texture.get(index));
    }

    @Override
    public void writeInitialData(RegistryFriendlyByteBuf buffer) {
        super.writeInitialData(buffer);
        if (indexSupplier != null) {
            index = indexSupplier.getAsInt();
        }
        buffer.writeVarInt(index);
    }

    @Override
    public void readInitialData(RegistryFriendlyByteBuf buffer) {
        super.readInitialData(buffer);
        index = buffer.readVarInt();
        setBackground(texture.get(index));
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (!isClientSideWidget && indexSupplier != null) {
            var newIndex = indexSupplier.getAsInt();
            if (newIndex != index) {
                index = newIndex;
                writeUpdateInfo(1, buf -> buf.writeVarInt(index));
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void updateScreen() {
        super.updateScreen();
        if (isClientSideWidget && indexSupplier != null) {
            var newIndex = indexSupplier.getAsInt();
            if (newIndex != index) {
                index = newIndex;
                setBackground(texture.get(index));
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOverElement(mouseX, mouseY)) {
            index++;
            if (index >= range) {
                index = 0;
            }
            setBackground(texture.get(index));
            if (onChanged != null) {
                onChanged.accept(index);
            }
            writeClientAction(1, buf -> buf.writeVarInt(index));
            playButtonClickSound();
            return true;
        }
        return false;
    }

    @Override
    public void handleClientAction(int id, RegistryFriendlyByteBuf buffer) {
        super.handleClientAction(id, buffer);
        if (id == 1) {
            index = buffer.readVarInt();
            if (onChanged != null) {
                onChanged.accept(index);
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void readUpdateInfo(int id, RegistryFriendlyByteBuf buffer) {
        if (id == 1) {
            index = buffer.readVarInt();
            setBackground(texture.get(index));
        } else {
            super.readUpdateInfo(id, buffer);
        }
    }
}
