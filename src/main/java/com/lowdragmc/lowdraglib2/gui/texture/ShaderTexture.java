package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.shader.Shaders;
import com.lowdragmc.lowdraglib2.client.shader.management.Shader;
import com.lowdragmc.lowdraglib2.client.shader.management.ShaderManager;
import com.lowdragmc.lowdraglib2.client.shader.management.ShaderProgram;
import com.lowdragmc.lowdraglib2.client.shader.uniform.UniformCache;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.appliedenergistics.yoga.YogaAlign;

import javax.annotation.Nullable;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@LDLRegisterClient(name = "shader_texture", registry = "ldlib2:gui_texture")
public class ShaderTexture extends TransformTexture {
    private static final Map<ResourceLocation, ShaderTexture> CACHE = new HashMap<>();

    @Configurable(name = "ldlib.gui.editor.name.resource", tips = "ldlib.gui.editor.tips.shader_location")
    public ResourceLocation location;

    @OnlyIn(Dist.CLIENT)
    private ShaderProgram program;

    @OnlyIn(Dist.CLIENT)
    private Shader shader;

    @Configurable(tips = "ldlib.gui.editor.tips.shader_resolution")
    @ConfigNumber(range = {1, 3})
    private float resolution = 2;

    @Configurable
    @ConfigColor
    private int color = -1;

    private Consumer<UniformCache> uniformCache;

    private final boolean isRaw;

    private ShaderTexture(boolean isRaw) {
        this.isRaw = isRaw;
    }

    public ShaderTexture() {
        this(false);
        this.location = LDLib2.id("fbm");
        if (LDLib2.isRemote() && ShaderManager.allowedShader()) {
            Shader shader = Shaders.load(Shader.ShaderType.FRAGMENT, location);
            if (shader == null) return;
            this.program = new ShaderProgram();
            this.shader = shader;
            program.attach(Shaders.GUI_IMAGE_V);
            program.attach(shader);
        }
    }

    public static void clearCache() {
        CACHE.values().forEach(ShaderTexture::dispose);
        CACHE.clear();
    }

    public void dispose() {
        if (isRaw && shader != null) {
            shader.deleteShader();
        }
        if (program != null) {
            program.delete();
        }
        shader = null;
        program = null;
    }

    @Override
    public ShaderTexture setColor(int color) {
        this.color = color;
        return this;
    }

    @ConfigSetter(field = "location")
    public void updateShader(ResourceLocation location) {
        if (LDLib2.isRemote() && ShaderManager.allowedShader()) {
            this.location = location;
            dispose();
            Shader shader = Shaders.load(Shader.ShaderType.FRAGMENT, location);
            if (shader == null) return;
            this.program = new ShaderProgram();
            this.shader = shader;
            program.attach(Shaders.GUI_IMAGE_V);
            program.attach(shader);
        }
    }

    public void updateRawShader(String rawShader) {
        if (LDLib2.isRemote() && ShaderManager.allowedShader()) {
            dispose();
            shader = new Shader(Shader.ShaderType.FRAGMENT, rawShader).compileShader();
            program = new ShaderProgram();
            program.attach(Shaders.GUI_IMAGE_V);
            program.attach(shader);
        }
    }

    public String getRawShader() {
        if (LDLib2.isRemote() && ShaderManager.allowedShader() && shader != null) {
            return shader.source;
        }
        return "";
    }

    @OnlyIn(Dist.CLIENT)
    private ShaderTexture(Shader shader, boolean isRaw) {
        this.isRaw = isRaw;
        if (shader == null) return;
        this.program = new ShaderProgram();
        this.shader = shader;
        program.attach(Shaders.GUI_IMAGE_V);
        program.attach(shader);
    }

    public static ShaderTexture createShader(ResourceLocation location) {
        if (CACHE.containsKey(location) && CACHE.get(location).shader != null) {
            return CACHE.get(location);
        }
        ShaderTexture texture;
        if (LDLib2.isRemote() && ShaderManager.allowedShader()) {
            Shader shader = Shaders.load(Shader.ShaderType.FRAGMENT, location);
            texture = new ShaderTexture(shader, false);
            CACHE.put(location, texture);
        } else {
            texture = new ShaderTexture(false);
        }
        texture.location = location;
        return texture;
    }

    public static ShaderTexture createRawShader(String rawShader) {
        if (LDLib2.isRemote() && ShaderManager.allowedShader()) {
            Shader shader = new Shader(Shader.ShaderType.FRAGMENT, rawShader).compileShader();
            return new ShaderTexture(shader, true);
        } else {
            return new ShaderTexture(true);
        }
    }

    public ShaderTexture setUniformCache(Consumer<UniformCache> uniformCache) {
        this.uniformCache = uniformCache;
        return this;
    }

    public ShaderTexture setResolution(float resolution) {
        this.resolution = resolution;
        return this;
    }

    public float getResolution() {
        return resolution;
    }

    public void bindTexture(String samplerName, int id) {
        if (LDLib2.isRemote() && ShaderManager.allowedShader()) {
            if (program != null) {
                program.bindTexture(samplerName, id);
            }
        }
    }

    public void bindTexture(String samplerName, ResourceLocation location) {
        if (LDLib2.isRemote() && ShaderManager.allowedShader()) {
            if (program != null) {
                program.bindTexture(samplerName, location);
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void drawInternal(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, float width, float height, float partialTicks) {
        if (program != null) {
            try {
                program.use(cache->{
                    Minecraft mc = Minecraft.getInstance();
                    float time;
                    if (mc.player != null) {
                        time = (mc.player.tickCount + mc.getTimer().getGameTimeDeltaTicks()) / 20;
                    } else {
                        time = System.currentTimeMillis() / 1000f;
                    }
                    float mX = Mth.clamp((mouseX - x), 0, width);
                    float mY = Mth.clamp((mouseY - y), 0, height);
                    cache.glUniformMatrix4F("ModelViewMat", RenderSystem.getModelViewMatrix());
                    cache.glUniformMatrix4F("ProjMat", RenderSystem.getProjectionMatrix());
                    cache.glUniform2F("iResolution", width * resolution, height * resolution);
                    cache.glUniform2F("iMouse", mX * resolution, mY * resolution);
                    cache.glUniform1F("iTime", time);
                    if (uniformCache != null) {
                        uniformCache.accept(cache);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                dispose();
                return;
            }

            RenderSystem.enableBlend();
            Tesselator tessellator = Tesselator.getInstance();
            var mat = graphics.pose().last().pose();
            BufferBuilder buffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            buffer.addVertex(mat, x, y + height, 0).setUv(0, 0).setColor(color);
            buffer.addVertex(mat, x + width, y + height, 0).setUv(1, 0).setColor(color);
            buffer.addVertex(mat, x + width, y, 0).setUv(1, 1).setColor(color);
            buffer.addVertex(mat, x, y, 0).setUv(0, 1).setColor(color);
            BufferUploader.draw(buffer.buildOrThrow());

            program.release();
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
            var mui = e.currentElement.getModularUI();
            if (mui == null) return;
            Dialog.showFileDialog("ldlib.gui.editor.tips.select_shader", LDLib2.getAssetsDir(), true, node -> {
                if (!node.getKey().isFile() || node.getKey().getName().toLowerCase().endsWith(".fsh".toLowerCase())) {
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
                        updateShader(location);
                        configurator.notifyChanges();
                    }
                }).show(mui.ui.rootElement);
            }).layout(layout -> layout.setAlignSelf(YogaAlign.CENTER)))
        );
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
        var shaderIndex = subPath.indexOf("shaders/");
        if (shaderIndex == -1) {
            return null;
        }

        var shaderPath = subPath.substring(shaderIndex + "shaders/".length());
        if (!shaderPath.endsWith(".fsh")) {
            return null;
        }

        var location = modId + ":" + shaderPath.substring(0, shaderPath.length() - 4); // remove ".fsh" suffix

        if (LDLib2.isValidResourceLocation(location)) {
            return ResourceLocation.parse(location);
        }
        return null;
    }
}
