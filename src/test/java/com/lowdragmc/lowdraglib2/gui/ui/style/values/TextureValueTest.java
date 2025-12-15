package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextureValueTest {

    /**
     * Tests the case where the input is blank or "empty".
     * The expected result is the IGuiTexture.EMPTY constant.
     */
    @Test
    void testComputeBlankOrEmpty() {
        TextureValue textureValue = new TextureValue("");
        assertEquals(IGuiTexture.EMPTY, textureValue.compute());

        textureValue = new TextureValue("empty");
        assertEquals(IGuiTexture.EMPTY, textureValue.compute());
    }

    /**
     * Tests the case where the input matches the "border"
     * syntax. Ensures that the ColorBorderTexture is created correctly.
     */
    @Test
    void testComputeBorderTexture() {
        TextureValue textureValue = new TextureValue("border(5, #FF00FF)");
        IGuiTexture result = textureValue.compute();

        assertNotNull(result);
        assertInstanceOf(ColorBorderTexture.class, result);
        ColorBorderTexture texture = (ColorBorderTexture) result;
        assertEquals(5, texture.border);
        assertEquals(ColorUtils.parseColor("#FF00FF"), texture.color);
    }

    /**
     * Tests the case where the input matches the "sprite"
     * syntax. Ensures that the SpriteTexture is created correctly with optional parameters.
     */
    @Test
    void testComputeSpriteTexture() {
        TextureValue textureValue = new TextureValue("sprite(some_sprite)");
        IGuiTexture result = textureValue.compute();

        assertNotNull(result);
        assertInstanceOf(SpriteTexture.class, result);
        SpriteTexture sprite = (SpriteTexture) result;
        assertEquals(ResourceLocation.parse("minecraft:some_sprite"), sprite.getImageLocation());
    }

    /**
     * Tests the creation of a SpriteTexture with advanced parameters such as
     * sprite dimensions, border, and color values.
     */
    @Test
    void testComputeAdvancedSpriteTexture() {
        TextureValue textureValue = new TextureValue("sprite(sprite_path, 0, 1, 2, 3, 4, 5, 6, 7, #FFFFFF)");
        IGuiTexture result = textureValue.compute();

        assertNotNull(result);
        assertInstanceOf(SpriteTexture.class, result);
        SpriteTexture sprite = (SpriteTexture) result;
        assertEquals(ResourceLocation.parse("minecraft:sprite_path"), sprite.getImageLocation());
        assertEquals(0, sprite.spritePosition.getX());
        assertEquals(1, sprite.spritePosition.getY());
        assertEquals(2, sprite.spriteSize.getWidth());
        assertEquals(3, sprite.spriteSize.getHeight());
        assertEquals(4, sprite.borderLT.getX());
        assertEquals(5, sprite.borderLT.getY());
        assertEquals(6, sprite.borderRB.getX());
        assertEquals(7, sprite.borderRB.getY());
        assertEquals(ColorUtils.parseColor("#FFFFFF"), sprite.color);
    }

//    /**
//     * Tests the case where the input matches the "file" syntax.
//     * Ensures that the correct file texture is fetched from the resource system.
//     */
//    @Test
//    void testComputeFilePathTexture() {
//        TextureValue textureValue = new TextureValue("file /example/path");
//        IGuiTexture result = textureValue.compute();
//
//        assertNotNull(result);
//    }
//
//    /**
//     * Tests the case where the input matches the "builtin" syntax.
//     * Ensures that the correct builtin texture is fetched from the resource system.
//     */
//    @Test
//    void testComputeBuiltinPathTexture() {
//        TextureValue textureValue = new TextureValue("builtin built-in:missing");
//        IGuiTexture result = textureValue.compute();
//
//        assertNotNull(result);
//        assertEquals(IGuiTexture.MISSING_TEXTURE, result);
//    }

    /**
     * Tests the case where the input is a valid color string.
     * The expected result is a ColorRectTexture with the parsed color.
     */
    @Test
    void testComputeColorRectTexture() {
        TextureValue textureValue = new TextureValue("#FF0000");
        IGuiTexture result = textureValue.compute();

        assertNotNull(result);
        assertInstanceOf(ColorRectTexture.class, result);
        ColorRectTexture texture = (ColorRectTexture) result;
        assertEquals(ColorUtils.parseColor("#FF0000"), texture.color);
    }

    /**
     * Tests the case where the input does not match any known syntax.
     * The expected result is null.
     */
    @Test
    void testComputeInvalidTexture() {
        TextureValue textureValue = new TextureValue("invalid_texture");
        IGuiTexture result = textureValue.compute();

        assertNull(result);
    }
}