package com.lowdragmc.lowdraglib2.gui.ui.layout;

import com.lowdragmc.lowdraglib2.gui.ui.data.*;
import com.lowdragmc.lowdraglib2.utils.LDLibExtraCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.*;

import lombok.experimental.UtilityClass;
import net.minecraft.nbt.*;

import java.util.Optional;

@UtilityClass
public final class TaffyCodecs {
    // ==================== LengthPercentage Codec ====================
    public static final Codec<LengthPercentage> LP_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LDLibExtraCodecs.enumCodec(LengthPercentage.Type.class, LengthPercentage.Type.LENGTH)
                    .fieldOf("type").forGetter(LengthPercentage::getType),
            Codec.FLOAT.optionalFieldOf("value", 0f).forGetter(LengthPercentage::getValue)
    ).apply(instance, (type, value) -> switch (type) {
        case LENGTH -> LengthPercentage.length(value);
        case PERCENT -> LengthPercentage.percent(value);
        case CALC -> LengthPercentage.ZERO;
    }));

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
                tag.put("value", LP_CODEC.encodeStart(NbtOps.INSTANCE, func.getFixedValue())
                        .result().orElseGet(() -> IntTag.valueOf(0)));
                break;
            case FIT_CONTENT:
                tag.put("limit", LP_CODEC.encodeStart(NbtOps.INSTANCE, func.getFitContentArgument())
                        .result().orElseGet(() -> IntTag.valueOf(0)));
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
                var value = LP_CODEC.parse(NbtOps.INSTANCE, compoundTag.get("value")).result().orElse(LengthPercentage.ZERO);
                yield TrackSizingFunction.fixed(value);
            }
            case MIN_CONTENT -> TrackSizingFunction.minContent();
            case MAX_CONTENT -> TrackSizingFunction.maxContent();
            case FIT_CONTENT -> {
                var limit = LP_CODEC.parse(NbtOps.INSTANCE, compoundTag.get("limit")).result().orElse(LengthPercentage.ZERO);
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
    public static final Codec<GridRepetition> GRID_REPETITION_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LDLibExtraCodecs.enumCodec(GridRepetition.RepetitionType.class, GridRepetition.RepetitionType.COUNT)
                    .fieldOf("type").forGetter(GridRepetition::getType),
            Codec.INT.optionalFieldOf("count", 0).forGetter(GridRepetition::getCount),
            TRACK_SIZING_FUNCTION_CODEC.listOf().fieldOf("tracks").forGetter(GridRepetition::getTracks)
    ).apply(instance, (type, count, tracks) -> switch (type) {
        case COUNT -> GridRepetition.count(count, tracks);
        case AUTO_FILL -> GridRepetition.autoFill(tracks);
        case AUTO_FIT -> GridRepetition.autoFit(tracks);
    }));

    // ==================== GridTemplateComponent Codec ====================
    public static final Codec<GridTemplateComponent> GRID_TEMPLATE_COMPONENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LDLibExtraCodecs.enumCodec(GridTemplateComponent.Type.class, GridTemplateComponent.Type.SINGLE)
                    .fieldOf("type").forGetter(GridTemplateComponent::getType),
            TRACK_SIZING_FUNCTION_CODEC.optionalFieldOf("single").forGetter(component -> Optional.ofNullable(component.getSingle())),
            GRID_REPETITION_CODEC.optionalFieldOf("repeat").forGetter(component -> Optional.ofNullable(component.getRepeat()))
    ).apply(instance, (type, single, repeat) -> switch (type) {
        case SINGLE -> GridTemplateComponent.single(single.orElseThrow());
        case REPEAT -> GridTemplateComponent.repeat(repeat.orElseThrow());
    }));

    // ==================== NamedGridLine Codec ====================
    public static final Codec<NamedGridLine> NAMED_GRID_LINE_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("name").forGetter(NamedGridLine::getName),
                    Codec.INT.fieldOf("index").forGetter(NamedGridLine::getIndex)
            ).apply(instance, NamedGridLine::new)
    );

    // ==================== GridTemplate Codec ====================
    public static final Codec<GridTemplate> GRID_TEMPLATE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TRACK_SIZING_FUNCTION_CODEC.listOf().fieldOf("simples").forGetter(GridTemplate::simples),
            GRID_TEMPLATE_COMPONENT_CODEC.listOf().fieldOf("repeats").forGetter(GridTemplate::repeats),
            NAMED_GRID_LINE_CODEC.listOf().fieldOf("names").forGetter(GridTemplate::names)
    ).apply(instance, GridTemplate::new));

    // ==================== GridAuto Codec ====================
    public static final Codec<GridAuto> GRID_AUTO_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TRACK_SIZING_FUNCTION_CODEC.listOf().fieldOf("values").forGetter(GridAuto::values)
    ).apply(instance, GridAuto::new));

    // ==================== LengthPercentageAuto Codec ====================
    public static final Codec<LengthPercentageAuto> LPA_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LDLibExtraCodecs.enumCodec(LengthPercentageAuto.Type.class, LengthPercentageAuto.Type.AUTO)
                    .fieldOf("type").forGetter(LengthPercentageAuto::getType),
            Codec.FLOAT.optionalFieldOf("value", 0f).forGetter(LengthPercentageAuto::getValue)
    ).apply(instance, (type, value) -> switch (type) {
        case LENGTH -> LengthPercentageAuto.length(value);
        case PERCENT -> LengthPercentageAuto.percent(value);
        case AUTO, CALC -> LengthPercentageAuto.auto();
        case MIN_CONTENT -> LengthPercentageAuto.minContent();
        case MAX_CONTENT -> LengthPercentageAuto.maxContent();
        case FIT_CONTENT -> LengthPercentageAuto.fitContent();
        case STRETCH -> LengthPercentageAuto.stretch();
    }));

    @Deprecated(since = "26.1")
    public static final Codec<LengthPercentageAuto> LPA_STYLE_LENGTH_COMPAT_CODEC = LDLibExtraCodecs.compat(LPA_CODEC, YogaCodecs.STYLE_LENGTH_CODEC.xmap(
            styleLength -> {
                if (styleLength.isPercent()) return LengthPercentageAuto.length(styleLength.value().getValue() / 100f);
                if (styleLength.isPoints()) return LengthPercentageAuto.length(styleLength.value().getValue());
                return LengthPercentageAuto.auto();
            },
            lpa -> {
                throw new IllegalArgumentException("Cannot convert TaffyDimension to StyleLength");
            }
    ));

    // ==================== LPARect Codec ====================
    public static final Codec<LPARect> LPA_RECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LPA_CODEC.fieldOf("left").forGetter(rect -> rect.rect().left),
            LPA_CODEC.fieldOf("right").forGetter(rect -> rect.rect().right),
            LPA_CODEC.fieldOf("top").forGetter(rect -> rect.rect().top),
            LPA_CODEC.fieldOf("bottom").forGetter(rect -> rect.rect().bottom)
    ).apply(instance, (left, right, top, bottom) ->
            new LPARect(TaffyRect.of(left, right, top, bottom))));

    // ==================== GridPlacement Codec ====================
    public static final Codec<GridPlacement> GRID_PLACEMENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LDLibExtraCodecs.enumCodec(GridPlacement.Type.class, GridPlacement.Type.AUTO)
                    .fieldOf("type").forGetter(GridPlacement::getType),
            Codec.INT.optionalFieldOf("value", 0).forGetter(GridPlacement::getValue),
            Codec.STRING.optionalFieldOf("lineName").forGetter(placement -> Optional.ofNullable(placement.getLineName())),
            Codec.INT.optionalFieldOf("nthIndex", 0).forGetter(GridPlacement::getNthIndex)
    ).apply(instance, (type, value, lineName, nthIndex) -> switch (type) {
        case AUTO -> GridPlacement.auto();
        case LINE -> GridPlacement.line(value);
        case NAMED_LINE -> GridPlacement.namedLine(lineName.orElseThrow(), nthIndex);
        case SPAN -> GridPlacement.span(value);
        case NAMED_SPAN -> GridPlacement.namedSpan(lineName.orElseThrow(), value);
    }));

    // ==================== Grid Codec ====================
    public static final Codec<Grid> GRID_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GRID_PLACEMENT_CODEC.fieldOf("start").forGetter(grid -> grid.grid().start),
            GRID_PLACEMENT_CODEC.fieldOf("end").forGetter(grid -> grid.grid().end)
    ).apply(instance, (start, end) -> new Grid(new TaffyLine<>(start, end))));


    // ==================== GridTemplateAreas Codec ====================
    public static final Codec<GridTemplateArea> GRID_TEMPLATE_AREA_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(GridTemplateArea::getName),
            Codec.INT.fieldOf("rowStart").forGetter(GridTemplateArea::getRowStart),
            Codec.INT.fieldOf("rowEnd").forGetter(GridTemplateArea::getRowStart),
            Codec.INT.fieldOf("columnStart").forGetter(GridTemplateArea::getRowStart),
            Codec.INT.fieldOf("columnEnd").forGetter(GridTemplateArea::getRowStart)
    ).apply(instance, GridTemplateArea::new));

    public static final Codec<GridTemplateAreas> GRID_TEMPLATE_AREAS_CODEC = GRID_TEMPLATE_AREA_CODEC.listOf().xmap(
            GridTemplateAreas::new, GridTemplateAreas::areas);

    // ==================== TaffyDimension Codec ====================
    public static final Codec<TaffyDimension> DIMENSION_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LDLibExtraCodecs.enumCodec(TaffyDimension.Type.class, TaffyDimension.Type.AUTO)
                    .fieldOf("type").forGetter(TaffyDimension::getType),
            Codec.FLOAT.optionalFieldOf("value", 0f).forGetter(TaffyDimension::getValue)
    ).apply(instance, (((type, value) -> switch (type) {
        case LENGTH -> TaffyDimension.length(value);
        case PERCENT -> TaffyDimension.percent(value);
        case AUTO, CALC -> TaffyDimension.auto();
        case MIN_CONTENT -> TaffyDimension.minContent();
        case MAX_CONTENT -> TaffyDimension.maxContent();
        case FIT_CONTENT -> TaffyDimension.fitContent();
        case STRETCH -> TaffyDimension.stretch();
        case CONTENT -> TaffyDimension.content();
    }))));

    @Deprecated(since = "26.1")
    public static final Codec<TaffyDimension> DIMENSION_STYLE_SIZE_LENGTH_COMPAT_CODEC = LDLibExtraCodecs.compat(DIMENSION_CODEC, YogaCodecs.STYLE_SIZE_LENGTH_CODEC.xmap(
            styleSizeLength -> {
                if (styleSizeLength.isPercent()) return TaffyDimension.percent(styleSizeLength.value().getValue() / 100f);
                if (styleSizeLength.isPoints()) return TaffyDimension.length(styleSizeLength.value().getValue());
                if (styleSizeLength.isAuto()) return TaffyDimension.auto();
                if (styleSizeLength.isFitContent()) return TaffyDimension.fitContent();
                if (styleSizeLength.isMaxContent()) return TaffyDimension.maxContent();
                if (styleSizeLength.isStretch()) return TaffyDimension.stretch();
                return TaffyDimension.auto();
            },
            dimension -> {
                throw new IllegalArgumentException("Cannot convert TaffyDimension to StyleSizeLength");
            }
    ));


    // ==================== LPSize Codec ====================
    public static final Codec<LPSize> LP_SIZE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LP_CODEC.fieldOf("width").forGetter(size -> size.size().width),
            LP_CODEC.fieldOf("height").forGetter(size -> size.size().height)
    ).apply(instance, (width, height) -> new LPSize(TaffySize.of(width, height))));


    // ==================== DimensionSize Codec ====================
    public static final Codec<DimensionSize> DIMENSION_SIZE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DIMENSION_CODEC.fieldOf("width").forGetter(size -> size.size().width),
            DIMENSION_CODEC.fieldOf("height").forGetter(size -> size.size().height)
    ).apply(instance, (width, height) -> new DimensionSize(TaffySize.of(width, height))));
}
