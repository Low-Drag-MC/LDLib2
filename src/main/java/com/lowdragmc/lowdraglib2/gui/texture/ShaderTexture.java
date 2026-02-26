package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.shader.LDShaderHolder;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.TagBuilder;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.vfyjxf.taffy.style.AlignItems;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector4f;

import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

@KJSBindings
@LDLRegisterClient(name = "shader_texture", registry = "ldlib2:gui_texture")
public class ShaderTexture extends TransformTexture implements AutoCloseable {
    @Getter
    @Configurable(name = "ldlib.gui.editor.name.resource", tips = "ldlib.gui.editor.tips.shader_location")
    private ResourceLocation shaderLocation;
    @Configurable(name = "widget.basic.color")
    @ConfigColor
    @Getter @Setter @Accessors(chain = true)
    private int color = -1;

    //runtime
    @Getter @Nullable
    private LDShaderHolder shaderHolder;

    public ShaderTexture() {
        this(LDLib2.id("fbm"));
    }

    public ShaderTexture(ResourceLocation shaderLocation) {
        setShader(shaderLocation);
    }

    @ConfigSetter(field = "shaderLocation")
    public ShaderTexture setShader(ResourceLocation shaderLocation) {
        this.shaderLocation = shaderLocation;
        if (LDLib2.isClient()) {
            if (!RenderSystem.isOnRenderThread()) {
                RenderSystem.recordRenderCall(() -> {
                    if (shaderHolder != null) shaderHolder.close();
                    shaderHolder = LDShaderHolder.createSafe(shaderLocation, DefaultVertexFormat.POSITION_TEX_COLOR);
                });
            } else {
                if (shaderHolder != null) shaderHolder.close();
                shaderHolder = LDShaderHolder.createSafe(shaderLocation, DefaultVertexFormat.POSITION_TEX_COLOR);
            }
        }
        return this;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        return TagBuilder.compound(super.serializeNBT(provider))
                .add("config", shaderHolder == null ? null : shaderHolder.serializeNBT(provider))
                .build();
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag tag) {
        super.deserializeNBT(provider, tag);
        if (tag.contains("config") && shaderHolder != null) {
            shaderHolder.deserializeNBT(provider, tag.getCompound("config"));
        }
    }

    @Override
    public void afterDeserialize() {
        super.afterDeserialize();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void drawInternal(GuiGraphics graphics, float mouseX, float mouseY,
                                float x, float y, float width, float height, float partialTicks) {

        if (shaderHolder != null) {
            // TODO use rendertype instead?
            graphics.flush();

            shaderHolder.addDynamicUniform("U_GuiRect", uniform -> uniform.set(new Vector4f(x, y, width, height)));
            shaderHolder.addDynamicUniform("U_GuiMouse", uniform -> uniform.set(mouseX, mouseY));

            RenderSystem.setShader(shaderHolder::getShaderInstance);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ZERO
            );
            RenderSystem.disableDepthTest();
            var mat = graphics.pose().last().pose();
            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            buffer.addVertex(mat, x, y + height, 0).setUv(0, 0).setColor(color);
            buffer.addVertex(mat, x + width, y + height, 0).setUv(1, 0).setColor(color);
            buffer.addVertex(mat, x + width, y, 0).setUv(1, 1).setColor(color);
            buffer.addVertex(mat, x, y, 0).setUv(0, 1).setColor(color);
            BufferUploader.drawWithShader(buffer.buildOrThrow());

            shaderHolder.removeDynamicUniform("U_GuiRect");
            shaderHolder.removeDynamicUniform("U_GuiMouse");
        } else {
            DrawerHelper.drawText(graphics, "Error compiling shader", x + 2, y + 2, 1, 0xffff0000);
        }
    }

    @Override
    public void createPreview(ConfiguratorGroup father) {
        super.createPreview(father);
        var configurator = new Configurator();

        // button to select image
        father.addConfigurator(configurator.addInlineChild(new Button().setText("ldlib.gui.editor.tips.select_shader").setOnClick(e -> {
            Dialog.showFileDialog("ldlib.gui.editor.tips.select_shader", LDLib2.getAssetsDir(), true, node -> {
                if (!node.getKey().isFile() || node.getKey().getName().toLowerCase().endsWith(".json".toLowerCase())) {
                    if (node.getKey().isFile()) {
                            return getShaderFromFile(node.getKey()) != null;
                        }
                        return true; // allow directories
                    }
                    return false;
                }, r -> {
                    if (r != null && r.isFile()) {
                        var location = getShaderFromFile(r);
                        if (location == null) return;
                        setShader(location);
                        configurator.notifyChanges();
                    }
                }).show(e.currentElement.getModularUI());
            }).layout(layout -> layout.alignSelf(AlignItems.CENTER)))
        );

        // button to reload shader
        father.addConfigurator(new Configurator().addInlineChild(new Button().setText("reload").setOnClick(e -> {
            setShader(this.shaderLocation);
        }).layout(layout -> layout.alignSelf(AlignItems.CENTER))));
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        super.buildConfigurator(father);
        var holderConfigContainer = new ConfiguratorGroup().hideTitle().setCollapse(false);
        AtomicReference<LDShaderHolder> holderRef = new AtomicReference<>(shaderHolder);
        // holder configurator
        if (shaderHolder != null) {
            shaderHolder.buildConfigurator(holderConfigContainer);
        }
        holderConfigContainer.configuratorContainer.setDisplay(!holderConfigContainer.getConfigurators().isEmpty());
        holderConfigContainer.addEventListener(UIEvents.TICK, e -> {
           if (holderRef.get() != shaderHolder) {
               holderConfigContainer.removeAllConfigurators();
               if (shaderHolder != null) {
                   shaderHolder.buildConfigurator(holderConfigContainer);
               }
               holderConfigContainer.configuratorContainer.setDisplay(!holderConfigContainer.getConfigurators().isEmpty());
               holderRef.set(shaderHolder);
           }
        });
        father.addConfigurator(holderConfigContainer);
    }

    @Nullable
    public ResourceLocation getShaderFromFile(File filePath) {
        String fullPath = filePath.getPath().replace('\\', '/');

        // find the "assets/" directory in the path
        var assetsIndex = fullPath.indexOf("assets/");
        if (assetsIndex == -1) {
            return null;
        }

        var relativePath = fullPath.substring(assetsIndex + "assets/".length());

        // find mod_id
        var slashIndex = relativePath.indexOf('/');
        if (slashIndex == -1) {
            return null;
        }

        var modId = relativePath.substring(0, slashIndex);
        var subPath = relativePath.substring(slashIndex + 1);

        // find shader location
        var shaderIndex = subPath.indexOf("shaders/core/");
        if (shaderIndex == -1) {
            return null;
        }

        var shaderPath = subPath.substring(shaderIndex + "shaders/core/".length());
        if (!shaderPath.endsWith(".json")) {
            return null;
        }

        var location = modId + ":" + shaderPath.substring(0, shaderPath.length() - 5); // remove ".json" suffix

        if (LDLib2.isValidResourceLocation(location)) {
            return ResourceLocation.parse(location);
        }
        return null;
    }

    @Override
    public void close() {
        if (LDLib2.isClient() && shaderHolder != null) {
            shaderHolder.close();
        }
    }
}
