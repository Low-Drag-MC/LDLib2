package com.lowdragmc.lowdraglib2.editor.ui.resource;

import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.editor.resource.IResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceInstance;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SplitView;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ResourceContainer<T> extends UIElement {
    public final SplitView.Horizontal splitView = new SplitView.Horizontal();
    public final ScrollerView providerList = new ScrollerView();
    public final UIElement providerContainer = new UIElement();
    public final ResourceInstance<T> resourceInstance;
    public final Editor editor;

    // runtime
    private final Map<IResourceProvider<T>, UIElement> providerToggles = new java.util.HashMap<>();
    @Getter
    @Nullable
    private IResourceProvider<T> selectedProvider = null;
    @Nullable @Setter
    private Consumer<T> onResourceSelect;

    public ResourceContainer(ResourceInstance<T> resourceInstance, Editor editor) {
        getLayout().flex(1);
        getLayout().heightPercent(100);
        getLayout().flexDirection(FlexDirection.ROW);

        this.resourceInstance = resourceInstance;
        this.editor = editor;
        addChildren(splitView.left(new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
        }).addChildren(providerList.layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
        }))).right(providerContainer.layout(layout -> {
            layout.heightPercent(100);
            layout.widthPercent(100);
        })).setPercentage(13));

        this.providerList.addEventListener(UIEvents.MOUSE_DOWN, this::onProviderListMouseDown);

        loadResource();
    }

    protected void onProviderListMouseDown(UIEvent event) {
        if (event.button == 1) {
            editor.openMenu(event.x, event.y, createMenu());
        }
    }

    protected TreeBuilder.Menu createMenu() {
        var menu = TreeBuilder.Menu.start();
        menu.branch("ldlib.gui.editor.menu.new", m -> {
            for (var type : LDLib2Registries.RESOURCE_PROVIDER_TYPES) {
                if (type.supportCustom()) {
                    m.leaf(type.getIcon(), type.getTypeName(), () -> type.onCreateCustom(this));
                }
            }
        });
        if (selectedProvider != null && selectedProvider.getType().supportCustom() &&
                resourceInstance.getCustomProviders().getOrDefault(selectedProvider.getType(), Collections.emptyList()).contains(selectedProvider)) {
            menu.leaf(Icons.REMOVE, "ldlib.gui.editor.menu.remove", () -> {
                Dialog.showCheckBox("ldlib.gui.resource.remove_provider", "editor.remove.confirm", result -> {
                    if (result) {
                        resourceInstance.removeCustomProvider(selectedProvider);
                        selectProvider(null);
                    }
                }).show(editor);
            });
        }
        return menu;
    }

    public void loadResource() {
        var lastSelectedProvider = selectedProvider;
        selectedProvider = null;
        providerToggles.clear();
        providerList.clearAllScrollViewChildren();
        providerContainer.clearAllChildren();

        // builtin
        resourceInstance.getBuiltinProviders().forEach((name, providers) -> addProviderToggles(providers));

        // split
        providerList.addScrollViewChild(new UIElement().layout(layout -> {
            layout.alignSelf(AlignItems.CENTER);
            layout.widthPercent(95);
            layout.height(1);
            layout.marginVertical(1);
        }).style(style -> style.backgroundTexture(ColorPattern.T_WHITE.rectTexture())));

        // custom
        resourceInstance.getCustomProviders().forEach((name, providers) -> addProviderToggles(providers));

        if (providerToggles.containsKey(lastSelectedProvider)) {
            selectProvider(lastSelectedProvider);
        } else if (!providerToggles.isEmpty()) {
            selectProvider(providerToggles.keySet().iterator().next());
        } else {
            selectProvider(null);
        }
    }

    private void addProviderToggles(List<IResourceProvider<T>> providers) {
        for (var provider : providers) {
            var toggle = new UIElement().layout(layout -> {
                layout.height(12);
                layout.widthPercent(100);
                layout.flexDirection(FlexDirection.ROW);
                layout.alignItems(AlignItems.CENTER);
                layout.paddingRight(2);
            }).addChildren(provider.createProviderToggle()).addEventListener(UIEvents.MOUSE_DOWN, event -> {
                if (event.button == 0) {
                    selectProvider(provider);
                }
            });
            providerList.addScrollViewChild(toggle);
            providerToggles.put(provider, toggle);
        }
    }

    public void selectProvider(@Nullable IResourceProvider<T> provider) {
        if (selectedProvider == provider) return;
        if (selectedProvider != null) {
            var oldToggle = providerToggles.get(selectedProvider);
            if (oldToggle != null) {
                oldToggle.style(style -> style.overlayTexture(IGuiTexture.EMPTY));
            }
        }
        providerContainer.clearAllChildren();
        selectedProvider = provider;
        if (selectedProvider != null) {
            var toggle = providerToggles.get(selectedProvider);
            if (toggle != null) {
                toggle.style(style -> style.overlayTexture(ColorPattern.T_LIGHT_GRAY.rectTexture()));
            }
            var providerView = resourceInstance.resource.createResourceProviderContainer(selectedProvider);
            providerView.setEditor(editor);
            providerView.reloadResourceContainer();
            providerView.setOnResourceSelect(res -> {
                if (onResourceSelect != null) {
                    onResourceSelect.accept(res);
                }
            });
            providerContainer.addChild(providerView);
        }
    }

    @Override
    public void screenTick() {
        super.screenTick();
        // check if the providers have changed
        boolean changed = (resourceInstance.getBuiltinProviders().values().stream().mapToInt(List::size).sum() +
                        resourceInstance.getCustomProviders().values().stream().mapToInt(List::size).sum()
                ) != providerToggles.size();
        if (!changed) {
            for (var provider : resourceInstance.getBuiltinProviders().values().stream().flatMap(List::stream).toList()) {
                if (!providerToggles.containsKey(provider)) {
                    changed = true;
                    break;
                }
            }
            for (var provider : resourceInstance.getCustomProviders().values().stream().flatMap(List::stream).toList()) {
                if (!providerToggles.containsKey(provider)) {
                    changed = true;
                    break;
                }
            }
        }
        if (changed) {
            loadResource();
        }
    }
}
