package com.lowdragmc.lowdraglib2.test;

import com.lowdragmc.lowdraglib2.CommonProxy;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.FlexIcons;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.annotation.*;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncPersistRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaGutter;
import org.appliedenergistics.yoga.YogaJustify;

import java.util.List;


public class TestBlockEntity extends BlockEntity implements ISyncPersistRPCBlockEntity {
    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Persisted
    @DescSynced
    @UpdateListener(methodName = "onIntValueChanged")
    @ConditionalSynced(methodName = "shouldSyncIntValue")
    private int intValue = 10;
    @Persisted
    @DescSynced
    @DropSaved
    private ItemStack itemStack = ItemStack.EMPTY;

    public TestBlockEntity(BlockPos pWorldPosition, BlockState pBlockState) {
        super(CommonProxy.TEST_BE_TYPE.get(), pWorldPosition, pBlockState);
    }

    private void onIntValueChanged(int oldValue, int newValue) {
        LDLib2.LOGGER.info("Int value changed from {} to {}", oldValue, newValue);
    }

    private boolean shouldSyncIntValue(int value) {
        return value > 0;
    }

    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        var root = new UIElement().layout(layout -> layout
                .setPadding(YogaEdge.ALL, 4)
                .setGap(YogaGutter.ALL, 2)
                .setJustifyContent(YogaJustify.CENTER)
        ).addClass("panel_bg");
        root.addChild(new Label().setText("Test Block UI"));
        root.addChild(new TextField());
        root.addChild(new Button().setText("Change Random Value").setOnServerClick(e -> {
            intValue = (int) (Math.random() * 100);
            itemStack = new ItemStack(BuiltInRegistries.ITEM.getRandom(LDLib2.RANDOM).orElse(Items.APPLE.builtInRegistryHolder()));
        }));
        root.addChild(
                new UIElement().layout(layout -> layout.widthPercent(100).flexDirection(YogaFlexDirection.ROW))
                        .addChildren(
                                new Button().setText("+").setOnServerClick(e -> intValue++).layout(l -> l.flex(1)),
                                new Button().setText("-").setOnServerClick(e -> intValue--).layout(l -> l.flex(1))
                        )
        );
        root.addChild(new ItemSlot().slotStyle(slotStyle -> slotStyle.slotOverlay(FlexIcons.ALIGN_CONTENTS_CENTER_ROW)).bindDataSource(SupplierDataSource.of(() -> itemStack)));
        root.addChild(new Label().bindDataSource(SupplierDataSource.of(() -> Component.literal(String.valueOf(intValue)))));
        root.addChild(new Button().setText("Test C2S RPC").setOnClick(e -> rpcToServer("rpcTest", "Hello from client!")));
        root.addChild(new Button().setText("Test C2S RPC").setOnServerClick(e -> rpcToTracking("rpcTest", "Hello from server!")));
        root.addChild(new Button().setText("Test C2S RPC Packet").setOnClick(e -> RPCPacketDistributor.rpcToServer("rpcPacketTest", "Hello from client!", true)));
        root.addChild(new Button().setText("Test C2S RPC Packet").setOnServerClick(e -> RPCPacketDistributor.rpcToAllPlayers("rpcPacketTest", "Hello from server!", false)));
        return new ModularUI(UI.of(root, List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC))), holder.player);
    }

    @RPCMethod
    public void rpcTest(RPCSender sender, String message) {
        if (sender.isServer()) {
            LDLib2.LOGGER.info("Received RPC from server: {}", message);
        } else {
            LDLib2.LOGGER.info("Received RPC from client: {}", message);
        }
    }

    @RPCPacket("rpcPacketTest")
    public static void rpcPacketTest(RPCSender sender, String message, boolean message2) {
        if (sender.isServer()) {
            LDLib2.LOGGER.info("Received RPC packet from server: {}, {}", message, message2);
        } else {
            LDLib2.LOGGER.info("Received RPC packet from client: {}, {}", message, message2);
        }
    }
}
