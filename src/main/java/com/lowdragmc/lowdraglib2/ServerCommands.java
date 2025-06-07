package com.lowdragmc.lowdraglib2;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.lowdragmc.lowdraglib2.gui.factory.UIEditorFactory;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.block.Blocks;

/**
 * @author KilaBash
 * @date 2023/2/9
 * @implNote ServerCommands
 */
public class ServerCommands {
	public static List<LiteralArgumentBuilder<CommandSourceStack>> createServerCommands() {
		return List.of(
				Commands.literal("ldlib2")
						.then(Commands.literal("ui_editor")
								.executes(context -> {
									UIEditorFactory.INSTANCE.openUI(UIEditorFactory.INSTANCE,
											context.getSource().getPlayerOrException());
									return 1;
								}))
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
				Commands.literal("compass_server").then(Commands.literal("build_scene")
						.then(Commands.argument("start", BlockPosArgument.blockPos())
								.then(Commands.argument("end", BlockPosArgument.blockPos())
										.executes(context -> runBuildScene(context, false, new BlockPos(0, 0, 0)))
										.then(Commands.argument("saveNbt", BoolArgumentType.bool())
												.executes(context -> runBuildScene(context,
														BoolArgumentType.getBool(context, "saveNbt"),
														new BlockPos(0, 0, 0)))
												.then(Commands.argument("offset", BlockPosArgument.blockPos())
														.executes(context -> runBuildScene(context,
																BoolArgumentType.getBool(context, "saveNbt"),
																BlockPosArgument.getBlockPos(context, "offset")))))))));
	}

	public static int runBuildScene(CommandContext<CommandSourceStack> context, boolean saveNbt, BlockPos offset) {

		var start = BlockPosArgument.getBlockPos(context, "start");
		var end = BlockPosArgument.getBlockPos(context, "end");
		var world = context.getSource().getLevel();

		int smallestX = start.getX() <= end.getX() ? start.getX() : end.getX();
		int smallestY = start.getY() <= end.getY() ? start.getY() : end.getY();
		int smallestZ = start.getZ() <= end.getZ() ? start.getZ() : end.getZ();

		int largestX = start.getX() >= end.getX() ? start.getX() : end.getX();
		int largestY = start.getY() >= end.getY() ? start.getY() : end.getY();
		int largestZ = start.getZ() >= end.getZ() ? start.getZ() : end.getZ();

		int offsetX = -((largestX - smallestX) / 2) + offset.getX();
		int offsetY = offset.getY();
		int offsetZ = -((largestZ - smallestZ) / 2) + offset.getZ();

		ArrayList<String> nodes = new ArrayList<>();

		for (int x = smallestX; x <= largestX; x++) {
			for (int y = smallestY; y <= largestY; y++) {
				for (int z = smallestZ; z <= largestZ; z++) {
					var block = world.getBlockState(new BlockPos(x, y, z));
					var blockentity = world
							.getBlockEntity(new BlockPos(x, y, z));
					if (block.getBlock() != Blocks.AIR) {
						String id = BuiltInRegistries.BLOCK
								.getKey(block.getBlock()).toString();
						nodes.add(
								String.format(
										"<add pos=\"%d %d %d\" block=\"%s\">",
										x - smallestX + offsetX, y - smallestY + offsetY,
										z - smallestZ + offsetZ, id));
						nodes.addAll(block.getValues().entrySet().stream()
								.map(e -> String.format(
										"<properties name=\"%s\" value=\"%s\" />",
										e.getKey().getName(),
										e.getValue().toString()))
								.collect(Collectors.toList()));
						if (saveNbt && blockentity != null) {
							var tag = blockentity.saveWithoutMetadata(context.getSource().registryAccess());
							nodes.add("<nbt>");
							nodes.add(NbtUtils.toPrettyComponent(tag)
									.getString());
							nodes.add("</nbt>");
						}
						nodes.add("</add>");
					}
				}
			}
		}

		var text = nodes.stream().collect(Collectors.joining("\n"));

		context.getSource().sendSuccess(() -> Component
				.literal("[Copy XML to clipboard]")
				.withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)
						.withClickEvent(new ClickEvent(
								ClickEvent.Action.COPY_TO_CLIPBOARD,
								text))),
				true);

		return 1;

	}
}
