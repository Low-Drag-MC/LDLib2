package com.lowdragmc.lowdraglib2.core.mixins.accessor;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author KilaBash
 * @date 2023/7/1
 * @implNote MouseHandlerAccessor. Behavioural changes live in
 *           {@link com.lowdragmc.lowdraglib2.core.mixins.ui.MouseHandlerMixin}.
 */
@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
    /**
     * So a scripted run that starts inside an already-grabbed game can drop the grab without
     * {@code releaseMouse}'s warp of the physical pointer to the window centre.
     */
    @Accessor("mouseGrabbed") void setMouseGrabbed(boolean grabbed);
}
