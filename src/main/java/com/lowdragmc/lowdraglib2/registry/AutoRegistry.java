package com.lowdragmc.lowdraglib2.registry;

import com.google.common.base.Predicates;
import com.google.common.util.concurrent.Runnables;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.utils.ReflectionUtils;
import net.minecraft.resources.ResourceLocation;
import com.lowdragmc.lowdraglib2.registry.annotation.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * AutoRegistry is used to register objects automatically by detecting the class with the given annotation {@link LDLRegister} and {@link LDLRegisterClient}.
 */
public class AutoRegistry<A extends Annotation, C, V> extends LDLRegistry.String<AutoRegistry.Holder<A, C, V>> {
    public record Holder<A extends Annotation, C, V>(A annotation, Class<? extends C> clazz, V value) {
        public static <A extends Annotation, C, V> Holder<A, C, V> of(A annotation, Class<? extends C> clazz, V value) {
            return new Holder<>(annotation, clazz, value);
        }
    }

    private final Class<A> annotationClass;
    private final Class<C> baseClazz;
    @Nullable
    private final Predicate<Map<java.lang.String, Object>> annotationFilter;
    @Nullable
    private final Predicate<Class<? extends C>> classFilter;
    private final BiFunction<A, Class<? extends C>, java.lang.String> keyFactory;
    private final BiFunction<A, Class<? extends C>, V> supplier;
    @Nullable
    private final Comparator<AutoRegistry.Holder<A, C, V>> sorter;


    protected AutoRegistry(ResourceLocation registryName,
                         Class<A> annotationClass,
                         Class<C> baseClazz,
                         @Nullable Predicate<Map<java.lang.String, Object>> annotationFilter,
                         @Nullable Predicate<Class<? extends C>> classFilter,
                         BiFunction<A, Class<? extends C>, java.lang.String> keyFactory,
                         BiFunction<A, Class<? extends C>, V> supplier,
                         @Nullable Comparator<Holder<A, C, V>> sorter) {
        super(registryName);
        this.annotationClass = annotationClass;
        this.baseClazz = baseClazz;
        this.annotationFilter = annotationFilter;
        this.classFilter = classFilter;
        this.supplier = supplier;
        this.keyFactory = keyFactory;
        this.sorter = sorter;
        autoRegister();
    }

    public static <A extends Annotation, C, V> AutoRegistry<A, C, V> create(ResourceLocation registryName,
                                                                            Class<A> annotationClass,
                                                                            Class<C> baseClazz,
                                                                            @Nullable Predicate<Map<java.lang.String, Object>> annotationFilter,
                                                                            @Nullable Predicate<Class<? extends C>> classFilter,
                                                                            BiFunction<A, Class<? extends C>, java.lang.String> keyFactory,
                                                                            BiFunction<A, Class<? extends C>, V> supplier,
                                                                            @Nullable Comparator<Holder<A, C, V>> sorter) {
        return new AutoRegistry<>(registryName, annotationClass, baseClazz, annotationFilter, classFilter, keyFactory, supplier, sorter);
    }

    public AutoRegistry<A, C, V> autoRegister() {
        var state = isFrozen();
        if (state) unfreeze();
        ReflectionUtils.findAnnotationClasses(annotationClass, annotationFilter == null ? Predicates.alwaysTrue() : annotationFilter,
                clazz -> {
                    if (baseClazz.isAssignableFrom(clazz)) {
                        try {
                            Class<? extends C> realClass =  (Class<? extends C>) clazz;
                            if (classFilter == null || classFilter.test(realClass)) {
                                var annotation = clazz.getAnnotation(annotationClass);
                                var key = keyFactory.apply(annotation, realClass);
                                register(key, new Holder<>(annotation, realClass, supplier.apply(annotation, realClass)));
                            }
                        } catch (Throwable e) {
                            LDLib2.LOGGER.error("failed to scan annotation {} + base class {} while handling class {} ", annotationClass, baseClazz, clazz, e);
                        }
                    }
                }, Runnables.doNothing());
        if (state) freeze();
        return this;
    }

    public static <T> T noArgsInstance(Annotation annotation, Class<? extends T> clazz) {
        try {
            var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> Supplier<T> noArgsCreator(Annotation annotation, Class<? extends T> clazz) {
        try {
            var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return () ->  {
                try {
                    return constructor.newInstance();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static <A1, T> Function<A1, T> oneArgCreator(Class<? extends T> clazz, Class<A1> argType) {
        try {
            var constructor = clazz.getDeclaredConstructor(argType);
            constructor.setAccessible(true);
            return (a) ->  {
                try {
                    return constructor.newInstance(a);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull Iterator<Holder<A, C, V>> iterator() {
        if (sorter == null) return super.iterator();
        return registry.values().stream().sorted(sorter).iterator();
    }

    public static final class LDLibRegister<C extends ILDLRegister, V> extends AutoRegistry<LDLRegister, C, V> {
        private LDLibRegister(ResourceLocation registryName,
                              Class<C> baseClazz,
                              BiFunction<LDLRegister, Class<? extends C>, V> supplier) {
            super(registryName, LDLRegister.class, baseClazz, annotationData -> {
                if (annotationData.containsKey("modID") && annotationData.get("modID") instanceof java.lang.String modID) {
                    return modID.isEmpty() || Platform.isModLoaded(modID);
                } else return !annotationData.containsKey("manual") || !(annotationData.get("manual") instanceof Boolean manual) || !manual;
            }, null, (annotation, clazz) -> annotation.name(), supplier, (a, b) -> b.annotation().priority() - a.annotation().priority());
        }

        public static <C extends ILDLRegister, V> LDLibRegister<C, V> create(ResourceLocation registryName, Class<C> baseClazz, BiFunction<LDLRegister, Class<? extends C>, V> supplier) {
            return new LDLibRegister<>(registryName, baseClazz, supplier);
        }

        @Override
        public LDLibRegister<C, V> autoRegister() {
            super.autoRegister();
            return this;
        }
    }

    public static final class LDLibRegisterClient<C extends ILDLRegisterClient, V> extends AutoRegistry<LDLRegisterClient, C, V> {
        private LDLibRegisterClient(ResourceLocation registryName,
                              Class<C> baseClazz,
                              BiFunction<LDLRegisterClient, Class<? extends C>, V> supplier) {
            super(registryName, LDLRegisterClient.class, baseClazz, annotationData -> {
                if (annotationData.containsKey("modID") && annotationData.get("modID") instanceof java.lang.String modID) {
                    return modID.isEmpty() || Platform.isModLoaded(modID);
                } else return !annotationData.containsKey("manual") || !(annotationData.get("manual") instanceof Boolean manual) || !manual;
            }, null, (annotation, clazz) -> annotation.name(), supplier, (a, b) -> b.annotation().priority() - a.annotation().priority());
        }

        public static <C extends ILDLRegisterClient, V> LDLibRegisterClient<C, V> create(ResourceLocation registryName, Class<C> baseClazz, BiFunction<LDLRegisterClient, Class<? extends C>, V> supplier) {
            return new LDLibRegisterClient<>(registryName, baseClazz, supplier);
        }

        @Override
        public LDLibRegisterClient<C, V> autoRegister() {
            if (LDLib2.isClient()) {
                super.autoRegister();
            }
            return this;
        }
    }
}
