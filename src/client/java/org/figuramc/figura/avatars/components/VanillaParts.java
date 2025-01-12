package org.figuramc.figura.avatars.components;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.FiguraModClient;
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
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Component that manages vanilla model parts.
 * Requires an EntityUser component before it in the list.
 */
public class VanillaParts implements AvatarComponent {

    // Whether this uses a LivingEntityRenderer
    public boolean isLivingEntityRenderer;
    // Whether to cancel all vanilla model part rendering
    public boolean cancelAllModelParts;

    // Mapping from vanilla model parts to the corresponding FiguraModelPart. Refreshes occasionally.
    public final Map<ModelPart, VanillaRootModelPart> partMap = new IdentityHashMap<>();
    // Does not refresh, can only be added to. Can eventually change through scripts though.
    // Note: Values in this map don't necessarily always have a corresponding value in partMap; since there might
    // not be a `ModelPart` that connects to it. However, compat with other mods can involve looking at this map.
    public final Map<String, VanillaRootModelPart> partNameMap = new HashMap<>();

    // Whether a part map refresh is needed. Starts true.
    private boolean needsPartMapRefresh = true;

    private Textures texturesComponent;
    private EntityUser entityUserComponent;

    @Override
    public void initialize(AvatarMaterials materials, Avatar<?> self) throws AvatarLoadingException {
        // Depends on Textures and EntityUser
        texturesComponent = self.assertDependency(Textures.class, getClass());
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
        // If we need a refresh, or the entity changed, regenerate.
        if (needsPartMapRefresh || entityUserComponent.changed()) {
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
        EntityRenderer<?,?> renderer = RenderUtils.getRenderer(entity);

        // Iterate the part name map
        for (var partByName : partNameMap.entrySet()) {
            String name = partByName.getKey();
            VanillaRootModelPart figuraPart = partByName.getValue();
            // If there's a corresponding vanilla part, use it
            @Nullable ModelPart vanillaPart = ModelPartTracker.getModelPartByName(renderer, name);
            if (vanillaPart != null) partMap.put(vanillaPart, figuraPart);
            else FiguraMod.LOGGER.warn("Part with vanilla root \"" + name + "\" does not have an analogue");
        }
        // Set the variable for whether it's a living entity
        isLivingEntityRenderer = renderer instanceof LivingEntityRenderer<?,?,?>;
        // Mark as not needing a refresh
        needsPartMapRefresh = false;
    }

    // Get the part with the given name, if any. If there is none, add a new,
    // empty part to partNameMap with the given name and return it.
    public VanillaRootModelPart getOrCreatePart(String name) {
        VanillaRootModelPart part = partNameMap.get(name);
        if (part != null) return part;
        // Create new part with empty, default materials, store in map, and return
        VanillaRootModelPart newPart = new VanillaRootModelPart(emptyMaterials, texturesComponent.textures);
        partNameMap.put(name, newPart);
        // We need a refresh!
        needsPartMapRefresh = true;
        return newPart;
    }
    private static final AvatarMaterials.VanillaRootPartMaterials emptyMaterials = new AvatarMaterials.VanillaRootPartMaterials(
            new AvatarMaterials.ModelPartMaterials("root", new Vector3f(), new Vector3f(), List.of(), -1, List.of(), List.of()),
            new MutableBoolean(false)
    );

}
