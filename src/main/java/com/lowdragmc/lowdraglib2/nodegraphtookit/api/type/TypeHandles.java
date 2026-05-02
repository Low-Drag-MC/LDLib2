package com.lowdragmc.lowdraglib2.nodegraphtookit.api.type;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ColorConfigurator;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

public class TypeHandles {
    public static final class Unknown { private Unknown() {} }
    public static final class ExecutionFlow { private ExecutionFlow() {} }
    public static final class Subgraph { private Subgraph() {} }
    public static final class MissingPort { private MissingPort() {} }

    public static final TypeHandle AUTOMATIC;
    public static final TypeHandle MISSING;
    public static final TypeHandle UNKNOWN;
    public static final TypeHandle EXECUTION_FLOW;
    public static final TypeHandle SUBGRAPH;

    public static final TypeHandle MISSING_PORT;

    public static final TypeHandle BOOL;
    public static final TypeHandle VOID;
    public static final TypeHandle CHAR;
    public static final TypeHandle DOUBLE;
    public static final TypeHandle FLOAT;
    public static final TypeHandle INT;
    public static final TypeHandle LONG;
    public static final TypeHandle OBJECT;
    public static final TypeHandle STRING;

    public static final TypeHandle COLOR;

    // Minecraft
    public static final TypeHandle DIRECTION;
    public static final TypeHandle BLOCK;
    public static final TypeHandle ITEM;
    public static final TypeHandle FLUID;
    public static final TypeHandle ENTITY_TYPE;
    public static final TypeHandle ITEM_STACK;
    public static final TypeHandle FLUID_STACK;


    static {
        // Normal type handles
        MISSING_PORT = TypeHandleHelpers.fromType(MissingPort.class);
        VOID = TypeHandleHelpers.fromType(Void.class);
        AUTOMATIC = TypeHandleHelpers.customType("AUTOMATIC", "Automatic");
        MISSING = TypeHandleHelpers.customType("MISSING_TYPE", null);
        UNKNOWN = TypeHandleHelpers.customType(Unknown.class, "UNKNOWN");
        EXECUTION_FLOW = TypeHandleHelpers.customType(ExecutionFlow.class, "EXECUTION_FLOW");
        SUBGRAPH = TypeHandleHelpers.customType(Subgraph.class, "SUBGRAPH");

        BOOL = TypeHandleHelpers.fromType(Boolean.class);
        TypeHandleHelpers.setCustomColorAndIcon(BOOL, 0xFF8c85ff, Icons.BOOL.copy().setColor(0xFF8c85ff));
        TypeHandleHelpers.setCustomDefaultValue(BOOL, () -> false);
        CHAR = TypeHandleHelpers.fromType(Character.class);
        TypeHandleHelpers.setCustomDefaultValue(CHAR, () -> '\0');
        DOUBLE = TypeHandleHelpers.fromType(Double.class);
        TypeHandleHelpers.setCustomColorAndIcon(DOUBLE, 0xFF10B4C5, Icons.FLOAT.copy().setColor(0xFF10B4C5));
        TypeHandleHelpers.setCustomDefaultValue(DOUBLE, () -> 0.0);
        FLOAT = TypeHandleHelpers.fromType(Float.class);
        TypeHandleHelpers.setCustomColorAndIcon(FLOAT, 0xFF10B4C5, Icons.FLOAT.copy().setColor(0xFF10B4C5));
        TypeHandleHelpers.setCustomDefaultValue(FLOAT, () -> 0.0f);
        INT = TypeHandleHelpers.fromType(Integer.class);
        TypeHandleHelpers.setCustomColorAndIcon(INT, 0xFF0C9EFF, Icons.INT.copy().setColor(0xFF0C9EFF));
        TypeHandleHelpers.setCustomDefaultValue(INT, () -> 0);
        LONG = TypeHandleHelpers.fromType(Long.class);
        TypeHandleHelpers.setCustomColorAndIcon(LONG, 0xFF0C9EFF, Icons.LONG.copy().setColor(0xFF0C9EFF));
        TypeHandleHelpers.setCustomDefaultValue(LONG, () -> 0L);
        STRING = TypeHandleHelpers.fromType(String.class);
        TypeHandleHelpers.setCustomColorAndIcon(STRING, 0xFFE3890B, Icons.STRING.copy().setColor(0xFFE3890B));
        TypeHandleHelpers.setCustomDefaultValue(STRING, () -> "");

        OBJECT = TypeHandleHelpers.fromType(Object.class);

        COLOR = TypeHandleHelpers.customType(Integer.class, "COLOR", "Color");
        TypeHandleHelpers.setCustomDefaultValue(COLOR, () -> -1);
        TypeHandleHelpers.setCustomIcon(COLOR, Icons.COLOR);
        TypeHandleHelpers.setCustomConfigurable(COLOR, (valueConfigurable, typeHandle) ->
                IConfigurable.create(group -> group.addConfigurator(new ColorConfigurator("",
                        valueConfigurable::getValue, valueConfigurable::setValue, -1,
                        valueConfigurable.forceUpdate()))));

        DIRECTION = TypeHandleHelpers.fromType(Direction.class);
        TypeHandleHelpers.setCustomColorAndIcon(DIRECTION, 0xFF5BFF94, Icons.MOVE.copy().setColor(0xFF5BFF94));
        BLOCK = TypeHandleHelpers.fromType(Block.class);
        TypeHandleHelpers.setCustomDefaultValue(BLOCK, () -> Blocks.STONE);
        ITEM = TypeHandleHelpers.fromType(Item.class);
        TypeHandleHelpers.setCustomDefaultValue(ITEM, () -> Items.AIR);
        FLUID = TypeHandleHelpers.fromType(Fluid.class);
        TypeHandleHelpers.setCustomDefaultValue(FLUID, () -> Fluids.EMPTY);
        ENTITY_TYPE = TypeHandleHelpers.fromType(EntityType.class);
        TypeHandleHelpers.setCustomDefaultValue(ENTITY_TYPE, () -> EntityType.PIG);
        ITEM_STACK = TypeHandleHelpers.fromType(ItemStack.class);
        TypeHandleHelpers.setCustomDefaultValue(ITEM_STACK, () -> ItemStack.EMPTY);
        FLUID_STACK = TypeHandleHelpers.fromType(FluidStack.class);
        TypeHandleHelpers.setCustomDefaultValue(FLUID_STACK, () -> FluidStack.EMPTY);
    }

    public static void init() {}
}
