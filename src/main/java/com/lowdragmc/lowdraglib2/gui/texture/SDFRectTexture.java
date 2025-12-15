    package com.lowdragmc.lowdraglib2.gui.texture;

    import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
    import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
    import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
    import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
    import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
    import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
    import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
    import com.lowdragmc.lowdraglib2.utils.ColorUtils;
    import com.mojang.blaze3d.systems.RenderSystem;
    import com.mojang.blaze3d.vertex.BufferUploader;
    import com.mojang.blaze3d.vertex.Tesselator;
    import com.mojang.blaze3d.vertex.VertexFormat;
    import lombok.Getter;
    import lombok.Setter;
    import net.minecraft.client.gui.GuiGraphics;
    import net.neoforged.api.distmarker.Dist;
    import net.neoforged.api.distmarker.OnlyIn;
    import org.joml.Vector4f;

    import static com.mojang.blaze3d.vertex.DefaultVertexFormat.*;

    @KJSBindings
    @LDLRegisterClient(name = "sdf_rect_texture", registry = "ldlib2:gui_texture")
    public class SDFRectTexture extends TransformTexture {
        @Getter @Setter
        @Configurable
        @ConfigNumber(range = {0f, Float.MAX_VALUE}, wheel = 1)
        private Vector4f radius = new Vector4f(0, 0, 0, 0);
        @Getter @Setter
        @Configurable
        @ConfigNumber(range = {0f, Float.MAX_VALUE}, wheel = 1)
        private float stroke = 0;
        @Getter
        @Configurable
        @ConfigColor
        private int color = 0xFFFFFFFF;
        @Getter
        @Configurable
        @ConfigColor
        private int borderColor = 0xff000000;
        // runtime
        private Vector4f colorVec4 = ColorUtils.toVector4f(color);
        private Vector4f borderColorVec4 = ColorUtils.toVector4f(borderColor);

        @Override
        @ConfigSetter(field = "color")
        public SDFRectTexture setColor(int color) {
            this.color = color;
            this.colorVec4 = ColorUtils.toVector4f(color);
            return this;
        }

        @ConfigSetter(field = "borderColor")
        public SDFRectTexture setBorderColor(int borderColor) {
            this.borderColor = borderColor;
            this.borderColorVec4 = ColorUtils.toVector4f(borderColor);
            return this;
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        protected void drawInternal(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, float width, float height, float partialTicks) {
            var halfWidth = width / 2f;
            var halfHeight = height / 2f;

            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(x + halfWidth, y + halfHeight, 0);
            var mat = pose.last().pose();

            var modelView = RenderSystem.getModelViewStack();
            modelView.pushMatrix();
            modelView.mul(mat);
            RenderSystem.applyModelViewMatrix();

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();   // ONE, ONE_MINUS_SRC_ALPHA
            RenderSystem.disableDepthTest();

            RenderSystem.setShader(LDLibShaders::getSDFRect);
            var shader = LDLibShaders.getSDFRect();
            shader.safeGetUniform("Radius").set(radius);
            shader.safeGetUniform("HalfSize").set(halfWidth, halfHeight);
            shader.safeGetUniform("FillColor").set(colorVec4);

            shader.safeGetUniform("Border").set(stroke);

            if (stroke > 0) {
                shader.safeGetUniform("BorderColor").set(borderColorVec4);
            }

            var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, POSITION);

            buffer.addVertex(-halfWidth, halfHeight, 0);
            buffer.addVertex(halfWidth, halfHeight, 0);
            buffer.addVertex(halfWidth, -halfHeight, 0);
            buffer.addVertex(-halfWidth, -halfHeight, 0);
            BufferUploader.drawWithShader(buffer.buildOrThrow());

            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            pose.popPose();
        }

    }
