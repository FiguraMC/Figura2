package org.figuramc.figura.mixin.client.item_render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.figuramc.figura.FiguraModClient;
import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.components.CustomItems;
import org.figuramc.figura.model.optimized.RenderingMode;
import org.figuramc.figura.model.part.CustomItemModelPart;
import org.figuramc.figura.model.part.RootModelPart;
import org.figuramc.figura.script_hooks.ScriptError;
import org.figuramc.figura.util.FiguraMatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @Unique private static final FiguraMatrixStack MATRIX_STACK = new FiguraMatrixStack();
    @Unique private static final Quaternionf TEMP_QUAT = new Quaternionf(); // Temporary for calculations

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void renderCustomItem(ItemStack itemStack, ItemDisplayContext itemDisplayContext, boolean bl, PoseStack poseStack, MultiBufferSource multiBufferSource, int light, int overlay, BakedModel bakedModel, CallbackInfo ci) {
        // Look for an overriding model part
        if (FiguraModClient.AVATAR_RENDERING_STACK.isEmpty()) return;
        Avatar<?> avatar = FiguraModClient.AVATAR_RENDERING_STACK.peek();
        CustomItems itemsComponent;
        if (avatar == null || (itemsComponent = avatar.getComponent(CustomItems.class)) == null) return;
        RootModelPart modelPart = itemsComponent.getModelPart(itemStack, itemDisplayContext);
        if (modelPart == null) return;
        // If we found one, render it:
        MATRIX_STACK.setFromVanilla(poseStack);
        if (modelPart instanceof CustomItemModelPart customModel) {
            // Custom model, apply its own transforms:
            applyTransform(customModel.itemTransforms.getTransform(itemDisplayContext), bl);
            MATRIX_STACK.translate(0, -0.5f, 0); // Undo BB's 8-unit translation they like to do
        } else {
            // PNG model, apply default item transforms...
            if (itemDisplayContext == ItemDisplayContext.GROUND) {
                MATRIX_STACK.translate(0, 0.125f, 0);
                MATRIX_STACK.scale(0.5f, 0.5f, 0.5f);
            }
            MATRIX_STACK.translate(-0.5f, -0.5f, -0.5f);
        }
        float tickDelta = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        try {
            if (RenderingMode.isOptimized())
                modelPart.renderOptimized(MATRIX_STACK, tickDelta);
            else
                modelPart.renderImmediate(multiBufferSource, MATRIX_STACK, tickDelta, light, overlay);
        } catch (ScriptError ex) {
            avatar.error(Component.literal("Error inside model part render callback"), ex);
        } catch (StackOverflowError ex) {
            avatar.error(Component.literal("Stack overflow during part rendering - tree too deep!"), ex);
        } catch (Throwable other) {
            avatar.error(Component.literal("Unexpected error during model part rendering"), other);
        }

        // And cancel out of the original rendering
        ci.cancel();
    }

    /**
     * Copy-pasted from ItemTransform class, changed to use our custom matrix stack to avoid allocating
     */
    @Unique
    private static void applyTransform(ItemTransform transform, boolean bl) {
        if (transform == ItemTransform.NO_TRANSFORM) {
            return;
        }
        float f = transform.rotation.x();
        float g = transform.rotation.y();
        float h = transform.rotation.z();
        if (bl) {
            g = -g;
            h = -h;
        }
        int i = bl ? -1 : 1;
        MATRIX_STACK.translate((float)i * transform.translation.x(), transform.translation.y(), transform.translation.z());
        MATRIX_STACK.rotate(TEMP_QUAT.rotationXYZ(f * ((float)Math.PI / 180), g * ((float)Math.PI / 180), h * ((float)Math.PI / 180)));
        MATRIX_STACK.scale(transform.scale.x(), transform.scale.y(), transform.scale.z());
    }

}
