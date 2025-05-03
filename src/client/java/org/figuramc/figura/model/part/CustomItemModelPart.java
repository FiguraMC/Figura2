package org.figuramc.figura.model.part;

import org.figuramc.figura.avatars.components.VanillaRendering;
import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.model.texture.AvatarTexture;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

/**
 * A root which works for rendering a custom item.
 * Contains information about how to transform the item in different contexts.
 */
public class CustomItemModelPart extends FiguraModelPart {

    public final ItemTransforms itemTransforms;

    public CustomItemModelPart(AvatarMaterials.ModelPartMaterials materials, Map<ItemDisplayContext, AvatarMaterials.ItemPartTransform> transforms, List<AvatarTexture> textures, @Nullable VanillaRendering vanilla) {
        super(materials, null, textures, vanilla);
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

    private static ItemTransform getTransformOrNone(ItemDisplayContext context, Map<ItemDisplayContext, AvatarMaterials.ItemPartTransform> transforms) {
        if (transforms.containsKey(context)) {
            AvatarMaterials.ItemPartTransform transform = transforms.get(context);
            return new ItemTransform(transform.rotation(), transform.translation().mul(1.0f / 16, new Vector3f()), transform.scale());
        }
        return ItemTransform.NO_TRANSFORM;
    }

    private static ItemTransform getTransformOrNone(ItemDisplayContext context, Map<ItemDisplayContext, AvatarMaterials.ItemPartTransform> transforms, ItemDisplayContext fallback) {
        if (transforms.containsKey(context)) {
            AvatarMaterials.ItemPartTransform transform = transforms.get(context);
            return new ItemTransform(transform.rotation(), transform.translation().mul(1.0f / 16, new Vector3f()), transform.scale());
        }
        return getTransformOrNone(fallback, transforms);
    }

}
