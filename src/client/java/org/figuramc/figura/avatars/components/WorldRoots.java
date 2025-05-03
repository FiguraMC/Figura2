package org.figuramc.figura.avatars.components;

import org.figuramc.figura.avatars.AvatarComponent;
import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.model.part.WorldRootedModelPart;
import org.figuramc.figura.model.renderers.Renderable;
import org.figuramc.figura.util.ListUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WorldRoots implements AvatarComponent {

    public static final int ID = AvatarComponent.createId(Textures.class);
    public int getId() { return ID; }

    private final List<Renderable<WorldRootedModelPart>> worldRoots;

    public WorldRoots(AvatarMaterials materials, Textures textures, @Nullable VanillaRendering vanillaRendering) {
        worldRoots = ListUtils.map(materials.worldRoots(), mats -> new Renderable<>(new WorldRootedModelPart(mats, textures.textures, vanillaRendering)));
    }

    // Destroy renderer data
    @Override
    public void destroy() {
        for (var root : worldRoots) root.destroy();
    }
}
