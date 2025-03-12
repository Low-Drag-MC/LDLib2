package com.lowdragmc.lowdraglib.syncdata.managed;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import lombok.Getter;
import lombok.Setter;

public class ManagedRef implements IRef {
    @Getter
    @Setter
    private String persistedPrefixName;
    protected final IManagedVar<?> field;
    @Getter
    protected boolean isSyncDirty, isPersistedDirty;
    protected boolean lazy = false;
    protected ManagedKey key;
    @Setter
    protected BooleanConsumer onSyncListener = changed -> {
    };
    @Setter
    protected BooleanConsumer onPersistedListener = changed -> {
    };

    protected ManagedRef(IManagedVar<?> field) {
        this.field = field;
    }

    public static ManagedRef create(IManagedVar<?> field, boolean lazy) {
        return switch (field) {
            case IManagedVar.Int anInt -> new IntRef(anInt).setLazy(lazy);
            case IManagedVar.Long aLong -> new LongRef(aLong).setLazy(lazy);
            case IManagedVar.Float aFloat -> new FloatRef(aFloat).setLazy(lazy);
            case IManagedVar.Double aDouble -> new DoubleRef(aDouble).setLazy(lazy);
            case IManagedVar.Boolean aBoolean -> new BooleanRef(aBoolean).setLazy(lazy);
            case IManagedVar.Byte aByte -> new ByteRef(aByte).setLazy(lazy);
            case IManagedVar.Short aShort -> new ShortRef(aShort).setLazy(lazy);
            case IManagedVar.Char aChar -> new CharRef(aChar).setLazy(lazy);
            case ReadOnlyManagedField readOnlyManagedField -> new ReadOnlyManagedRef(readOnlyManagedField).setLazy(lazy);
            case null, default -> new SimpleObjectRef(field).setLazy(lazy);
        };
    }

    @Override
    public ManagedKey getKey() {
        return key;
    }

    public IRef setKey(ManagedKey key) {
        this.key = key;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T extends IManagedVar<?>> T getField() {
        return (T) field;
    }

    @Override
    public void clearSyncDirty() {
        isSyncDirty = false;
        if (key.isDestSync()) {
            onSyncListener.accept(false);
        }
    }

    @Override
    public void clearPersistedDirty() {
        isPersistedDirty = false;
        if (key.isPersist()) {
            onPersistedListener.accept(false);
        }
    }

    @Override
    public void markAsDirty() {
        if (key.isDestSync()) {
            isSyncDirty = true;
            onSyncListener.accept(true);
        }
        if (key.isPersist()) {
            isPersistedDirty = true;
            onPersistedListener.accept(true);
        }
    }

    @Override
    public boolean isLazy() {
        return lazy;
    }

    @Override
    public <T> T readRaw() {
        return this.<IManagedVar<T>>getField().value();
    }

    protected ManagedRef setLazy(boolean lazy) {
        this.lazy = lazy;
        return this;
    }

    public void update() {
    }

    static class IntRef extends ManagedRef {
        private int oldValue;

        IntRef(IManagedVar.Int field) {
            super(field);
            oldValue = this.<IManagedVar.Int>getField().intValue();
        }

        @Override
        public void update() {
            int newValue = this.<IManagedVar.Int>getField().intValue();
            if (oldValue != newValue) {
                oldValue = newValue;
                markAsDirty();
            }
        }
    }

    static class LongRef extends ManagedRef {
        private long oldValue;

        LongRef(IManagedVar.Long field) {
            super(field);
            oldValue = this.<IManagedVar.Long>getField().longValue();
        }

        @Override
        public void update() {
            long newValue = this.<IManagedVar.Long>getField().longValue();
            if (oldValue != newValue) {
                oldValue = newValue;
                markAsDirty();
            }
        }
    }

    static class FloatRef extends ManagedRef {
        private float oldValue;

        FloatRef(IManagedVar.Float field) {
            super(field);
            oldValue = this.<IManagedVar.Float>getField().floatValue();
        }

        @Override
        public void update() {
            float newValue = this.<IManagedVar.Float>getField().floatValue();
            if (oldValue != newValue) {
                oldValue = newValue;
                markAsDirty();
            }
        }
    }

    static class DoubleRef extends ManagedRef {
        private double oldValue;

        DoubleRef(IManagedVar.Double field) {
            super(field);
            oldValue = this.<IManagedVar.Double>getField().doubleValue();
        }

        @Override
        public void update() {
            double newValue = this.<IManagedVar.Double>getField().doubleValue();
            if (oldValue != newValue) {
                oldValue = newValue;
                markAsDirty();
            }
        }
    }

    static class BooleanRef extends ManagedRef {
        private boolean oldValue;

        BooleanRef(IManagedVar.Boolean field) {
            super(field);
            oldValue = this.<IManagedVar.Boolean>getField().booleanValue();
        }

        @Override
        public void update() {
            boolean newValue = this.<IManagedVar.Boolean>getField().booleanValue();
            if (oldValue != newValue) {
                oldValue = newValue;
                markAsDirty();
            }
        }
    }

    static class ByteRef extends ManagedRef {
        private byte oldValue;

        ByteRef(IManagedVar.Byte field) {
            super(field);
            oldValue = this.<IManagedVar.Byte>getField().byteValue();
        }

        @Override
        public void update() {
            byte newValue = this.<IManagedVar.Byte>getField().byteValue();
            if (oldValue != newValue) {
                oldValue = newValue;
                markAsDirty();
            }
        }
    }

    static class ShortRef extends ManagedRef {
        private short oldValue;

        ShortRef(IManagedVar.Short field) {
            super(field);
            oldValue = this.<IManagedVar.Short>getField().shortValue();
        }

        @Override
        public void update() {
            short newValue = this.<IManagedVar.Short>getField().shortValue();
            if (oldValue != newValue) {
                oldValue = newValue;
                markAsDirty();
            }
        }
    }

    static class CharRef extends ManagedRef {
        private char oldValue;

        CharRef(IManagedVar.Char field) {
            super(field);
            oldValue = this.<IManagedVar.Char>getField().charValue();
        }

        @Override
        public void update() {
            char newValue = this.<IManagedVar.Char>getField().charValue();
            if (oldValue != newValue) {
                oldValue = newValue;
                markAsDirty();
            }
        }
    }

}
