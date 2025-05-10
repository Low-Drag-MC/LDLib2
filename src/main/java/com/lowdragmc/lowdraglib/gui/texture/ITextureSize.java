package com.lowdragmc.lowdraglib.gui.texture;

public interface ITextureSize {
    /**
     * Get the width of the texture
     *
     * @return the width of the texture
     */
    int ldlib$getImageWidth();
    default int getWidth() {
        return ldlib$getImageWidth();
    }

    /**
     * Get the height of the texture
     *
     * @return the height of the texture
     */
    int ldlib$getImageHeight();
    default int getHeight() {
        return ldlib$getImageHeight();
    }

}
