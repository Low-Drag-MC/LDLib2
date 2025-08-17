package com.lowdragmc.lowdraglib2.gui.sync.rpc;


import com.lowdragmc.lowdraglib2.gui.sync.SyncValueHolder;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RPCEventBuilder {
    private final List<Type> args = new ArrayList<>();
    private final List<Object> initialArgs = new ArrayList<>();
    @Nullable
    private Type returnType;
    @Nullable
    private Object initialReturnValue;

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

    public RPCEvent build() {
        var syncArgs = new SyncValueHolder[args.size()];
        for (int i = 0; i < args.size(); i++) {
            syncArgs[i] = new SyncValueHolder("arg" + i, args.get(i), initialArgs.get(i));
        }
        var syncReturn = returnType != null ? new SyncValueHolder("return", returnType, initialReturnValue) : null;
        return new RPCEvent(syncArgs, syncReturn);
    }
}
