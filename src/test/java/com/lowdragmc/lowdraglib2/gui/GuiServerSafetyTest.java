package com.lowdragmc.lowdraglib2.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.RectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.AnimationTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.FluidStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.ShaderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.texture.TransformTexture;
import com.lowdragmc.lowdraglib2.gui.texture.UIResourceTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ColorSelector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.GraphView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SearchComponent;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextArea;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SplitView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TreeList;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.CodeEditor;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import net.neoforged.api.distmarker.OnlyIn;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiServerSafetyTest {
    @Test
    void modularUiDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(ModularUI.class);
    }

    @Test
    void modularUiDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(ModularUI.class);
    }

    @Test
    void uiElementDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(UIElement.class);
    }

    @Test
    void uiElementDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(UIElement.class);
    }

    @Test
    void uiElementSourceDoesNotReferenceClientRenderingTypes() throws IOException {
        assertSourceDoesNotContain("src/main/java/com/lowdragmc/lowdraglib2/gui/ui/UIElement.java",
                "net.minecraft.client.",
                "Minecraft.getInstance()",
                "ScreenRectangle");
    }

    @Test
    void textElementDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(TextElement.class);
    }

    @Test
    void textElementSourceDoesNotReferenceClientRenderingTypes() throws IOException {
        assertSourceDoesNotContain("src/main/java/com/lowdragmc/lowdraglib2/gui/ui/elements/TextElement.java",
                "GUIContext",
                "net.minecraft.client.",
                "Minecraft.getInstance()");
    }

    @Test
    void tabDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(Tab.class);
    }

    @Test
    void tabDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(Tab.class);
    }

    @Test
    void buttonDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(Button.class);
    }

    @Test
    void buttonDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(Button.class);
    }

    @Test
    void textFieldDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(TextField.class);
    }

    @Test
    void textFieldDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(TextField.class);
    }

    @Test
    void textFieldSourceDoesNotReferenceClientRenderingTypes() throws IOException {
        assertSourceDoesNotContain("src/main/java/com/lowdragmc/lowdraglib2/gui/ui/elements/TextField.java",
                "gui.ui.rendering.GUIContext",
                "net.minecraft.client.",
                "Minecraft.getInstance()",
                "KeyEvent(");
    }

    @Test
    void textAreaDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(TextArea.class);
    }

    @Test
    void textAreaDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(TextArea.class);
    }

    @Test
    void textAreaSourceDoesNotReferenceClientRenderingTypes() throws IOException {
        assertSourceDoesNotContain("src/main/java/com/lowdragmc/lowdraglib2/gui/ui/elements/TextArea.java",
                "gui.ui.rendering.GUIContext",
                "net.minecraft.client.",
                "Minecraft.getInstance()",
                "KeyEvent(",
                "RenderSystem.",
                "RenderPipelines",
                "Screen.");
    }

    @Test
    void codeEditorDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(CodeEditor.class);
    }

    @Test
    void codeEditorDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(CodeEditor.class);
    }

    @Test
    void styledLineSourceDoesNotReferenceClientRenderingTypes() throws IOException {
        assertSourceDoesNotContain("src/main/java/com/lowdragmc/lowdraglib2/gui/ui/elements/codeeditor/StyledLine.java",
                "net.minecraft.client.",
                "gui.Font");
    }

    @Test
    void colorSelectorDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(ColorSelector.class);
    }

    @Test
    void colorSelectorDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(ColorSelector.class);
    }

    @Test
    void graphViewDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(GraphView.class);
    }

    @Test
    void graphViewDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(GraphView.class);
    }

    @Test
    void itemSlotDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(ItemSlot.class);
    }

    @Test
    void itemSlotDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(ItemSlot.class);
    }

    @Test
    void itemSlotSourceDoesNotReferenceClientRenderingTypes() throws IOException {
        assertSourceDoesNotContain("src/main/java/com/lowdragmc/lowdraglib2/gui/ui/elements/ItemSlot.java",
                "net.minecraft.client.",
                "gui.ui.rendering.GUIContext",
                "Minecraft.getInstance()",
                "DrawerHelper");
    }

    @Test
    void fluidSlotDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(FluidSlot.class);
    }

    @Test
    void fluidSlotDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(FluidSlot.class);
    }

    @Test
    void progressBarDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(ProgressBar.class);
    }

    @Test
    void progressBarDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(ProgressBar.class);
    }

    @Test
    void searchComponentDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(SearchComponent.class);
    }

    @Test
    void searchComponentDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(SearchComponent.class);
    }

    @Test
    void selectorDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(Selector.class);
    }

    @Test
    void selectorDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(Selector.class);
    }

    @Test
    void splitViewDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(SplitView.class);
    }

    @Test
    void splitViewDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(SplitView.class);
    }

    @Test
    void treeListDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(TreeList.class);
    }

    @Test
    void treeListDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(TreeList.class);
    }

    @Test
    void sceneDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(Scene.class);
    }

    @Test
    void sceneDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(Scene.class);
    }

    @Test
    void hoverTooltipsDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(HoverTooltips.class);
    }

    @Test
    void hoverTooltipsSourceDoesNotReferenceClientRenderingTypes() throws IOException {
        assertSourceDoesNotContain("src/main/java/com/lowdragmc/lowdraglib2/gui/ui/event/HoverTooltips.java",
                "net.minecraft.client.",
                "ClientTooltipComponent",
                "ClientTooltipPositioner");
    }

    @Test
    void uiSoundUtilsSourceDoesNotReferenceClientRenderingTypes() throws IOException {
        assertSourceDoesNotContain("src/main/java/com/lowdragmc/lowdraglib2/gui/util/UISoundUtils.java",
                "net.minecraft.client.",
                "Minecraft.getInstance()",
                "SimpleSoundInstance");
    }

    @Test
    void localizationUtilsSourceDoesNotReferenceClientRenderingTypes() throws IOException {
        assertSourceDoesNotContain("src/main/java/com/lowdragmc/lowdraglib2/utils/LocalizationUtils.java",
                "net.minecraft.client.",
                "import net.minecraft.client.resources.language.I18n;");
    }

    @Test
    void fluidHelperSourceDoesNotReferenceClientRenderingTypes() throws IOException {
        assertSourceDoesNotContain("src/main/java/com/lowdragmc/lowdraglib2/utils/FluidHelper.java",
                "net.minecraft.client.",
                "@OnlyIn",
                "IClientFluidTypeExtensions",
                "TextureAtlasSprite");
    }

    @Test
    void transformTextureDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(TransformTexture.class);
    }

    @Test
    void transformTextureDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(TransformTexture.class);
    }

    @Test
    void iGuiTextureSourceDoesNotReferenceClientRenderingTypes() throws IOException {
        assertSourceDoesNotContain("src/main/java/com/lowdragmc/lowdraglib2/gui/texture/IGuiTexture.java",
                "gui.ui.rendering.GUIContext",
                "net.minecraft.client.",
                "@OnlyIn",
                " void draw(",
                "createPreview(",
                "buildConfigurator(");
    }

    @Test
    void transformTextureSourceDoesNotReferenceClientRenderingTypes() throws IOException {
        assertSourceDoesNotContain("src/main/java/com/lowdragmc/lowdraglib2/gui/texture/TransformTexture.java",
                "gui.ui.rendering.GUIContext",
                "net.minecraft.client.",
                "@OnlyIn",
                "drawInternal(",
                "preDraw(",
                "postDraw(",
                " void draw(");
    }

    @Test
    void rectTextureDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(RectTexture.class);
    }

    @Test
    void rectTextureDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(RectTexture.class);
    }

    @Test
    void rectTextureSourceDoesNotReferenceClientRenderingTypes() throws IOException {
        assertSourceDoesNotContain("src/main/java/com/lowdragmc/lowdraglib2/gui/texture/RectTexture.java",
                "gui.ui.rendering.GUIContext",
                "net.minecraft.client.",
                "@OnlyIn",
                "drawInternal(",
                "drawFill(",
                "drawBorder(");
    }

    @Test
    void colorRectTextureDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(ColorRectTexture.class);
    }

    @Test
    void colorRectTextureSourceDoesNotReferenceClientRenderingTypes() throws IOException {
        assertSourceDoesNotContain("src/main/java/com/lowdragmc/lowdraglib2/gui/texture/ColorRectTexture.java",
                "gui.ui.rendering.GUIContext",
                "net.minecraft.client.",
                "@OnlyIn",
                "drawInternal(");
    }

    @Test
    void colorBorderTextureDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(ColorBorderTexture.class);
    }

    @Test
    void colorBorderTextureSourceDoesNotReferenceClientRenderingTypes() throws IOException {
        assertSourceDoesNotContain("src/main/java/com/lowdragmc/lowdraglib2/gui/texture/ColorBorderTexture.java",
                "gui.ui.rendering.GUIContext",
                "net.minecraft.client.",
                "@OnlyIn",
                "drawInternal(");
    }

    @Test
    void spriteTextureDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(SpriteTexture.class);
    }

    @Test
    void spriteTextureDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(SpriteTexture.class);
    }

    @Test
    void spriteTextureSourceDoesNotReferenceClientRenderingTypes() throws IOException {
        assertSourceDoesNotContain("src/main/java/com/lowdragmc/lowdraglib2/gui/texture/SpriteTexture.java",
                "gui.ui.rendering.GUIContext",
                "net.minecraft.client.",
                "@OnlyIn",
                "drawInternal(",
                "drawQuad(",
                "getImageSize(",
                "createPreview(",
                "drawRawTextureGuides(");
    }

    @Test
    void animationTextureDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(AnimationTexture.class);
    }

    @Test
    void animationTextureDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(AnimationTexture.class);
    }

    @Test
    void animationTextureSourceDoesNotReferenceClientRenderingTypes() throws IOException {
        assertSourceDoesNotContain("src/main/java/com/lowdragmc/lowdraglib2/gui/texture/AnimationTexture.java",
                "gui.ui.rendering.GUIContext",
                "net.minecraft.client.",
                "@OnlyIn",
                "updateTick(",
                "drawInternal(",
                "createPreview(",
                "drawRawTextureGuides(");
    }

    @Test
    void itemStackTextureDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(ItemStackTexture.class);
    }

    @Test
    void itemStackTextureDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(ItemStackTexture.class);
    }

    @Test
    void itemStackTextureSourceDoesNotReferenceClientRenderingTypes() throws IOException {
        assertSourceDoesNotContain("src/main/java/com/lowdragmc/lowdraglib2/gui/texture/ItemStackTexture.java",
                "gui.ui.rendering.GUIContext",
                "net.minecraft.client.",
                "@OnlyIn",
                "updateTick(",
                "drawInternal(");
    }

    @Test
    void fluidStackTextureDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(FluidStackTexture.class);
    }

    @Test
    void fluidStackTextureDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(FluidStackTexture.class);
    }

    @Test
    void fluidStackTextureSourceDoesNotReferenceClientRenderingTypes() throws IOException {
        assertSourceDoesNotContain("src/main/java/com/lowdragmc/lowdraglib2/gui/texture/FluidStackTexture.java",
                "gui.ui.rendering.GUIContext",
                "net.minecraft.client.",
                "@OnlyIn",
                "updateTick(",
                "drawInternal(");
    }

    @Test
    void guiTextureGroupDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(GuiTextureGroup.class);
    }

    @Test
    void guiTextureGroupDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(GuiTextureGroup.class);
    }

    @Test
    void guiTextureGroupSourceDoesNotReferenceClientRenderingTypes() throws IOException {
        assertSourceDoesNotContain("src/main/java/com/lowdragmc/lowdraglib2/gui/texture/GuiTextureGroup.java",
                "gui.ui.rendering.GUIContext",
                "net.minecraft.client.",
                "@OnlyIn",
                "drawInternal(");
    }

    @Test
    void uiResourceTextureDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(UIResourceTexture.class);
    }

    @Test
    void uiResourceTextureDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(UIResourceTexture.class);
    }

    @Test
    void uiResourceTextureSourceDoesNotReferenceClientRenderingTypes() throws IOException {
        assertSourceDoesNotContain("src/main/java/com/lowdragmc/lowdraglib2/gui/texture/UIResourceTexture.java",
                "gui.ui.rendering.GUIContext",
                "net.minecraft.client.",
                "@OnlyIn",
                "drawInternal(");
    }

    @Test
    void textTextureDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(TextTexture.class);
    }

    @Test
    void textTextureDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(TextTexture.class);
    }

    @Test
    void textTextureSourceDoesNotReferenceClientRenderingTypes() throws IOException {
        assertSourceDoesNotContain("src/main/java/com/lowdragmc/lowdraglib2/gui/texture/TextTexture.java",
                "gui.ui.rendering.GUIContext",
                "net.minecraft.client.",
                "@OnlyIn",
                "drawInternal(",
                "drawTextLine(",
                "drawRollTextLine(",
                "updateTick(");
    }

    @Test
    void shaderTextureDoesNotUseMemberLevelOnlyInAnnotations() {
        assertNoMemberLevelOnlyIn(ShaderTexture.class);
    }

    @Test
    void shaderTextureDoesNotExposeClientTypesInSharedApi() {
        assertNoClientTypeExposure(ShaderTexture.class);
    }

    @Test
    void shaderTextureSourceDoesNotReferenceClientRenderingTypes() throws IOException {
        assertSourceDoesNotContain("src/main/java/com/lowdragmc/lowdraglib2/gui/texture/ShaderTexture.java",
                "gui.ui.rendering.GUIContext",
                "net.minecraft.client.",
                "@OnlyIn",
                "drawInternal(");
    }

    private void assertNoMemberLevelOnlyIn(Class<?> type) {
        List<String> annotatedFields = Arrays.stream(type.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(OnlyIn.class))
                .map(Field::getName)
                .toList();
        List<String> annotatedMethods = Arrays.stream(type.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(OnlyIn.class))
                .map(Method::getName)
                .distinct()
                .toList();

        assertTrue(annotatedFields.isEmpty(), "Expected no member-level @OnlyIn fields on " + type.getSimpleName() + ", found: " + annotatedFields);
        assertTrue(annotatedMethods.isEmpty(), "Expected no member-level @OnlyIn methods on " + type.getSimpleName() + ", found: " + annotatedMethods);
    }

    private void assertNoClientTypeExposure(Class<?> type) {
        List<String> clientFields = Arrays.stream(type.getDeclaredFields())
                .filter(field -> isClientType(field.getType()))
                .map(field -> field.getName() + ":" + field.getType().getName())
                .toList();
        List<String> clientMethods = Arrays.stream(type.getDeclaredMethods())
                .filter(this::hasClientTypeInSignature)
                .map(this::describeMethod)
                .distinct()
                .toList();
        List<String> clientNestedClasses = Arrays.stream(type.getDeclaredClasses())
                .filter(this::isClientType)
                .map(Class::getName)
                .toList();

        assertTrue(clientFields.isEmpty(), "Expected no client-only field types on " + type.getSimpleName() + ", found: " + clientFields);
        assertTrue(clientMethods.isEmpty(), "Expected no client-only method signatures on " + type.getSimpleName() + ", found: " + clientMethods);
        assertTrue(clientNestedClasses.isEmpty(), "Expected no client-only nested classes on " + type.getSimpleName() + ", found: " + clientNestedClasses);
    }

    private boolean hasClientTypeInSignature(Method method) {
        if (isClientType(method.getReturnType())) {
            return true;
        }
        return Arrays.stream(method.getParameterTypes()).anyMatch(this::isClientType);
    }

    private boolean isClientType(Class<?> type) {
        String name = type.getName();
        if (name.equals("com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext")) {
            return false;
        }
        return name.startsWith("net.minecraft.client.")
                || name.startsWith("com.lowdragmc.lowdraglib2.gui.ui.rendering.")
                || type.isAnnotationPresent(OnlyIn.class)
                || name.contains(".gui.holder.")
                || name.equals("com.lowdragmc.lowdraglib2.gui.ui.ModularUI$ModularUIWidget");
    }

    private String describeMethod(Method method) {
        return method.getName() + Arrays.toString(method.getParameterTypes()) + "->" + method.getReturnType().getName();
    }

    private void assertSourceDoesNotContain(String path, String... forbiddenSnippets) throws IOException {
        var root = Path.of(System.getProperty("user.dir")).normalize();
        while (root != null && !Files.exists(root.resolve("build.gradle")) && !Files.exists(root.resolve("build.gradle.kts"))) {
            root = root.getParent();
        }
        assertTrue(root != null, "Could not locate project root from " + System.getProperty("user.dir"));
        var sourcePath = root.resolve(path).normalize();
        var source = Files.readString(sourcePath);
        var found = Arrays.stream(forbiddenSnippets)
                .filter(source::contains)
                .toList();
        assertTrue(found.isEmpty(), "Expected source " + sourcePath + " not to contain client-only snippets, found: " + found);
    }
}
