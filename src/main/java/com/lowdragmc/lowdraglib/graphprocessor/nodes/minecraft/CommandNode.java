package com.lowdragmc.lowdraglib.graphprocessor.nodes.minecraft;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.trigger.LinearTriggerNode;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nonnull;

@LDLRegister(name = "command", group = "graph_processor.node.minecraft", registry = "ldlib:graph_node")
public class CommandNode extends LinearTriggerNode {
    @InputPort
    public String command;
    @InputPort
    public Level level;
    @InputPort
    public Vector3f xyz;
    @OutputPort
    public String output;

    @Override
    public int getMinWidth() {
        return 100;
    }

    @Override
    public void process() {
        if (level instanceof ServerLevel serverLevel) {
            performCommand(serverLevel);
        }
    }

    public void performCommand(ServerLevel serverLevel) {
        output = "";
        if (this.command != null) {
            var minecraftServer = serverLevel.getServer();
            if (minecraftServer.isCommandBlockEnabled() && !StringUtil.isNullOrEmpty(this.command)) {
                try {
                    var commandSourceStack = new CommandSourceStack(
                            new CommandSource() {
                                public void sendSystemMessage(@Nonnull Component component) {
                                    output = component.getString();
                                }

                                public boolean acceptsSuccess() {
                                    return false;
                                }

                                public boolean acceptsFailure() {
                                    return false;
                                }

                                public boolean shouldInformAdmins() {
                                    return false;
                                }
                            },
                            xyz == null ? Vec3.atLowerCornerOf(serverLevel.getSharedSpawnPos()) : new Vec3(xyz.x, xyz.y, xyz.z),
                            Vec2.ZERO, serverLevel, 5,
                            "Server",
                            Component.literal("Server"),
                            minecraftServer, null);
                    minecraftServer.getCommands().performPrefixedCommand(commandSourceStack, this.command);
                } catch (Throwable var6) {
                    LDLib.LOGGER.error("Error while executing command: {}", this.command, var6);
                }
            }
        }
    }

}
