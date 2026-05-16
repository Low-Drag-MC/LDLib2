package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.google.common.base.Predicates;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
@KJSBindings
@LDLRegister(name = "structured-tag-editor", group = "basic", registry = "ldlib2:ui_element")
public class StructuredTagEditor extends BindableUIElement<Tag> {
    public enum TagKind {
        END("End"),
        BYTE("Byte"),
        SHORT("Short"),
        INT("Int"),
        LONG("Long"),
        FLOAT("Float"),
        DOUBLE("Double"),
        STRING("String"),
        LIST("List"),
        COMPOUND("Compound"),
        BYTE_ARRAY("Byte Array"),
        INT_ARRAY("Int Array"),
        LONG_ARRAY("Long Array"),
        BOOLEAN("Boolean");

        public final String displayName;

        TagKind(String displayName) {
            this.displayName = displayName;
        }
    }

    @FunctionalInterface
    private interface TagSetter {
        void set(Tag tag, boolean reload);
    }

    @FunctionalInterface
    private interface TagRemover {
        void remove();
    }

    public final ScrollerView scrollerView = new ScrollerView();
    private final UIElement rows = new UIElement();
    private final Set<String> expandedPaths = new LinkedHashSet<>();
    private final Set<String> selectedPaths = new LinkedHashSet<>();
    private final Map<String, TagRemover> visibleRemovers = new HashMap<>();
    private final LinkedHashSet<String> visiblePaths = new LinkedHashSet<>();
    @Nullable
    private String hoveredPath;
    @Nullable
    private String lastSelectedPath;

    @Getter
    private Tag value = EndTag.INSTANCE;
    @Setter
    private Predicate<Tag> tagValidator = Predicates.alwaysTrue();
    @Setter
    private Predicate<TagKind> rootTypeFilter = kind -> true;

    public StructuredTagEditor() {
        getLayout().widthPercent(100);
        getLayout().heightPercent(100);
        scrollerView.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        scrollerView.scrollerStyle(style -> style
                .mode(ScrollerMode.BOTH)
                .horizontalScrollDisplay(ScrollDisplay.AUTO)
                .verticalScrollDisplay(ScrollDisplay.AUTO));
        scrollerView.viewContainer(view -> view.layout(layout -> {
            layout.widthAuto();
            layout.gapAll(1);
        }));
        rows.layout(layout -> {
            layout.widthAuto();
            layout.gapAll(1);
        });
        rows.addEventListener(UIEvents.LAYOUT_CHANGED, e -> updateRowsWidth());
        setFocusable(true);
        addEventListener(UIEvents.KEY_DOWN, this::onKeyDown);
        scrollerView.addScrollViewChild(rows);
        addChild(scrollerView);
        internalSetup();
        reloadRows();
    }

    public StructuredTagEditor setTagResponder(Consumer<Tag> tagResponder) {
        registerValueListener(tagResponder);
        return this;
    }

    @Override
    public StructuredTagEditor setValue(@Nullable Tag value, boolean notify) {
        if (value == null) value = EndTag.INSTANCE;
        if (!this.value.equals(value)) {
            this.value = value.copy();
            reloadRows();
            if (notify) {
                notifyListeners();
            }
        }
        return this;
    }

    public StructuredTagEditor setCompoundTagOnly() {
        setRootTypeFilter(kind -> kind == TagKind.COMPOUND);
        return setTagValidator(tag -> tag instanceof CompoundTag);
    }

    public StructuredTagEditor setListOnly() {
        setRootTypeFilter(kind -> kind == TagKind.LIST);
        return setTagValidator(tag -> tag instanceof ListTag);
    }

    public StructuredTagEditor setAny() {
        setRootTypeFilter(kind -> true);
        return setTagValidator(Predicates.alwaysTrue());
    }

    private void replaceRoot(Tag tag) {
        if (tagValidator.test(tag)) {
            setValue(tag, true);
        }
    }

    private void replaceRoot(Tag tag, boolean reload) {
        if (!tagValidator.test(tag)) return;
        if (reload) {
            setValue(tag, true);
        } else if (!this.value.equals(tag)) {
            this.value = tag.copy();
            notifyListeners();
        }
    }

    private void changed() {
        reloadRows();
        notifyListeners();
    }

    private void notifyOnly() {
        notifyListeners();
    }

    private void reloadRows() {
        rows.clearAllChildren();
        visibleRemovers.clear();
        visiblePaths.clear();
        addRows(value, null, null, 0, "$", this::replaceRoot, null, true);
        selectedPaths.retainAll(visiblePaths);
    }

    private void addRows(Tag tag, @Nullable String key, @Nullable CompoundTag keyParent, int depth, String path, TagSetter setter, @Nullable TagRemover remover, boolean root) {
        visiblePaths.add(path);
        if (remover != null) {
            visibleRemovers.put(path, remover);
        }
        rows.addChild(createRow(tag, key, keyParent, depth, path, setter, remover, root));
        if (!expandedPaths.contains(path)) return;
        if (tag instanceof CompoundTag compoundTag) {
            for (var childKey : compoundTag.getAllKeys()) {
                var childPath = path + "." + childKey;
                var childTag = compoundTag.get(childKey);
                if (childTag == null) continue;
                addRows(childTag, childKey, compoundTag, depth + 1, childPath,
                        (child, reload) -> {
                            compoundTag.put(childKey, child);
                            if (child.getId() == childTag.getId() && !reload) {
                                notifyOnly();
                            } else {
                                changed();
                            }
                        },
                        () -> {
                            compoundTag.remove(childKey);
                            changed();
                        },
                        false);
            }
        } else if (tag instanceof ListTag listTag) {
            for (int i = 0; i < listTag.size(); i++) {
                var index = i;
                var childPath = path + "[" + index + "]";
                addRows(listTag.get(index), "[" + index + "]", null, depth + 1, childPath,
                        (child, reload) -> {
                            if (kindOf(child) != listElementKind(listTag) && !listTag.isEmpty()) {
                                setter.set(changeListElementType(listTag, kindOf(child)), true);
                            } else {
                                listTag.setTag(index, child);
                                if (reload) {
                                    changed();
                                } else {
                                    notifyOnly();
                                }
                            }
                        },
                        () -> {
                            listTag.remove(index);
                            changed();
                        },
                        false);
            }
        } else if (tag instanceof ByteArrayTag arrayTag) {
            var values = arrayTag.getAsByteArray();
            for (int i = 0; i < values.length; i++) {
                var index = i;
                addRows(ByteTag.valueOf(values[index]), "[" + index + "]", null, depth + 1, path + "[" + index + "]",
                        (child, reload) -> {
                            setter.set(setByteArrayElement(arrayTag, index, numberValue(child).byteValue()), false);
                        },
                        () -> {
                            setter.set(removeByteArrayElement(arrayTag, index), true);
                        },
                        false);
            }
        } else if (tag instanceof IntArrayTag arrayTag) {
            var values = arrayTag.getAsIntArray();
            for (int i = 0; i < values.length; i++) {
                var index = i;
                addRows(IntTag.valueOf(values[index]), "[" + index + "]", null, depth + 1, path + "[" + index + "]",
                        (child, reload) -> {
                            setter.set(setIntArrayElement(arrayTag, index, numberValue(child).intValue()), false);
                        },
                        () -> {
                            setter.set(removeIntArrayElement(arrayTag, index), true);
                        },
                        false);
            }
        } else if (tag instanceof LongArrayTag arrayTag) {
            var values = arrayTag.getAsLongArray();
            for (int i = 0; i < values.length; i++) {
                var index = i;
                addRows(LongTag.valueOf(values[index]), "[" + index + "]", null, depth + 1, path + "[" + index + "]",
                        (child, reload) -> {
                            setter.set(setLongArrayElement(arrayTag, index, numberValue(child).longValue()), false);
                        },
                        () -> {
                            setter.set(removeLongArrayElement(arrayTag, index), true);
                        },
                        false);
            }
        }
    }

    private UIElement createRow(Tag tag, @Nullable String key, @Nullable CompoundTag keyParent, int depth, String path, TagSetter setter, @Nullable TagRemover remover, boolean root) {
        var row = new UIElement().layout(layout -> {
            layout.widthAuto();
            layout.minWidth(Math.max(320, 180 + depth * 18) - 120);
            layout.height(16);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(2);
        }).style(style -> style.backgroundTexture(DynamicTexture.of(() -> {
            if (selectedPaths.contains(path)) return ColorPattern.BLUE.rectTexture();
            if (path.equals(hoveredPath)) return ColorPattern.T_WHITE.rectTexture();
            return ColorPattern.T_GRAY.rectTexture();
        })));
        row.addEventListener(UIEvents.LAYOUT_CHANGED, event -> updateRowsWidth());
        row.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 1) {
                openContextMenu(event, tag, setter, remover, root, path);
                event.stopPropagation();
            } else if (event.button == 0 && isRowSelectionTarget(event.target, row)) {
                selectPath(path, event);
                focus();
            }
        });
        row.addEventListener(UIEvents.MOUSE_ENTER, event -> hoveredPath = path, true);
        row.addEventListener(UIEvents.MOUSE_LEAVE, event -> {
            if (path.equals(hoveredPath)) hoveredPath = null;
        }, true);

        var expandable = isExpandable(tag);
        var expandButton = new UIElement();
        expandButton.layout(layout -> {
            layout.marginLeft(6 * depth);
            layout.width(12);
            layout.height(12);
            layout.paddingAll(2);
        });
        expandButton.addChild(new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        }).style(style -> style.backgroundTexture(expandable ? (expandedPaths.contains(path) ? Icons.DOWN_ARROW_NO_BAR_S_WHITE : Icons.RIGHT_ARROW_NO_BAR_S_WHITE) : IGuiTexture.EMPTY)));
        expandButton.addEventListener(UIEvents.MOUSE_DOWN, e -> {
                    if (e.button != 0) return;
                    if (!expandable) return;
                    if (expandedPaths.contains(path)) {
                        expandedPaths.remove(path);
                    } else {
                        expandedPaths.add(path);
                    }
                    reloadRows();
                    e.stopPropagation();
                });
        row.addChild(expandButton);

        if (key != null) {
            var keyContainer = new UIElement().layout(layout -> {
                layout.width(50);
                layout.height(14);
            });
            keyContainer.addChild(createKeyLabel(key, keyParent, keyContainer, root));
            row.addChild(keyContainer);
        }

        var typeButton = new Button().setText(kindOf(tag).displayName, false);
        typeButton.setOnClick(e -> openTypeMenu(e, tag, setter, root, path));
        typeButton.layout(layout -> {
            layout.width(70);
            layout.height(14);
        });
        row.addChild(typeButton);

        var valueEditor = createValueEditor(tag, setter);
        valueEditor.layout(layout -> {
            if (!(valueEditor instanceof Switch)) {
                layout.flex(1);
            }
            layout.height(14);
        });
        row.addChild(valueEditor);
        return row;
    }

    private static boolean isRowSelectionTarget(UIElement target, UIElement row) {
        var current = target;
        while (current != null && current != row) {
            if (current instanceof Button || current instanceof TextField || current instanceof Switch) {
                return false;
            }
            current = current.getParent();
        }
        return true;
    }

    private void updateRowsWidth() {
        var width = 0f;
        for (var child : rows.getChildren()) {
            width = Math.max(width, child.getSizeWidth());
        }
        width = Math.max(width, scrollerView.viewPort.getContentWidth());
        if (width > 0 && Math.abs(rows.getSizeWidth() - width) > 0.5f) {
            var targetWidth = width;
            rows.layout(layout -> layout.minWidth(targetWidth));
        }
    }

    private UIElement createKeyLabel(String key, @Nullable CompoundTag keyParent, UIElement keyContainer, boolean root) {
        var label = new Label().setText(key, false).textStyle(style -> style
                .textAlignVertical(Vertical.CENTER)
                .textAlignHorizontal(Horizontal.LEFT)
                .textWrap(com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap.HOVER_ROLL)
        ).layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        }).setOverflowVisible(false);
        label.addEventListener(UIEvents.DOUBLE_CLICK, e -> {
            if (root || keyParent == null) return;
            keyContainer.clearAllChildren();
            var field = new TextField().setText(key, false);
            field.setTextValidator(text -> isRenameValid(keyParent, key, text));
            field.addEventListener(UIEvents.KEY_DOWN, event -> {
                if (event.keyCode == GLFW.GLFW_KEY_ENTER || event.keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                    if (renameCompoundKey(keyParent, key, field.getRawText())) {
                        changed();
                    } else {
                        reloadRows();
                    }
                    event.stopPropagation();
                } else if (event.keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    reloadRows();
                    event.stopPropagation();
                }
            });
            field.addEventListener(UIEvents.BLUR, event -> reloadRows());
            keyContainer.addChild(field.layout(layout -> {
                layout.widthPercent(100);
                layout.heightPercent(100);
            }));
            field.focus();
        });
        return label;
    }

    private void selectPath(String path, UIEvent event) {
        if (event.isShiftDown() && lastSelectedPath != null) {
            selectedPaths.clear();
            var visible = List.copyOf(visiblePaths);
            var from = visible.indexOf(lastSelectedPath);
            var to = visible.indexOf(path);
            if (from >= 0 && to >= 0) {
                for (int i = Math.min(from, to); i <= Math.max(from, to); i++) {
                    selectedPaths.add(visible.get(i));
                }
            } else {
                selectedPaths.add(path);
            }
        } else if (event.isCtrlDown()) {
            if (!selectedPaths.remove(path)) {
                selectedPaths.add(path);
            }
            lastSelectedPath = path;
        } else {
            selectedPaths.clear();
            selectedPaths.add(path);
            lastSelectedPath = path;
        }
    }

    private void onKeyDown(UIEvent event) {
        if (isFocused() && (event.keyCode == GLFW.GLFW_KEY_DELETE || event.keyCode == GLFW.GLFW_KEY_BACKSPACE)) {
            deleteSelectedPaths();
            event.stopPropagation();
        }
    }

    private void deleteSelectedPaths() {
        if (selectedPaths.isEmpty()) return;
        var visible = List.copyOf(visiblePaths);
        selectedPaths.stream()
                .sorted((a, b) -> Integer.compare(visible.indexOf(b), visible.indexOf(a)))
                .forEach(this::removePath);
        selectedPaths.clear();
        lastSelectedPath = null;
        changed();
    }

    private void convertSelectedPaths(TagKind kind) {
        if (selectedPaths.isEmpty()) return;
        var visible = List.copyOf(visiblePaths);
        selectedPaths.stream()
                .sorted((a, b) -> Integer.compare(visible.indexOf(b), visible.indexOf(a)))
                .forEach(path -> {
                    var current = findPath(path);
                    if (current != null) {
                        replacePath(path, convertTag(current, kind));
                    }
                });
        changed();
    }

    private void removePath(String path) {
        if ("$".equals(path)) return;
        var bracket = path.lastIndexOf('[');
        if (bracket >= 0 && path.endsWith("]")) {
            var parent = findPath(path.substring(0, bracket));
            var index = Integer.parseInt(path.substring(bracket + 1, path.length() - 1));
            if (parent instanceof ListTag listTag && index >= 0 && index < listTag.size()) {
                listTag.remove(index);
            } else if (parent instanceof ByteArrayTag arrayTag && index >= 0 && index < arrayTag.size()) {
                replacePath(path.substring(0, bracket), removeByteArrayElement(arrayTag, index));
            } else if (parent instanceof IntArrayTag arrayTag && index >= 0 && index < arrayTag.size()) {
                replacePath(path.substring(0, bracket), removeIntArrayElement(arrayTag, index));
            } else if (parent instanceof LongArrayTag arrayTag && index >= 0 && index < arrayTag.size()) {
                replacePath(path.substring(0, bracket), removeLongArrayElement(arrayTag, index));
            }
            return;
        }
        var dot = path.lastIndexOf('.');
        if (dot > 0) {
            var parent = findPath(path.substring(0, dot));
            if (parent instanceof CompoundTag compoundTag) {
                compoundTag.remove(path.substring(dot + 1));
            }
        }
    }

    @Nullable
    private Tag findPath(String path) {
        if ("$".equals(path)) return value;
        Tag current = value;
        var index = 1;
        while (index < path.length()) {
            if (path.charAt(index) == '.') {
                var nextDot = path.indexOf('.', index + 1);
                var nextBracket = path.indexOf('[', index + 1);
                var next = nextDot == -1 ? nextBracket : (nextBracket == -1 ? nextDot : Math.min(nextDot, nextBracket));
                if (next == -1) next = path.length();
                if (!(current instanceof CompoundTag compoundTag)) return null;
                current = compoundTag.get(path.substring(index + 1, next));
                if (current == null) return null;
                index = next;
            } else if (path.charAt(index) == '[') {
                var end = path.indexOf(']', index);
                if (end < 0 || !(current instanceof CollectionTag collectionTag)) return null;
                var childIndex = Integer.parseInt(path.substring(index + 1, end));
                if (childIndex < 0 || childIndex >= collectionTag.size()) return null;
                current = (Tag) collectionTag.get(childIndex);
                index = end + 1;
            } else {
                return null;
            }
        }
        return current;
    }

    private void replacePath(String path, Tag replacement) {
        if ("$".equals(path)) {
            value = replacement;
            return;
        }
        var bracket = path.lastIndexOf('[');
        if (bracket >= 0 && path.endsWith("]")) {
            var parent = findPath(path.substring(0, bracket));
            var index = Integer.parseInt(path.substring(bracket + 1, path.length() - 1));
            if (parent instanceof ListTag listTag && index >= 0 && index < listTag.size()) {
                listTag.setTag(index, replacement);
            }
            return;
        }
        var dot = path.lastIndexOf('.');
        if (dot > 0 && findPath(path.substring(0, dot)) instanceof CompoundTag compoundTag) {
            compoundTag.put(path.substring(dot + 1), replacement);
        }
    }

    private UIElement createValueEditor(Tag tag, TagSetter setter) {
        if (tag instanceof CompoundTag || tag instanceof ListTag || tag instanceof ByteArrayTag || tag instanceof IntArrayTag || tag instanceof LongArrayTag || tag instanceof EndTag) {
            return new Label().setText(tagSummary(tag), false).textStyle(style -> style.textAlignVertical(Vertical.CENTER));
        }
        if (tag instanceof StringTag stringTag) {
            return new TextField().setText(stringTag.getAsString(), false)
                    .setTextResponder(text -> setter.set(StringTag.valueOf(text), false));
        }
        if (tag instanceof ByteTag byteTag) {
            if (kindOf(byteTag) == TagKind.BOOLEAN) {
                return new Switch()
                        .setOn(byteTag.getAsByte() != 0, false)
                        .setOnSwitchChanged(value -> setter.set(ByteTag.valueOf(value), false));
            }
            var field = new TextField().setNumbersOnlyByte(Byte.MIN_VALUE, Byte.MAX_VALUE);
            field.setText(Byte.toString(byteTag.getAsByte()), false);
            field.setTextResponder(text -> parseByte(text, value -> setter.set(ByteTag.valueOf(value), false)));
            return field;
        }
        if (tag instanceof ShortTag shortTag) {
            var field = new TextField().setNumbersOnlyShort(Short.MIN_VALUE, Short.MAX_VALUE);
            field.setText(Short.toString(shortTag.getAsShort()), false);
            field.setTextResponder(text -> parseShort(text, value -> setter.set(ShortTag.valueOf(value), false)));
            return field;
        }
        if (tag instanceof IntTag intTag) {
            var field = new TextField().setNumbersOnlyInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
            field.setText(Integer.toString(intTag.getAsInt()), false);
            field.setTextResponder(text -> parseInt(text, value -> setter.set(IntTag.valueOf(value), false)));
            return field;
        }
        if (tag instanceof LongTag longTag) {
            var field = new TextField().setNumbersOnlyLong(Long.MIN_VALUE, Long.MAX_VALUE);
            field.setText(Long.toString(longTag.getAsLong()), false);
            field.setTextResponder(text -> parseLong(text, value -> setter.set(LongTag.valueOf(value), false)));
            return field;
        }
        if (tag instanceof FloatTag floatTag) {
            var field = new TextField().setNumbersOnlyFloat(-Float.MAX_VALUE, Float.MAX_VALUE);
            field.setText(Float.toString(floatTag.getAsFloat()), false);
            field.setTextResponder(text -> parseFloat(text, value -> setter.set(FloatTag.valueOf(value), false)));
            return field;
        }
        if (tag instanceof DoubleTag doubleTag) {
            var field = new TextField().setNumbersOnlyDouble(-Double.MAX_VALUE, Double.MAX_VALUE);
            field.setText(Double.toString(doubleTag.getAsDouble()), false);
            field.setTextResponder(text -> parseDouble(text, value -> setter.set(DoubleTag.valueOf(value), false)));
            return field;
        }
        return new Label().setText(tag.toString(), false);
    }

    private void openContextMenu(UIEvent event, Tag tag, TagSetter setter, @Nullable TagRemover remover, boolean root, String path) {
        var menu = TreeBuilder.Menu.start();
        if (tag instanceof CompoundTag compoundTag) {
            menu.branch(Component.literal("Add"), add -> {
                for (var kind : TagKind.values()) {
                    add.leaf(Component.literal(kind.displayName), () -> addCompoundChild(compoundTag, kind));
                }
            });
        } else if (tag instanceof ListTag listTag) {
            if (listTag.isEmpty()) {
                menu.branch(Component.literal("Add"), add -> {
                    for (var kind : TagKind.values()) {
                        if (kind == TagKind.END) continue;
                        add.leaf(Component.literal(kind.displayName), () -> {
                            listTag.add(defaultTag(kind));
                            changed();
                        });
                    }
                });
            } else {
                menu.leaf(Component.literal("Add " + listElementKind(listTag).displayName), () -> {
                    listTag.add(defaultTag(listElementKind(listTag)));
                    changed();
                });
            }
            menu.branch(Component.literal("Convert Elements"), branch -> {
                for (var kind : TagKind.values()) {
                    if (kind == TagKind.END) continue;
                    branch.leaf(Component.literal(kind.displayName), () -> setter.set(changeListElementType(listTag, kind), true));
                }
            });
        } else if (tag instanceof ByteArrayTag arrayTag) {
            menu.leaf(Component.literal("Add Byte"), () -> setter.set(insertByteArrayElement(arrayTag, arrayTag.size(), (byte) 0), true));
        } else if (tag instanceof IntArrayTag arrayTag) {
            menu.leaf(Component.literal("Add Int"), () -> setter.set(insertIntArrayElement(arrayTag, arrayTag.size(), 0), true));
        } else if (tag instanceof LongArrayTag arrayTag) {
            menu.leaf(Component.literal("Add Long"), () -> setter.set(insertLongArrayElement(arrayTag, arrayTag.size(), 0L), true));
        }
        if (remover != null) {
            menu.crossLine();
            menu.leaf(Icons.REMOVE, Component.literal(selectedPaths.size() > 1 && selectedPaths.contains(path) ? "Delete Selected" : "Delete"),
                    () -> deleteFromContextMenu(path, remover));
        }
        menu.crossLine();
        menu.branch(Component.literal(root ? "Root Type" : "Change Type"), branch -> addTypeLeaves(branch, tag, setter, root, path));
        showMenu(event, menu);
    }

    private void openTypeMenu(UIEvent event, Tag tag, TagSetter setter, boolean root, String path) {
        var menu = TreeBuilder.Menu.start();
        addTypeLeaves(menu, tag, setter, root, path);
        showMenu(event, menu);
    }

    private void addTypeLeaves(TreeBuilder.Menu menu, Tag tag, TagSetter setter, boolean root, String path) {
        for (var kind : TagKind.values()) {
            if (kind == TagKind.BOOLEAN) {
                menu.leaf(Component.literal("Boolean"), () -> setType(tag, setter, root, path, TagKind.BOOLEAN));
                continue;
            }
            if (root && !rootTypeFilter.test(kind)) continue;
            if (root && !tagValidator.test(defaultTag(kind))) continue;
            menu.leaf(Component.literal(kind.displayName), () -> setType(tag, setter, root, path, kind));
        }
    }

    private void setType(Tag tag, TagSetter setter, boolean root, String path, TagKind kind) {
        if (!root && selectedPaths.size() > 1 && selectedPaths.contains(path)) {
            convertSelectedPaths(kind);
        } else {
            setter.set(convertTag(tag, kind), true);
        }
    }

    private void deleteFromContextMenu(String path, TagRemover remover) {
        if (selectedPaths.size() > 1 && selectedPaths.contains(path)) {
            deleteSelectedPaths();
        } else {
            remover.remove();
        }
    }

    private void showMenu(UIEvent event, TreeBuilder.Menu menu) {
        if (menu.isEmpty()) return;
        var mui = getModularUI();
        if (mui == null) return;
        var root = mui.ui.rootElement;
        var layoutOffset = root.worldToLocalLayoutOffset(new Vector2f(event.x, event.y));
        root.addChild(new Menu<>(menu.build(), TreeBuilder.Menu::uiProvider)
                .setHoverTextureProvider(TreeBuilder.Menu::hoverTextureProvider)
                .setOnNodeClicked(TreeBuilder.Menu::handle)
                .layout(layout -> {
                    layout.left(layoutOffset.x);
                    layout.top(layoutOffset.y);
                }));
    }

    private void addCompoundChild(CompoundTag compoundTag, TagKind kind) {
        var base = "new_" + kind.name().toLowerCase();
        var key = base;
        var index = 1;
        while (compoundTag.contains(key)) {
            key = base + "_" + index++;
        }
        compoundTag.put(key, defaultTag(kind));
        changed();
    }

    private static boolean isExpandable(Tag tag) {
        return tag instanceof CompoundTag compoundTag && !compoundTag.isEmpty() ||
                tag instanceof ListTag listTag && !listTag.isEmpty() ||
                tag instanceof ByteArrayTag byteArrayTag && byteArrayTag.size() > 0 ||
                tag instanceof IntArrayTag intArrayTag && intArrayTag.size() > 0 ||
                tag instanceof LongArrayTag longArrayTag && longArrayTag.size() > 0;
    }

    private static String tagSummary(Tag tag) {
        if (tag instanceof CompoundTag compoundTag) return compoundTag.size() + " entries";
        if (tag instanceof ListTag listTag) return listTag.size() + " elements";
        if (tag instanceof ByteArrayTag arrayTag) return arrayTag.size() + " bytes";
        if (tag instanceof IntArrayTag arrayTag) return arrayTag.size() + " ints";
        if (tag instanceof LongArrayTag arrayTag) return arrayTag.size() + " longs";
        if (tag instanceof EndTag) return "";
        return tag.toString();
    }

    public static boolean renameCompoundKey(CompoundTag tag, String oldKey, String newKey) {
        if (!isRenameValid(tag, oldKey, newKey)) return false;
        var value = tag.get(oldKey);
        if (value == null) return false;
        tag.remove(oldKey);
        tag.put(newKey, value);
        return true;
    }

    private static boolean isRenameValid(CompoundTag tag, String oldKey, String newKey) {
        return !newKey.isBlank() && (oldKey.equals(newKey) || !tag.contains(newKey));
    }

    public static Tag convertTag(Tag source, TagKind kind) {
        if (kind == TagKind.BOOLEAN) return ByteTag.valueOf(source instanceof ByteTag byteTag && byteTag.getAsByte() != 0);
        if (kindOf(source) == kind) return source.copy();
        return defaultTag(kind);
    }

    private static Tag defaultTag(TagKind kind) {
        return switch (kind) {
            case END -> EndTag.INSTANCE;
            case BYTE -> ByteTag.valueOf((byte) 2);
            case SHORT -> ShortTag.valueOf((short) 0);
            case INT -> IntTag.valueOf(0);
            case LONG -> LongTag.valueOf(0L);
            case FLOAT -> FloatTag.valueOf(0);
            case DOUBLE -> DoubleTag.valueOf(0);
            case STRING -> StringTag.valueOf("");
            case LIST -> new ListTag();
            case COMPOUND -> new CompoundTag();
            case BYTE_ARRAY -> new ByteArrayTag(new byte[0]);
            case INT_ARRAY -> new IntArrayTag(new int[0]);
            case LONG_ARRAY -> new LongArrayTag(new long[0]);
            case BOOLEAN -> ByteTag.valueOf(false);
        };
    }

    private static TagKind kindOf(Tag tag) {
        if (tag instanceof ByteTag byteTag) {
            return byteTag.getAsByte() == 0 || byteTag.getAsByte() == 1 ? TagKind.BOOLEAN : TagKind.BYTE;
        }
        if (tag instanceof ShortTag) return TagKind.SHORT;
        if (tag instanceof IntTag) return TagKind.INT;
        if (tag instanceof LongTag) return TagKind.LONG;
        if (tag instanceof FloatTag) return TagKind.FLOAT;
        if (tag instanceof DoubleTag) return TagKind.DOUBLE;
        if (tag instanceof StringTag) return TagKind.STRING;
        if (tag instanceof ListTag) return TagKind.LIST;
        if (tag instanceof CompoundTag) return TagKind.COMPOUND;
        if (tag instanceof ByteArrayTag) return TagKind.BYTE_ARRAY;
        if (tag instanceof IntArrayTag) return TagKind.INT_ARRAY;
        if (tag instanceof LongArrayTag) return TagKind.LONG_ARRAY;
        return TagKind.END;
    }

    private static TagKind listElementKind(ListTag listTag) {
        return listTag.isEmpty() ? TagKind.END : kindOf(listTag.getFirst());
    }

    public static ListTag changeListElementType(ListTag listTag, TagKind kind) {
        var converted = new ListTag();
        for (int i = 0; i < listTag.size(); i++) {
            converted.add(defaultTag(kind));
        }
        return converted;
    }

    public static ByteArrayTag setByteArrayElement(ByteArrayTag tag, int index, byte value) {
        var values = tag.getAsByteArray();
        values[index] = value;
        return new ByteArrayTag(values);
    }

    public static ByteArrayTag insertByteArrayElement(ByteArrayTag tag, int index, byte value) {
        var values = tag.getAsByteArray();
        var result = Arrays.copyOf(values, values.length + 1);
        System.arraycopy(result, index, result, index + 1, values.length - index);
        result[index] = value;
        return new ByteArrayTag(result);
    }

    public static ByteArrayTag removeByteArrayElement(ByteArrayTag tag, int index) {
        var values = tag.getAsByteArray();
        var result = new byte[values.length - 1];
        System.arraycopy(values, 0, result, 0, index);
        System.arraycopy(values, index + 1, result, index, values.length - index - 1);
        return new ByteArrayTag(result);
    }

    public static IntArrayTag setIntArrayElement(IntArrayTag tag, int index, int value) {
        var values = tag.getAsIntArray();
        values[index] = value;
        return new IntArrayTag(values);
    }

    public static IntArrayTag insertIntArrayElement(IntArrayTag tag, int index, int value) {
        var values = tag.getAsIntArray();
        var result = Arrays.copyOf(values, values.length + 1);
        System.arraycopy(result, index, result, index + 1, values.length - index);
        result[index] = value;
        return new IntArrayTag(result);
    }

    public static IntArrayTag removeIntArrayElement(IntArrayTag tag, int index) {
        var values = tag.getAsIntArray();
        var result = new int[values.length - 1];
        System.arraycopy(values, 0, result, 0, index);
        System.arraycopy(values, index + 1, result, index, values.length - index - 1);
        return new IntArrayTag(result);
    }

    public static LongArrayTag setLongArrayElement(LongArrayTag tag, int index, long value) {
        var values = tag.getAsLongArray();
        values[index] = value;
        return new LongArrayTag(values);
    }

    public static LongArrayTag insertLongArrayElement(LongArrayTag tag, int index, long value) {
        var values = tag.getAsLongArray();
        var result = Arrays.copyOf(values, values.length + 1);
        System.arraycopy(result, index, result, index + 1, values.length - index);
        result[index] = value;
        return new LongArrayTag(result);
    }

    public static LongArrayTag removeLongArrayElement(LongArrayTag tag, int index) {
        var values = tag.getAsLongArray();
        var result = new long[values.length - 1];
        System.arraycopy(values, 0, result, 0, index);
        System.arraycopy(values, index + 1, result, index, values.length - index - 1);
        return new LongArrayTag(result);
    }

    private static Number numberValue(Tag tag) {
        if (tag instanceof NumericTag numericTag) return numericTag.getAsNumber();
        return 0;
    }

    private static void parseByte(String text, Consumer<Byte> consumer) {
        try { consumer.accept(Byte.parseByte(text)); } catch (NumberFormatException ignored) {}
    }

    private static void parseShort(String text, Consumer<Short> consumer) {
        try { consumer.accept(Short.parseShort(text)); } catch (NumberFormatException ignored) {}
    }

    private static void parseInt(String text, Consumer<Integer> consumer) {
        try { consumer.accept(Integer.parseInt(text)); } catch (NumberFormatException ignored) {}
    }

    private static void parseLong(String text, Consumer<Long> consumer) {
        try { consumer.accept(Long.parseLong(text)); } catch (NumberFormatException ignored) {}
    }

    private static void parseFloat(String text, Consumer<Float> consumer) {
        try { consumer.accept(Float.parseFloat(text)); } catch (NumberFormatException ignored) {}
    }

    private static void parseDouble(String text, Consumer<Double> consumer) {
        try { consumer.accept(Double.parseDouble(text)); } catch (NumberFormatException ignored) {}
    }
}
