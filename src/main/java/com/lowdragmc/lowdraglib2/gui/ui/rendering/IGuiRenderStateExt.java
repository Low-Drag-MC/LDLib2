package com.lowdragmc.lowdraglib2.gui.ui.rendering;

/// Duck-type interface implemented on {@code GuiRenderState} via mixin, for
/// {@link com.lowdragmc.lowdraglib2.client.GameFrameCapture}: where in the frame's strata the copy of
/// the game is taken.
public interface IGuiRenderStateExt {
    /// Ends the current stratum and remembers the next one as where the copy is taken: what was
    /// extracted so far is in the picture, what comes after is not. Only the first call of a frame
    /// counts.
    void ldlib2$markFrameCaptureBoundary();

    /// The first stratum left out of the copy, or {@link Integer#MAX_VALUE} for none marked.
    int ldlib2$getFrameCaptureStratum();

    int ldlib2$getFirstStratumAfterBlur();

    void ldlib2$setFirstStratumAfterBlur(int stratum);
}
