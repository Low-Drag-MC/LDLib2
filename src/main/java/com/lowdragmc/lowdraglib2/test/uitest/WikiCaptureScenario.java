package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.client.LDLibClientConfig;
import com.lowdragmc.lowdraglib2.gui.LDLibFonts;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Slider;
import com.lowdragmc.lowdraglib2.gui.ui.elements.StructuredTagEditor;
import com.lowdragmc.lowdraglib2.gui.ui.elements.VirtualItemHeightMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.VirtualScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.stream.IntStream;

/** Produces stable, real-client screenshots consumed by the LDLib2 Wiki. */
@LDLRegisterClient(name = "wiki_captures", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class WikiCaptureScenario implements UIScenario {

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(120).tags("wiki", "visual").requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("slider wiki capture", WikiCaptureScenario::sliderUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#slider_demo")
                .screenshot("01_slider_page")
                .screenshotElement("01_slider_component", "#slider_demo")
                .closeScreen()

                .openModularUI("virtual scroller wiki capture", WikiCaptureScenario::virtualScrollerUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#virtual_scroller_demo")
                .screenshot("02_virtual_scroller_page")
                .screenshotElement("02_virtual_scroller_component", "#virtual_scroller_demo")
                .closeScreen()

                .openModularUI("structured tag wiki capture", WikiCaptureScenario::structuredTagUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#structured_tag_demo")
                .step("hover the root expand arrow", ctx -> {
                    var bounds = ctx.el("#structured_tag_editor").bounds();
                    ctx.input().moveTo(bounds.x() + 11, bounds.y() + 12);
                })
                .step("press the root expand arrow", ctx -> {
                    var bounds = ctx.el("#structured_tag_editor").bounds();
                    ctx.input().mouseDown(bounds.x() + 11, bounds.y() + 12, Keys.MOUSE_LEFT);
                })
                .step("release the root expand arrow", ctx -> {
                    var bounds = ctx.el("#structured_tag_editor").bounds();
                    ctx.input().mouseUp(bounds.x() + 11, bounds.y() + 12, Keys.MOUSE_LEFT);
                })
                .screenshot("03_structured_tag_page")
                .screenshotElement("03_structured_tag_component", "#structured_tag_demo")
                .closeScreen()

                .openModularUI("font wiki capture", WikiCaptureScenario::fontUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#font_demo")
                .screenshot("04_font_rendering")
                .screenshotElement("04_font_component", "#font_demo")
                .closeScreen();
    }

    private static ModularUI sliderUI(TestContext ctx) {
        var horizontal = new Slider.Horizontal();
        horizontal.setId("volume_slider");
        horizontal.setRange(0, 100).setValue(68f);
        horizontal.layout(layout -> layout.width(260));
        horizontal.sliderStyle(style -> style.trackSize(2).handleSize(8));

        var vertical = new Slider.Vertical();
        vertical.setId("balance_slider");
        vertical.setRange(-1, 1).setValue(0.25f);
        vertical.layout(layout -> layout.height(120));
        vertical.sliderStyle(style -> style.trackSize(2).handleSize(8));

        var panel = panel("slider_demo", "Slider — range, fill and draggable handle");
        var row = new UIElement();
        row.layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(18).alignItems(AlignItems.CENTER));
        row.addChildren(
                new UIElement().layout(layout -> layout.gapAll(6)).addChildren(
                        new Label().setText("Volume: 68 / 100"), horizontal,
                        new Label().setText("Mouse wheel and track click are supported")),
                vertical
        );
        panel.addChild(row);
        return modular(ctx, panel);
    }

    private static ModularUI virtualScrollerUI(TestContext ctx) {
        var items = IntStream.rangeClosed(1, 500).mapToObj(i -> "Resource entry %03d".formatted(i)).toList();
        var list = new VirtualScrollerView<String>();
        list.setId("resource_list");
        list.setItems(items)
                .setItemUIProvider(value -> new Label().setText(value).layout(layout -> layout.height(14)));
        list.layout(layout -> layout.width(300).height(180));
        list.virtualScrollerViewStyle(style -> style
                .itemHeightMode(VirtualItemHeightMode.FIXED)
                .estimatedItemHeight(14)
                .overscanPixels(28));

        var panel = panel("virtual_scroller_demo", "VirtualScrollerView — 500 items");
        panel.addChildren(list, new Label().setText("Only rows near the viewport exist in the UI tree."));
        return modular(ctx, panel);
    }

    private static ModularUI structuredTagUI(TestContext ctx) {
        var root = new CompoundTag();
        root.putString("name", "LDLib2 Wiki Capture");
        root.putInt("version", 2235);
        root.put("bytes", new ByteArrayTag(new byte[]{1, 2, 3, 4}));
        root.put("samples", new IntArrayTag(new int[]{16, 32, 64}));
        var modes = new ListTag();
        modes.add(StringTag.valueOf("VANILLA"));
        modes.add(StringTag.valueOf("SDF"));
        modes.add(StringTag.valueOf("RASTER"));
        modes.add(StringTag.valueOf("AUTO"));
        root.put("font_modes", modes);

        var editor = new StructuredTagEditor();
        editor.setId("structured_tag_editor");
        editor.setCompoundTagOnly().setValue(root, false);
        editor.layout(layout -> layout.width(390).height(220));

        var panel = panel("structured_tag_demo", "StructuredTagEditor — typed NBT tree");
        panel.addChild(editor);
        return modular(ctx, panel);
    }

    private static ModularUI fontUI(TestContext ctx) {
        var panel = panel("font_demo", "Font rendering — " + LDLibClientConfig.fontRenderMode());
        panel.layout(layout -> layout.width(480));

        panel.addChildren(
                new Label().setText("Unicode fallback: 中文 日本語 한국어 العربية Ελληνικά")
                        .textStyle(style -> style.fontSize(14).adaptiveHeight(true)),
                new Label().setText(Component.literal("Bold  ").withStyle(ChatFormatting.BOLD)
                                .append(Component.literal("Italic  ").withStyle(ChatFormatting.ITALIC))
                                .append(Component.literal("Underlined  ").withStyle(ChatFormatting.UNDERLINE))
                                .append(Component.literal("Coloured").withStyle(ChatFormatting.GOLD)))
                        .textStyle(style -> style.fontSize(14).adaptiveHeight(true)),
                new Label().setText("Small 9 px — crisp interface text")
                        .textStyle(style -> style.fontSize(9).adaptiveHeight(true)),
                new Label().setText("Medium 16 px — smooth scaling")
                        .textStyle(style -> style.fontSize(16).adaptiveHeight(true)),
                new Label().setText("Large 28 px")
                        .textStyle(style -> style.fontSize(28).adaptiveHeight(true)),
                new Label().setText("minecraft:default — The quick brown fox 0123456789")
                        .textStyle(style -> style.fontSize(11).adaptiveHeight(true)),
                new Label().setText("ldlib2:jetbrains_mono_bold — The quick brown fox 0123456789")
                        .textStyle(style -> style.font(LDLibFonts.JETBRAINS_MONO_BOLD)
                                .fontSize(11).adaptiveHeight(true)),
                new Label().setText("Modes: VANILLA · SDF · RASTER · AUTO")
                        .textStyle(style -> style.fontSize(11).textColor(0xffaaaaaa).adaptiveHeight(true))
        );
        return modular(ctx, panel);
    }

    private static UIElement panel(String id, String title) {
        var root = new UIElement().setId(id).addClass("panel_bg");
        root.layout(layout -> layout.paddingAll(10).gapAll(8).widthAuto().heightAuto());
        root.addChild(new Label().setText(title)
                .textStyle(style -> style.fontSize(12).textColor(0xff88ccff).textShadow(true)));
        return root;
    }

    private static ModularUI modular(TestContext ctx, UIElement panel) {
        var root = new UIElement();
        root.layout(layout -> layout.widthPercent(100).heightPercent(100)
                .alignItems(AlignItems.CENTER).justifyContent(AlignContent.CENTER));
        root.addChild(panel);
        return new ModularUI(UI.of(root,
                StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.GDP)), ctx.player());
    }
}
