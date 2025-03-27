package com.lowdragmc.lowdraglib.compass;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.json.SimpleIGuiTextureJsonUtils;
import com.lowdragmc.lowdraglib.math.Position;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author KilaBash
 * @date 2023/7/27
 * @implNote CompassNode
 */
@Accessors(chain = true)
public class CompassNode {
    @Getter
    protected final JsonObject config;
    @Getter @Setter
    protected ResourceLocation nodeName;
    @Getter
    protected CompassSection section;
    @Getter @Setter
    protected Position position;
    @Getter
    protected int size;
    @Getter @Setter @Nullable
    protected IGuiTexture background, hoverBackground;
    @Getter @Setter
    protected IGuiTexture buttonTexture;
    @Getter
    protected Set<CompassNode> preNodes = new HashSet<>();
    @Getter
    protected Set<CompassNode> childNodes = new HashSet<>();
    protected List<Item> items;

    public CompassNode(ResourceLocation nodeName, JsonObject config) {
        this.config = config;
        this.nodeName = nodeName;
        this.background = config.has("background") ? SimpleIGuiTextureJsonUtils.fromJson(config.get("background").getAsJsonObject()) : null;
        this.hoverBackground = config.has("hover_background") ? SimpleIGuiTextureJsonUtils.fromJson(config.get("hover_background").getAsJsonObject()) : null;
        this.buttonTexture = SimpleIGuiTextureJsonUtils.fromJson(config.get("button_texture").getAsJsonObject());
        JsonArray position = config.get("position").getAsJsonArray();
        this.position = (Position.of(position.get(0).getAsInt(), position.get(1).getAsInt()));
        this.size = GsonHelper.getAsInt(config, "size", 24);
    }

    public JsonObject updateJson() {
        var pos = new JsonArray();
        pos.add(position.x);
        pos.add(position.y);
        config.add("position", pos);
        config.addProperty("section", section.sectionName.toString());
        if (size != 24) {
            config.addProperty("size", size);
        } else {
            config.remove("size");
        }
        if (background != null) {
            config.add("background", SimpleIGuiTextureJsonUtils.toJson(background));
        } else {
            config.remove("background");
        }
        if (hoverBackground != null) {
            config.add("hover_background", SimpleIGuiTextureJsonUtils.toJson(hoverBackground));
        } else {
            config.remove("hover_background");
        }
        config.add("button_texture", SimpleIGuiTextureJsonUtils.toJson(buttonTexture));
        if (preNodes.isEmpty()) {
            config.remove("pre_nodes");
        } else {
            var pre = new JsonArray();
            for (CompassNode node : preNodes) {
                pre.add(node.getNodeName().toString());
            }
            config.add("pre_nodes", pre);
        }
        return config;
    }

    public void setSection(CompassSection section) {
        this.section = section;
        this.section.addNode(this);
    }

    public void initRelation() {
        if (config.has("pre_nodes")) {
            JsonArray pre = config.get("pre_nodes").getAsJsonArray();
            for (JsonElement element : pre) {
                var nodeName = ResourceLocation.parse(element.getAsString());
                CompassNode node = CompassManager.INSTANCE.getNodeByName(nodeName);
                if (node != null) {
                    preNodes.add(node);
                    node.childNodes.add(this);
                }
            }
        }
    }

    @Override
    public final String toString() {
        return nodeName.toString();
    }

    public ResourceLocation getPage() {
        return ResourceLocation.parse(GsonHelper.getAsString(config, "page", "ldlib:missing"));
    }

    public List<Item> getItems() {
        if (items == null) {
            items = new ArrayList<>();
            JsonArray items = GsonHelper.getAsJsonArray(config, "items", new JsonArray());
            for (JsonElement element : items) {
                var data = element.getAsString();
                if (LDLib.isValidResourceLocation(data)) {
                    Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(data));
                    if (item != Items.AIR) {
                        this.items.add(item);
                    }
                } else if (data.startsWith("#") && LDLib.isValidResourceLocation(data.substring(1))) {
                    var tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(data.substring(1)));
                    var tagCollection = BuiltInRegistries.ITEM.getTag(tag);
                    tagCollection.ifPresent(named -> named.forEach(holder -> this.items.add(holder.value())));
                }
            }
        }
        return items;
    }

    public Component getChatComponent() {
        return Component.translatable(nodeName.toLanguageKey("compass.node"));
    }
}
