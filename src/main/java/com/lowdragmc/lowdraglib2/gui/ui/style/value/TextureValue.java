package com.lowdragmc.lowdraglib2.gui.ui.style.value;

import com.lowdragmc.lowdraglib2.editor.resource.BuiltinPath;
import com.lowdragmc.lowdraglib2.editor.resource.FilePath;
import com.lowdragmc.lowdraglib2.editor.resource.TexturesResource;
import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public class TextureValue extends StyleValue<IGuiTexture> {
    public static final Pattern BORDER = Pattern.compile("border\\((\\d+)\\)");

    public TextureValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected @Nullable IGuiTexture doCompute(String rawValue) {
        if (rawValue.isBlank() || rawValue.equalsIgnoreCase("empty")) {
            return IGuiTexture.EMPTY;
        }
        if (rawValue.startsWith("border ")) {
            var split = rawValue.substring(7).trim().split(" ");
            if (split.length == 2) {
                var border = Integer.parseInt(split[0]);
                var color = ColorUtils.parseColor(split[1]);
                if (color != null) {
                    return new ColorBorderTexture(border, color);
                }
            }
        } else if (rawValue.startsWith("sprite ")) {
            var split = rawValue.substring(8).trim().split(" ");
            if (split.length > 0) {
                var sprite = SpriteTexture.of(split[0]);
                if (split.length > 4) {
                    sprite.setSprite(Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3]), Integer.parseInt(split[4]));
                }
                if (split.length > 8) {
                    sprite.setBorder(Integer.parseInt(split[5]), Integer.parseInt(split[6]), Integer.parseInt(split[7]), Integer.parseInt(split[8]));
                }
                if (split.length > 9) {
                    sprite.setColor(ColorUtils.parseColor(split[9]));
                }
                return sprite;
            }
        } else if (rawValue.startsWith("file ")) {
            var path = rawValue.substring(4).trim();
            return TexturesResource.INSTANCE.getResourceInstance().getResource(new FilePath(path));
        } else if (rawValue.startsWith("builtin ")) {
            var path = rawValue.substring(8).trim();
            return TexturesResource.INSTANCE.getResourceInstance().getResource(new BuiltinPath(path));
        } else {
            var color = ColorUtils.parseColor(rawValue);
            if (color != null) {
                return new ColorRectTexture(color);
            }
        }
        return null;
    }
}
