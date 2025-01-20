package org.figuramc.figura.avatars.components;

import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.AvatarComponent;
import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.model.renderers.FiguraRenderers;
import org.figuramc.figura.model.part.RootModelPart;
import org.figuramc.figura.script_hooks.ScriptError;
import org.figuramc.figura.util.FiguraTransformStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class EntityRoot implements AvatarComponent {

    private RootModelPart modelPart;

    @Override
    public void initialize(AvatarMaterials materials, Avatar<?> self) throws AvatarLoadingException {
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
    public void render(Avatar<?> self, float tickDelta, MultiBufferSource bufferSource, FiguraTransformStack matrixStack, int light, int overlay) {
        try {
            FiguraRenderers.getCurrentRenderer().render(modelPart, bufferSource, matrixStack, tickDelta, light, overlay);
        } catch (ScriptError ex) {
            self.error(Component.literal("Error inside model part render callback"), ex);
        } catch (StackOverflowError ex) {
            self.error(Component.literal("Stack overflow during part rendering - tree too deep!"), ex);
        } catch (Throwable other) {
            self.error(Component.literal("Unexpected error during model part rendering"), other);
        }
    }

    @Override
    public void destroy() {
        modelPart.destroy();
    }
}
