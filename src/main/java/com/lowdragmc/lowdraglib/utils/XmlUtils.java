package com.lowdragmc.lowdraglib.utils;

import com.google.gson.JsonParser;
import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.misc.FluidHelper;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import lombok.val;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Vector3f;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.*;

/**
 * @author KilaBash
 * @date 2022/9/4
 * @implNote XmlUtils
 */
public class XmlUtils {
    public final static DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

    @Nullable
    public static Document loadXml(InputStream inputstream) {
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            return documentBuilder.parse(inputstream);
        } catch (Exception e) {
            return null;
        }
    }

    public static int getAsInt(Element element, String name, int defaultValue) {
        if (element.hasAttribute(name)) {
            try {
                return Integer.parseInt(element.getAttribute(name));
            } catch (Exception ignored) {

            }
        }
        return defaultValue;
    }

    public static long getAsLong(Element element, String name, long defaultValue) {
        if (element.hasAttribute(name)) {
            try {
                return Long.parseLong(element.getAttribute(name));
            } catch (Exception ignored) {

            }
        }
        return defaultValue;
    }

    public static boolean getAsBoolean(Element element, String name, boolean defaultValue) {
        if (element.hasAttribute(name)) {
            try {
                return Boolean.parseBoolean(element.getAttribute(name));
            } catch (Exception ignored) {

            }
        }
        return defaultValue;
    }

    public static String getAsString(Element element, String name, String defaultValue) {
        if (element.hasAttribute(name)) {
            return element.getAttribute(name);
        }
        return defaultValue;
    }

    public static float getAsFloat(Element element, String name, float defaultValue) {
        if (element.hasAttribute(name)) {
            try {
                return Float.parseFloat(element.getAttribute(name));
            } catch (Exception ignored) {

            }
        }
        return defaultValue;
    }

    public static int getAsColor(Element element, String name, int defaultValue) {
        if (element.hasAttribute(name)) {
            try {
                var value = Long.decode(element.getAttribute(name)).intValue();
                if (value != 0 && ((value & 0xff000000) == 0)) {
                    value = value | 0xff000000;
                }
                return value;
            } catch (Exception ignored) {

            }
        }
        return defaultValue;
    }

    public static BlockPos getAsBlockPos(Element element, String name, BlockPos defaultValue) {
        if (element.hasAttribute(name)) {
            String pos = getAsString(element, name, "0 0 0");
            try {
                var s = pos.split(" ");
                return new BlockPos(Integer.parseInt(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2]));
            } catch (Exception ignored) {}
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> T getAsEnum(Element element, String name, Class<T> enumClass, T defaultValue) {
        if (element.hasAttribute(name)) {
            try {
                String data = element.getAttribute(name);
                Enum<T>[] values = enumClass.getEnumConstants();
                for (Enum<T> value : values) {
                    if (value.name().equals(data)) {
                        return (T)value;
                    }
                }
            } catch (Exception ignored) {

            }
        }
        return defaultValue;
    }

    public static CompoundTag getCompoundTag(Element element) {
        NodeList nodeList = element.getChildNodes();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            String text = node.getTextContent().replaceAll("\\h*\\R+\\h*", " ");
            if (!text.isEmpty() && text.charAt(0) == ' ') {
                text = text.substring(1);
            }
            builder.append(text);
        }
        if (!builder.isEmpty()) {
            try {
                return TagParser.parseTag(builder.toString());
            } catch (CommandSyntaxException ignored) {}
        }
        return new CompoundTag();
    }

    public static DataComponentMap getComponents(Element element) {
        NodeList nodeList = element.getChildNodes();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            String text = node.getTextContent().replaceAll("\\h*\\R+\\h*", " ");
            if (!text.isEmpty() && text.charAt(0) == ' ') {
                text = text.substring(1);
            }
            builder.append(text);
        }
        if (!builder.isEmpty()) {
            return DataComponentMap.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(builder.toString())).result().orElse(DataComponentMap.EMPTY);
        }
        return DataComponentMap.EMPTY;
    }

    public static ItemStack getItemStack(Element element) {
        var ingredient = getIngredient(element);
        if (ingredient.ingredient.getItems().length > 0) {
            var stack = ingredient.ingredient.getItems()[0];
            stack.setCount(ingredient.count);
            return stack;
        }
        return ItemStack.EMPTY;
    }

    public static Vector3f getAsVector3f(Element element, String name, Vector3f defaultValue) {
        if (element.hasAttribute(name)) {
            String pos = getAsString(element, name, "0 0 0");
            try {
                var s = pos.split(" ");
                return new Vector3f(Float.parseFloat(s[0]), Float.parseFloat(s[1]), Float.parseFloat(s[2]));
            } catch (Exception ignored) {}
        }
        return defaultValue;
    }

    public static Vec2 getAsVec2(Element element, String name, Vec2 defaultValue) {
        if (element.hasAttribute(name)) {
            String pos = getAsString(element, name, "0 0");
            try {
                var s = pos.split(" ");
                return new Vec2(Float.parseFloat(s[0]), Float.parseFloat(s[1]));
            } catch (Exception ignored) {}
        }
        return defaultValue;
    }

    public static EntityInfo getEntityInfo(Element element) {
        int id = getAsInt(element, "id", LDLib.RANDOM.nextInt());
        EntityType<?> entityType = null;
        if (element.hasAttribute("type")) {
            entityType = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(element.getAttribute("type")));
        }
        CompoundTag tag = null;
        NodeList nodeList = element.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i) instanceof Element subElement && subElement.getNodeName().equals("nbt")) {
                tag = getCompoundTag(subElement);
                break;
            }
        }
        return new EntityInfo(id, entityType, tag);
    }

    public record SizedIngredient(Ingredient ingredient, int count) {};

    public static SizedIngredient getIngredient(Element element) {
        int count = getAsInt(element, "count", 1);
        var ingredient = new SizedIngredient(Ingredient.EMPTY, 0);
        if (element.hasAttribute("item")) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(element.getAttribute("item")));
            if (item != Items.AIR) {
                ItemStack itemStack = new ItemStack(item, count);
                NodeList nodeList = element.getChildNodes();
                for (int i = 0; i < nodeList.getLength(); i++) {
                    if (nodeList.item(i) instanceof Element subElement && subElement.getNodeName().equals("components")) {
                        itemStack.applyComponents(getComponents(subElement));
                        break;
                    }
                }
                ingredient = new SizedIngredient(Ingredient.of(itemStack), count);
            }
        } else if (Platform.isForge() && element.hasAttribute("forge-tag")){
            ingredient = new SizedIngredient(Ingredient.of(TagKey.create(Registries.ITEM, ResourceLocation.parse(element.getAttribute("forge-tag")))), count);
        } else if (!Platform.isForge() && element.hasAttribute("fabric-tag")) {
            ingredient = new SizedIngredient(Ingredient.of(TagKey.create(Registries.ITEM, ResourceLocation.parse(element.getAttribute("fabric-tag")))), count);
        } else if (element.hasAttribute("tag")) {
            ingredient = new SizedIngredient(Ingredient.of(TagKey.create(Registries.ITEM, ResourceLocation.parse(element.getAttribute("tag")))), count);
        }
        return ingredient;
    }

    public static FluidStack getFluidStack(Element element) {
        int amount = getAsInt(element, "amount", 1) * FluidHelper.getBucket() / 1000;
        FluidStack fluidStack = FluidStack.EMPTY;
        if (element.hasAttribute("fluid")) {
            var fluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(element.getAttribute("fluid")));
            if (fluid != Fluids.EMPTY) {
                fluidStack = new FluidStack(fluid, amount);
                NodeList nodeList = element.getChildNodes();
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node node = nodeList.item(i);
                    String text = node.getTextContent().replaceAll("\\h*\\R+\\h*", " ");
                    if (!text.isEmpty() && text.charAt(0) == ' ') {
                        text = text.substring(1);
                    }
                    builder.append(text);
                }
                if (!builder.isEmpty()) {
                    fluidStack.applyComponents(DataComponentMap.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(builder.toString())).result().orElse(DataComponentMap.EMPTY));
                }
            }
        }
        return fluidStack;
    }

    public static BlockInfo getBlockInfo(Element element) {
        BlockInfo blockInfo = BlockInfo.EMPTY;
        if (element.hasAttribute("block")) {
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(element.getAttribute("block")));
            if (block != Blocks.AIR) {
                var blockState = block.defaultBlockState();
                val nodeList = element.getChildNodes();
                CompoundTag tag = new CompoundTag();
                for (int i = 0; i < nodeList.getLength(); i++) {
                    var node = nodeList.item(i);
                    if (node instanceof Element subElement) {
                        if (subElement.getNodeName().equals("properties")) {
                            blockState = setBlockState(blockState, subElement);
                        } else if (subElement.getNodeName().equals("nbt")) {
                            tag = getCompoundTag(subElement);
                        }
                    }
                }
                blockInfo = BlockInfo.fromBlockState(blockState);
                if (!tag.isEmpty()) {
                    blockInfo.setTag(tag);
                }
            }
        }
        return blockInfo;
    }

    public static BlockState setBlockState(BlockState blockState, Element element) {
        StateDefinition<Block, BlockState> stateDefinition = blockState.getBlock().getStateDefinition();
        var name = getAsString(element, "name", "");
        var value = getAsString(element, "value", "");
        if (!name.isEmpty()) {
            var property = stateDefinition.getProperty(name);
            if (property != null) {
                blockState = setValueHelper(blockState, property, value);
            }
        }

        return blockState;
    }

    private static BlockState setValueHelper(BlockState stateHolder, Property property, String value) {
        var optional = property.getValue(value);
        if (optional.isPresent()) {
            return stateHolder.setValue(property, (Comparable) optional.get());
        }
        return stateHolder;
    }

    public static String getContent(Element element, boolean pretty) {
        NodeList nodeList = element.getChildNodes();
        StringBuilder builder = new StringBuilder();
        boolean lastNodeIsText = false;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.TEXT_NODE) {
                String text = node.getTextContent();
                if (pretty) {
                    text = text.replaceAll("\\h*\\R+\\h*", " ");
                }
                if (!lastNodeIsText && !text.isEmpty() && text.charAt(0) == ' ') {
                    text = text.substring(1);
                }
                builder.append(text);
                lastNodeIsText = false;
            } else if (node instanceof Element nodeElement) {
                var nodeName = nodeElement.getNodeName();
                lastNodeIsText = false;
                switch (nodeName) {
                    case "lang" -> {
                        var key = XmlUtils.getAsString(nodeElement, "key", "");
                        builder.append(LocalizationUtils.format(key));
                        lastNodeIsText = true;
                    }
                    case "br" -> builder.append('\n');
                }
            }
        }
        return builder.toString();
    }

    public static List<MutableComponent> getComponents(Element element, Style style) {
        NodeList nodeList = element.getChildNodes();
        List<MutableComponent> results = new ArrayList<>();
        MutableComponent component = null;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.TEXT_NODE) {
                String text = node.getTextContent();
                text = text.replaceAll("\\h*\\R+\\h*", " ");
                if (component == null) {
                    component = Component.literal(text).withStyle(style);
                } else {
                    component = component.append(Component.literal(text).withStyle(style));
                }
            } else if (node instanceof Element nodeElement) {
                var nodeName = nodeElement.getNodeName();
                switch (nodeName) {
                    case "lang" -> {
                        var key = XmlUtils.getAsString(nodeElement, "key", "");
                        if (component == null) {
                            component = Component.translatable(key).withStyle(style);
                        } else {
                            component = component.append(Component.translatable(key).withStyle(style));
                        }
                    }
                    case "br" -> {
                        results.add(Objects.requireNonNullElseGet(component, Component::empty));
                        component = Component.empty();
                    }
                    case "style" -> {
                        Style newStyle = style.withColor(style.getColor());
                        if (nodeElement.hasAttribute("color")) {
                            newStyle = newStyle.withColor(getAsColor(nodeElement, "color", 0XFFFFFFFF));
                        }
                        if (nodeElement.hasAttribute("bold")) {
                            newStyle = newStyle.withBold(getAsBoolean(nodeElement, "bold", true));
                        }
                        if (nodeElement.hasAttribute("font")) {
                            newStyle = newStyle.withFont(ResourceLocation.parse(nodeElement.getAttribute("font")));
                        }
                        if (nodeElement.hasAttribute("italic")) {
                            newStyle = newStyle.withItalic(getAsBoolean(nodeElement, "italic", true));
                        }
                        if (nodeElement.hasAttribute("underlined")) {
                            newStyle = newStyle.withUnderlined(getAsBoolean(nodeElement, "underlined", true));
                        }
                        if (nodeElement.hasAttribute("strikethrough")) {
                            newStyle = newStyle.withStrikethrough(getAsBoolean(nodeElement, "strikethrough", true));
                        }
                        if (nodeElement.hasAttribute("obfuscated")) {
                            newStyle = newStyle.withObfuscated(getAsBoolean(nodeElement, "obfuscated", true));
                        }
                        if (nodeElement.hasAttribute("hover-info")) {
                            newStyle = newStyle.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(nodeElement.getAttribute("hover-info"))));
                        }
                        if (nodeElement.hasAttribute("link")) {
                            newStyle = newStyle.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "@!" + nodeElement.getAttribute("link")));
                        }
                        if (nodeElement.hasAttribute("url-link")) {
                            newStyle = newStyle.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "@#" + nodeElement.getAttribute("url-link")));
                        }
                        var components = getComponents(nodeElement, newStyle);
                        for (int j = 0; j < components.size(); j++) {
                            if (j == 0) {
                                if (component != null) {
                                    component.append(components.get(j));
                                } else {
                                    component = components.get(j);
                                }
                            } else {
                                results.add(component);
                                component = components.get(j);
                            }
                        }
                    }
                }
            }
        }
        if (component != null) {
            results.add(component);
        }
        return results;
    }
}
