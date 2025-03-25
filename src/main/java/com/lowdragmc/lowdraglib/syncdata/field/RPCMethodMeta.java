package com.lowdragmc.lowdraglib.syncdata.field;

import com.lowdragmc.lowdraglib.syncdata.AccessorRegistries;
import com.lowdragmc.lowdraglib.syncdata.accessor.direct.IDirectAccessor;
import com.lowdragmc.lowdraglib.syncdata.var.ManagedHolderVar;
import com.lowdragmc.lowdraglib.syncdata.rpc.RPCSender;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class RPCMethodMeta {
    @Getter
    private final String name;
    private final IDirectAccessor<?>[] argsAccessor;
    private final Class<?>[] argsType;
    private final Method method;
    private final boolean isFirstArgSender;

    public RPCMethodMeta(Method method) {
        this.method = method;
        method.setAccessible(true);
        this.name = method.getName();

        var args = method.getParameters();

        if (args.length == 0) {
            argsAccessor = new IDirectAccessor[0];
            argsType = new Class[0];
            isFirstArgSender = false;
        } else {
            var firstArg = args[0];
            if (RPCSender.class.isAssignableFrom(firstArg.getType())) {
                argsAccessor = new IDirectAccessor[args.length - 1];
                argsType = new Class[args.length - 1];
                for (int i = 1; i < args.length; i++) {
                    var arg = args[i];
                    argsAccessor[i - 1] = getAccessor(arg.getType());
                    argsType[i - 1] = arg.getType();
                }
                isFirstArgSender = true;
            } else {
                argsAccessor = new IDirectAccessor[args.length];
                argsType = new Class[args.length];
                for (int i = 0; i < args.length; i++) {
                    var arg = args[i];
                    argsAccessor[i] = getAccessor(arg.getType());
                    argsType[i] = arg.getType();
                }
                isFirstArgSender = false;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void invoke(Object instance, RPCSender sender, RegistryFriendlyByteBuf buf) {
        Object[] args;
        if (isFirstArgSender) {
            args = new Object[argsAccessor.length + 1];
            args[0] = sender;
            for (int i = 0; i < argsAccessor.length; i++) {
                var holder = ManagedHolderVar.ofType(argsType[i]);
                ((IDirectAccessor)argsAccessor[i]).writeDirectVarFromStream(buf, holder);
                args[i + 1] = holder.value();
            }
        } else {
            args = new Object[argsAccessor.length];
            for (int i = 0; i < argsAccessor.length; i++) {
                var holder = ManagedHolderVar.ofType(argsType[i]);
                ((IDirectAccessor)argsAccessor[i]).writeDirectVarFromStream(buf, holder);
                args[i] = holder.value();
            }
        }

        try {
            method.invoke(instance, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public void serializeArgs(RegistryFriendlyByteBuf buf, Object[] args) {
        if(argsAccessor.length != args.length) {
            throw new IllegalArgumentException("Invalid number of arguments, expected " + argsAccessor.length + " but got " + args.length);
        }
        for (int i = 0; i < argsAccessor.length; i++) {
            ((IDirectAccessor)argsAccessor[i]).readDirectVarToStream(buf, ManagedHolderVar.of(args[i]));
        }
    }

    private static IDirectAccessor<?> getAccessor(Type type) {
        var accessor = AccessorRegistries.findByType(type);
        if (accessor == null) {
            throw new IllegalArgumentException("Cannot find accessor for type " + type);
        }
        if (accessor instanceof IDirectAccessor<?> directAccessor) {
            return directAccessor;
        }
        throw new IllegalArgumentException("Accessor for type " + type + " is not a ManagedAccessor");
    }

}
