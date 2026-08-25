package com.lowdragmc.lowdraglib2.core.mixins.ui;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.lowdragmc.lowdraglib2.gui.ui.utils.CursorState;
import com.lowdragmc.lowdraglib2.gui.ui.utils.RawInputGate;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets the process drive the game window's pointer itself, instead of dragging the physical one
 * around with {@code glfwSetCursorPos}.
 *
 * <p>Descriptors are spelled out on every injection because {@code ldlib2.mixins.json} sets
 * {@code injectors.defaultRequire = 1}: a target that stops resolving must fail loudly at startup
 * rather than silently stop applying, and three of these are private methods.
 *
 * @see CursorState
 * @see RawInputGate
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    // Everything downstream derives the GUI-space mouse from these: Gui#extractRenderState's
    // arguments to Screen#render, and so ModularUI's per-frame hover, tooltips, the carried item
    // stack and AbstractContainerScreen's hovered slot. Overriding them here is what makes a whole
    // synthetic pointer work without touching the one belonging to whoever is at the machine.

    // The scaled pair is the one that actually matters, and it is not reached through the raw pair:
    // getScaledXPos(Window) reads the xpos *field*, so overriding only the getter below leaves every
    // rendered frame recomputing hover from the physical pointer and quietly undoing the placement
    // the harness just made. That showed up as a hover assertion that passed or failed depending on
    // where the mouse happened to be sitting.
    //
    // No conversion: a Source reports in GUI-scaled coordinates, which is exactly what these return.

    @ModifyReturnValue(method = "getScaledXPos(Lcom/mojang/blaze3d/platform/Window;)D", at = @At("RETURN"))
    private double ldlib2$virtualScaledXpos(double original) {
        var source = CursorState.getSource();
        return source == null ? original : source.cursorX();
    }

    @ModifyReturnValue(method = "getScaledYPos(Lcom/mojang/blaze3d/platform/Window;)D", at = @At("RETURN"))
    private double ldlib2$virtualScaledYpos(double original) {
        var source = CursorState.getSource();
        return source == null ? original : source.cursorY();
    }

    // Kept for anything reading the unscaled pair directly — a test asserting what Minecraft thinks
    // the cursor is, and any third-party code that skips the scaled accessors.

    @ModifyReturnValue(method = "xpos()D", at = @At("RETURN"))
    private double ldlib2$virtualXpos(double original) {
        var source = CursorState.getSource();
        return source == null ? original : CursorState.toPhysicalX(source.cursorX());
    }

    @ModifyReturnValue(method = "ypos()D", at = @At("RETURN"))
    private double ldlib2$virtualYpos(double original) {
        var source = CursorState.getSource();
        return source == null ? original : CursorState.toPhysicalY(source.cursorY());
    }

    /**
     * A synthetic pointer and a captured physical one are mutually exclusive by definition.
     *
     * <p>{@code Gui#setScreen(null)} calls this, and an automated run closes the screen between
     * every scenario, so without this it hides and captures the pointer of whoever is using the
     * machine. Cancelling at HEAD leaves {@code mouseGrabbed} false, which is consistent rather than
     * lossy: {@code releaseMouse}'s whole body is inside {@code if (mouseGrabbed)}, so it stays a
     * no-op instead of warping the cursor to the window centre later.
     */
    @Inject(method = "grabMouse()V", at = @At("HEAD"), cancellable = true)
    private void ldlib2$doNotGrabMouse(CallbackInfo ci) {
        if (CursorState.getSource() != null) {
            ci.cancel();
        }
    }

    // OS input isolation. onMove as well as the buttons: handleAccumulatedMovement dispatches
    // mouseMoved/mouseDragged from the raw xpos/ypos *fields*, which the getters above do not cover,
    // the moment a stray click makes the window active again. Cancelling the movement here rather
    // than there also keeps accumulatedDX/DY from growing while the gate is up.

    @Inject(method = "onMove(JDD)V", at = @At("HEAD"), cancellable = true)
    private void ldlib2$dropOsMove(long windowPointer, double xpos, double ypos, CallbackInfo ci) {
        if (RawInputGate.isBlocked()) {
            ci.cancel();
        }
    }

    @Inject(method = "onButton(JLnet/minecraft/client/input/MouseButtonInfo;I)V", at = @At("HEAD"), cancellable = true)
    private void ldlib2$dropOsPress(long windowPointer, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        if (RawInputGate.isBlocked()) {
            ci.cancel();
        }
    }

    @Inject(method = "onScroll(JDD)V", at = @At("HEAD"), cancellable = true)
    private void ldlib2$dropOsScroll(long windowPointer, double xOffset, double yOffset, CallbackInfo ci) {
        if (RawInputGate.isBlocked()) {
            ci.cancel();
        }
    }
}
