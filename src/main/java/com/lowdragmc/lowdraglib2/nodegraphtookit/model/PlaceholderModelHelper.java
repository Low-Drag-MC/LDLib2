package com.lowdragmc.lowdraglib2.nodegraphtookit.model;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.mojang.serialization.DataResult;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@UtilityClass
public final class PlaceholderModelHelper {

    /**
     * Retrieves a {@link GraphElementModel} instance from the provided {@link GraphModel} if the model
     * identified by the specified {@link UUID} is an instance of {@code IPlaceHolder}.
     *
     * @param graphModel The {@link GraphModel} from which the model should be retrieved. Must not be null.
     * @param originalModelGuid The {@link UUID} that identifies the model to be retrieved. Must not be null.
     * @return The corresponding {@link GraphElementModel} if the retrieved model is an instance of {@code IPlaceHolder},
     *         or {@code null} otherwise.
     */
    @Nullable
    public static GraphElementModel getPlaceholderGraphElementModel(GraphModel graphModel, UUID originalModelGuid) {
        if (graphModel.getModel(originalModelGuid) instanceof IPlaceHolder placeHolder) {
            return (GraphElementModel) placeHolder;
        }
        return null;
    }

    /**
     * Configures the specified {@link GraphElementModel} with placeholder capabilities.
     * This allows the model to be both deletable and selectable by setting the respective capabilities.
     * Before applying the new capabilities, the method clears any existing capabilities on the model.
     *
     * @param model the {@link GraphElementModel} to which the placeholder capabilities will be applied.
     *              Must not be null.
     */
    public static void setPlaceholderCapabilities(GraphElementModel model) {
        model.clearCapabilities();
        model.setCapability(Capabilities.DELETABLE, true);
        model.setCapability(Capabilities.SELECTABLE, true);
    }

    public static DataResult<GraphElementModel> tryGetPlaceholderGraphElementModel(GraphModel graphModel, UUID originalUid) {
        if (graphModel.getModel(originalUid) instanceof IPlaceHolder placeHolder) {
            return DataResult.success((GraphElementModel) placeHolder);
        }
        return DataResult.error(() -> "No placeholder with UID " + originalUid + " found in graph " + graphModel.getUid());
    }
}
