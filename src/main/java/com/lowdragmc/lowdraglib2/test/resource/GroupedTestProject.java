package com.lowdragmc.lowdraglib2.test.resource;

import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.project.ProjectType;
import com.lowdragmc.lowdraglib2.editor.resource.ColorsResource;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceGroup;
import com.lowdragmc.lowdraglib2.editor.resource.Resources;
import com.lowdragmc.lowdraglib2.editor.resource.TexturesResource;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * A project whose resource types are partly grouped: two groups, each added to in two places, and two types
 * with tabs of their own.
 */
public class GroupedTestProject implements IProject {
    public static final ProjectType TYPE = ProjectType.of("project.test_grouped", ".test_grouped.nbt", GroupedTestProject::new);

    public static final ResourceGroup ALPHA = ResourceGroup.of("test_alpha", "test_alpha");
    /** A second instance with the same id: a group is its id, so it must land with the others. */
    public static final ResourceGroup ALPHA_AGAIN = new ResourceGroup("test_alpha", Component.literal("ignored"));
    public static final ResourceGroup BETA = new ResourceGroup("test_beta", Component.literal("Beta"));

    public static final GroupedColors ALPHA_ONE = new GroupedColors("test_alpha_one", "Alpha One", Icons.ADD);
    public static final GroupedColors ALPHA_TWO = new GroupedColors("test_alpha_two", "Alpha Two", Icons.REMOVE);
    public static final GroupedColors ALPHA_THREE = new GroupedColors("test_alpha_three", "Alpha Three", Icons.COPY);
    public static final GroupedColors BETA_ONE = new GroupedColors("test_beta_one", "Beta One", Icons.EYE);
    public static final GroupedColors BETA_TWO = new GroupedColors("test_beta_two", "Beta Two", Icons.EDIT_FILE);

    @Getter
    private final Resources resources;

    public GroupedTestProject() {
        this.resources = Resources.builder()
                .add(ColorsResource.INSTANCE)
                .group(ALPHA, ALPHA_ONE, ALPHA_TWO)
                .group(BETA, BETA_ONE)
                .add(TexturesResource.INSTANCE)
                .group(ALPHA_AGAIN, ALPHA_THREE)
                .group(BETA, BETA_TWO)
                .build();
    }

    /**
     * The same types grouped another way, as a second editor would: the built-in colours under beta, Alpha One on
     * its own.
     */
    public static Resources regrouped() {
        return Resources.builder()
                .group(BETA, ColorsResource.INSTANCE, BETA_ONE, BETA_TWO)
                .add(ALPHA_ONE)
                .group(ALPHA, ALPHA_TWO, ALPHA_THREE)
                .add(TexturesResource.INSTANCE)
                .build();
    }

    @Override
    public ProjectType getProjectType() {
        return TYPE;
    }

    @Override
    public CompoundTag serializeProject(@NotNull HolderLookup.Provider provider) {
        return new CompoundTag();
    }

    @Override
    public void deserializeProject(@NotNull HolderLookup.Provider provider, @NotNull CompoundTag nbt) {
    }

    public static class GroupedColors extends ColorsResource {
        private final String name;
        private final String displayName;
        private final IGuiTexture icon;

        public GroupedColors(String name, String displayName, IGuiTexture icon) {
            this.name = name;
            this.displayName = displayName;
            this.icon = icon;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Component getDisplayName() {
            return Component.literal(displayName);
        }

        @Override
        public IGuiTexture getIcon() {
            return icon;
        }
    }
}
