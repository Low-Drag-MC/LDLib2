package com.lowdragmc.lowdraglib.compass;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.json.SimpleIGuiTextureJsonUtils;
import com.mojang.realmsclient.util.JsonUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * @author KilaBash
 * @date 2023/7/27
 * @implNote CompassNode
 */
public class CompassSection {
    @Getter
    private final JsonObject config;
    @Getter @Setter
    public ResourceLocation sectionName;
    public int priority;
    public final Map<ResourceLocation, CompassNode> nodes;

    @Getter @Setter
    protected IGuiTexture buttonTexture;
    @Getter @Setter
    protected IGuiTexture backgroundTexture;

    public CompassSection(ResourceLocation sectionName, JsonObject config) {
        this.config = config;
        this.sectionName = sectionName;
        this.nodes = new HashMap<>();
        this.priority = JsonUtils.getIntOr("priority", config, 0);
        this.setButtonTexture(SimpleIGuiTextureJsonUtils.fromJson(config.get("button_texture").getAsJsonObject()));
        this.setBackgroundTexture(config.has("background_texture") ? SimpleIGuiTextureJsonUtils.fromJson(config.get("background_texture").getAsJsonObject()): null);
    }

    public JsonObject updateJson() {
        config.addProperty("priority", priority);
        config.add("button_texture", SimpleIGuiTextureJsonUtils.toJson(buttonTexture));
        if (backgroundTexture != null) {
            config.add("background_texture", SimpleIGuiTextureJsonUtils.toJson(backgroundTexture));
        }
        return config;
    }

    public void addNode(CompassNode compassNode) {
        nodes.put(compassNode.getNodeName(), compassNode);
    }

    @Override
    public final String toString() {
        return sectionName.toString();
    }

    public CompassNode getNode(ResourceLocation nodeName) {
        return nodes.get(nodeName);
    }

    public Component getChatComponent() {
        return Component.translatable(sectionName.toLanguageKey("compass.section"));
    }
}
