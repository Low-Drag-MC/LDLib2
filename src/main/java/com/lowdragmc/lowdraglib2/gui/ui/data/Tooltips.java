package com.lowdragmc.lowdraglib2.gui.ui.data;

import com.mojang.serialization.Codec;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public record Tooltips(Component[] tooltips) {
    public static final Codec<Tooltips> CODEC = ComponentSerialization.CODEC.listOf().xmap(Tooltips::of, Tooltips::asList);

    static Tooltips EMPTY = new Tooltips(new Component[0]);

    public static Tooltips of(String... tooltips) {
        return new Tooltips(Arrays.stream(tooltips).map(Component::translatable).toArray(Component[]::new));
    }

    public static Tooltips of(Component... tooltips) {
        return new Tooltips(tooltips);
    }

    public static Tooltips of(List<Component> tooltips) {
        return new Tooltips(tooltips.toArray(new Component[0]));
    }

    public static Tooltips empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return tooltips.length == 0;
    }

    public Tooltips merge(Tooltips... tooltips) {
        if (tooltips.length == 0) return this;
        return new Tooltips(Stream.concat(Stream.of(this.tooltips),
                Stream.of(tooltips).flatMap(t -> Arrays.stream(t.tooltips()))).toArray(Component[]::new));
    }

    public Tooltips append(Component... tooltips) {
        if (tooltips.length == 0) return this;
        return new Tooltips(Stream.concat(Stream.of(this.tooltips), Stream.of(tooltips)).toArray(Component[]::new));
    }

    public List<Component> asList() {
        return List.of(tooltips);
    }
}
