package com.lowdragmc.lowdraglib2.gui.ingredient;

import net.minecraft.client.renderer.Rect2i;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public interface Target extends Consumer<Object> {
    @Nonnull
    Rect2i getArea();
    void accept(Object var1);
}