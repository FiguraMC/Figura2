package org.figuramc.figura.avatars.components;

import net.minecraft.client.renderer.MultiBufferSource;
import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.AvatarComponent;
import org.figuramc.figura.avatars.AvatarModules;
import org.figuramc.figura.data.ModuleMaterials;
import org.figuramc.figura.model.part.FiguraModelPart;
import org.figuramc.figura.model.renderers.Renderable;
import org.figuramc.figura.util.FiguraTransformStack;
import org.figuramc.figura.util.ListUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EntityRoot implements AvatarComponent {

    public static final int ID = AvatarComponent.createId(Textures.class);
    public int getId() { return ID; }

    private final Renderable<FiguraModelPart> root;

    // Vanilla rendering parameter if possible, this will allow mimics to work
    public EntityRoot(AvatarModules modules, Textures texturesComponent, @Nullable VanillaRendering vanillaRendering) {
        // Wrap the entity roots of each module into a new wrapper part
        List<FiguraModelPart> roots = ListUtils.mapNonNull(modules.modules, mod -> {
            if (mod.materials.entityRoot() == null) return null;
            FiguraModelPart part = new FiguraModelPart(mod.materials.entityRoot(), null, mod.index, texturesComponent, vanillaRendering);
            // Also store the parts in the module objects to be later accessed
            mod.entityRoot = part;
            return part;
        });
        this.root = new Renderable<>(new FiguraModelPart("entity_root", null, roots));
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
