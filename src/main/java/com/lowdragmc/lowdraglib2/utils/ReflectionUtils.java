package com.lowdragmc.lowdraglib2.utils;

import com.lowdragmc.lowdraglib2.LDLib2;
import lombok.experimental.UtilityClass;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.HashMap;
import java.util.List;

@UtilityClass
public final class ReflectionUtils {

    public record EnumHolder(String desc, String value) {}

    public static Class<?> getRawType(Type type, Class<?> fallback) {
        var rawType = getRawType(type);
        return rawType != null ? rawType : fallback;
    }

    public static Class<?> getRawType(Type type) {
        if (type instanceof Class<?> aClass) return aClass;
        if (type instanceof GenericArrayType genericArrayType) return getRawType(genericArrayType.getGenericComponentType());
        if (type instanceof ParameterizedType parameterizedType) return getRawType(parameterizedType.getRawType());
        return null;
    }

    public static <A extends Annotation> void findAnnotationClasses(Class<A> annotationClass,
                                                                    @Nullable Predicate<Map<String, Object>> annotationPredicate,
                                                                    Consumer<Class<?>> consumer,
                                                                    Runnable onFinished) {
        String annotationDescriptor = org.objectweb.asm.Type.getDescriptor(annotationClass);
        LDLib2.LOGGER.info("Scanning for annotation: {}", annotationClass.getName());
        boolean foundPhoton = false;
        
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            String modId = mod.getMetadata().getId();
            if (modId.equals("photon")) foundPhoton = true;
            if (modId.startsWith("java") || modId.equals("minecraft")) continue;

            LDLib2.LOGGER.info("Scanning mod: {}", modId);
            
            // Collect all potential roots
            java.util.Set<Path> roots = new java.util.HashSet<>();
            mod.getRootPaths().forEach(roots::add);
            try {
                var origin = mod.getOrigin();
                if (origin.getKind() != net.fabricmc.loader.api.metadata.ModOrigin.Kind.NESTED) {
                    roots.addAll(origin.getPaths());
                }
            } catch (Exception ignored) {}

            for (Path root : roots) {
                if (!Files.exists(root)) continue;
                LDLib2.LOGGER.info("Scanning path: {} for mod {}", root, modId);
                scanPathRecursively(root, annotationDescriptor, annotationPredicate, modId, consumer);

                // IDE/Gradle Fallback: If we only found resources, look for classes nearby
                String pathStr = root.toString().replace('\\', '/');
                if (pathStr.endsWith("/build/resources/main")) {
                    Path classesRoot = Path.of(pathStr.replace("/build/resources/main", "/build/classes/java/main"));
                    if (Files.exists(classesRoot)) {
                        LDLib2.LOGGER.info("Detected Gradle dev classes: {}. Scanning...", classesRoot);
                        scanPathRecursively(classesRoot, annotationDescriptor, annotationPredicate, modId, consumer);
                    }
                    Path classesRootKotlin = Path.of(pathStr.replace("/build/resources/main", "/build/classes/kotlin/main"));
                    if (Files.exists(classesRootKotlin)) {
                        LDLib2.LOGGER.info("Detected Gradle Kotlin dev classes: {}. Scanning...", classesRootKotlin);
                        scanPathRecursively(classesRootKotlin, annotationDescriptor, annotationPredicate, modId, consumer);
                    }
                }
            }
        }
        
        // Final fallback: scan the whole classpath if we are desperate (only if mod photon still not found or found but 0 classes)
        if (!foundPhoton) {
            LDLib2.LOGGER.error("CRITICAL: Mod 'photon' not found in FabricLoader!");
        }
        onFinished.run();
    }

    private static void scanPathRecursively(Path root, String annotationDescriptor, @Nullable Predicate<Map<String, Object>> annotationPredicate, String modId, Consumer<Class<?>> consumer) {
        try (var stream = Files.walk(root)) {
            stream.filter(path -> path.toString().endsWith(".class")).forEach(path -> {
                scanClassPath(path, annotationDescriptor, annotationPredicate, modId, consumer);
            });
        } catch (IOException e) {
            // Ignore
        }
    }

    private static void scanClassPath(Path path, String annotationDescriptor, @Nullable Predicate<Map<String, Object>> annotationPredicate, String modId, Consumer<Class<?>> consumer) {
        try (InputStream is = Files.newInputStream(path)) {
            ClassReader reader = new ClassReader(is);
            ClassNode node = new ClassNode();
            reader.accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            if (node.visibleAnnotations != null) {
                for (AnnotationNode annotation : node.visibleAnnotations) {
                    if (annotation.desc.equals(annotationDescriptor)) {
                        Map<String, Object> data = parseAnnotationData(annotation);
                        if (annotationPredicate == null || annotationPredicate.test(data)) {
                            String className = node.name.replace('/', '.');
                            try {
                                Class<?> clazz = null;
                                try {
                                    clazz = Class.forName(className, false, ReflectionUtils.class.getClassLoader());
                                } catch (ClassNotFoundException e) {
                                    clazz = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
                                }
                                if (clazz != null) {
                                    consumer.accept(clazz);
                                    LDLib2.LOGGER.info("Found annotated class: {} in mod {}", className, modId);
                                }
                            } catch (ClassNotFoundException e) {
                                LDLib2.LOGGER.error("Failed to load class: {} from mod {}", className, modId);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            // Ignore
        }
    }

    private static Map<String, Object> parseAnnotationData(AnnotationNode node) {
        Map<String, Object> data = new HashMap<>();
        if (node.values != null) {
            for (int i = 0; i < node.values.size(); i += 2) {
                Object value = node.values.get(i + 1);
                if (value instanceof String[] enumData) {
                    value = new EnumHolder(enumData[0], enumData[1]);
                }
                data.put((String) node.values.get(i), value);
            }
        }
        return data;
    }

    public static <A extends Annotation> void findAnnotationStaticField(Class<A> annotationClass,
                                                                        @Nullable Predicate<Map<String, Object>> annotationPredicate,
                                                                        BiConsumer<Field, Object> consumer,
                                                                        Runnable onFinished) {
        String annotationDescriptor = org.objectweb.asm.Type.getDescriptor(annotationClass);
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
             String modId = mod.getMetadata().getId();
             if (modId.startsWith("java") || modId.equals("minecraft")) continue;

             java.util.Set<Path> roots = new java.util.HashSet<>();
             mod.getRootPaths().forEach(roots::add);
             try {
                 var origin = mod.getOrigin();
                 if (origin.getKind() != net.fabricmc.loader.api.metadata.ModOrigin.Kind.NESTED) {
                     roots.addAll(origin.getPaths());
                 }
             } catch (Exception ignored) {}

             for (Path root : roots) {
                if (!Files.exists(root)) continue;
                scanPathStaticFieldRecursively(root, annotationDescriptor, annotationPredicate, modId, consumer);

                // IDE/Gradle Fallback
                String pathStr = root.toString().replace('\\', '/');
                if (pathStr.endsWith("/build/resources/main")) {
                    Path classesRoot = Path.of(pathStr.replace("/build/resources/main", "/build/classes/java/main"));
                    if (Files.exists(classesRoot)) {
                        scanPathStaticFieldRecursively(classesRoot, annotationDescriptor, annotationPredicate, modId, consumer);
                    }
                    Path classesRootKotlin = Path.of(pathStr.replace("/build/resources/main", "/build/classes/kotlin/main"));
                    if (Files.exists(classesRootKotlin)) {
                        scanPathStaticFieldRecursively(classesRootKotlin, annotationDescriptor, annotationPredicate, modId, consumer);
                    }
                }
            }
        }
        onFinished.run();
    }

    private static void scanPathStaticFieldRecursively(Path root, String annotationDescriptor, @Nullable Predicate<Map<String, Object>> annotationPredicate, String modId, BiConsumer<Field, Object> consumer) {
        try (var stream = Files.walk(root)) {
            stream.filter(path -> path.toString().endsWith(".class")).forEach(path -> {
                scanStaticField(path, annotationDescriptor, annotationPredicate, modId, consumer);
            });
        } catch (IOException e) {
            // Ignore
        }
    }

    private static void scanStaticField(Path path, String annotationDescriptor, @Nullable Predicate<Map<String, Object>> annotationPredicate, String modId, BiConsumer<Field, Object> consumer) {
        try (InputStream is = Files.newInputStream(path)) {
            ClassReader reader = new ClassReader(is);
            ClassNode node = new ClassNode();
            reader.accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            if (node.fields != null) {
                for (FieldNode fieldNode : node.fields) {
                    if (fieldNode.visibleAnnotations != null) {
                        for (AnnotationNode annotation : fieldNode.visibleAnnotations) {
                            if (annotation.desc.equals(annotationDescriptor)) {
                                Map<String, Object> data = parseAnnotationData(annotation);
                                if (annotationPredicate == null || annotationPredicate.test(data)) {
                                    String className = node.name.replace('/', '.');
                                    loadAndAcceptField(className, fieldNode.name, consumer);
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            // Ignore
        }
    }

    private static void loadAndAcceptField(String className, String fieldName, BiConsumer<Field, Object> consumer) {
        try {
            Class<?> clazz = null;
            try {
                clazz = Class.forName(className, false, ReflectionUtils.class.getClassLoader());
            } catch (ClassNotFoundException e) {
                clazz = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            }
            if (clazz != null) {
                Field field = clazz.getDeclaredField(fieldName);
                if (Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    consumer.accept(field, field.get(null));
                }
            }
        } catch (Exception e) {
            LDLib2.LOGGER.error("Failed to load field: {} in {}", fieldName, className, e);
        }
    }

    public static <A extends Annotation> void findAnnotationStaticMethod(Class<A> annotationClass,
                                                                         @Nullable Predicate<Map<String, Object>> annotationPredicate,
                                                                         Consumer<Method> consumer,
                                                                         Runnable onFinished) {
        String annotationDescriptor = org.objectweb.asm.Type.getDescriptor(annotationClass);
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
             String modId = mod.getMetadata().getId();
             if (modId.startsWith("java") || modId.equals("minecraft")) continue;

             java.util.Set<Path> roots = new java.util.HashSet<>();
             mod.getRootPaths().forEach(roots::add);
             try {
                 var origin = mod.getOrigin();
                 if (origin.getKind() != net.fabricmc.loader.api.metadata.ModOrigin.Kind.NESTED) {
                     roots.addAll(origin.getPaths());
                 }
             } catch (Exception ignored) {}

             for (Path root : roots) {
                if (!Files.exists(root)) continue;
                scanPathStaticMethodRecursively(root, annotationDescriptor, annotationPredicate, modId, consumer);

                // IDE/Gradle Fallback
                String pathStr = root.toString().replace('\\', '/');
                if (pathStr.endsWith("/build/resources/main")) {
                    Path classesRoot = Path.of(pathStr.replace("/build/resources/main", "/build/classes/java/main"));
                    if (Files.exists(classesRoot)) {
                        scanPathStaticMethodRecursively(classesRoot, annotationDescriptor, annotationPredicate, modId, consumer);
                    }
                    Path classesRootKotlin = Path.of(pathStr.replace("/build/resources/main", "/build/classes/kotlin/main"));
                    if (Files.exists(classesRootKotlin)) {
                        scanPathStaticMethodRecursively(classesRootKotlin, annotationDescriptor, annotationPredicate, modId, consumer);
                    }
                }
            }
        }
        onFinished.run();
    }

    private static void scanPathStaticMethodRecursively(Path root, String annotationDescriptor, @Nullable Predicate<Map<String, Object>> annotationPredicate, String modId, Consumer<Method> consumer) {
        try (var stream = Files.walk(root)) {
            stream.filter(path -> path.toString().endsWith(".class")).forEach(path -> {
                scanStaticMethod(path, annotationDescriptor, annotationPredicate, modId, consumer);
            });
        } catch (IOException e) {
            // Ignore
        }
    }

    private static void scanStaticMethod(Path path, String annotationDescriptor, @Nullable Predicate<Map<String, Object>> annotationPredicate, String modId, Consumer<Method> consumer) {
        try (InputStream is = Files.newInputStream(path)) {
            ClassReader reader = new ClassReader(is);
            ClassNode node = new ClassNode();
            reader.accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            if (node.methods != null) {
                for (MethodNode methodNode : node.methods) {
                    if (methodNode.visibleAnnotations != null) {
                        for (AnnotationNode annotation : methodNode.visibleAnnotations) {
                            if (annotation.desc.equals(annotationDescriptor)) {
                                Map<String, Object> data = parseAnnotationData(annotation);
                                if (annotationPredicate == null || annotationPredicate.test(data)) {
                                    String className = node.name.replace('/', '.');
                                    loadAndAcceptMethod(className, methodNode.name, methodNode.desc, consumer);
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            // Ignore
        }
    }

    private static void loadAndAcceptMethod(String className, String methodName, String methodDesc, Consumer<Method> consumer) {
        try {
            Class<?> clazz = null;
            try {
                clazz = Class.forName(className, false, ReflectionUtils.class.getClassLoader());
            } catch (ClassNotFoundException e) {
                clazz = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            }
            if (clazz != null) {
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.getName().equals(methodName) && 
                        org.objectweb.asm.Type.getMethodDescriptor(method).equals(methodDesc)) {
                        if (Modifier.isStatic(method.getModifiers())) {
                            method.setAccessible(true);
                            consumer.accept(method);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LDLib2.LOGGER.error("Failed to load method: {} in {}", methodName, className, e);
        }
    }
}
