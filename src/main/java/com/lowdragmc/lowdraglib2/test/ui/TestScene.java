package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.DummyWorld;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.TrackedDummyWorld;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.appliedenergistics.yoga.YogaEdge;

@LDLRegisterClient(name="scene", registry = "ldlib2:screen_test")
@NoArgsConstructor
public class TestScene implements IScreenTest {

    public static DummyWorld createTestScene() {
        var dummyWorld = new TrackedDummyWorld();
        // prepare the dummy world
        dummyWorld.setBlockAndUpdate(new BlockPos(0, 0, 0), Blocks.GRASS_BLOCK.defaultBlockState());
        dummyWorld.setBlockAndUpdate(new BlockPos(1, 0, 0), Blocks.STONE.defaultBlockState());
        dummyWorld.setBlockAndUpdate(new BlockPos(2, 0, 0), Blocks.DIRT.defaultBlockState());
        dummyWorld.setBlockAndUpdate(new BlockPos(0, 1, 0), Blocks.OAK_LOG.defaultBlockState());
        dummyWorld.setBlockAndUpdate(new BlockPos(1, 1, 0), Blocks.OAK_LEAVES.defaultBlockState());
        dummyWorld.setBlockAndUpdate(new BlockPos(2, 1, 0), Blocks.COBBLESTONE.defaultBlockState());
        dummyWorld.setBlockAndUpdate(new BlockPos(0, 2, 0), Blocks.WATER.defaultBlockState());
        dummyWorld.setBlockAndUpdate(new BlockPos(1, 2, 0), Blocks.LAVA.defaultBlockState());
        dummyWorld.setBlockAndUpdate(new BlockPos(2, 2, 0), Blocks.BEDROCK.defaultBlockState());
        dummyWorld.setBlockAndUpdate(new BlockPos(0, 0, -1), Blocks.GLASS.defaultBlockState());
        dummyWorld.setBlockAndUpdate(new BlockPos(1, 0, -1), Blocks.GREEN_STAINED_GLASS.defaultBlockState());
        dummyWorld.setBlockAndUpdate(new BlockPos(2, 0, -1), Blocks.REDSTONE_BLOCK.defaultBlockState());
        dummyWorld.setBlockAndUpdate(new BlockPos(0, 1, -1), Blocks.CHEST.defaultBlockState());

        var entityPlayer = Minecraft.getInstance().player;
        if (entityPlayer != null && dummyWorld.getBlockEntity(new BlockPos(0, 1, -1)) instanceof ChestBlockEntity chest) {
            // add some items to the chest
            chest.startOpen(entityPlayer);
        }

        // add some entities
        var sheep = EntityType.SHEEP.create(dummyWorld);
        if (sheep != null) {
            sheep.setPos(0.5, 3, -1.5);
            dummyWorld.addEntity(sheep);
        }
        var item = EntityType.ITEM.create(dummyWorld);
        if (item != null) {
            item.setPos(1.5, 3, -0.5);
            item.setItem(Items.DIAMOND.getDefaultInstance());
            dummyWorld.addEntity(item);
        }
        return dummyWorld;
    }

    @Override
    public ModularUI createUI(Player entityPlayer) {
        var root = new UIElement();
        var scene = new Scene();
        var dummyWorld = createTestScene();
        root.layout(layout -> {
            layout.width(300);
            layout.height(300);
            layout.paddingAll(10);
        }).setId("root").getStyle().backgroundTexture(Sprites.BORDER);
        root.addChildren(scene
                .createScene(dummyWorld)
                .setTickWorld(true)
                .setRenderedCore(dummyWorld.getFilledBlocks().longStream().mapToObj(BlockPos::of).toList())
                .useCacheBuffer()
                .setClipContext(ClipContext.Block.VISUAL, ClipContext.Fluid.SOURCE_ONLY)
                .layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
        }), new Button().setOnClick(event -> {
            for (int i = 0; i < 50; i++) {
                var x = Math.random() * 2 - 1 + 2;
                var y = Math.random() * 2 - 1 + 2;
                var z = Math.random() * 2 - 1;
                var speedX = Math.random() * 0.1 - 0.05;
                var speedY = Math.random() * 0.1 - 0.05;
                var speedZ = Math.random() * 0.1 - 0.05;
                dummyWorld.addParticle(ParticleTypes.ASH, x, y, z, speedX, speedY, speedZ);
            }
        }).setText("spawn particles"), new Button().setOnClick(event -> scene.useOrtho(!scene.isUseOrtho())).setText("toggle ortho"));
        return new ModularUI(UI.of(root));
    }

}