package com.lowdragmc.lowdraglib2.test.xei;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.ScrollDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import com.lowdragmc.lowdraglib2.test.TestItem;
import com.lowdragmc.lowdraglib2.test.ui.TestScene;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaWrap;

import java.util.ArrayList;
import java.util.List;

public class TestRecipe {
    public static int WIDTH = 170;
    public static int HEIGHT = 120;

    public ModularUI createModularUI() {
        var dummyWorld = TestScene.createTestScene();
        var allLogs = new ArrayList<ItemStack>();
        BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.LOGS).forEach(item -> allLogs.add(new ItemStack(item.value())));
        return ModularUI.of(UI.of(
                new UIElement().layout(layout -> layout.widthPercent(100).heightPercent(100)).addChildren(
                        new SplitView.Horizontal().left(new ScrollerView().addScrollViewChildren(
                                new UIElement().layout(layout -> layout.gapAll(3)).addChildren(
                                        // items
                                        new UIElement().addChildren(
                                                new ItemSlot()
                                                        .xeiRecipeIngredient(IngredientIO.INPUT, allLogs::stream)
                                                        .xeiRecipeSlot(IngredientIO.INPUT, 1, 1, allLogs::stream)
                                                        .bindDataSource(ScrollDataSource.of((allLogs)))
                                                        .style(style -> style.tooltips("this is additional tooltips")),
                                                new ItemSlot().setItem(Items.STONE.getDefaultInstance())
                                                        .xeiRecipeIngredient(IngredientIO.INPUT)
                                                        .xeiRecipeSlot(IngredientIO.INPUT, 0.3f),
                                                new UIElement().layout(layout -> layout.height(18).aspectRatio(1))
                                                        .style(style -> style.backgroundTexture(Icons.RIGHT_ARROW_NO_BAR)),
                                                new ItemSlot().setItem(new ItemStack(Items.CHEST, 2))
                                                        .xeiRecipeIngredient(IngredientIO.OUTPUT)
                                                        .xeiRecipeSlot()
                                        ).layout(layout -> layout.flexDirection(FlexDirection.ROW).wrap(FlexWrap.WRAP)),
                                        // fluids
                                        new UIElement().addChildren(
                                                new FluidSlot().setFluid(new FluidStack(Fluids.WATER, 1000))
                                                        .xeiRecipeIngredient(IngredientIO.INPUT)
                                                        .xeiRecipeSlot(),
                                                new UIElement().layout(layout -> layout.height(18).aspectRatio(1))
                                                        .style(style -> style.backgroundTexture(Icons.RIGHT_ARROW_NO_BAR)),
                                                new FluidSlot().setFluid(new FluidStack(Fluids.LAVA, 1000))
                                                        .xeiRecipeIngredient(IngredientIO.OUTPUT)
                                                        .xeiRecipeSlot(),
                                                new FluidSlot().setFluid(new FluidStack(Fluids.WATER, 30))
                                                        .xeiRecipeIngredient(IngredientIO.OUTPUT)
                                                        .xeiRecipeSlot(IngredientIO.OUTPUT, 0.7f)
                                        ).layout(layout -> layout.flexDirection(FlexDirection.ROW).wrap(FlexWrap.WRAP)),
                                        new Button().setOnClick(event -> {
                                            for (int i = 0; i < 50; i++) {
                                                var x = Math.random() * 2 - 1 + 2;
                                                var y = Math.random() * 2 - 1 + 2;
                                                var z = Math.random() * 2 - 1;
                                                var speedX = Math.random() * 0.1 - 0.05;
                                                var speedY = Math.random() * 0.1 - 0.05;
                                                var speedZ = Math.random() * 0.1 - 0.05;
                                                dummyWorld.addParticle(ParticleTypes.ANGRY_VILLAGER, x, y, z, speedX, speedY, speedZ);
                                            }
                                        }),
                                        new ItemSlot().setItem(TestItem.ITEM.getDefaultInstance())
                                                .xeiRecipeIngredient(IngredientIO.CATALYST)
                                                .xeiRecipeSlot(),
                                        new Label().setText("This is a Test")
                                )
                        ).layout(layout -> layout.widthPercent(100).heightPercent(100)).addClass("panel_bg")).right(new UIElement().layout(layout -> layout.widthPercent(100).heightPercent(100)).addChildren(
                            // scene
                                new Scene().createScene(dummyWorld)
                                        .useOrtho()
                                        .setOrthoRange(.5f)
                                        .setTickWorld(true)
                                        .setRenderedCore(dummyWorld.getFilledBlocks().longStream().mapToObj(BlockPos::of).toList())
                                        .useCacheBuffer()
                                        .layout(layout -> layout.widthPercent(100).heightPercent(100))
                        ).addClass("panel_bg")).setPercentage(50)
                )
        , List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC))));
    }
}
