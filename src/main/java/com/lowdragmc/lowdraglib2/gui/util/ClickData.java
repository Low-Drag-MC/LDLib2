package com.lowdragmc.lowdraglib2.gui.util;

import com.lowdragmc.lowdraglib2.core.mixins.accessor.MouseHandlerAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.network.FriendlyByteBuf;
import org.lwjgl.glfw.GLFW;

public class ClickData {
    public final int button;
    public final boolean isShiftClick;
    public final boolean isCtrlClick;
    public final boolean isRemote;

    private ClickData(int button, boolean isShiftClick, boolean isCtrlClick, boolean isRemote) {
        this.button = button;
        this.isShiftClick = isShiftClick;
        this.isCtrlClick = isCtrlClick;
        this.isRemote = isRemote;
    }

    @OnlyIn(Dist.CLIENT)
    public ClickData() {
        MouseHandler mouseHelper = Minecraft.getInstance().mouseHandler;
        long id = Minecraft.getInstance().getWindow().getWindow();
        this.button = mouseHelper instanceof MouseHandlerAccessor accessor ? accessor.getActiveButton() : -1;
        this.isShiftClick = InputConstants.isKeyDown(id, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(id, GLFW.GLFW_KEY_RIGHT_SHIFT);
        this.isCtrlClick = InputConstants.isKeyDown(id, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(id, GLFW.GLFW_KEY_RIGHT_CONTROL);
        this.isRemote = true;
    }

    @OnlyIn(Dist.CLIENT)
    public void writeToBuf(FriendlyByteBuf buf) {
        buf.writeVarInt(button);
        buf.writeBoolean(isShiftClick);
        buf.writeBoolean(isCtrlClick);
    }

    public static ClickData readFromBuf(FriendlyByteBuf buf) {
        int button = buf.readVarInt();
        boolean shiftClick = buf.readBoolean();
        boolean ctrlClick = buf.readBoolean();
        return new ClickData(button, shiftClick, ctrlClick, false);
    }
}
