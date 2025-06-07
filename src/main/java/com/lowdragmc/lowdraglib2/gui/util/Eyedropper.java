package com.lowdragmc.lowdraglib2.gui.util;
//
//import com.lowdragmc.lowdraglib.LDLib;
//import com.lowdragmc.lowdraglib.client.utils.ShaderUtils;
//import com.mojang.blaze3d.pipeline.RenderTarget;
//import com.mojang.blaze3d.platform.NativeImage;
//import com.mojang.blaze3d.platform.Window;
//import com.mojang.blaze3d.systems.RenderSystem;
//import com.mojang.blaze3d.vertex.*;
//import net.minecraft.Util;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.gui.GuiComponent;
//import net.minecraft.client.renderer.ShaderInstance;
//import net.minecraft.network.chat.Component;
//import net.minecraft.network.chat.TextComponent;
//import net.minecraft.util.FastColor;
//import net.neoforged.neoforge.api.distmarker.Dist;
//import net.neoforged.neoforge.api.distmarker.OnlyIn;
//import org.apache.http.util.Asserts;
//
//import java.util.Arrays;
//
///**
// * @author KilaBash
// * @date 2022/12/11
// * @implNote Eyedropper
// */
//@OnlyIn(Dist.CLIENT)
//public enum Eyedropper {
//    DOWNLOAD {
//
//        private static NativeImage nativeImage;
//        private static int lastWidth;
//        private static int lastHeight;
//        private static int openCount = 0;
//        private static int closeCount = 0;
//
//        @Override
//        public void updateCurrentColor() {
//            RenderTarget renderTarget = Minecraft.getInstance().getMainRenderTarget();
//
//            if (lastWidth != renderTarget.width || lastHeight != renderTarget.height) {
//                if (nativeImage != null) {
//                    nativeImage.close();
//                    closeCount++;
//                }
//                nativeImage = new NativeImage(renderTarget.width, renderTarget.height, false);
//                openCount++;
//                lastWidth = renderTarget.width;
//                lastHeight = renderTarget.height;
//            }
//
//            RenderSystem.bindTexture(renderTarget.getColorTextureId());
//            nativeImage.downloadTexture(0, true);
//            nativeImage.flipY();
//
//            Window window = Minecraft.getInstance().getWindow();
//
//            int rgba = nativeImage.getPixelRGBA(window.getWidth() / 2, window.getHeight() / 2);
//            currentColor[0] = NativeImage.getR(rgba) / 255f;
//            currentColor[1] = NativeImage.getG(rgba) / 255f;
//            currentColor[2] = NativeImage.getB(rgba) / 255f;
//
//            if (Math.abs(closeCount - openCount) >= 5) {
//                throw new RuntimeException();
//            }
//        }
//
//        @Override
//        public void setShader(ShaderInstance shader) {
//
//        }
//
//        @Override
//        public void init() {
//        }
//
//        @Override
//        public void destroy() {
//            if (nativeImage != null) {
//                nativeImage.close();
//                nativeImage = null;
//                closeCount++;
//            }
//            lastWidth = -1;
//            lastHeight = -1;
//        }
//
//        @Override
//        public String modeName() {
//            return "glGetTexImage";
//        }
//    };
//
//
//    private static boolean dataAvailable = false;
//    private static final float[] eyedropperColor = new float[3];
//
//    private static float[] currentColor = new float[3];
//
//    private static boolean enable = false;
//    private static boolean readyForRecord = true;
//
//    private static final String colorPreviewChar = Util.make(() -> {
//        var holder = "⬛";
//        if (LDLib.isModLoaded("modernui")) {
//            holder = "\u200c" + holder + "\u200c";
//        }
//        return holder;
//    });
//
//    public static int pack(float[] color) {
//        Asserts.check(color.length == 3, "raw color array's length must be 3");
//        return FastColor.ARGB32.setColor(255, (int) (color[0] * 255), (int) (color[1] * 255), (int) (color[2] * 255));
//    }
//
//    private static Component makeColorPreview(float[] color) {
//        return new TextComponent(colorPreviewChar).withStyle((style) -> style.withColor(pack(color)));
//    }
//
//    protected abstract void updateCurrentColor();
//
//    public static void update(PoseStack poseStack) {
//        if (enable) {
//            mode.updateCurrentColor();
//            mode.renderIndicator(poseStack);
//
//            if (ShimmerConstants.recordScreenColor.isDown() && readyForRecord) {
//                eyedropperColor[0] = currentColor[0];
//                eyedropperColor[1] = currentColor[1];
//                eyedropperColor[2] = currentColor[2];
//                dataAvailable = true;
//                readyForRecord = false;
//                Minecraft.getInstance().player.sendMessage(new TextComponent("set color " + formatRGB(eyedropperColor))
//                        .append(makeColorPreview(eyedropperColor)), Util.NIL_UUID);
//            } else if (!ShimmerConstants.recordScreenColor.isDown()) {
//                readyForRecord = true;
//            }
//
//        }
//    }
//
//    private static String formatRGB(float[] color) {
//        var r = (int) (color[0] * 255);
//        var g = (int) (color[1] * 255);
//        var b = (int) (color[2] * 255);
//        var str = new StringBuilder();
//        str.append("r:").append(r);
//        do {
//            str.append(' ');
//        } while (str.length() != 6);
//        str.append("g:").append(g);
//        do {
//            str.append(' ');
//        } while (str.length() != 12);
//        str.append("b:").append(b);
//        do {
//            str.append(' ');
//        } while (str.length() != 18);
//        return str.toString();
//    }
//
//    private void renderIndicator(@Nonnull GuiGraphics graphics) {
//
//        var window = Minecraft.getInstance().getWindow();
//        var scale = window.getGuiScale();
//
//        var centerX = (int) (window.getWidth() / 2f / scale);
//        var centerY = (int) (window.getHeight() / 2f / scale);
//
//        var backWidth = 1;
//
//        ShaderUtils. warpGLDebugLabel("draw_back", () ->
//                GuiComponent.fill(poseStack, centerX + 10 - backWidth, centerY + 10 - backWidth, centerX + 30 + backWidth, centerY + 10 + 20 + backWidth, 0x7F_FF_FF_FF));
//
//        ShaderUtils.warpGLDebugLabel("draw_current_color_block", () ->
//                GuiComponent.fill(poseStack, centerX + 10, centerY + 10, centerX + 30, centerY + 10 + 20, pack(currentColor)));
//
//        if (dataAvailable) {
//            ShaderUtils.warpGLDebugLabel("draw_selected_color_block", () ->
//                    GuiComponent.fill(poseStack, centerX + 10 + 10, centerY + 10, centerX + 30, centerY + 10 + 20, pack(eyedropperColor)));
//        }
//
//    }
//
//    public abstract void setShader(ShaderInstance shader);
//
//    public abstract String modeName();
//
//    protected abstract void init();
//
//    protected abstract void destroy();
//
//    public static void switchState() {
//        //called from command execute, but may not on render thread
//        RenderSystem.recordRenderCall(() -> {
//            if (enable) {
//                mode.destroy();
//                dataAvailable = false;
//                enable = false;
//            } else {
//                mode.init();
//                enable = true;
//            }
//        });
//    }
//
//    public static boolean getState() {
//        return enable;
//    }
//
//
//    public static void switchMode(Eyedropper newMode) {
//        //called from command execute, but may not on render thread
//        RenderSystem.recordRenderCall(() -> {
//            if (newMode == mode) {
//                Minecraft.getInstance().player.sendMessage(new TextComponent("already in " + mode.modeName()), Util.NIL_UUID);
//                return;
//            }
//            var isEnable = enable;
//            if (isEnable) {
//                switchState();//close
//            }
//            switch (newMode) {
//                case ShaderStorageBufferObject -> DOWNLOAD.destroy();
//                case DOWNLOAD -> ShaderStorageBufferObject.destroy();
//            }
//            mode = newMode;
//            if (isEnable) {
//                switchState();//open
//            }
//            Minecraft.getInstance().player.sendMessage(new TextComponent("switch to " + mode.modeName()), Util.NIL_UUID);
//        });
//    }
//
//    public static boolean isDataAvailable() {
//        return dataAvailable;
//    }
//
//    public static float[] getCurrentColor() {
//        if (enable) {
//            return Arrays.copyOf(currentColor, 3);
//        } else {
//            throw new RuntimeException("can't get current when eyedropper mode is disabled");
//        }
//    }
//
//    public static float[] getEyedropperColor() {
//        if (dataAvailable) {
//            return Arrays.copyOf(eyedropperColor, 3);
//        } else {
//            throw new RuntimeException("can't get eyedropper color while data is unavailable");
//        }
//    }
//
//}
