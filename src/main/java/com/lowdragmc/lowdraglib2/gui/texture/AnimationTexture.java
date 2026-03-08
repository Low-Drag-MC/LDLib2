package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import lombok.Getter;
import net.minecraft.resources.Identifier;

/**
 * @author KilaBash
 * @date 2022/9/14
 * @implNote AnimationTexture
 */
@KJSBindings
@LDLRegisterClient(name = "animation_texture", registry = "ldlib2:gui_texture")
public class AnimationTexture extends TransformTexture {
    @Configurable(name = "ldlib.gui.editor.name.resource")
    public Identifier imageLocation;

    @Configurable(tips = "ldlib.gui.editor.tips.cell_size")
    @ConfigNumber(range = {1, Integer.MAX_VALUE})
    @Getter
    protected int cellSize;

    @Configurable(tips = "ldlib.gui.editor.tips.cell_from")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    @Getter
    protected int from;

    @Configurable(tips = "ldlib.gui.editor.tips.cell_to")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    @Getter
    protected int to;

    @Configurable(tips = "ldlib.gui.editor.tips.cell_animation")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    @Getter
    protected int animation;

    @Configurable
    @ConfigColor
    @Getter
    protected int color = -1;

    protected int currentFrame;
    protected int currentTime;
    protected long lastTick;

    public AnimationTexture() {
        this("ldlib2:textures/gui/particles.png");
        setCellSize(8).setAnimation(32, 44).setAnimation(1);
    }

    public AnimationTexture(String imageLocation) {
        this.imageLocation = Identifier.parse(imageLocation);
    }

    public AnimationTexture(Identifier imageLocation) {
        this.imageLocation = imageLocation;
    }

    @Override
    public AnimationTexture copy() {
        var copied = new AnimationTexture(imageLocation).setCellSize(cellSize).setAnimation(from, to).setAnimation(animation).setColor(color);
        copied.copyTransform(this);
        return copied;
    }

    public AnimationTexture setTexture(String imageLocation) {
        this.imageLocation = Identifier.parse(imageLocation);
        return this;
    }

    public AnimationTexture setCellSize(int cellSize) {
        this.cellSize = cellSize;
        return this;
    }

    public AnimationTexture setAnimation(int from, int to) {
        this.currentFrame = from;
        this.from = from;
        this.to = to;
        return this;
    }

    public AnimationTexture setAnimation(int animation) {
        this.animation = animation;
        return this;
    }

    @Override
    public AnimationTexture setColor(int color) {
        this.color = color;
        return this;
    }
}
