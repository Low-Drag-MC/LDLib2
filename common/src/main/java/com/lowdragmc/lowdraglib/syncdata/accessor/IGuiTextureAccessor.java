package com.lowdragmc.lowdraglib.syncdata.accessor;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.UIResourceTexture;
import com.lowdragmc.lowdraglib.syncdata.AccessorOp;
import com.lowdragmc.lowdraglib.syncdata.payload.ITypedPayload;
import com.lowdragmc.lowdraglib.syncdata.payload.NbtTagPayload;
import com.mojang.datafixers.util.Either;
import net.minecraft.nbt.CompoundTag;

import java.io.File;

/**
 * @author KilaBash
 * @date 2022/9/7
 * @implNote BlockStateAccessor
 */
public class IGuiTextureAccessor extends CustomObjectAccessor<IGuiTexture>{

    public IGuiTextureAccessor() {
        super(IGuiTexture.class, true);
    }

    @Override
    public ITypedPayload<?> serialize(AccessorOp op, IGuiTexture value) {
        var tag = IGuiTexture.serializeWrapper(value);
        if (tag == null) {
            tag = new CompoundTag();
            if (value instanceof UIResourceTexture uiResourceTexture) {
                tag.putString("type", "ui_resource");
                tag.put("key", uiResourceTexture.key.map(
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
            } else {
                tag.putString("type", "empty");
            }
        }
        return NbtTagPayload.of(tag);
    }

    @Override
    public IGuiTexture deserialize(AccessorOp op, ITypedPayload<?> payload) {
        if (payload instanceof NbtTagPayload nbtTagPayload) {
            var tag = (CompoundTag)nbtTagPayload.getPayload();
            var type = tag.getString("type");
            if (type.equals("ui_resource") && tag.contains("key")) {
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
                var resource = UIResourceTexture.getProjectResource();
                if (resource == null) {
                    return new UIResourceTexture(key);
                }
                if (UIResourceTexture.isProject()) {
                    return new UIResourceTexture(resource, key);
                } else {
                    return key == null ? IGuiTexture.MISSING_TEXTURE: resource.getResourceOrDefault(key, IGuiTexture.MISSING_TEXTURE);
                }
            }
            return IGuiTexture.deserializeWrapper(tag);
        }
        return null;
    }

}
