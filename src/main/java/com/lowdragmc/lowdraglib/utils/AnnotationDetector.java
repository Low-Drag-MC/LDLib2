package com.lowdragmc.lowdraglib.utils;

import com.google.common.base.Predicates;
import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.client.renderer.ISerializableRenderer;
import com.lowdragmc.lowdraglib.editor.accessors.IConfiguratorAccessor;
import com.lowdragmc.lowdraglib.editor.annotation.ConfigAccessor;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.editor.data.IProject;
import com.lowdragmc.lowdraglib.editor.data.resource.Resource;
import com.lowdragmc.lowdraglib.editor.ui.menu.MenuTab;
import com.lowdragmc.lowdraglib.editor.ui.view.FloatViewWidget;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib.test.ui.IUITest;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * @author KilaBash
 * @date 2022/12/3
 * @implNote AnnotationDetector
 */
@SuppressWarnings("unchecked")
public class AnnotationDetector {

    public record Wrapper<A extends Annotation, T>(A annotation, Class<? extends T> clazz, Supplier<T> creator) { }

    public static final List<Wrapper<LDLRegister, Resource>> REGISTER_RESOURCES = scanClasses(LDLRegister.class, Resource.class, AnnotationDetector::checkNoArgsConstructor, AnnotationDetector::toUINoArgsBuilder, AnnotationDetector::UIWrapperSorter, l -> {});
    public static final List<Wrapper<LDLRegister, IConfigurableWidget>> REGISTER_WIDGETS = scanClasses(LDLRegister.class, IConfigurableWidget.class, AnnotationDetector::checkNoArgsConstructor, AnnotationDetector::toUINoArgsBuilder, AnnotationDetector::UIWrapperSorter, l -> {});
    public static final List<Wrapper<LDLRegister, FloatViewWidget>> REGISTER_FLOAT_VIEWS = scanClasses(LDLRegister.class, FloatViewWidget.class, AnnotationDetector::checkNoArgsConstructor, AnnotationDetector::toUINoArgsBuilder, AnnotationDetector::UIWrapperSorter, l -> {});
    public static final List<Wrapper<LDLRegister, MenuTab>> REGISTER_MENU_TABS = scanClasses(LDLRegister.class, MenuTab.class, AnnotationDetector::checkNoArgsConstructor, AnnotationDetector::toUINoArgsBuilder, AnnotationDetector::UIWrapperSorter, l -> {});
    public static final List<Wrapper<LDLRegister, IProject>> REGISTER_PROJECTS = scanClasses(LDLRegister.class, IProject.class, AnnotationDetector::checkNoArgsConstructor, AnnotationDetector::toUINoArgsBuilder, AnnotationDetector::UIWrapperSorter, l -> {});
    public static final Map<String, Wrapper<LDLRegisterClient, ? extends ISerializableRenderer>> REGISTER_RENDERERS = new HashMap<>();
    public static final List<Wrapper<LDLRegisterClient, IUITest>> REGISTER_UI_TESTS = new ArrayList<>();
    public static final List<TypeAdapter.ITypeAdapter> REGISTER_TYPE_ADAPTERS = scanClasses(LDLRegister.class, TypeAdapter.ITypeAdapter.class, AnnotationDetector::checkNoArgsConstructor, AnnotationDetector::createNoArgsInstance, (a, b) -> 0, l -> {});
    public static final Map<String, Wrapper<LDLRegister, ? extends BaseNode>> REGISTER_GP_NODES = new HashMap<>();

    public static void init() {
        if (LDLib.isClient()) {
            AnnotationDetector.scanClasses(
                    LDLRegisterClient.class,
                    ISerializableRenderer.class,
                    AnnotationDetector::checkNoArgsConstructor,
                    AnnotationDetector::toClientUINoArgsBuilder,
                    AnnotationDetector::clientUIWrapperSorter, l -> REGISTER_RENDERERS.putAll(l.stream().collect(Collectors.toMap(w -> w.annotation().name(), w -> w))));
            if (Platform.isDevEnv()) {
                AnnotationDetector.scanClasses(
                        LDLRegisterClient.class,
                        IUITest.class,
                        AnnotationDetector::checkNoArgsConstructor,
                        AnnotationDetector::toClientUINoArgsBuilder,
                        AnnotationDetector::clientUIWrapperSorter,
                        l -> {
                            for (Wrapper<LDLRegisterClient, ? extends IUITest> wrapper : l) {
                                REGISTER_UI_TESTS.add((Wrapper<LDLRegisterClient, IUITest>) wrapper)
                            }
                        });
            }
        }
        AnnotationDetector.scanClasses(
                LDLRegister.class,
                BaseNode.class,
                AnnotationDetector::checkNoArgsConstructor,
                AnnotationDetector::toUINoArgsBuilder,
                AnnotationDetector::UIWrapperSorter,
                l -> REGISTER_GP_NODES.putAll(l.stream().collect(Collectors.toMap(w -> w.annotation().name(), w -> w))));
    }

    public static <A extends Annotation, T, C> void scanClasses(
            Class<A> annotationClass,
            @Nullable Class<T> baseClazz,
            @Nullable Predicate<Map<String, Object>> annotationPredicate,
            @Nullable BiPredicate<A, Class<? extends T>> predicate,
            Function<Class<? extends T>, C> mapping,
            TriConsumer<A, Class<? extends T>, C> consumer) {
        ArrayList<C> result = new ArrayList<>();
        ReflectionUtils.findAnnotationClasses(annotationClass,
                annotationPredicate == null ? Predicates.alwaysTrue() : annotationPredicate,
                clazz -> {
                    if (baseClazz == null || baseClazz.isAssignableFrom(clazz)) {
                        try {
                            Class<? extends T> realClass =  (Class<? extends T>) clazz;
                            if (predicate == null || predicate.test(clazz.getAnnotation(annotationClass), realClass)) {
                                result.add(mapping.apply(realClass));
                            }
                        } catch (Throwable e) {
                            LDLib.LOGGER.error("failed to scan annotation {} + base class {} while handling class {} ", annotationClass, baseClazz, clazz, e);
                        }
                    }
                }, () -> {
                    if (sorter != null) result.sort(sorter);
                    if (onFinished != null) onFinished.accept(result);
                });
        return result;
    }

    public static <A, T> boolean checkNoArgsConstructor(A annotation, Class<? extends T> clazz) {
        if (annotation instanceof LDLRegister LDLRegister) {
            if (!LDLRegister.modID().isEmpty() && !LDLib.isModLoaded(LDLRegister.modID())) {
                return false;
            }
        } else if (annotation instanceof LDLRegisterClient LDLRegisterClient) {
            if (!LDLRegisterClient.modID().isEmpty() && !LDLib.isModLoaded(LDLRegisterClient.modID())) {
                return false;
            }
        }
        try {
            clazz.getDeclaredConstructor();
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public static <T> T createNoArgsInstance(Class<? extends T> clazz) {
        try {
            var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> Wrapper<LDLRegister, T> toUINoArgsBuilder(Class<? extends T> clazz) {
        return new Wrapper<>(clazz.getAnnotation(LDLRegister.class), clazz, () -> createNoArgsInstance(clazz));
    }

    public static int UIWrapperSorter(Wrapper<LDLRegister, ?> a, Wrapper<LDLRegister, ?> b) {
        return b.annotation.priority() - a.annotation.priority();
    }

    public static <T> AnnotationDetector.Wrapper<LDLRegisterClient, T> toClientUINoArgsBuilder(Class<? extends T> clazz) {
        return new AnnotationDetector.Wrapper<>(clazz.getAnnotation(LDLRegisterClient.class), clazz, () -> AnnotationDetector.createNoArgsInstance(clazz));
    }

    public static int clientUIWrapperSorter(AnnotationDetector.Wrapper<LDLRegisterClient, ?> a, AnnotationDetector.Wrapper<LDLRegisterClient, ?> b) {
        return b.annotation().priority() - a.annotation().priority();
    }

}
