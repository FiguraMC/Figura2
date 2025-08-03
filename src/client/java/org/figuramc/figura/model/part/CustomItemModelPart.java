package org.figuramc.figura.model.part;

import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.world.item.ItemDisplayContext;
import org.figuramc.figura.avatars.components.Textures;
import org.figuramc.figura.avatars.components.VanillaRendering;
import org.figuramc.figura.data.ModuleMaterials;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Map;

/**
 * A root which works for rendering a custom item.
 * Contains information about how to transform the item in different contexts.
 */
public class CustomItemModelPart extends FiguraModelPart {

    public final ItemTransforms itemTransforms;

    public CustomItemModelPart(ModuleMaterials.ModelPartMaterials materials, Map<ItemDisplayContext, ModuleMaterials.ItemPartTransform> transforms, int moduleIndex, Textures texturesComponent, @Nullable VanillaRendering vanilla) {
        super(materials, null, moduleIndex, texturesComponent, vanilla);
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
