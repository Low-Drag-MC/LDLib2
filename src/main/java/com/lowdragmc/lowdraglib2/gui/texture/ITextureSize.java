package com.lowdragmc.lowdraglib2.gui.texture;

public interface ITextureSize {
    /**
     * Get the width of the texture
     *
     * @return the width of the texture
     */
    int ldlib2$getImageWidth();
    default int getWidth() {
        return ldlib2$getImageWidth();
    }

    /**
     * Get the height of the texture
     *
     * @return the height of the texture
     */
    int ldlib2$getImageHeight();
    default int getHeight() {
        return ldlib2$getImageHeight();
    }

}
