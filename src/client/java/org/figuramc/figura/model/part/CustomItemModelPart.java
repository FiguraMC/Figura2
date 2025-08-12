package org.figuramc.figura.model.part;

import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.world.item.ItemDisplayContext;
import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.avatars.AvatarModules;
import org.figuramc.figura.avatars.components.MolangStateComponent;
import org.figuramc.figura.avatars.components.Textures;
import org.figuramc.figura.avatars.components.VanillaRendering;
import org.figuramc.figura.data.ModuleMaterials;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Map;

/**
 * A root which works for rendering a custom item.
 * Contains information about how to transform the item in different contexts.
 */
public class CustomItemModelPart extends FigmodelModelPart {

    public final ItemTransforms itemTransforms;

    public static final int SIZE_ESTIMATE =
            AllocationTracker.OBJECT_SIZE
            + AllocationTracker.REFERENCE_SIZE
            // ItemTransforms
            + AllocationTracker.OBJECT_SIZE
            + AllocationTracker.REFERENCE_SIZE * 8
            // ItemTransform instances
            + (AllocationTracker.OBJECT_SIZE + AllocationTracker.VEC3F_SIZE * 3) * 8;

    public CustomItemModelPart(AvatarModules.LoadTimeModule module, ModuleMaterials.FigmodelMaterials materials, Map<ItemDisplayContext, ModuleMaterials.ItemPartTransform> transforms, @Nullable AllocationTracker allocationTracker, Textures texturesComponent, @Nullable MolangStateComponent molangState, @Nullable VanillaRendering vanilla) throws AvatarError {
        super(module, materials, allocationTracker, texturesComponent, molangState, vanilla);
        this.itemTransforms = new ItemTransforms(
                getTransformOrNone(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, transforms, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND),
                getTransformOrNone(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, transforms),
                getTransformOrNone(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, transforms, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND),
                getTransformOrNone(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, transforms),
                getTransformOrNone(ItemDisplayContext.HEAD, transforms),
                getTransformOrNone(ItemDisplayContext.GUI, transforms),
                getTransformOrNone(ItemDisplayContext.GROUND, transforms),
                getTransformOrNone(ItemDisplayContext.FIXED, transforms)
        );
        if (allocationTracker != null)
            allocationTracker.track(this, SIZE_ESTIMATE);
    }

    private static ItemTransform getTransformOrNone(ItemDisplayContext context, Map<ItemDisplayContext, ModuleMaterials.ItemPartTransform> transforms) {
        if (transforms.containsKey(context)) {
            ModuleMaterials.ItemPartTransform transform = transforms.get(context);
            return new ItemTransform(transform.rotation(), transform.translation().mul(1.0f / 16, new Vector3f()), transform.scale());
        }
        return ItemTransform.NO_TRANSFORM;
    }

    private static ItemTransform getTransformOrNone(ItemDisplayContext context, Map<ItemDisplayContext, ModuleMaterials.ItemPartTransform> transforms, ItemDisplayContext fallback) {
        if (transforms.containsKey(context)) {
            ModuleMaterials.ItemPartTransform transform = transforms.get(context);
            return new ItemTransform(transform.rotation(), transform.translation().mul(1.0f / 16, new Vector3f()), transform.scale());
        }
        return getTransformOrNone(fallback, transforms);
    }

}
