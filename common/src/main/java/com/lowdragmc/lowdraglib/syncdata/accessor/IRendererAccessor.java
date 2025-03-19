package com.lowdragmc.lowdraglib.syncdata.accessor;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.client.renderer.ISerializableRenderer;
import com.lowdragmc.lowdraglib.client.renderer.impl.UIResourceRenderer;
import com.lowdragmc.lowdraglib.syncdata.AccessorOp;
import com.lowdragmc.lowdraglib.syncdata.payload.ITypedPayload;
import com.lowdragmc.lowdraglib.syncdata.payload.NbtTagPayload;
import com.mojang.datafixers.util.Either;
import net.minecraft.nbt.CompoundTag;

import java.io.File;

public class IRendererAccessor extends CustomObjectAccessor<IRenderer> {

    public IRendererAccessor() {
        super(IRenderer.class, true);
    }

    @Override
    public ITypedPayload<?> serialize(AccessorOp op, IRenderer value) {
        if (value instanceof ISerializableRenderer serializableRenderer) {
            return NbtTagPayload.of(ISerializableRenderer.serializeWrapper(serializableRenderer));
        } else if (value instanceof UIResourceRenderer renderer) {
            var tag = new CompoundTag();
            tag.putString("_type", "ui_resource");
            tag.put("key", renderer.key.map(
                    l -> {
                        var key = new CompoundTag();
                        key.putString("key", l);
                        key.putString("type", "builtin");
                        return key;
                    }, r-> {
                        var key = new CompoundTag();
                        key.putString("key", r.getPath());
                        key.putString("type", "project");
                        return key;
                    }
            ));
            return NbtTagPayload.of(tag);
        }
        return NbtTagPayload.of(new CompoundTag());
    }

    @Override
    public IRenderer deserialize(AccessorOp op, ITypedPayload<?> payload) {
        if (payload instanceof NbtTagPayload nbtTagPayload && nbtTagPayload.getPayload() instanceof CompoundTag tag) {
            if (tag.contains("_type") && tag.getString("_type").equals("ui_resource")) {
                var keyTag = tag.get("key");
                Either<String, File> key = null;
                if (keyTag instanceof CompoundTag compoundTag) {
                    var keyType = compoundTag.getString("type");
                    var keyValue = compoundTag.getString("key");
                    if (keyType.equals("builtin")) {
                        key = Either.left(keyValue);
                    } else if (keyType.equals("project")) {
                        key = Either.right(new File(keyValue));
                    }
                } else if (keyTag != null){
                    key = Either.left(keyTag.getAsString());
                }
                var resource = UIResourceRenderer.getProjectResource();
                if (resource == null) {
                    return new UIResourceRenderer(key);
                }
                if (UIResourceRenderer.isProject()) {
                    return new UIResourceRenderer(resource, key);
                } else {
                    return key == null ? IRenderer.EMPTY : resource.getResourceOrDefault(key, IRenderer.EMPTY);
                }
            }
            var renderer = ISerializableRenderer.deserializeWrapper(tag);
            if (renderer != null) {
                return renderer;
            }
        }
        return IRenderer.EMPTY;
    }

}
