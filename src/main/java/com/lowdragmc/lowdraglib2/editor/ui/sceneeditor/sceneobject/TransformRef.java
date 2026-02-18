package com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject;

import com.lowdragmc.lowdraglib2.math.Transform;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.StringTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.UnknownNullability;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TransformRef implements INBTSerializable<StringTag> {
    @Nullable
    @Getter @Setter
    private UUID transformId = null;

    public TransformRef() {

    }

    public TransformRef(@Nullable Transform transform) {
        this.transformId = transform == null ? null : transform.id();
    }

    public TransformRef(UUID transformId) {
        this.transformId = transformId;
    }

    public void setTransform(Transform transform) {
        this.transformId = transform.id();
    }

    @Nullable
    public Transform getTransform(@Nullable IScene scene) {
        if (transformId == null) return null;
        if (scene == null) return null;
        var obj = scene.getSceneObject(transformId);
        if (obj != null) return obj.transform();
        return null;
    }

    @Override
    public @UnknownNullability StringTag serializeNBT(HolderLookup.Provider provider) {
        if (transformId == null) return StringTag.valueOf("");
        return StringTag.valueOf(transformId.toString());
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, StringTag nbt) {
        if (nbt.getAsString().isEmpty()) {
            transformId = null;
        } else {
            try {
                transformId = UUID.fromString(nbt.getAsString());
            } catch (Exception e) {
                transformId = null;
            }
        }
    }

    @Override
    public String toString() {
        if (transformId == null) return "null";
        return transformId.toString();
    }
}
