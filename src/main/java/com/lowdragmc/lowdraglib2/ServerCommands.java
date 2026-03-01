package com.lowdragmc.lowdraglib2;

import java.util.ArrayList;
import java.util.List;

import com.lowdragmc.lowdraglib2.gui.editor.UIEditor;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/**
 * @author KilaBash
 * @date 2023/2/9
 * @implNote ServerCommands
 */
public class ServerCommands {
	public static List<LiteralArgumentBuilder<CommandSourceStack>> createServerCommands() {
        var commands = new ArrayList<LiteralArgumentBuilder<CommandSourceStack>>();
        commands.addAll(List.of(
                Commands.literal("ldlib2_utils")
						.then(Commands.literal("copy_block_tag")
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.executes(context -> {
											var pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
											var world = context.getSource().getLevel();
											var blockEntity = world.getBlockEntity(pos);
											if (blockEntity != null) {
												var tag = blockEntity.saveWithoutMetadata(context.getSource().registryAccess());
												var value = NbtUtils.structureToSnbt(tag);
												context.getSource().sendSuccess(() -> Component
														.literal("[Copy to clipboard]")
														.withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)
																.withClickEvent(new ClickEvent(
																		ClickEvent.Action.COPY_TO_CLIPBOARD, value)))
														.append(NbtUtils.toPrettyComponent(tag)), true);
											} else {
												context.getSource().sendSuccess(
														() -> Component.literal("No block entity at " + pos)
																.withStyle(Style.EMPTY.withColor(ChatFormatting.RED)),
														true);
											}
											return 1;
										})))
						.then(Commands.literal("copy_entity_tag")
								.then(Commands.argument("entity", EntityArgument.entity())
										.executes(context -> {
											var entity = EntityArgument.getEntity(context, "entity");
											var tag = entity.saveWithoutId(new CompoundTag());
											var value = NbtUtils.structureToSnbt(tag);
											context.getSource().sendSuccess(() -> Component
													.literal("[Copy to clipboard]")
													.withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)
															.withClickEvent(new ClickEvent(
																	ClickEvent.Action.COPY_TO_CLIPBOARD, value)))
													.append(NbtUtils.toPrettyComponent(tag)), true);
											return 1;
										}))),
                Commands.literal("ldlib2_ui_editor").requires(s -> s.getServer().isSingleplayer())
                        .executes(context -> {
                    if (!context.getSource().getServer().isSingleplayer()) {
                        context.getSource().sendFailure(Component.literal("This command can only be used in singleplayer"));
                        return 0;
                    }
                    if (context.getSource().getPlayer() == null) return 0;
                    PlayerUIMenuType.openUI(context.getSource().getPlayer(), UIEditor.WINDOW_ID);
                    return 1;
                })
        ));
        if (LDLib2Registries.MENU_TESTS != null && !LDLib2Registries.MENU_TESTS.values().isEmpty()) {
            commands.add(createMenuTestCommands());
        }
        return commands;
	}

    private static LiteralArgumentBuilder<CommandSourceStack> createMenuTestCommands() {
        var builder = Commands.literal("ldlib2_menu_test");
        if (LDLib2Registries.MENU_TESTS == null) {
            return builder;
        }
        for (var uiTest : LDLib2Registries.MENU_TESTS) {
            builder = builder.then(Commands.literal(uiTest.annotation().name())
                    .executes(context -> {
                        var player = context.getSource().getPlayer();
                        PlayerUIMenuType.openUI(player, LDLib2.id(uiTest.annotation().name()));
                        return 1;
                    }));
        }
        return builder;
    }

}
