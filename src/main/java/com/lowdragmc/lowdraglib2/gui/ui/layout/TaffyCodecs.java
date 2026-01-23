package com.lowdragmc.lowdraglib2.gui.ui.layout;

import com.lowdragmc.lowdraglib2.gui.ui.data.*;
import com.lowdragmc.lowdraglib2.utils.LDLibExtraCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.style.*;

import java.util.ArrayList;
import lombok.experimental.UtilityClass;
import net.minecraft.nbt.*;
import org.appliedenergistics.yoga.YogaValue;
import org.appliedenergistics.yoga.style.StyleLength;

import java.util.List;

@UtilityClass
public final class TaffyCodecs {

    // ==================== LengthPercentage Codec ====================
    public static final Codec<LengthPercentage> LENGTH_PERCENTAGE_CODEC = LDLibExtraCodecs.TAG.xmap(
            TaffyCodecs::decodeLengthPercentage,
            TaffyCodecs::encodeLengthPercentage
    );

    public static Tag encodeLengthPercentage(LengthPercentage value) {
        if (value.isLength()) {
            return FloatTag.valueOf(value.getValue());
        } else if (value.isPercent()) {
            var tag = new CompoundTag();
            tag.putString("type", "percent");
            tag.putFloat("value", value.getValue());
            return tag;
        } else if (value.isCalc()) {
            // CalcExpression is a functional interface and cannot be easily serialized
            // For now, we'll encode it as a string placeholder
            var tag = new CompoundTag();
            tag.putString("type", "calc");
            tag.putString("value", "calc(...)");
            return tag;
        }
        return FloatTag.valueOf(0);
    }

    public static LengthPercentage decodeLengthPercentage(Tag tag) {
        if (tag instanceof FloatTag floatTag) {
            return LengthPercentage.length(floatTag.getAsFloat());
        } else if (tag instanceof CompoundTag compoundTag) {
            String type = compoundTag.getString("type");
            if ("percent".equals(type)) {
                return LengthPercentage.percent(compoundTag.getFloat("value"));
            } else if ("calc".equals(type)) {
                // Cannot deserialize CalcExpression, default to zero
                return LengthPercentage.length(0);
            }
        }
        return LengthPercentage.length(0);
    }

    // ==================== TrackSizingFunction Codec ====================
    public static final Codec<TrackSizingFunction> TRACK_SIZING_FUNCTION_CODEC = LDLibExtraCodecs.TAG.xmap(
            TaffyCodecs::decodeTrackSizingFunction,
            TaffyCodecs::encodeTrackSizingFunction
    );

    public static Tag encodeTrackSizingFunction(TrackSizingFunction func) {
        var tag = new CompoundTag();
        tag.putString("type", func.getType().name());

        switch (func.getType()) {
            case FIXED:
                tag.put("value", encodeLengthPercentage(func.getFixedValue()));
                break;
            case FIT_CONTENT:
                tag.put("limit", encodeLengthPercentage(func.getFitContentArgument()));
                break;
            case FLEX:
                tag.putFloat("fr", func.getFlexValue());
                break;
            case MINMAX:
                tag.put("min", encodeTrackSizingFunction(func.getMinFunc()));
                tag.put("max", encodeTrackSizingFunction(func.getMaxFunc()));
                break;
            case MIN_CONTENT:
            case MAX_CONTENT:
            case AUTO:
                // No additional data needed
                break;
        }

        return tag;
    }

    public static TrackSizingFunction decodeTrackSizingFunction(Tag tag) {
        if (!(tag instanceof CompoundTag compoundTag)) {
            return TrackSizingFunction.auto();
        }

        String typeName = compoundTag.getString("type");
        TrackSizingFunction.Type type;
        try {
            type = TrackSizingFunction.Type.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return TrackSizingFunction.auto();
        }

        return switch (type) {
            case FIXED -> {
                LengthPercentage value = decodeLengthPercentage(compoundTag.get("value"));
                yield TrackSizingFunction.fixed(value);
            }
            case MIN_CONTENT -> TrackSizingFunction.minContent();
            case MAX_CONTENT -> TrackSizingFunction.maxContent();
            case FIT_CONTENT -> {
                LengthPercentage limit = decodeLengthPercentage(compoundTag.get("limit"));
                yield TrackSizingFunction.fitContent(limit);
            }
            case AUTO -> TrackSizingFunction.auto();
            case FLEX -> TrackSizingFunction.flex(compoundTag.getFloat("fr"));
            case MINMAX -> {
                TrackSizingFunction min = decodeTrackSizingFunction(compoundTag.get("min"));
                TrackSizingFunction max = decodeTrackSizingFunction(compoundTag.get("max"));
                yield TrackSizingFunction.minmax(min, max);
            }
        };
    }

    // ==================== GridRepetition Codec ====================

    public static final Codec<GridRepetition> GRID_REPETITION_CODEC = LDLibExtraCodecs.TAG.xmap(
            TaffyCodecs::decodeGridRepetition,
            TaffyCodecs::encodeGridRepetition
    );

    public static Tag encodeGridRepetition(GridRepetition repetition) {
        var tag = new CompoundTag();
        tag.putString("type", repetition.getType().name());

        if (repetition.getType() == GridRepetition.RepetitionType.COUNT) {
            tag.putInt("count", repetition.getCount());
        }

        var tracksList = new ListTag();
        for (TrackSizingFunction track : repetition.getTracks()) {
            tracksList.add(encodeTrackSizingFunction(track));
        }
        tag.put("tracks", tracksList);

        return tag;
    }

    public static GridRepetition decodeGridRepetition(Tag tag) {
        if (!(tag instanceof CompoundTag compoundTag)) {
            return GridRepetition.count(1, TrackSizingFunction.auto());
        }

        String typeName = compoundTag.getString("type");
        GridRepetition.RepetitionType type;
        try {
            type = GridRepetition.RepetitionType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return GridRepetition.count(1, TrackSizingFunction.auto());
        }

        ListTag tracksTag = compoundTag.getList("tracks", Tag.TAG_COMPOUND);
        List<TrackSizingFunction> tracks = new ArrayList<>();
        for (int i = 0; i < tracksTag.size(); i++) {
            tracks.add(decodeTrackSizingFunction(tracksTag.get(i)));
        }

        return switch (type) {
            case COUNT -> GridRepetition.count(compoundTag.getInt("count"), tracks);
            case AUTO_FILL -> GridRepetition.autoFill(tracks);
            case AUTO_FIT -> GridRepetition.autoFit(tracks);
        };
    }

    // ==================== GridTemplateComponent Codec ====================

    public static final Codec<GridTemplateComponent> GRID_TEMPLATE_COMPONENT_CODEC = LDLibExtraCodecs.TAG.xmap(
            TaffyCodecs::decodeGridTemplateComponent,
            TaffyCodecs::encodeGridTemplateComponent
    );

    public static Tag encodeGridTemplateComponent(GridTemplateComponent component) {
        var tag = new CompoundTag();
        tag.putString("type", component.getType().name());

        if (component.isSingle()) {
            tag.put("track", encodeTrackSizingFunction(component.getSingle()));
        } else if (component.isRepeat()) {
            tag.put("repeat", encodeGridRepetition(component.getRepeat()));
        }

        return tag;
    }

    public static GridTemplateComponent decodeGridTemplateComponent(Tag tag) {
        if (!(tag instanceof CompoundTag compoundTag)) {
            return GridTemplateComponent.single(TrackSizingFunction.auto());
        }

        String typeName = compoundTag.getString("type");
        GridTemplateComponent.Type type;
        try {
            type = GridTemplateComponent.Type.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return GridTemplateComponent.single(TrackSizingFunction.auto());
        }

        return switch (type) {
            case SINGLE -> {
                TrackSizingFunction track = decodeTrackSizingFunction(compoundTag.get("track"));
                yield GridTemplateComponent.single(track);
            }
            case REPEAT -> {
                GridRepetition repeat = decodeGridRepetition(compoundTag.get("repeat"));
                yield GridTemplateComponent.repeat(repeat);
            }
        };
    }

    // ==================== NamedGridLine Codec ====================

    public static final Codec<NamedGridLine> NAMED_GRID_LINE_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("name").forGetter(NamedGridLine::getName),
                    Codec.INT.fieldOf("index").forGetter(NamedGridLine::getIndex)
            ).apply(instance, NamedGridLine::new)
    );

    // ==================== GridTemplate Codec ====================

    public static final Codec<GridTemplate> GRID_TEMPLATE_CODEC = LDLibExtraCodecs.TAG.xmap(
            TaffyCodecs::decodeGridTemplate,
            TaffyCodecs::encodeGridTemplate
    );

    public static Tag encodeGridTemplate(GridTemplate template) {
        var tag = new CompoundTag();

        // Encode simples array
        var simplesList = new ListTag();
        for (TrackSizingFunction simple : template.simples()) {
            simplesList.add(encodeTrackSizingFunction(simple));
        }
        tag.put("simples", simplesList);

        // Encode repeats array
        var repeatsList = new ListTag();
        for (GridTemplateComponent repeat : template.repeats()) {
            repeatsList.add(encodeGridTemplateComponent(repeat));
        }
        tag.put("repeats", repeatsList);

        // Encode names array
        var namesList = new ListTag();
        for (NamedGridLine name : template.names()) {
            var nameTag = new CompoundTag();
            nameTag.putString("name", name.getName());
            nameTag.putInt("index", name.getIndex());
            namesList.add(nameTag);
        }
        tag.put("names", namesList);

        return tag;
    }

    public static GridTemplate decodeGridTemplate(Tag tag) {
        if (!(tag instanceof CompoundTag compoundTag)) {
            return new GridTemplate(new TrackSizingFunction[0], new GridTemplateComponent[0], new NamedGridLine[0]);
        }

        // Decode simples array
        ListTag simplesTag = compoundTag.getList("simples", Tag.TAG_COMPOUND);
        TrackSizingFunction[] simples = new TrackSizingFunction[simplesTag.size()];
        for (int i = 0; i < simplesTag.size(); i++) {
            simples[i] = decodeTrackSizingFunction(simplesTag.get(i));
        }

        // Decode repeats array
        ListTag repeatsTag = compoundTag.getList("repeats", Tag.TAG_COMPOUND);
        GridTemplateComponent[] repeats = new GridTemplateComponent[repeatsTag.size()];
        for (int i = 0; i < repeatsTag.size(); i++) {
            repeats[i] = decodeGridTemplateComponent(repeatsTag.get(i));
        }

        // Decode names array
        ListTag namesTag = compoundTag.getList("names", Tag.TAG_COMPOUND);
        NamedGridLine[] names = new NamedGridLine[namesTag.size()];
        for (int i = 0; i < namesTag.size(); i++) {
            CompoundTag nameTag = namesTag.getCompound(i);
            names[i] = new NamedGridLine(nameTag.getString("name"), nameTag.getInt("index"));
        }

        return new GridTemplate(simples, repeats, names);
    }

    // ==================== GridAuto Codec ====================

    public static final Codec<GridAuto> GRID_AUTO_CODEC = LDLibExtraCodecs.TAG.xmap(
            TaffyCodecs::decodeGridAuto,
            TaffyCodecs::encodeGridAuto
    );

    public static Tag encodeGridAuto(GridAuto gridAuto) {
        var tag = new CompoundTag();

        // Encode values array
        var valuesList = new ListTag();
        for (TrackSizingFunction value : gridAuto.values()) {
            valuesList.add(encodeTrackSizingFunction(value));
        }
        tag.put("values", valuesList);

        return tag;
    }

    public static GridAuto decodeGridAuto(Tag tag) {
        if (!(tag instanceof CompoundTag compoundTag)) {
            return GridAuto.EMPTY;
        }

        // Decode values array
        ListTag valuesTag = compoundTag.getList("values", Tag.TAG_COMPOUND);
        TrackSizingFunction[] values = new TrackSizingFunction[valuesTag.size()];
        for (int i = 0; i < valuesTag.size(); i++) {
            values[i] = decodeTrackSizingFunction(valuesTag.get(i));
        }

        return new GridAuto(List.of(values));
    }

    // ==================== LengthPercentageAuto Codec ====================
    public static final Codec<LengthPercentageAuto> LENGTH_PERCENTAGE_AUTO_CODEC = LDLibExtraCodecs.TAG.xmap(
            TaffyCodecs::decodeLengthPercentageAuto,
            TaffyCodecs::encodeLengthPercentageAuto
    );

    public static final Codec<LengthPercentageAuto> LPA_CODEC = LDLibExtraCodecs.compat(LENGTH_PERCENTAGE_AUTO_CODEC, YogaCodecs.STYLE_LENGTH_CODEC.xmap(
            styleLength -> {
                if (styleLength.isPercent()) return LengthPercentageAuto.length(styleLength.value().getValue() / 100f);
                if (styleLength.isPoints()) return LengthPercentageAuto.length(styleLength.value().getValue());
                return LengthPercentageAuto.auto();
            },
            lpa -> switch (lpa.getType()) {
                case LENGTH -> StyleLength.points(lpa.getValue());
                case PERCENT -> StyleLength.percent(lpa.getValue() * 100f);
                default -> StyleLength.ofAuto();
            }
    ));

    public static Tag encodeLengthPercentageAuto(LengthPercentageAuto value) {
        var tag = new CompoundTag();
        tag.putString("type", value.getType().name());

        switch (value.getType()) {
            case LENGTH:
                tag.putFloat("value", value.getValue());
                break;
            case PERCENT:
                tag.putFloat("value", value.getValue());
                break;
            case AUTO:
            case MIN_CONTENT:
            case MAX_CONTENT:
            case FIT_CONTENT:
            case STRETCH:
                // No additional data needed
                break;
            case CALC:
                // CalcExpression cannot be easily serialized, use placeholder
                tag.putString("value", "calc(...)");
                break;
        }

        return tag;
    }

    public static LengthPercentageAuto decodeLengthPercentageAuto(Tag tag) {
        if (!(tag instanceof CompoundTag compoundTag)) {
            return LengthPercentageAuto.auto();
        }

        String typeName = compoundTag.getString("type");
        LengthPercentageAuto.Type type;
        try {
            type = LengthPercentageAuto.Type.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return LengthPercentageAuto.auto();
        }

        return switch (type) {
            case LENGTH -> LengthPercentageAuto.length(compoundTag.getFloat("value"));
            case PERCENT -> LengthPercentageAuto.percent(compoundTag.getFloat("value"));
            case AUTO -> LengthPercentageAuto.auto();
            case MIN_CONTENT -> LengthPercentageAuto.minContent();
            case MAX_CONTENT -> LengthPercentageAuto.maxContent();
            case FIT_CONTENT -> LengthPercentageAuto.fitContent();
            case STRETCH -> LengthPercentageAuto.stretch();
            case CALC -> LengthPercentageAuto.length(0); // Cannot deserialize calc, default to 0
        };
    }

    // ==================== LPARect Codec ====================
    public static final Codec<LPARect> LPA_RECT_CODEC = LDLibExtraCodecs.TAG.xmap(
            TaffyCodecs::decodeLPARect,
            TaffyCodecs::encodeLPARect
    );

    public static Tag encodeLPARect(LPARect lpaRect) {
        var tag = new CompoundTag();
        tag.put("left", encodeLengthPercentageAuto(lpaRect.rect().left));
        tag.put("right", encodeLengthPercentageAuto(lpaRect.rect().right));
        tag.put("top", encodeLengthPercentageAuto(lpaRect.rect().top));
        tag.put("bottom", encodeLengthPercentageAuto(lpaRect.rect().bottom));
        return tag;
    }

    public static LPARect decodeLPARect(Tag tag) {
        if (!(tag instanceof CompoundTag compoundTag)) {
            return new LPARect(
                    TaffyRect.all(LengthPercentageAuto.auto())
            );
        }

        LengthPercentageAuto left = decodeLengthPercentageAuto(compoundTag.get("left"));
        LengthPercentageAuto right = decodeLengthPercentageAuto(compoundTag.get("right"));
        LengthPercentageAuto top = decodeLengthPercentageAuto(compoundTag.get("top"));
        LengthPercentageAuto bottom = decodeLengthPercentageAuto(compoundTag.get("bottom"));

        return new LPARect(
                new TaffyRect<>(left, right, top, bottom)
        );
    }

    // ==================== GridPlacement Codec ====================

    public static final Codec<GridPlacement> GRID_PLACEMENT_CODEC = LDLibExtraCodecs.TAG.xmap(
            TaffyCodecs::decodeGridPlacement,
            TaffyCodecs::encodeGridPlacement
    );

    public static Tag encodeGridPlacement(GridPlacement placement) {
        var tag = new CompoundTag();
        tag.putString("type", placement.getType().name());

        switch (placement.getType()) {
            case AUTO:
                // No additional data needed
                break;
            case LINE:
                tag.putInt("value", placement.getValue());
                break;
            case NAMED_LINE:
                tag.putString("lineName", placement.getLineName());
                tag.putInt("nthIndex", placement.getNthIndex());
                break;
            case SPAN:
                tag.putInt("value", placement.getValue());
                break;
            case NAMED_SPAN:
                tag.putString("lineName", placement.getLineName());
                tag.putInt("value", placement.getValue());
                break;
        }

        return tag;
    }

    public static GridPlacement decodeGridPlacement(Tag tag) {
        if (!(tag instanceof CompoundTag compoundTag)) {
            return GridPlacement.auto();
        }

        String typeName = compoundTag.getString("type");
        GridPlacement.Type type;
        try {
            type = GridPlacement.Type.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return GridPlacement.auto();
        }

        return switch (type) {
            case AUTO -> GridPlacement.auto();
            case LINE -> GridPlacement.line(compoundTag.getInt("value"));
            case NAMED_LINE -> GridPlacement.namedLine(
                    compoundTag.getString("lineName"),
                    compoundTag.getInt("nthIndex")
            );
            case SPAN -> GridPlacement.span(compoundTag.getInt("value"));
            case NAMED_SPAN -> GridPlacement.namedSpan(
                    compoundTag.getString("lineName"),
                    compoundTag.getInt("value")
            );
        };
    }

    // ==================== Grid Codec ====================

    public static final Codec<Grid> GRID_CODEC = LDLibExtraCodecs.TAG.xmap(
            TaffyCodecs::decodeGrid,
            TaffyCodecs::encodeGrid
    );

    public static Tag encodeGrid(Grid grid) {
        var tag = new CompoundTag();
        tag.put("start", encodeGridPlacement(grid.grid().start));
        tag.put("end", encodeGridPlacement(grid.grid().end));
        return tag;
    }

    public static Grid decodeGrid(Tag tag) {
        if (!(tag instanceof CompoundTag compoundTag)) {
            return Grid.EMPTY;
        }

        GridPlacement start = decodeGridPlacement(compoundTag.get("start"));
        GridPlacement end = decodeGridPlacement(compoundTag.get("end"));

        return new Grid(new TaffyLine<>(start, end));
    }

    // ==================== GridTemplateAreas Codec ====================

    public static final Codec<GridTemplateAreas> GRID_TEMPLATE_AREAS_CODEC = LDLibExtraCodecs.TAG.xmap(
            TaffyCodecs::decodeGridTemplateAreas,
            TaffyCodecs::encodeGridTemplateAreas
    );

    public static Tag encodeGridTemplateAreas(GridTemplateAreas gridTemplateAreas) {
        var tag = new CompoundTag();

        // Encode list of GridTemplateArea objects
        var areasList = new ListTag();
        for (GridTemplateArea area : gridTemplateAreas.areas()) {
            var areaTag = new CompoundTag();
            areaTag.putString("name", area.getName());
            areaTag.putInt("rowStart", area.getRowStart());
            areaTag.putInt("rowEnd", area.getRowEnd());
            areaTag.putInt("columnStart", area.getColumnStart());
            areaTag.putInt("columnEnd", area.getColumnEnd());
            areasList.add(areaTag);
        }
        tag.put("areas", areasList);

        return tag;
    }

    public static GridTemplateAreas decodeGridTemplateAreas(Tag tag) {
        if (!(tag instanceof CompoundTag compoundTag)) {
            return GridTemplateAreas.EMPTY;
        }

        // Decode list of GridTemplateArea objects
        ListTag areasTag = compoundTag.getList("areas", Tag.TAG_COMPOUND);
        List<GridTemplateArea> areas = new ArrayList<>();
        for (int i = 0; i < areasTag.size(); i++) {
            CompoundTag areaTag = areasTag.getCompound(i);
            String name = areaTag.getString("name");
            int rowStart = areaTag.getInt("rowStart");
            int rowEnd = areaTag.getInt("rowEnd");
            int columnStart = areaTag.getInt("columnStart");
            int columnEnd = areaTag.getInt("columnEnd");
            areas.add(new GridTemplateArea(name, rowStart, rowEnd, columnStart, columnEnd));
        }

        return new GridTemplateAreas(areas);
    }
}
