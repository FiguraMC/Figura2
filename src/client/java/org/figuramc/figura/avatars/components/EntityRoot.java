package org.figuramc.figura.avatars.components;

import net.minecraft.client.renderer.MultiBufferSource;
import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.AvatarComponent;
import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.model.part.FiguraModelPart;
import org.figuramc.figura.model.renderers.Renderable;
import org.figuramc.figura.util.FiguraTransformStack;
import org.jetbrains.annotations.Nullable;

public class EntityRoot implements AvatarComponent {

    public static final int ID = AvatarComponent.createId(Textures.class);
    public int getId() { return ID; }

    private final Renderable<FiguraModelPart> root;

    // Vanilla rendering parameter if possible, this will allow mimics to work
    public EntityRoot(AvatarMaterials materials, Textures texturesComponent, @Nullable VanillaRendering vanillaRendering) {
        // Create the model part from materials
        root = new Renderable<>(new FiguraModelPart(materials.entityRoot(), null, texturesComponent.textures, vanillaRendering));
    }

    // Ensure called after initialize()
    public FiguraModelPart getModelPart() {
        return root.part;
    }

    // Render the entity root.
    public void render(Avatar<?> self, MultiBufferSource bufferSource, FiguraTransformStack matrixStack, float tickDelta, int light, int overlay) {
        self.tryRenderModelPart(root.renderer, bufferSource, matrixStack, tickDelta, light, overlay);
    }

    @Override
    public void destroy() {
        root.destroy();
    }
}
