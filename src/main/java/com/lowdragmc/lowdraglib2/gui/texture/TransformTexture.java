package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.syncdata.annotation.SkipPersistedValue;
import lombok.Getter;

/**
 * @author KilaBash
 * @date 2022/12/5
 * @implNote TransformTexture
 */
@Getter
public abstract class TransformTexture implements IGuiTexture {
    @Configurable(name = "Transform", subConfigurable = true)
    protected final Transform2D transform2D = new Transform2D();

    public TransformTexture rotate(float degree) {
        transform2D.rotation(degree);
        return this;
    }

    public TransformTexture scale(float scale) {
        transform2D.scale(scale);
        return this;
    }

    public TransformTexture scale(float width, float height) {
        transform2D.scale(width, height);
        return this;
    }

    public TransformTexture transform(float xOffset, float yOffset) {
        transform2D.translate(xOffset, yOffset);
        return this;
    }

    @Override
    public void beforeDeserialize() {
        transform2D.setIdentity();
    }

    @SkipPersistedValue(field = "transform2D")
    private boolean skipTransform2DPersisted(Transform2D transform2D) {
        return transform2D.isIdentity();
    }

    public void copyTransform(TransformTexture transformTexture) {
        transform2D.copyFrom(transformTexture.transform2D);
    }

    public void copyTransform(Transform2D transform) {
        transform2D.copyFrom(transform);
    }
}
