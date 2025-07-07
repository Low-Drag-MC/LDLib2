package com.lowdragmc.lowdraglib2.client.shader;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.accessors.*;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.ui.ColorConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.HDRColorConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.ShaderInstanceAccessor;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.UnknownNullability;
import org.joml.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

public class LDShaderInstance extends ShaderInstance implements IConfigurable, INBTSerializable<CompoundTag> {

    @Nullable
    public static LDShaderInstance create(ResourceLocation location, VertexFormat format) {
        try {
            return new LDShaderInstance(Minecraft.getInstance().getResourceManager(), location, format);
        } catch (Exception e) {
            return null;
        }
    }

    public LDShaderInstance(ResourceProvider resourceProvider, ResourceLocation shaderLocation, VertexFormat vertexFormat) throws IOException {
        super(resourceProvider, shaderLocation, vertexFormat);
    }

    public ShaderInstanceAccessor getShaderInstanceAccessor() {
        return (ShaderInstanceAccessor) this;
    }

    public boolean supportConfigurator(Uniform uniform) {
        return uniform != MODEL_VIEW_MATRIX &&
                uniform != PROJECTION_MATRIX &&
                uniform != TEXTURE_MATRIX &&
                uniform != SCREEN_SIZE &&
                uniform != COLOR_MODULATOR &&
                uniform != LIGHT0_DIRECTION &&
                uniform != LIGHT1_DIRECTION &&
                uniform != GLINT_ALPHA &&
                uniform != FOG_START &&
                uniform != FOG_END &&
                uniform != FOG_COLOR &&
                uniform != FOG_SHAPE &&
                uniform != LINE_WIDTH &&
                uniform != GAME_TIME &&
                uniform != CHUNK_OFFSET;
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        for (var entry : getShaderInstanceAccessor().getUniformMap().entrySet()) {
            var name = entry.getKey();
            var uniform = entry.getValue();
            if (!supportConfigurator(uniform)) continue;
            if (uniform.getType() <= 3) {
                var current = readInt(uniform);
                if (current.length == 1) {
                    father.addConfigurator(new NumberConfigurator(name, () -> readInt(uniform)[0],
                            v -> uniform.set(v.intValue()), current[0], true)
                            .setType(ConfigNumber.Type.INTEGER));
                } else if (current.length == 2) {
                    father.addConfigurator(new Vector2iAccessor().create(name, () -> {
                        var data = readInt(uniform);
                        return new Vector2i(data[0], data[1]);
                        }, v -> uniform.set(v.x, v.y), true, ConfiguratorGroup.class.getDeclaredFields()[0], this));
                } else if (current.length == 3) {
                    father.addConfigurator(new Vector3iAccessor().create(name, () -> {
                        var data = readInt(uniform);
                        return new Vector3i(data[0], data[1], data[2]);
                        }, v -> uniform.set(v.x, v.y, v.z), true, ConfiguratorGroup.class.getDeclaredFields()[0], this));
                } else if (current.length == 4) {
                    father.addConfigurator(new Vector4iAccessor().create(name, () -> {
                        var data = readInt(uniform);
                        return new Vector4i(data[0], data[1], data[2], data[3]);
                        }, v -> uniform.set(v.x, v.y, v.z, v.w), true, ConfiguratorGroup.class.getDeclaredFields()[0], this));
                }
            } else {
                var current = readFloat(uniform);
                if (current.length == 1) {
                    father.addConfigurator(new NumberConfigurator(name, () -> readFloat(uniform)[0],
                            v -> uniform.set(v.floatValue()), current[0], true)
                            .setType(ConfigNumber.Type.FLOAT));
                } else if (current.length == 2) {
                    father.addConfigurator(new Vector2fAccessor().create(name, () -> {
                        var data = readFloat(uniform);
                        return new Vector2f(data[0], data[1]);
                        }, v -> uniform.set(v.x, v.y), true, ConfiguratorGroup.class.getDeclaredFields()[0], this));
                } else if (current.length == 3) {
                    var lowerName = name.toLowerCase();
                    if (lowerName.contains("color") || lowerName.contains("rgb")) {
                        father.addConfigurator(new ColorConfigurator(name, () -> {
                            var data = readFloat(uniform);
                            return ColorUtils.color(1, data[0], data[1], data[2]);
                        }, v -> uniform.set(ColorUtils.red(v), ColorUtils.green(v), ColorUtils.blue(v)),
                                ColorUtils.color(1, current[0], current[1], current[2]), true));
                    } else {
                        father.addConfigurator(new Vector3fAccessor().create(name, () -> {
                            var data = readFloat(uniform);
                            return new Vector3f(data[0], data[1], data[2]);
                            }, v -> uniform.set(v.x, v.y, v.z), true, ConfiguratorGroup.class.getDeclaredFields()[0], this));
                    }
                } else if (current.length == 4) {
                    var lowerName = name.toLowerCase();
                    if (lowerName.contains("hdr") || lowerName.contains("emission")) {
                        father.addConfigurator(new HDRColorConfigurator(name, () -> {
                            var data = readFloat(uniform);
                            return new Vector4f(data[0], data[1], data[2], data[3]);
                        }, hdr -> uniform.set(hdr.x, hdr.y, hdr.z, hdr.w),
                                new Vector4f(current[0], current[1], current[2], current[3]), true));
                    } else if (lowerName.contains("color") || lowerName.contains("rgba")) {
                        father.addConfigurator(new ColorConfigurator(name, () -> {
                            var data = readFloat(uniform);
                            return ColorUtils.color(data[3], data[0], data[1], data[2]);
                            }, v -> uniform.set(ColorUtils.red(v), ColorUtils.green(v), ColorUtils.blue(v), ColorUtils.alpha(v)),
                                ColorUtils.color(current[3], current[0], current[1], current[2]), true));
                    } else {
                        father.addConfigurator(new Vector4fAccessor().create(name, () -> {
                            var data = readFloat(uniform);
                            return new Vector4f(data[0], data[1], data[2], data[3]);
                            }, v -> uniform.set(v.x, v.y, v.z, v.w), true, ConfiguratorGroup.class.getDeclaredFields()[0], this));
                    }
                }
            }
        }
    }

    @Override
    public @UnknownNullability CompoundTag serializeNBT(@Nonnull HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        // uniform
        var uniforms = new CompoundTag();
        for (var entry : getShaderInstanceAccessor().getUniformMap().entrySet()) {
            var name = entry.getKey();
            var uniform = entry.getValue();
            if (uniform.getType() <= 3) {
                uniforms.put(name, new IntArrayTag(readInt(uniform)));
            } else {
                var list = new ListTag();
                for (var v : readFloat(uniform)) {
                    list.add(FloatTag.valueOf(v));
                }
                uniforms.put(name, list);
            }
        }
        tag.put("uniforms", uniforms);
        return tag;
    }

    @Override
    public void deserializeNBT(@Nonnull HolderLookup.Provider provider, @Nonnull CompoundTag tag) {
        var uniforms = tag.getCompound("uniforms");
        for (var name : uniforms.getAllKeys()) {
            var uniform = getUniform(name);
            if (uniform == null) continue;
            var data = uniforms.get(name);
            if (data instanceof IntArrayTag intArrayTag) {
                var intArray = intArrayTag.getAsIntArray();
                if (intArray.length > uniform.getCount()) {
                    LDLib2.LOGGER.warn("Uniform.set called with a too-large value array (expected {}, got {}). Ignoring.", uniform.getCount(), intArray.length);
                } else if (intArray.length == 1) {
                    uniform.set(intArray[0]);
                } else if (intArray.length == 2) {
                    uniform.set(intArray[0], intArray[1]);
                } else if (intArray.length == 3) {
                    uniform.set(intArray[0], intArray[1], intArray[2]);
                } else if (intArray.length == 4) {
                    uniform.set(intArray[0], intArray[1], intArray[2], intArray[3]);
                }
            } else if (data instanceof ListTag floatArrayTag) {
                var floatArray = new float[floatArrayTag.size()];
                for (int i = 0; i < floatArrayTag.size(); i++) {
                    floatArray[i] = floatArrayTag.getFloat(i);
                }
                uniform.set(floatArray);
            }
        }
    }

    private int[] readInt(Uniform uniform) {
        if (uniform.getType() > 3) return new int[0];
        var buffer = uniform.getIntBuffer().duplicate();
        var count = uniform.getCount();
        var result = new int[count];
        buffer.position(0);
        buffer.get(result);
        return result;
    }

    private float[] readFloat(Uniform uniform) {
        if (uniform.getType() <= 3) return new float[0];
        var buffer = uniform.getFloatBuffer().duplicate();
        var count = uniform.getCount();
        var result = new float[count];
        buffer.position(0);
        buffer.get(result);
        return result;
    }
}
