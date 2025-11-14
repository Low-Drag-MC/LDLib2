package com.lowdragmc.lowdraglib2.client.shader;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.accessors.*;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.ui.*;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.ShaderInstanceAccessor;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaEdge;
import org.jetbrains.annotations.UnknownNullability;
import org.joml.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR;

public class LDShaderInstance extends ShaderInstance implements ILDShaderInstance, IConfigurable, INBTSerializable<CompoundTag> {
    @Getter
    @Nullable
    private Program geometry;
    // runtime
    private final Map<String, Object> samplerCache = new HashMap<>();
    private final Map<String, Supplier<Object>> dynamicSampler = new HashMap<>();
    private final Map<String, Consumer<Uniform>> dynamicUniform = new HashMap<>();
    private boolean isSamplerCacheDirty = true;

    @Nullable
    public static LDShaderInstance create(ResourceLocation location, VertexFormat format) {
        try {
            return new LDShaderInstance(Minecraft.getInstance().getResourceManager(), location, format);
        } catch (Exception e) {
            LDLib2.LOGGER.warn("Could not create LDLib shader instance", e);
            return null;
        }
    }

    public LDShaderInstance(ResourceProvider resourceProvider, ResourceLocation shaderLocation, VertexFormat vertexFormat) throws IOException {
        super(resourceProvider, shaderLocation, vertexFormat);
    }

    @Override
    public void onCreateShader(ResourceProvider resourceProvider, ResourceLocation shaderLocation, VertexFormat vertexFormat, JsonObject json) {
        var geometryShader = GsonHelper.getAsString(json, "geometry", null);
        if (geometryShader != null) {
            try {
                this.geometry = ShaderInstanceAccessor.invokeGetOrCreate(resourceProvider, LDLibShaders.GEOMETRY_TYPE, geometryShader);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void attachToProgram() {
        super.attachToProgram();
        if (this.geometry != null) {
            this.geometry.attachToShader(this);
        }
    }

    public void removeDynamicSampler(String name) {
        dynamicSampler.remove(name);
    }
    public void removeDynamicUniform(String name) {
        dynamicUniform.remove(name);
    }

    public void addDynamicSampler(String name, Supplier<Object> supplier) {
        dynamicSampler.put(name, supplier);
    }

    public void addDynamicUniform(String name, Consumer<Uniform> consumer) {
        dynamicUniform.put(name, consumer);
    }

    @Override
    public void apply() {
        if (isSamplerCacheDirty) {
            applySamplers();
        }
        dynamicSampler.forEach((name, supplier) ->
                getShaderInstanceAccessor().getSamplerMap().put(name, supplier.get()));

        dynamicUniform.forEach((name, consumer) -> {
            var uniform = getUniform(name);
            if (uniform != null) consumer.accept(uniform);
        });
        super.apply();
    }

    public void applySamplers() {
        for (var entry : samplerCache.entrySet()) {
            var name = entry.getKey();
            var sampler = entry.getValue();
            if (sampler instanceof ResourceLocation location) {
                setSampler(name, Minecraft.getInstance().getTextureManager().getTexture(location));
            }
        }
        isSamplerCacheDirty = false;
    }

    public int getSamplerID(String name) {
        var sampler = samplerCache.get(name);
        if (sampler instanceof ResourceLocation location) {
            return Minecraft.getInstance().getTextureManager().getTexture(location).getId();
        } if (sampler instanceof RenderTarget renderTarget) {
            return renderTarget.getColorTextureId();
        }
        return -1;
    }

    @Nullable
    public CompoundTag serializeSampler(Object sampler) {
        CompoundTag tag = new CompoundTag();
        if (sampler instanceof ResourceLocation textureLocation) {
            tag.putString("type", "texture");
            tag.putString("resource", textureLocation.toString());
            return tag;
        }
        return null;
    }

    @Nullable
    public Object deserializeSampler(CompoundTag tag) {
        var type  = tag.getString("type");
        if (type.equals("texture")) {
            return ResourceLocation.parse(tag.getString("resource"));
        }
        return null;
    }

    @Override
    public @UnknownNullability CompoundTag serializeNBT(@Nonnull HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        // uniform
        var uniforms = new CompoundTag();
        for (var entry : getShaderInstanceAccessor().getUniformMap().entrySet()) {
            var name = entry.getKey();
            var uniform = entry.getValue();
            if (isBuiltinUniform(uniform)) continue;
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
        var samplers = new CompoundTag();
        for (var entry : samplerCache.entrySet()) {
            var name = entry.getKey();
            var samplerData = serializeSampler(entry.getValue());
            if (samplerData != null) {
                samplers.put(name, samplerData);
            }
        }
        tag.put("samplers", samplers);
        return tag;
    }

    @Override
    public void deserializeNBT(@Nonnull HolderLookup.Provider provider, @Nonnull CompoundTag tag) {
        samplerCache.clear();
        dynamicSampler.clear();
        dynamicUniform.clear();
        isSamplerCacheDirty = true;
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
        var samplers = tag.getCompound("samplers");
        for (var name : samplers.getAllKeys()) {
            var sampler = deserializeSampler(samplers.getCompound(name));
            if (sampler == null) continue;
            samplerCache.put(name, sampler);
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

    public boolean isBuiltinSampler(String name) {
        return name.startsWith("Sampler");
    }

    public boolean isBuiltinUniform(Uniform uniform) {
        return uniform.getName().startsWith("U_") ||
                uniform == MODEL_VIEW_MATRIX ||
                uniform == PROJECTION_MATRIX ||
                uniform == TEXTURE_MATRIX ||
                uniform == SCREEN_SIZE ||
                uniform == COLOR_MODULATOR ||
                uniform == LIGHT0_DIRECTION ||
                uniform == LIGHT1_DIRECTION ||
                uniform == GLINT_ALPHA ||
                uniform == FOG_START ||
                uniform == FOG_END ||
                uniform == FOG_COLOR ||
                uniform == FOG_SHAPE ||
                uniform == LINE_WIDTH ||
                uniform == GAME_TIME ||
                uniform == CHUNK_OFFSET;
    }

    private IGuiTexture createSamplerPreview(String name) {
        return (GuiGraphics graphics, int mouseX, int mouseY, float x, float y, float width, float height, float partialTicks) -> {
            RenderSystem.enableBlend();
            float imageU = 0;
            float imageV = 0;
            float imageWidth = 1;
            float imageHeight = 1;
            var mat = graphics.pose().last().pose();
            var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, POSITION_TEX_COLOR);
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, getSamplerID(name));
            buffer.addVertex(mat, x, y + height, 0).setUv(imageU, imageV + imageHeight).setColor(-1);
            buffer.addVertex(mat, x + width, y + height, 0).setUv(imageU + imageWidth, imageV + imageHeight).setColor(-1);
            buffer.addVertex(mat, x + width, y, 0).setUv(imageU + imageWidth, imageV).setColor(-1);
            buffer.addVertex(mat, x, y, 0).setUv(imageU, imageV).setColor(-1);
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        };
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        // samplers
        for (var samplerName : getShaderInstanceAccessor().getSamplerNames()) {
            if (isBuiltinSampler(samplerName)) continue;
            var samplerConfigurator = new ValueConfigurator<>(samplerName,
                    () -> samplerCache.getOrDefault(samplerName, IGuiTexture.EMPTY),
                    object -> {
                isSamplerCacheDirty = true;
                samplerCache.put(samplerName, object);
                }, IGuiTexture.EMPTY, true);
            samplerConfigurator.setCanDropPredicate(obj -> {
                if (obj instanceof ResourceLocation) return true;
                if (obj instanceof RenderTarget) return true;
                return false;
            });
            samplerConfigurator.addChildren(
                    // preview
                    new UIElement().layout(layout -> {
                        layout.setAspectRatio(1.0f);
                        layout.setWidthPercent(80);
                        layout.setAlignSelf(YogaAlign.CENTER);
                        layout.setPadding(YogaEdge.ALL, 3);
                    }).style(style -> style.backgroundTexture(Sprites.BORDER1_RT1))
                    .addChild(new UIElement().layout(layout -> {
                        layout.setWidthPercent(100);
                        layout.setHeightPercent(100);
                    }).style(style -> style.backgroundTexture(createSamplerPreview(samplerName)))),
                    // button to select image
                    new Button().setText("ldlib.gui.editor.tips.select_image").setOnClick(e -> {
                        Dialog.showFileDialog("ldlib.gui.editor.tips.select_image", LDLib2.getAssetsDir(), true, Dialog.suffixFilter(".png"), r -> {
                            if (r != null && r.isFile()) {
                                var location = IGuiTexture.getTextureFromFile(r);
                                if (location == null) return;
                                isSamplerCacheDirty = true;
                                samplerCache.put(samplerName, location);
                                samplerConfigurator.notifyChanges();
                            }
                        }).show(e.currentElement.getModularUI());
                    }).layout(layout -> layout.setAlignSelf(YogaAlign.CENTER))
            );
            father.addConfigurator(samplerConfigurator);
        }
        // uniforms
        for (var entry : getShaderInstanceAccessor().getUniformMap().entrySet()) {
            var name = entry.getKey();
            var uniform = entry.getValue();
            if (isBuiltinUniform(uniform)) continue;
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
}
