package org.figuramc.figura.avatars.components;

import net.minecraft.client.renderer.MultiBufferSource;
import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.AvatarComponent;
import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.model.part.RootModelPart;
import org.figuramc.figura.util.FiguraTransformStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class EntityRoot implements AvatarComponent {

    private RootModelPart modelPart;

    @Override
    public void initialize(AvatarMaterials materials, Avatar<?> self) {
        // Depends on Textures component
        Textures texturesComponent = self.assertDependency(Textures.class, getClass());
        // Create the model part from materials
        modelPart = new RootModelPart(materials.entityRoot(), texturesComponent.textures);
    }

    // Ensure called after initialize()
    public @NotNull RootModelPart getModelPart() {
        return Objects.requireNonNull(modelPart);
    }

    // Render the entity root.
    public void render(Avatar<?> self, MultiBufferSource bufferSource, FiguraTransformStack matrixStack, float tickDelta, int light, int overlay) {
        self.tryRenderModelPart(modelPart, bufferSource, matrixStack, tickDelta, light, overlay);
    }

    @Override
    public void destroy() {
        modelPart.destroy();
    }
}
