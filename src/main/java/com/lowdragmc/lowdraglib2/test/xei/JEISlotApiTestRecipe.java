package com.lowdragmc.lowdraglib2.test.xei;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.RectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Focused manual cases for the recipe-slot capabilities requested in JEI issue #4442.
 */
public enum JEISlotApiTestRecipe {
    EXTERNAL_RENDERING,
    DISPLAYED_INGREDIENT,
    TRANSFORMED_HOVER,
    TOOLTIP;

    public static final int WIDTH = 170;
    public static final int HEIGHT = 84;

    public ModularUI createModularUI() {
        return switch (this) {
            case EXTERNAL_RENDERING -> externalRendering();
            case DISPLAYED_INGREDIENT -> displayedIngredient();
            case TRANSFORMED_HOVER -> transformedHover();
            case TOOLTIP -> tooltip();
        };
    }

    private static ModularUI externalRendering() {
        var slot = new FluidSlot();
        slot.setCapacity(4000);
        slot.setFluid(new FluidStack(Fluids.WATER, 1000));
        slot.layout(layout -> layout.width(72).height(24).paddingAll(2));
        slot.style(style -> style.backgroundTexture(new RectTexture()
                .setColor(0xff25364d)
                .setBorderColor(0xff69a7d8)
                .setStroke(1)
                .setRadius(new Vector4f(6))));
        slot.xeiRecipeSlot(IngredientIO.INPUT, 1);

        return page(
                "1. External rendering",
                "Expected: one LDLib-rendered 72x24 fluid bar.",
                centered(slot)
        );
    }

    private static ModularUI displayedIngredient() {
        var allLogs = allLogs();
        var displayedIndex = new int[]{0};
        var status = label("JEI cycles until the first button click", 6);
        var slot = new ItemSlot()
                .setItem(allLogs.getFirst())
                .xeiRecipeSlot(IngredientIO.INPUT, 1, 1, allLogs::stream);
        var changeSource = new Button()
                .setText("Next log")
                .setOnClick(event -> {
                    do {
                        displayedIndex[0] = (displayedIndex[0] + 1) % allLogs.size();
                    } while (allLogs.size() > 1 && ItemStack.matches(allLogs.get(displayedIndex[0]), slot.getValue()));
                    slot.setItem(allLogs.get(displayedIndex[0]));
                    status.setText("Showing tag member " + (displayedIndex[0] + 1) + " / " + allLogs.size());
                })
                .textStyle(style -> style.fontSize(6).textColor(ColorPattern.BLACK.color).textShadow(false));
        changeSource.layout(layout -> layout.width(82).height(14));

        return page(
                "2. Dynamic tag display",
                "Click repeatedly; hover for JEI's #minecraft:logs grid.",
                new UIElement().layout(layout -> layout
                                .flexDirection(FlexDirection.ROW)
                                .gapAll(6)
                                .height(22))
                        .addChildren(slot, changeSource),
                status
        );
    }

    private static ModularUI transformedHover() {
        var slot = new ItemSlot() {
            @Override
            public boolean isIntersectWithPoint(double localX, double localY) {
                var radiusX = getSizeWidth() / 2d;
                var radiusY = getSizeHeight() / 2d;
                var x = (localX - getPositionX() - radiusX) / radiusX;
                var y = (localY - getPositionY() - radiusY) / radiusY;
                return x * x + y * y <= 1;
            }
        };
        slot.setItem(Items.ENDER_PEARL.getDefaultInstance());
        slot.layout(layout -> layout
                .width(42)
                .height(22)
                .paddingVertical(3)
                .paddingHorizontal(12));
        slot.style(style -> style.backgroundTexture(new RectTexture()
                .setColor(0xff315c49)
                .setBorderColor(0xff8ce0b8)
                .setStroke(1)
                .setRadius(new Vector4f(11))));
        slot.transform(transform -> transform.rotation(25));
        slot.xeiRecipeSlot(IngredientIO.INPUT, 1);

        return page(
                "3. Transformed custom hover",
                "Only the rotated pill is hoverable; its corners are not.",
                centered(slot)
        );
    }

    private static ModularUI tooltip() {
        var allLogs = allLogs();
        var slot = new ItemSlot()
                .setItem(allLogs.getFirst())
                .xeiRecipeSlot(IngredientIO.INPUT, 1, 1, allLogs::stream)
                .style(style -> style.tooltips(Component.literal("LDLib appended tooltip line")
                        .withStyle(ChatFormatting.AQUA)));

        return page(
                "4. JEI tooltip + LDLib text",
                "Hover for JEI's log tag tooltip and the aqua line.",
                centered(slot)
        );
    }

    private static ModularUI page(String title, String expected, UIElement... content) {
        var root = new UIElement().layout(layout -> layout
                        .widthPercent(100)
                        .heightPercent(100)
                        .paddingAll(6)
                        .gapAll(5))
                .addClass("panel_bg")
                .addChildren(label(title, 7), label(expected, 6));
        root.addChildren(content);
        return ModularUI.of(UI.of(
                root,
                List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC))
        ));
    }

    private static UIElement centered(UIElement element) {
        return new UIElement().layout(layout -> layout
                        .flexDirection(FlexDirection.ROW)
                        .justifyContent(AlignContent.CENTER)
                        .height(30))
                .addChild(element);
    }

    private static Label label(String text, float fontSize) {
        var label = new Label();
        label.setText(text);
        label.textStyle(style -> style
                .fontSize(fontSize)
                .textColor(ColorPattern.BLACK.color)
                .textShadow(false)
                .textWrap(TextWrap.WRAP)
                .adaptiveHeight(true));
        label.layout(layout -> layout.widthPercent(100));
        return label;
    }

    private static ArrayList<ItemStack> allLogs() {
        var logs = new ArrayList<ItemStack>();
        BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.LOGS)
                .forEach(item -> logs.add(new ItemStack(item.value())));
        if (logs.isEmpty()) {
            logs.add(Items.OAK_LOG.getDefaultInstance());
        }
        return logs;
    }
}
