package com.lowdragmc.lowdraglib2.gui.sync.rpc;

import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.syncdata.SyncValueHolder;
import com.lowdragmc.lowdraglib2.utils.consumer.*;
import com.mojang.datafixers.util.*;
import org.apache.logging.log4j.util.TriConsumer;

import org.jetbrains.annotations.Nullable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.*;

@KJSBindings
public class RPCEventBuilder {
    private final List<Type> args = new ArrayList<>();
    private final List<Object> initialArgs = new ArrayList<>();
    @Nullable
    private Type returnType;
    @Nullable
    private Object initialReturnValue;
    private Function<Object[], Object> executor = args -> null;

    protected RPCEventBuilder() {

    }

    public static RPCEventBuilder create() {
        return new RPCEventBuilder();
    }

    public RPCEventBuilder arg(Type arg, Object initialValue) {
        args.add(arg);
        initialArgs.add(initialValue);
        return this;
    }

    public RPCEventBuilder args(Type... args) {
        for (Type arg : args) {
            arg(arg, null);
        }
        return this;
    }

    public RPCEventBuilder returnType(Type returnType, Object initialValue) {
        this.returnType = returnType;
        this.initialReturnValue = initialValue;
        return this;
    }

    public RPCEventBuilder returnType(Type returnType) {
        return returnType(returnType, null);
    }

    public RPCEventBuilder executor(Function<Object[], Object> executor) {
        this.executor = executor;
        return this;
    }

    public RPCEvent build() {
        var syncArgs = new SyncValueHolder[args.size()];
        for (int i = 0; i < args.size(); i++) {
            syncArgs[i] = new SyncValueHolder("arg" + i, args.get(i), initialArgs.get(i));
        }
        var syncReturn = returnType != null ? new SyncValueHolder("return", returnType, initialReturnValue) : null;
        return new RPCEvent(syncArgs, syncReturn, executor);
    }

    public static RPCEvent simple(Runnable executor) {
        return create().executor(args -> {
            executor.run();
            return null;
        }).build();
    }

    public static <R> RPCEvent simple(Class<R> returnType, Supplier<R> executor) {
        return create().returnType(returnType).executor(args -> executor.get()).build();
    }

    public static <A1> RPCEvent simple(Class<A1> a1, Consumer<A1> executor) {
        return create().args(a1).executor(args -> {
            executor.accept((A1) args[0]);
            return null;
        }).build();
    }

    public static <A1, R> RPCEvent simple(Class<A1> a1, Class<R> returnType, Function<A1, R> executor) {
        return create().args(a1).returnType(returnType)
                .executor(args -> executor.apply((A1) args[0])).build();
    }

    public static <A1, A2> RPCEvent simple(Class<A1> a1, Class<A2> a2, BiConsumer<A1, A2> executor) {
        return create().args(a1, a2).executor(args -> {
            executor.accept((A1) args[0], (A2) args[1]);
            return null;
        }).build();
    }

    public static <A1, A2, R> RPCEvent simple(Class<A1> a1, Class<A2> a2, Class<R> returnType, BiFunction<A1, A2, R> executor) {
        return create().args(a1, a2).returnType(returnType)
                .executor(args -> executor.apply((A1) args[0], (A2) args[1])).build();
    }

    public static <A1, A2, A3> RPCEvent simple(Class<A1> a1, Class<A2> a2, Class<A3> a3, TriConsumer<A1, A2, A3> executor) {
        return create().args(a1, a2, a3).executor(args -> {
            executor.accept((A1) args[0], (A2) args[1], (A3) args[2]);
            return null;
        }).build();
    }

    public static <A1, A2, A3, R> RPCEvent simple(Class<A1> a1, Class<A2> a2, Class<A3> a3, Class<R> returnType, Function3<A1, A2, A3, R> executor) {
        return create().args(a1, a2, a3).returnType(returnType)
                .executor(args -> executor.apply((A1) args[0], (A2) args[1], (A3) args[2])).build();
    }

    public static <A1, A2, A3, A4> RPCEvent simple(Class<A1> a1, Class<A2> a2, Class<A3> a3, Class<A4> a4, Consumer4<A1, A2, A3, A4> executor) {
        return create().args(a1, a2, a3, a4).executor(args -> {
            executor.accept((A1) args[0], (A2) args[1], (A3) args[2], (A4) args[3]);
            return null;
        }).build();
    }

    public static <A1, A2, A3, A4, R> RPCEvent simple(Class<A1> a1, Class<A2> a2, Class<A3> a3, Class<A4> a4, Class<R> returnType, Function4<A1, A2, A3, A4, R> executor) {
        return create().args(a1, a2, a3, a4).returnType(returnType)
                .executor(args -> executor.apply((A1) args[0], (A2) args[1], (A3) args[2], (A4) args[3])).build();
    }

    public static <A1, A2, A3, A4, A5> RPCEvent simple(Class<A1> a1, Class<A2> a2, Class<A3> a3, Class<A4> a4, Class<A5> a5, Consumer5<A1, A2, A3, A4, A5> executor) {
        return create().args(a1, a2, a3, a4, a5).executor(args -> {
            executor.accept((A1) args[0], (A2) args[1], (A3) args[2], (A4) args[3], (A5) args[4]);
            return null;
        }).build();
    }

    public static <A1, A2, A3, A4, A5, R> RPCEvent simple(Class<A1> a1, Class<A2> a2, Class<A3> a3, Class<A4> a4, Class<A5> a5, Class<R> returnType, Function5<A1, A2, A3, A4, A5, R> executor) {
        return create().args(a1, a2, a3, a4, a5).returnType(returnType)
                .executor(args -> executor.apply((A1) args[0], (A2) args[1], (A3) args[2], (A4) args[3], (A5) args[4])).build();
    }

    public static <A1, A2, A3, A4, A5, A6> RPCEvent simple(Class<A1> a1, Class<A2> a2, Class<A3> a3, Class<A4> a4, Class<A5> a5, Class<A6> a6, Consumer6<A1, A2, A3, A4, A5, A6> executor) {
        return create().args(a1, a2, a3, a4, a5, a6).executor(args -> {
            executor.accept((A1) args[0], (A2) args[1], (A3) args[2], (A4) args[3], (A5) args[4], (A6) args[5]);
            return null;
        }).build();
    }

    public static <A1, A2, A3, A4, A5, A6, R> RPCEvent simple(Class<A1> a1, Class<A2> a2, Class<A3> a3, Class<A4> a4, Class<A5> a5, Class<A6> a6, Class<R> returnType, Function6<A1, A2, A3, A4, A5, A6, R> executor) {
        return create().args(a1, a2, a3, a4, a5, a6).returnType(returnType)
                .executor(args -> executor.apply((A1) args[0], (A2) args[1], (A3) args[2], (A4) args[3], (A5) args[4], (A6) args[5])).build();
    }

    public static <A1, A2, A3, A4, A5, A6, A7> RPCEvent simple(Class<A1> a1, Class<A2> a2, Class<A3> a3, Class<A4> a4, Class<A5> a5, Class<A6> a6, Class<A7> a7, Consumer7<A1, A2, A3, A4, A5, A6, A7> executor) {
        return create().args(a1, a2, a3, a4, a5, a6, a7).executor(args -> {
            executor.accept((A1) args[0], (A2) args[1], (A3) args[2], (A4) args[3], (A5) args[4], (A6) args[5], (A7) args[6]);
            return null;
        }).build();
    }

    public static <A1, A2, A3, A4, A5, A6, A7, R> RPCEvent simple(Class<A1> a1, Class<A2> a2, Class<A3> a3, Class<A4> a4, Class<A5> a5, Class<A6> a6, Class<A7> a7, Class<R> returnType, Function7<A1, A2, A3, A4, A5, A6, A7, R> executor) {
        return create().args(a1, a2, a3, a4, a5, a6, a7).returnType(returnType)
                .executor(args -> executor.apply((A1) args[0], (A2) args[1], (A3) args[2], (A4) args[3], (A5) args[4], (A6) args[5], (A7) args[6])).build();
    }

    public static <A1, A2, A3, A4, A5, A6, A7, A8> RPCEvent simple(Class<A1> a1, Class<A2> a2, Class<A3> a3, Class<A4> a4, Class<A5> a5, Class<A6> a6, Class<A7> a7, Class<A8> a8, Consumer8<A1, A2, A3, A4, A5, A6, A7, A8> executor) {
        return create().args(a1, a2, a3, a4, a5, a6, a7, a8).executor(args -> {
            executor.accept((A1) args[0], (A2) args[1], (A3) args[2], (A4) args[3], (A5) args[4], (A6) args[5], (A7) args[6], (A8) args[7]);
            return null;
        }).build();
    }

    public static <A1, A2, A3, A4, A5, A6, A7, A8, R> RPCEvent simple(Class<A1> a1, Class<A2> a2, Class<A3> a3, Class<A4> a4, Class<A5> a5, Class<A6> a6, Class<A7> a7, Class<A8> a8, Class<R> returnType, Function8<A1, A2, A3, A4, A5, A6, A7, A8, R> executor) {
        return create().args(a1, a2, a3, a4, a5, a6, a7, a8).returnType(returnType)
                .executor(args -> executor.apply((A1) args[0], (A2) args[1], (A3) args[2], (A4) args[3], (A5) args[4], (A6) args[5], (A7) args[6], (A8) args[7])).build();
    }

    public static <A1, A2, A3, A4, A5, A6, A7, A8, A9> RPCEvent simple(Class<A1> a1, Class<A2> a2, Class<A3> a3, Class<A4> a4, Class<A5> a5, Class<A6> a6, Class<A7> a7, Class<A8> a8, Class<A9> a9, Consumer9<A1, A2, A3, A4, A5, A6, A7, A8, A9> executor) {
        return create().args(a1, a2, a3, a4, a5, a6, a7, a8, a9).executor(args -> {
            executor.accept((A1) args[0], (A2) args[1], (A3) args[2], (A4) args[3], (A5) args[4], (A6) args[5], (A7) args[6], (A8) args[7], (A9) args[8]);
            return null;
        }).build();
    }

    public static <A1, A2, A3, A4, A5, A6, A7, A8, A9, R> RPCEvent simple(Class<A1> a1, Class<A2> a2, Class<A3> a3, Class<A4> a4, Class<A5> a5, Class<A6> a6, Class<A7> a7, Class<A8> a8, Class<A9> a9, Class<R> returnType, Function9<A1, A2, A3, A4, A5, A6, A7, A8, A9, R> executor) {
        return create().args(a1, a2, a3, a4, a5, a6, a7, a8, a9).returnType(returnType)
                .executor(args -> executor.apply((A1) args[0], (A2) args[1], (A3) args[2], (A4) args[3], (A5) args[4], (A6) args[5], (A7) args[6], (A8) args[7], (A9) args[8])).build();
    }

    public static <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10> RPCEvent simple(Class<A1> a1, Class<A2> a2, Class<A3> a3, Class<A4> a4, Class<A5> a5, Class<A6> a6, Class<A7> a7, Class<A8> a8, Class<A9> a9, Class<A10> a10, Consumer10<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10> executor) {
        return create().args(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10).executor(args -> {
            executor.accept((A1) args[0], (A2) args[1], (A3) args[2], (A4) args[3], (A5) args[4], (A6) args[5], (A7) args[6], (A8) args[7], (A9) args[8], (A10) args[9]);
            return null;
        }).build();
    }

    public static <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R> RPCEvent simple(Class<A1> a1, Class<A2> a2, Class<A3> a3, Class<A4> a4, Class<A5> a5, Class<A6> a6, Class<A7> a7, Class<A8> a8, Class<A9> a9, Class<A10> a10, Class<R> returnType, Function10<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R> executor) {
        return create().args(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10).returnType(returnType)
                .executor(args -> executor.apply((A1) args[0], (A2) args[1], (A3) args[2], (A4) args[3], (A5) args[4], (A6) args[5], (A7) args[6], (A8) args[7], (A9) args[8], (A10) args[9])).build();
    }

    public static <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11> RPCEvent simple(Class<A1> a1, Class<A2> a2, Class<A3> a3, Class<A4> a4, Class<A5> a5, Class<A6> a6, Class<A7> a7, Class<A8> a8, Class<A9> a9, Class<A10> a10, Class<A11> a11, Consumer11<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11> executor) {
        return create().args(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11).executor(args -> {
            executor.accept((A1) args[0], (A2) args[1], (A3) args[2], (A4) args[3], (A5) args[4], (A6) args[5], (A7) args[6], (A8) args[7], (A9) args[8], (A10) args[9], (A11) args[10]);
            return null;
        }).build();
    }

    public static <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R> RPCEvent simple(Class<A1> a1, Class<A2> a2, Class<A3> a3, Class<A4> a4, Class<A5> a5, Class<A6> a6, Class<A7> a7, Class<A8> a8, Class<A9> a9, Class<A10> a10, Class<A11> a11, Class<R> returnType, Function11<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R> executor) {
        return create().args(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11).returnType(returnType)
                .executor(args -> executor.apply((A1) args[0], (A2) args[1], (A3) args[2], (A4) args[3], (A5) args[4], (A6) args[5], (A7) args[6], (A8) args[7], (A9) args[8], (A10) args[9], (A11) args[10])).build();
    }

    public static <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12> RPCEvent simple(Class<A1> a1, Class<A2> a2, Class<A3> a3, Class<A4> a4, Class<A5> a5, Class<A6> a6, Class<A7> a7, Class<A8> a8, Class<A9> a9, Class<A10> a10, Class<A11> a11, Class<A12> a12, Consumer12<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12> executor) {
        return create().args(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12).executor(args -> {
            executor.accept((A1) args[0], (A2) args[1], (A3) args[2], (A4) args[3], (A5) args[4], (A6) args[5], (A7) args[6], (A8) args[7], (A9) args[8], (A10) args[9], (A11) args[10], (A12) args[11]);
            return null;
        }).build();
    }

    public static <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R> RPCEvent simple(Class<A1> a1, Class<A2> a2, Class<A3> a3, Class<A4> a4, Class<A5> a5, Class<A6> a6, Class<A7> a7, Class<A8> a8, Class<A9> a9, Class<A10> a10, Class<A11> a11, Class<A12> a12, Class<R> returnType, Function12<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R> executor) {
        return create().args(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12).returnType(returnType)
                .executor(args -> executor.apply((A1) args[0], (A2) args[1], (A3) args[2], (A4) args[3], (A5) args[4], (A6) args[5], (A7) args[6], (A8) args[7], (A9) args[8], (A10) args[9], (A11) args[10], (A12) args[11])).build();
    }
}