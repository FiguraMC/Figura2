package org.figuramc.figura.mixin.client.item_render;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.figuramc.figura.FiguraModClient;
import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.components.CustomItems;
import org.figuramc.figura.ducks.client.ItemStackRenderStateAccess;
import org.figuramc.figura.model.part.CustomItemModelPart;
import org.figuramc.figura.model.part.RootModelPart;
import org.figuramc.figura.util.FiguraTransformStack;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Add a field containing the ItemStack itself, so we can access it
@Mixin(ItemStackRenderState.class)
public class ItemStackRenderStateMixin implements ItemStackRenderStateAccess {

    // New field for storing the item stack itself, and getter/setter
    @Unique public ItemStack itemStack;
    @Override public ItemStack figura$getItemStack() { return itemStack; }
    @Override public void figura$setItemStack(ItemStack itemStack) { this.itemStack = itemStack; }

    // Shadowed
    @Shadow ItemDisplayContext displayContext;
    @Shadow boolean isLeftHand;

    @Shadow private ItemStackRenderState.LayerRenderState[] layers;
    // Static math classes for not allocating
    @Unique private static final FiguraTransformStack MATRIX_STACK = new FiguraTransformStack();
    @Unique private static final Quaternionf TEMP_QUAT = new Quaternionf(); // Temporary for calculations

    @WrapMethod(method = "render")
    public void renderWrap(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, int j, Operation<Void> original) {
        Avatar<?> peekedAvatar = FiguraModClient.AVATAR_RENDERING_STACK.peek();
        // Push null, so that model parts inside this item are not considered part of the enclosing avatar's vanilla model...
        FiguraModClient.AVATAR_RENDERING_STACK.push(null);
        // Try to override with a figura custom part:
        boolean overrode = tryFiguraOverride(peekedAvatar, itemStack, displayContext, isLeftHand, poseStack, multiBufferSource, i, j);
        // If we didn't override, then call the original method
        if (!overrode) original.call(poseStack, multiBufferSource, i, j);
        // Finally, pop the stack and assert.
        if (FiguraModClient.AVATAR_RENDERING_STACK.pop() != null)
            throw new IllegalStateException("Illegal Avatar rendering stack manipulation - either a bug in Figura, or a compat issue!");
    }

    @Inject(method = "clear", at = @At("HEAD"))
    public void alsoClearItemStack(CallbackInfo ci) {
        itemStack = null;
    }

    // Try figura override. Return true if successfully overrode with a figura part, false otherwise.
    @Unique
    private boolean tryFiguraOverride(Avatar<?> avatar, ItemStack itemStack, ItemDisplayContext itemDisplayContext, boolean bl, PoseStack poseStack, MultiBufferSource multiBufferSource, int light, int overlay) {
        // Look for an overriding model part
        CustomItems itemsComponent;
        if (avatar == null || (itemsComponent = avatar.getComponent(CustomItems.class)) == null) return false;
        RootModelPart modelPart = itemsComponent.getModelPart(itemStack, itemDisplayContext);
        if (modelPart == null) return false;
        // If we found one, render it:
        MATRIX_STACK.setFromVanilla(poseStack);
        if (modelPart instanceof CustomItemModelPart customModel) {
            // Custom model, apply its own transform if it has one.
            // Otherwise, if no custom transform is specified, fall back to the vanilla one.
            ItemTransform customOrFallbackTransform = customModel.itemTransforms.getTransform(itemDisplayContext);
            if (this.layers.length > 0 && customOrFallbackTransform.equals(ItemTransform.NO_TRANSFORM))
                customOrFallbackTransform = this.layers[0].model.getTransforms().getTransform(itemDisplayContext);
            applyTransform(customOrFallbackTransform, bl);
            MATRIX_STACK.translate(0, -0.5f, 0); // Undo BB's 8-unit translation they like to do
        } else {
            // PNG model. If possible, copy transforms from the first layer.
            if (this.layers.length > 0) {
                applyTransform(this.layers[0].model.getTransforms().getTransform(itemDisplayContext), bl);
            } else if (itemDisplayContext == ItemDisplayContext.GROUND) {
                MATRIX_STACK.translate(0, 0.125f, 0);
                MATRIX_STACK.scale(0.5f, 0.5f, 0.5f);
            }
            MATRIX_STACK.translate(-0.5f, -0.5f, -0.5f);
        }
        float tickDelta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
        avatar.tryRenderModelPart(modelPart, multiBufferSource, MATRIX_STACK, tickDelta, light, overlay);
        // Successfully overrode it!
        return true;
    }

    /**
     * Copy-pasted from ItemTransform class, changed to use our custom matrix stack to avoid some allocations
     */
    @Unique
    private static void applyTransform(ItemTransform transform, boolean isLeftHanded) {
        if (transform == ItemTransform.NO_TRANSFORM) {
            return;
        }
        float f = transform.rotation.x();
        float g = transform.rotation.y();
        float h = transform.rotation.z();
        if (isLeftHanded) {
            g = -g;
            h = -h;
        }
        int i = isLeftHanded ? -1 : 1;
        MATRIX_STACK.translate((float)i * transform.translation.x(), transform.translation.y(), transform.translation.z());
        MATRIX_STACK.rotate(TEMP_QUAT.rotationXYZ(f * ((float)Math.PI / 180), g * ((float)Math.PI / 180), h * ((float)Math.PI / 180)));
        MATRIX_STACK.scale(transform.scale.x(), transform.scale.y(), transform.scale.z());
    }


}
