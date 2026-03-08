package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import org.joml.Matrix3x2f;

public interface IGUIContext {
    Matrix3x2f currentPose();

    void pushTransform(Transform2D transform, UIElement element);

    void popTransform(Transform2D transform);

    boolean isInsideScissor(float minX, float minY, float width, float height);

    void drawTexture(IGuiTexture texture, float x, float y, float width, float height);

    void enableScissor(float x, float y, float width, float height);

    void disableScissor();

    void pushVisualLayer(UIElement element);

    void popVisualLayer();

    int getElementColor();

    void setElementColor(int color);

    void resetElementColor();
}
