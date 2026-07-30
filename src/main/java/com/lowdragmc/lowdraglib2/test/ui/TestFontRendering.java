package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.gui.LDLibFonts;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextArea;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.NoArgsConstructor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Visual check for the smooth (SDF) font renderer. Everything on this screen is something the distance field
 * pipeline can get wrong, so it is meant to be eyeballed rather than asserted:
 * <ul>
 *     <li>font sizes from tiny to huge, checking weight stays consistent and edges stay smooth;</li>
 *     <li>a non integer scale, checking there is no aliasing or bleeding between atlas neighbours;</li>
 *     <li>scripts a custom font does not cover, checking the unifont fallback kicks in instead of tofu boxes;</li>
 *     <li>every text style, since bold, italic and the effect quads take different paths;</li>
 *     <li>editable fields, where a caret that drifts from the glyphs would mean advances no longer match.</li>
 * </ul>
 */
@LDLRegisterClient(name = "font_rendering", registry = "ldlib2:screen_test")
@NoArgsConstructor
public class TestFontRendering implements IScreenTest {

    @Override
    public ModularUI createUI(Player entityPlayer) {
        var root = new UIElement().addClass("panel_bg");
        root.getLayout().flexDirection(FlexDirection.ROW);
        root.addChildren(
                column(sizeRamp(), scaledBlock()),
                column(fallbackScripts(), styles()),
                column(fonts(), editable())
        );
        return new ModularUI(UI.of(root, StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.GDP)));
    }

    private UIElement column(UIElement... children) {
        var scroller = new ScrollerView();
        scroller.layout(layout -> layout.flex(1).marginAll(2));
        scroller.viewContainer(view -> {
            view.layout(layout -> layout.gapAll(4));
            view.addChildren(children);
        });
        return scroller;
    }

    private UIElement group(String title, UIElement... children) {
        var group = new UIElement().addClass("preview_bg");
        group.layout(layout -> layout.gapAll(2).paddingAll(2));
        group.addChild(new Label().setText(title)
                .textStyle(style -> style.textColor(0xff88ccff).textShadow(true)));
        group.addChildren(children);
        return group;
    }

    /**
     * The same string at many sizes. A pure distance field tends to look thin at the small end, so this is
     * where the sdfWeight and sdfSharpness settings get tuned.
     */
    private UIElement sizeRamp() {
        var group = group("size ramp");
        for (var size : new float[]{6, 9, 12, 18, 32, 64}) {
            group.addChild(new Label().setText("Handgloves " + (int) size)
                    .textStyle(style -> style.fontSize(size).adaptiveHeight(true).adaptiveWidth(true)));
        }
        return group;
    }

    /**
     * Non integer scales are exactly what the vanilla nearest filtered atlas cannot do cleanly.
     */
    private UIElement scaledBlock() {
        var group = group("non integer scale");
        for (var scale : new float[]{0.5f, 1.37f, 2.5f, 3.7f}) {
            var block = new UIElement();
            block.layout(layout -> layout.height(14 * scale));
            block.style(style -> style.transform2D(Transform2D.identity().pivot(0, 0).scale(scale)));
            block.addChild(new Label().setText("scale x" + scale)
                    .textStyle(style -> style.adaptiveWidth(true).adaptiveHeight(true)));
            group.addChild(block);
        }
        return group;
    }

    /**
     * None of these live in jetbrains_mono_bold, and most are not in the vanilla bitmap sheets either, so they
     * can only render if the unifont fallback at the end of the chain is working.
     */
    private UIElement fallbackScripts() {
        var group = group("unicode fallback (no tofu boxes)");
        var samples = List.of(
                "中文 日本語 한국어",
                "Кириллица Ελληνικά",
                "עברית العربية",
                "ไทย देवनागरी",
                "→ ← ↑ ↓ ★ ☆ ♪ ✓ ∑ ∞"
        );
        for (var sample : samples) {
            group.addChild(new Label().setText(sample)
                    .textStyle(style -> style.fontSize(14).adaptiveHeight(true).adaptiveWidth(true)));
        }
        // the same scripts through a font that definitely does not contain them
        group.addChild(new Label().setText("中文 via jetbrains: 中文 日本語")
                .textStyle(style -> style.font(LDLibFonts.JETBRAINS_MONO_BOLD).fontSize(14)
                        .adaptiveHeight(true).adaptiveWidth(true)));
        return group;
    }

    /**
     * Bold redraws the glyph twice, italic shears the quad, and the underline, strikethrough and background
     * quads come from the reserved white block in the atlas rather than from a glyph.
     */
    private UIElement styles() {
        var group = group("styles");
        group.addChildren(
                styled(Component.literal("plain")),
                styled(Component.literal("bold").withStyle(ChatFormatting.BOLD)),
                styled(Component.literal("italic").withStyle(ChatFormatting.ITALIC)),
                styled(Component.literal("underlined").withStyle(ChatFormatting.UNDERLINE)),
                styled(Component.literal("strikethrough").withStyle(ChatFormatting.STRIKETHROUGH)),
                styled(Component.literal("obfuscated").withStyle(ChatFormatting.OBFUSCATED)),
                styled(Component.literal("coloured").withStyle(ChatFormatting.GOLD)),
                styled(Component.literal("bold italic underlined")
                        .withStyle(ChatFormatting.BOLD)
                        .withStyle(ChatFormatting.ITALIC)
                        .withStyle(ChatFormatting.UNDERLINE))
        );
        var shadow = new Label().setText("with drop shadow");
        shadow.textStyle(style -> style.fontSize(16).textShadow(true).adaptiveHeight(true).adaptiveWidth(true));
        group.addChild(shadow);
        return group;
    }

    private Label styled(Component component) {
        var label = new Label();
        label.setText(component);
        label.textStyle(style -> style.fontSize(14).adaptiveHeight(true).adaptiveWidth(true));
        return label;
    }

    /**
     * A bitmap backed font and an outline backed font side by side, both going through the same atlas.
     */
    private UIElement fonts() {
        var group = group("fonts");
        group.addChildren(
                fontSample("minecraft:default", Style.DEFAULT_FONT),
                fontSample("ldlib2:jetbrains_mono_bold", LDLibFonts.JETBRAINS_MONO_BOLD)
        );
        return group;
    }

    private UIElement fontSample(String title, ResourceLocation font) {
        var element = new UIElement();
        element.layout(layout -> layout.gapAll(1));
        element.addChildren(
                new Label().setText(title).textStyle(style -> style.fontSize(8).textColor(0xffaaaaaa)),
                new Label().setText("The quick brown fox jumps over 0123456789")
                        .textStyle(style -> style.font(font).fontSize(12)
                                .textWrap(TextWrap.WRAP).adaptiveHeight(true))
        );
        return element;
    }

    /**
     * If the caret or the selection highlight drifts away from the glyphs, the SDF advances no longer match
     * what the splitter measured, which is the single most likely way this pipeline can break.
     */
    private UIElement editable() {
        var group = group("caret alignment");
        var field = new TextField();
        field.setText("click and drag to select");
        field.textFieldStyle(style -> style.fontSize(12));
        var bigField = new TextField();
        bigField.setText("larger font size");
        bigField.layout(layout -> layout.height(24));
        bigField.textFieldStyle(style -> style.fontSize(18));
        var area = new TextArea();
        area.setLines(List.of("multi line editing", "中文 fallback line", "third line"));
        area.layout(layout -> layout.height(60));
        group.addChildren(field, bigField, area);
        return group;
    }
}
