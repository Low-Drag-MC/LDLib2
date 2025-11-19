package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.google.common.base.Predicates;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.mojang.brigadier.StringReader;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
@LDLRegister(name = "tag-field", registry = "ldlib2:ui_element")
public class TagField extends BindableUIElement<Tag> {
    private static final ChatFormatting STRING_COLOR = ChatFormatting.GREEN;
    private static final ChatFormatting NUMBER_COLOR = ChatFormatting.GOLD;
    private static final ChatFormatting BOOLEAN_COLOR = ChatFormatting.LIGHT_PURPLE;
    private static final ChatFormatting BRACKET_COLOR = ChatFormatting.AQUA;
    private static final ChatFormatting COMMA_COLOR = ChatFormatting.GRAY;
    private static final ChatFormatting KEY_COLOR = ChatFormatting.LIGHT_PURPLE;
    public final TextField textField = new TextField();

    @Setter
    private Predicate<Tag> tagValidator = Predicates.alwaysTrue();
    @Getter
    private Tag value = EndTag.INSTANCE;

    public TagField() {
        textField.setFormatter(rawText -> {
            if (rawText.isEmpty()) return Component.empty();
            if (!isTagValid(rawText)) return Component.literal(rawText);
            try {
                var reader = new StringReader(rawText);
                return formatNbt(reader);
            } catch (Exception e) {
                return Component.literal(rawText);
            }
        });
        textField.setTextValidator(this::isTagValid);
        textField.setTextResponder(text -> {
            try {
                setValue(new TagParser(new StringReader(text)).readValue(), true);
            } catch (Exception e) {
                setValue(EndTag.INSTANCE, true);
            }
        });

        addChild(textField);
        internalSetup();
    }

    private boolean isTagValid(String rawText) {
        if (rawText.isEmpty()) return tagValidator.test(EndTag.INSTANCE);
        try {
            var reader = new StringReader(rawText);
            var tag = new TagParser(reader).readValue();
            if (!tagValidator.test(tag)) return false;
            reader.skipWhitespace();
            return !reader.canRead();
        } catch (Exception e) {
            return false;
        }
    }

    protected void refreshTextField() {
        try {
            var current = new TagParser(new StringReader(textField.getRawText())).readValue();;
            if (current.equals(value)) return;
        } catch (Exception ignored) {
        }
        textField.setValue(value == EndTag.INSTANCE ? "" : value.toString(), false);
    }

    public TagField setTagResponder(Consumer<Tag> tagResponder) {
        registerValueListener(tagResponder);
        return this;
    }

    @Override
    public TagField setValue(@Nullable Tag value, boolean notify) {
        if (value == null) value = EndTag.INSTANCE;
        if (!this.value.equals(value)) {
            this.value = value;
            refreshTextField();
            if (notify) {
                notifyListeners();
            }
        }
        return this;
    }

    public TagField setCompoundTagOnly() {
        return setTagValidator(tag -> tag instanceof CompoundTag);
    }

    public TagField setListOnly() {
        return setTagValidator(tag -> tag instanceof ListTag);
    }

    public TagField setAny() {
        return setTagValidator(Predicates.alwaysTrue());
    }

    private Component formatNbt(StringReader reader) {
        var result = Component.empty();
        while (reader.canRead()) {
            char c = reader.peek();
            if (c == '{' || c == '[') {
                result = result.append(Component.literal(String.valueOf(reader.read())).withStyle(BRACKET_COLOR));
            } else if (c == '}' || c == ']') {
                result = result.append(Component.literal(String.valueOf(reader.read())).withStyle(BRACKET_COLOR));
            } else if (c == ':') {
                result = result.append(Component.literal(String.valueOf(reader.read())).withStyle(COMMA_COLOR));
            } else if (c == ',') {
                result = result.append(Component.literal(String.valueOf(reader.read())).withStyle(COMMA_COLOR));
            } else if (c == '"') {
                reader.skip();
                StringBuilder sb = new StringBuilder();
                while (reader.canRead() && reader.peek() != '"') {
                    sb.append(reader.read());
                }
                var hasEndQuote = reader.canRead() && reader.peek() == '"';
                if (hasEndQuote) reader.skip();
                var text = sb.toString();
                if (reader.canRead() && reader.peek() == ':') {
                    result = result.append(Component.literal("\"" + text + (hasEndQuote ? "\"" : "")).withStyle(KEY_COLOR));
                } else {
                    result = result.append(Component.literal("\"" + text + (hasEndQuote ? "\"" : "")).withStyle(STRING_COLOR));
                }
            } else if (Character.isDigit(c) || c == '-') {
                StringBuilder sb = new StringBuilder();
                if (c == '-') {
                    sb.append(reader.read());
                }
                while (reader.canRead() && (Character.isDigit(reader.peek()) || reader.peek() == '.' || reader.peek() == 'f' || reader.peek() == 'd')) {
                    sb.append(reader.read());
                }
                result = result.append(Component.literal(sb.toString()).withStyle(NUMBER_COLOR));
            } else if (c == 't' || c == 'f') {
                StringBuilder sb = new StringBuilder();
                while (reader.canRead() && Character.isLetter(reader.peek())) {
                    sb.append(reader.read());
                }
                if (sb.toString().equals("true") || sb.toString().equals("false")) {
                    result = result.append(Component.literal(sb.toString()).withStyle(BOOLEAN_COLOR));
                } else {
                    result = result.append(Component.literal(sb.toString()));
                }
            } else if (Character.isWhitespace(c)) {
                result = result.append(Component.literal(String.valueOf(reader.read())));
            } else {
                StringBuilder sb = new StringBuilder();
                while (reader.canRead() && !Character.isWhitespace(reader.peek()) && reader.peek() != ',' && reader.peek() != ':' && reader.peek() != '}' && reader.peek() != ']') {
                    sb.append(reader.read());
                }
                result = result.append(Component.literal(sb.toString()));
            }
        }
        return result;
    }
}
