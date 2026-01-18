package com.lowdragmc.lowdraglib2.syncdata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field should be persisted for persistence.
 * <pre>{@code
 * @Persisted(key = "fluidAmount")
 * int value = 100;
 * @Persisted
 * boolean isWater = true;
 * }
 * </pre>
 * The nbt/json looks as below:
 * <pre>{@code
 * {
 *   "fluidAmount": 100,
 *   "isWater": true
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Persisted {
    /**
     * The key of the field in the persistence.
     * @return The key of the field in the persistence.
     */
    String key() default "";

    /**
     * If true, it will wrap the field's internal value based on its {@code non-null} instance.
     * It is very useful for `final` instance which doesn't allow new instance creation. If the filed set `subPersisted = true`, Ldlib2 will do:
     * <li> if the field inherits from {@link  net.neoforged.neoforge.common.util.INBTSerializable}, it will try to use its api for serialization. </li>
     * <li> otherwise, it will serialize the field's internal values and wrap it as a map. </li>
     *
     * <pre>{@code
     * @Persisted(subPersisted = true)
     * private final INBTSerializable<CompoundTag> stackHandler = new ItemStackHandler(5);
     * @Persisted(subPersisted = true)
     * private final TestContainer testContainer = new TestContainer();
     *
     * public static class TestContainer {
     *     @Persisted
     *     private Vector3f vector3fValue = new Vector3f(0, 0, 0);
     *     @Persisted
     *     private int[] intArray = new int[]{1, 2, 3};
     * }
     * }</pre>
     * The nbt/json looks as below:
     * <pre>{@code
     * {
     *     "stackHandler": {
     *         "Size": 5,
     *         "Items": [],
     *     },
     *     "testContainer": {
     *         "vector3fValue": [0, 0, 0],
     *         "intArray": [1, 2, 3],
     *     }
     * }
     * }</pre>
     */
    boolean subPersisted() default false;

    /**
     * Determines whether the persisted internal structure should be flattened in serialization.
     * If set to {@code true}, nested data structures will be flattened into the parent structure.
     * For example, inner object fields may no longer be encapsulated within a nested map or tag.
     *
     * @return {@code true} if the persisted structure should be flattened; {@code false} otherwise.
     */
    boolean subFlattenPersisted() default false;
}
