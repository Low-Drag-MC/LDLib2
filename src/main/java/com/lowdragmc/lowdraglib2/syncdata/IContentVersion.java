package com.lowdragmc.lowdraglib2.syncdata;

/**
 * A value that counts its own changes, so the auto sync compares one number instead of the whole value. Takes effect
 * wherever the sync would otherwise serialize the value to compare it: {@link net.neoforged.neoforge.common.util.ValueIOSerializable}
 * values, and fields marked with their codec (e.g. {@link com.lowdragmc.lowdraglib2.syncdata.annotation.ManagedCodec}).
 * <br>
 * {@link #getContentVersion()} must return a different number after any change to what the value saves or syncs, and
 * the same number while nothing changed. That includes changes others make to objects the value hands out, such as a
 * stack returned by {@code getStackInSlot} and grown in place: if the value can't notice those, don't implement this.
 * The sync reads the version from its own thread, so keep the counter in a {@code volatile} field or accept that a
 * change may be noticed a period later.
 */
public interface IContentVersion {
    long getContentVersion();
}
