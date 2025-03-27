package com.lowdragmc.lowdraglib.compass;

import com.google.gson.JsonParser;
import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.compass.component.*;
import com.lowdragmc.lowdraglib.compass.component.animation.Action;
import com.lowdragmc.lowdraglib.compass.component.animation.InformationAction;
import com.lowdragmc.lowdraglib.compass.component.animation.SceneAction;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.mojang.realmsclient.util.JsonUtils;
import lombok.val;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/9/3
 * @implNote LayoutComponentManager
 */
@OnlyIn(Dist.CLIENT)
public final class CompassManager implements ResourceManagerReloadListener {
    public final static CompassManager INSTANCE = new CompassManager();
    public final static int MAX_HOBER_TICK = 20;

    private int cHoverTick = 0;
    private long startedTick = Long.MAX_VALUE;
    private ItemStack lastStack = ItemStack.EMPTY;
    public boolean devMode = Platform.isDevEnv();
    private final Map<String, Supplier<ILayoutComponent>> COMPONENTS = new HashMap<>();
    private final Map<String, Function<Element, Action>> ACTION_CREATORS = new HashMap<>();
    private final Map<String, ICompassUIConfig> CONFIGS = new HashMap<>();

    private final Map<Item, Set<ResourceLocation>> itemLookup = new HashMap<>();
    public final Map<String, Map<ResourceLocation, CompassSection>> sections = new HashMap<>();
    public final Map<String, Map<ResourceLocation, CompassNode>> nodes = new HashMap<>();
    public final Map<ResourceLocation, Map<String, Document>> nodePages = new HashMap<>();

    private CompassManager() {
    }

    public void registerAction(String name, Function<Element, Action> creator) {
        ACTION_CREATORS.put(name, creator);
    }

    public void registerComponent(String name, Supplier<ILayoutComponent> clazz) {
        COMPONENTS.put(name, clazz);
    }

    public void registerUIConfig(String modID, ICompassUIConfig config) {
        CONFIGS.put(modID, config);
    }

    public void registerItemLookup(Item item, ResourceLocation nodeName) {
        itemLookup.computeIfAbsent(item, k -> new HashSet<>()).add(nodeName);
    }

    @Nullable
    public ILayoutComponent createComponent(String name, Element element) {
        var creator = COMPONENTS.get(name);
        return creator == null ? null : creator.get().fromXml(element);
    }

    @Nullable
    public Action createAction(Element element) {
        var creator = ACTION_CREATORS.get(element.getTagName());
        return creator == null ? null : creator.apply(element);
    }

    public ICompassUIConfig getUIConfig(String modID) {
        return CONFIGS.getOrDefault(modID, ICompassUIConfig.getDefault());
    }

    public void init() {
        registerComponent("text", TextBoxComponent::new);
        registerComponent("image", ImageComponent::new);
        for (HeaderComponent.Header header : HeaderComponent.Header.values()) {
            registerComponent(header.name(), HeaderComponent::new);
            registerComponent(header.name(), HeaderComponent::new);
            registerComponent(header.name(), HeaderComponent::new);
        }
        registerComponent("br", BlankComponent::new);
        registerComponent("recipe", RecipeComponent::new);
        registerComponent("scene", SceneComponent::new);
        registerComponent("ingredient", IngredientComponent::new);
        registerComponent("compass", CompassComponent::new);
        registerAction("scene", SceneAction::new);
        registerAction("information", InformationAction::new);
    }

    @Override
    public void onResourceManagerReload(@Nonnull ResourceManager resourceManager) {
        sections.clear();
        nodes.clear();
        nodePages.clear();

        for (var entry : resourceManager.listResources("compass/sections", rl -> rl.getPath().endsWith(".json")).entrySet()) {
            var key = entry.getKey();
            var resource = entry.getValue();
            try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                var path = key.getPath().replace("compass/sections/", "");
                path = path.substring(0, path.length() - 5);
                var section = new CompassSection(ResourceLocation.fromNamespaceAndPath(key.getNamespace(), path), JsonParser.parseReader(reader).getAsJsonObject());
                sections.computeIfAbsent(key.getNamespace(), k -> new HashMap<>()).put(section.sectionName, section);
            } catch (Exception e) {
                LDLib.LOGGER.error("loading compass section {} failed", entry.getKey(), e);
            }
        }

        for (var entry : resourceManager.listResources("compass/nodes", rl -> rl.getPath().endsWith(".json")).entrySet()) {
            var key = entry.getKey();
            var resource = entry.getValue();
            try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                var path = key.getPath().replace("compass/nodes/", "");
                path = path.substring(0, path.length() - 5);
                var node = new CompassNode(ResourceLocation.fromNamespaceAndPath(key.getNamespace(), path), JsonParser.parseReader(reader).getAsJsonObject());
                nodes.computeIfAbsent(key.getNamespace(), k -> new HashMap<>()).put(node.nodeName, node);
            } catch (Exception e) {
                LDLib.LOGGER.error("loading compass node {} failed", entry.getKey(), e);
            }
        }

        // init nodes' section
        for (var entry : nodes.entrySet()) {
            val iterator = entry.getValue().entrySet().iterator();
            while (iterator.hasNext()) {
                val node = iterator.next().getValue();
                var sectionName = ResourceLocation.parse(JsonUtils.getStringOr("section", node.getConfig(), "default"));
                var section = sections.getOrDefault(entry.getKey(), Collections.emptyMap()).get(sectionName);
                if (section != null) {
                    node.setSection(section);
                    node.getItems().forEach(item -> itemLookup.computeIfAbsent(item, k -> new HashSet<>()).add(node.nodeName));
                } else {
                    LDLib.LOGGER.error("node {}'s section {} not found", node.getNodeName(), sectionName);
                    iterator.remove();
                }
            }
        }

        // init relation
        for (Map<ResourceLocation, CompassNode> nodes : nodes.values()) {
            nodes.values().forEach(CompassNode::initRelation);
        }
    }

    public static void onComponentClick(String link, ClickData cd) {
        if (LDLib.isValidResourceLocation(link)) {
            CompassManager.INSTANCE.openCompass(ResourceLocation.parse(link));
        }
    }

    public void onCPressed(ItemStack itemStack) {
        var tick = Minecraft.getInstance().level.getGameTime();
        if (!ItemStack.isSameItemSameComponents(lastStack, itemStack)) {
            lastStack = itemStack;
            cHoverTick = 0;
            startedTick = tick;
        } else {
            cHoverTick = (int) (tick - startedTick);
        }
        if (cHoverTick < 0 || cHoverTick > MAX_HOBER_TICK) {
            lastStack = ItemStack.EMPTY;
            cHoverTick = 0;
            startedTick = tick;
        }
        if (cHoverTick == MAX_HOBER_TICK) {
            openCompass(getNodesByItem(itemStack.getItem()).toArray(new CompassNode[0]));
        }
    }

    public float getCHoverProgress() {
        return cHoverTick * 1f / MAX_HOBER_TICK;
    }

    public void clearCPressed() {
        cHoverTick = 0;
        startedTick = Long.MAX_VALUE;
        lastStack = ItemStack.EMPTY;
    }

    public void openCompass(ResourceLocation nodeLocation) {
        var node = nodes.getOrDefault(nodeLocation.getNamespace(), Collections.emptyMap()).getOrDefault(nodeLocation, null);
        if (node != null) {
            openCompass(node);
        }
    }

    public void openCompass(CompassNode... compassNodes) {
        var holder = new IUIHolder() {
            @Override
            public ModularUI createUI(Player entityPlayer) {
                return null;
            }

            @Override
            public boolean isInvalid() {
                return true;
            }

            @Override
            public boolean isRemote() {
                return true;
            }

            @Override
            public void markAsDirty() {

            }
        };

        ModularUI uiTemplate;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer entityPlayer = minecraft.player;

        if (compassNodes.length == 1) {
            uiTemplate = new ModularUI(holder, entityPlayer).widget(new CompassView(compassNodes[0]));
        } else if (compassNodes.length > 1) {
            uiTemplate = new ModularUI(210, 100, holder, entityPlayer).widget(new CompassSelectorWidget(Arrays.asList(compassNodes)));
        } else {
            return;
        }

        uiTemplate.initWidgets();
        ModularUIGuiContainer ModularUIGuiContainer = new ModularUIGuiContainer(uiTemplate, entityPlayer.containerMenu.containerId);
        minecraft.setScreen(ModularUIGuiContainer);
        entityPlayer.containerMenu = ModularUIGuiContainer.getMenu();
    }

    public boolean hasCompass(Item item) {
        return !getNodesByItem(item).isEmpty();
    }

    public List<CompassNode> getNodesByItem(Item item) {
        return itemLookup.getOrDefault(item, Collections.emptySet()).stream().map(nodeName -> nodes.getOrDefault(nodeName.getNamespace(), Collections.emptyMap()).get(nodeName)).toList();
    }

    @Nullable
    public CompassNode getNodeByName(ResourceLocation nodeName) {
        return nodes.getOrDefault(nodeName.getNamespace(), Collections.emptyMap()).get(nodeName);
    }

    @Nullable
    public CompassSection getSectionByName(ResourceLocation sectionName) {
        return sections.getOrDefault(sectionName.getNamespace(), Collections.emptyMap()).get(sectionName);
    }
}
