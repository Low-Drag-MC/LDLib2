package com.lowdragmc.lowdraglib2.client.renderer;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.client.renderer.block.RendererBlock;
import com.lowdragmc.lowdraglib2.client.renderer.block.RendererBlockEntity;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.registry.ILDLRegisterClient;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.lowdragmc.lowdraglib2.utils.data.BlockInfo;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.TrackedDummyWorld;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.vfyjxf.taffy.style.AlignItems;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;
import org.appliedenergistics.yoga.YogaEdge;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface IRenderer extends ILDLRegisterClient<IRenderer, Supplier<IRenderer>>, IConfigurable, IPersistedSerializable {
    //region builtin renderer
    @LDLRegisterClient(name = "empty", registry = "ldlib2:renderer", environment = RegistrationEnvironment.MANUAL)
    final class EmptyRenderer implements IRenderer {
        @Override
        public IRenderer copy() { return EMPTY; }
    }
    //endregion
    EmptyRenderer EMPTY = new EmptyRenderer();

    Codec<IRenderer> CODEC = createCodec();
    Set<IRenderer> EVENT_REGISTERS = ConcurrentHashMap.newKeySet();

    static Codec<IRenderer> createCodec() {
        if (LDLib2.isClient()) {
            return LDLib2Registries.RENDERERS.optionalCodec().dispatch(ILDLRegisterClient::getRegistryHolderOptional,
                    optional -> optional.map(holder -> PersistedParser.createCodec(holder.value()).fieldOf("data"))
                            .orElseGet(() -> MapCodec.unit(EMPTY)));
        } else {
            return Codec.unit(EMPTY);
        }
    }

    @Nullable
    default CompoundTag serializeWrapper() {
        return (CompoundTag) CODEC.encodeStart(Platform.getFrozenRegistry().createSerializationContext(NbtOps.INSTANCE), this).result().orElse(null);
    }

    static IRenderer deserializeWrapper(Tag tag) {
        return CODEC.parse(Platform.getFrozenRegistry().createSerializationContext(NbtOps.INSTANCE), tag).result().orElse(EMPTY);
    }

    @OnlyIn(Dist.CLIENT)
    default IRenderer copy() {
        return deserializeWrapper(serializeWrapper());
    }

    /**
     * Render itemstack.
     */
    @OnlyIn(Dist.CLIENT)
    default void renderItem(ItemStack stack,
                    ItemDisplayContext transformType,
                    boolean leftHand, PoseStack poseStack,
                    MultiBufferSource buffer, int combinedLight,
                    int combinedOverlay, BakedModel model) {

    }

    /**
     * Render static block model.
     */
    @OnlyIn(Dist.CLIENT)
    default List<BakedQuad> renderModel(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        return Collections.emptyList();
    }

    /**
     * Gets the set of {@link RenderType render types} to use when drawing this block in the level.
     * Supported types are those returned by {@link RenderType#chunkBufferLayers()}.
     * <p>
     * By default, defers query to {@link ItemBlockRenderTypes}.
     */
    @OnlyIn(Dist.CLIENT)
    default ChunkRenderTypeSet getRenderTypes(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource rand, ModelData modelData) {
        return ItemBlockRenderTypes.getRenderLayers(state);
    }

    /**
     * Register TextureSprite here.
     */
    @OnlyIn(Dist.CLIENT)
    default void onPrepareTextureAtlas(ResourceLocation atlasName, Consumer<ResourceLocation> register) {

    }

    /**
     * Register additional models here.
     */
    @OnlyIn(Dist.CLIENT)
    default void onAdditionalModel(Consumer<ModelResourceLocation> registry) {

    }

    @OnlyIn(Dist.CLIENT)
    default void clearCache() {

    }

    /**
     * If the renderer requires event registration either {@link #onPrepareTextureAtlas} or {@link #onAdditionalModel}, call this method in the constructor.
     */
    @OnlyIn(Dist.CLIENT)
    default void registerEvent() {
        EVENT_REGISTERS.add(this);
    }

    /**
     * Does the block entity have the {@link net.minecraft.client.renderer.blockentity.BlockEntityRenderer}.
     */
    @OnlyIn(Dist.CLIENT)
    default boolean hasBlockEntityRenderer(BlockEntity blockEntity) {
        return false;
    }

    /**
     * Does the block entity render offscreen {@link net.minecraft.client.renderer.blockentity.BlockEntityRenderer#shouldRenderOffScreen(BlockEntity)}.
     */
    @OnlyIn(Dist.CLIENT)
    default boolean shouldRenderOffScreen(BlockEntity blockEntity) {
        return false;
    }

    /**
     * Get the view distance for TESR.
     */
    @OnlyIn(Dist.CLIENT)
    default int getViewDistance() {
        return 64;
    }

    /**
     * Should the TESR {@link net.minecraft.client.renderer.blockentity.BlockEntityRenderer} render.
     */
    @OnlyIn(Dist.CLIENT)
    default boolean shouldRender(BlockEntity blockEntity, Vec3 cameraPos) {
        return Vec3.atCenterOf(blockEntity.getBlockPos()).closerThan(cameraPos, this.getViewDistance());
    }

    /**
     * Render the TESR {@link net.minecraft.client.renderer.blockentity.BlockEntityRenderer}.
     */
    @OnlyIn(Dist.CLIENT)
    default void render(BlockEntity blockEntity, float partialTicks, PoseStack stack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {

    }

    /**
     * Get the particle texture.
     */
    @OnlyIn(Dist.CLIENT)
    @Nonnull
    default TextureAtlasSprite getParticleTexture(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, ModelData modelData) {
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(MissingTextureAtlasSprite.getLocation());
    }

    /**
     * Whether to apply AO for the model.
     */
    @OnlyIn(Dist.CLIENT)
    default TriState useAO() {
        return TriState.FALSE;
    }

    /**
     * Whether to apply AO for the model.
     */
    @OnlyIn(Dist.CLIENT)
    default TriState useAO(BlockState state, ModelData modelData, RenderType renderType) {
        return useAO();
    }

    /**
     * Whether to apply block light during the itemstack rendering.
     */
    @OnlyIn(Dist.CLIENT)
    default boolean useBlockLight(ItemStack stack) {
        return false;
    }

    /**
     * Should we rebake quads for mcmeta data?
     */
    @OnlyIn(Dist.CLIENT)
    default boolean reBakeCustomQuads() {
        return false;
    }

    /**
     * Offset for rebake's quads sides while {@link #reBakeCustomQuads()} return true.
     */
    @OnlyIn(Dist.CLIENT)
    default float reBakeCustomQuadsOffset() {
        return 0;
    }

    /**
     * Whether to apply gui 3d transform during itemstack rendering.
     */
    @OnlyIn(Dist.CLIENT)
    default boolean isGui3d() {
        return true;
    }

    /**
     * Return an {@link AABB} that controls the visible scope of this {@link BlockEntityRenderer}.
     * Defaults to the unit cube at the given position. {@link AABB#INFINITE} can be used to declare the BER
     * should be visible everywhere.
     *
     * @return an appropriately sized {@link AABB} for the {@link BlockEntityRenderer}
     */
    @OnlyIn(Dist.CLIENT)
    default AABB getRenderBoundingBox(BlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos());
    }

    @OnlyIn(Dist.CLIENT)
    default Scene createPreviewScene() {
        var level = new TrackedDummyWorld();
        level.addBlock(BlockPos.ZERO, BlockInfo.fromBlock(RendererBlock.BLOCK));
        Optional.ofNullable(level.getBlockEntity(BlockPos.ZERO)).ifPresent(blockEntity -> {
            if (blockEntity instanceof RendererBlockEntity holder) {
                holder.setRenderer(this);
            }
        });

        var scene = new Scene();
        scene.setRenderFacing(false);
        scene.setRenderSelect(false);
        scene.createScene(level);
        assert scene.getRenderer() != null;
        scene.getRenderer().setOnLookingAt(null); // better performance
        scene.setRenderedCore(Collections.singleton(BlockPos.ZERO), null);
        scene.layout(layout -> {
            layout.setPipelineState(StyleOrigin.DEFAULT);
            layout.setAspectRatio(1.0f);
            layout.widthPercent(80);
            layout.alignSelf(AlignItems.CENTER);
            layout.paddingAll(3);
            layout.setPipelineState(StyleOrigin.INLINE);
        });
        scene.style(style -> Style.defaultPipeline(style, s -> s.backgroundTexture(Sprites.BORDER1_RT1)));
        scene.addClass("preview_bg");
        return scene;
    }

    /**
     * Preview of the renderer.
     */
    @OnlyIn(Dist.CLIENT)
    default void createPreview(ConfiguratorGroup father) {
        father.addConfigurators(new Configurator("ldlib.gui.editor.group.preview").addChild(createPreviewScene()));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    default void buildConfigurator(ConfiguratorGroup father) {
        createPreview(father);
        IConfigurable.super.buildConfigurator(father);
    }
}
