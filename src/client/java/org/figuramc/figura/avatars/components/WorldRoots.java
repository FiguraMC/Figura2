package org.figuramc.figura.avatars.components;

import org.figuramc.figura.avatars.AvatarComponent;
import org.figuramc.figura.avatars.AvatarModules;
import org.figuramc.figura.data.ModuleMaterials;
import org.figuramc.figura.model.part.WorldRootedModelPart;
import org.figuramc.figura.model.renderers.Renderable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WorldRoots implements AvatarComponent {

    public static final int ID = AvatarComponent.createId(Textures.class);
    public int getId() { return ID; }

    private final List<Renderable<WorldRootedModelPart>> worldRoots;

    public WorldRoots(AvatarModules modules, Textures textures, @Nullable VanillaRendering vanillaRendering) {
        worldRoots = new ArrayList<>();
        for (AvatarModules.Module module : modules.modules)
            for (ModuleMaterials.ModelPartMaterials worldRoot : module.materials.worldRoots())
                worldRoots.add(new Renderable<>(new WorldRootedModelPart(worldRoot, module.index, textures, vanillaRendering)));
    }

    // Destroy renderer data
    @Override
    public void destroy() {
        for (var root : worldRoots) root.destroy();
    }
}
