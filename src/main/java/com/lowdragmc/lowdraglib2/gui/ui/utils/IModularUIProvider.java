package com.lowdragmc.lowdraglib2.gui.ui.utils;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

import java.util.function.Function;

@FunctionalInterface
public interface IModularUIProvider<T> extends Function<T, ModularUI> {
    ModularUI createModularUI(T value);

    @Override
    default ModularUI apply(T value) {
        return createModularUI(value);
    }
}
