package com.lowdragmc.lowdraglib.syncdata.accessor;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.client.renderer.ISerializableRenderer;
import com.lowdragmc.lowdraglib.client.renderer.impl.UIResourceRenderer;
import com.lowdragmc.lowdraglib.syncdata.AccessorOp;
import com.lowdragmc.lowdraglib.syncdata.payload.ITypedPayload;
import com.lowdragmc.lowdraglib.syncdata.payload.NbtTagPayload;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public class IRendererAccessor extends CustomObjectAccessor<IRenderer> {

    public IRendererAccessor() {
        super(IRenderer.class, true);
    }

    @Override
    public ITypedPayload<?> serialize(AccessorOp op, IRenderer value, HolderLookup.Provider provider) {
        if (value instanceof ISerializableRenderer serializableRenderer) {
            return NbtTagPayload.of(ISerializableRenderer.serializeWrapper(provider, serializableRenderer));
        } else if (value instanceof UIResourceRenderer renderer) {
            var tag = new CompoundTag();
            tag.putString("_type", "ui_resource");
            tag.putString("key", renderer.key);
            return NbtTagPayload.of(tag);
        }
        return NbtTagPayload.of(new CompoundTag());
    }

    @Override
    public IRenderer deserialize(AccessorOp op, ITypedPayload<?> payload, HolderLookup.Provider provider) {
        if (payload instanceof NbtTagPayload nbtTagPayload && nbtTagPayload.getPayload() instanceof CompoundTag tag) {
            if (tag.contains("_type") && tag.getString("_type").equals("ui_resource")) {
                var resource = UIResourceRenderer.getProjectResource();
                var key = tag.getString("key");
                if (resource == null) {
                    return new UIResourceRenderer(key);
                }
                if (UIResourceRenderer.isProject()) {
                    return new UIResourceRenderer(resource, key);
                } else {
                    return resource.getResourceOrDefault(key, IRenderer.EMPTY);
                }
            }
            var renderer = ISerializableRenderer.deserializeWrapper(provider, tag);
            if (renderer != null) {
                return renderer;
            }
        }
        return IRenderer.EMPTY;
    }

}
