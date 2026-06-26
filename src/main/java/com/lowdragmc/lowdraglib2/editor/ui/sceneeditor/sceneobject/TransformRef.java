package com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject;

import com.lowdragmc.lowdraglib2.math.Transform;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class TransformRef implements ValueIOSerializable {
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
    public void serialize(ValueOutput valueOutput) {
        if (transformId != null) {
            valueOutput.putString("uuid", transformId.toString());
        }
    }

    @Override
    public void deserialize(ValueInput valueInput) {
        transformId = null;
        valueInput.getString("uuid").ifPresent(uuid -> {
            try {
                transformId = UUID.fromString(uuid);
            } catch (Exception e) {
                transformId = null;
            }
        });
    }

    @Override
    public String toString() {
        if (transformId == null) return "null";
        return transformId.toString();
    }
}
