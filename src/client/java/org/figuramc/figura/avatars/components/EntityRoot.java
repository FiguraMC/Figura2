package org.figuramc.figura.avatars.components;

import net.minecraft.client.renderer.MultiBufferSource;
import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.AvatarComponent;
import org.figuramc.figura.avatars.AvatarModules;
import org.figuramc.figura.model.part.FiguraModelPart;
import org.figuramc.figura.model.renderers.Renderable;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.util.FiguraTransformStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;

public class EntityRoot implements AvatarComponent<EntityRoot> {

    public static final Type<EntityRoot> TYPE = new Type<>(Textures.TYPE);
    public Type<EntityRoot> getType() { return TYPE; }

    private final Renderable<FiguraModelPart> root;

    // Vanilla rendering parameter if possible, this will allow mimics to work
    public EntityRoot(List<AvatarModules.LoadTimeModule> modules, @Nullable AllocationTracker allocationTracker, Textures texturesComponent, @Nullable VanillaRendering vanillaRendering) {
        // Wrap the entity roots of each module into a new wrapper part
        int name = 0;
        LinkedHashMap<String, FiguraModelPart> roots = new LinkedHashMap<>();
        for (AvatarModules.LoadTimeModule mod : modules) {
            if (mod.materials.entityRoot() == null) continue;
            FiguraModelPart part = new FiguraModelPart(mod.materials.entityRoot(), null, mod.index, texturesComponent, vanillaRendering);
            // Also store the parts in the module objects to be later accessed
            mod.entityRoot = part;
            roots.put(Integer.toString(name++), part);
        }

        // Return a wrapper around each of them
        this.root = new Renderable<>(new FiguraModelPart(null, roots));
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
