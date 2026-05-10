package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.SpriteTextureClientSupport;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.math.Position;
import com.lowdragmc.lowdraglib2.math.Size;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.resources.Identifier;

@KJSBindings
@LDLRegisterClient(name = "sprite_texture", registry = "ldlib2:gui_texture")
@Accessors(chain = true)
public class SpriteTexture extends TransformTexture {
    public enum WrapMode {
        CLAMP,
        REPEAT,
        MIRRORED_REPEAT
    }

    @Configurable(name = "ldlib.gui.editor.name.resource")
    @Getter
    private Identifier imageLocation = LDLib2.id("textures/gui/icon.png");

    @Configurable
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    @Setter
    public Position spritePosition = Position.of(0, 0);

    @Configurable
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    @Setter
    public Size spriteSize = Size.of(0, 0);

    @Configurable
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    @Setter
    public Position borderLT = Position.of(0, 0);

    @Configurable
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    @Setter
    public Position borderRB = Position.of(0, 0);

    @Configurable
    @ConfigColor
    public int color = -1;

    @Configurable
    @Setter
    public WrapMode wrapMode = WrapMode.CLAMP;

    public static SpriteTexture of(Identifier imageLocation) {
        return new SpriteTexture().setImageLocation(imageLocation);
    }

    public static SpriteTexture of(String imageLocation) {
        return of(Identifier.parse(imageLocation));
    }

    @ConfigSetter(field = "imageLocation")
    public SpriteTexture setImageLocation(Identifier imageLocation) {
        this.imageLocation = imageLocation;
        return this;
    }

    public SpriteTexture setSprite(int x, int y, int width, int height) {
        this.spritePosition = Position.of(x, y);
        this.spriteSize = Size.of(width, height);
        return this;
    }

    public SpriteTexture setBorder(int left, int top, int right, int bottom) {
        this.borderLT = Position.of(left, top);
        this.borderRB = Position.of(right, bottom);
        return this;
    }

    public SpriteTexture setBorder(int border) {
        return setBorder(border, border, border, border);
    }

    @Override
    public SpriteTexture setColor(int color) {
        this.color = color;
        return this;
    }

    @Override
    public SpriteTexture copy() {
        var copied = new SpriteTexture()
                .setImageLocation(imageLocation)
                .setSprite(spritePosition.getX(), spritePosition.getY(), spriteSize.getWidth(), spriteSize.getHeight())
                .setBorder(borderLT.getX(), borderLT.getY(), borderRB.getX(), borderRB.getY())
                .setColor(color)
                .setWrapMode(wrapMode);
        copied.copyTransform(this);
        return copied;
    }

    @Override
    public IGuiTexture interpolate(IGuiTexture other, float lerp) {
        if (other.getRawTexture() instanceof SpriteTexture spriteTexture) {
            return SpriteTextureInterpolation.of(copy(), spriteTexture.copy(), lerp);
        }
        return super.interpolate(other, lerp);
    }

    @Override
    public void createPreview(ConfiguratorGroup father) {
        SpriteTextureClientSupport.createPreview(this, father);
    }
}
