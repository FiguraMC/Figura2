package org.figuramc.figura.avatars.components;

import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.AvatarComponent;
import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.model.part.VanillaRootModelPart;
import org.figuramc.figura.util.RenderUtils;
import org.figuramc.figura.vanillamodel.ModelPartTracker;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Component that manages vanilla model parts.
 * Requires an EntityUser component before it in the list.
 */
public class VanillaParts implements AvatarComponent {

    public boolean isLivingEntityRenderer; // Whether this uses a LivingEntityRenderer

    // Mapping from vanilla model parts to the corresponding FiguraModelPart. Refreshes on resource reload.
    public final Map<ModelPart, VanillaRootModelPart> partMap = new IdentityHashMap<>();
    // Does not refresh. Can eventually change through scripts though.
    // Note: Values in this map don't necessarily always have a corresponding value in partMap; since there might
    // not be a `ModelPart` that connects to it. However, compat with other mods can involve looking at this map.
    public final Map<String, VanillaRootModelPart> partNameMap = new HashMap<>();

    private EntityUser entityUserComponent;

    @Override
    public void initialize(AvatarMaterials materials, Avatar<?> self) throws AvatarLoadingException {
        // Depends on Textures and EntityUser
        Textures texturesComponent = self.assertDependency(Textures.class, getClass());
        entityUserComponent = self.assertDependency(EntityUser.class, getClass());
        // Store entries in partNameMap
        for (var entry : materials.vanillaPartRoots().entrySet()) {
            VanillaRootModelPart part = new VanillaRootModelPart(entry.getValue(), texturesComponent.textures);
            partNameMap.put(entry.getKey(), part);
        }
    }

    // Regenerate the part map when the entity user changes
    @Override
    public boolean tick() {
        if (entityUserComponent.changed()) {
            regeneratePartMap();
        }
        return false;
    }

    @Override
    public void destroy() {
        for (VanillaRootModelPart root : partNameMap.values())
            root.destroy();
    }

    // Regenerate the part map by looking at the ModelPartTracker and the partNameMap.
    public void regeneratePartMap() {
        // Return early if there's no entity to generate from
        Entity entity = entityUserComponent.getEntity();
        if (entity == null) return;
        // (Re)generate the part map
        partMap.clear();
        EntityRenderer<?> renderer = RenderUtils.getRenderer(entity);
        List<ModelPart> vanillaModelParts = ModelPartTracker.traceEntityRenderer(renderer);
        vanillaModelParts.stream().flatMap(ModelPart::getAllParts).forEach(vanillaPart -> {
            String fullName = ModelPartTracker.getFullName(vanillaPart, "/");
            VanillaRootModelPart part = partNameMap.get(fullName);
            if (part != null) partMap.put(vanillaPart, part);
        });
        // Set the variable for whether it's a living entity
        isLivingEntityRenderer = renderer instanceof LivingEntityRenderer<?,?>;
    }

    // If the setup is valid, updates the part map. Returns true if successful.
    public boolean updatePartMap(String name, VanillaRootModelPart value) {
        Entity entity = entityUserComponent.getEntity();
        if (entity == null) return false;
        EntityRenderer<?> renderer = RenderUtils.getRenderer(entity);
        @Nullable ModelPart vanillaModelPart = ModelPartTracker.getModelPartByName(renderer, name);
        if (vanillaModelPart == null) return false;
        partMap.put(vanillaModelPart, value);
        return true;
    }

}
