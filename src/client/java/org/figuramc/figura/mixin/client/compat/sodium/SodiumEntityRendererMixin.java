package org.figuramc.figura.mixin.client.compat.sodium;

import com.mojang.blaze3d.vertex.PoseStack;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import org.figuramc.figura.FiguraModClient;
import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.components.VanillaParts;
import org.figuramc.figura.model.part.VanillaRootModelPart;
import org.figuramc.figura.script_hooks.ScriptError;
import org.figuramc.figura.script_hooks.ScriptCallback;
import org.figuramc.figura.util.DeferredVanillaPartRenderQueue;
import org.figuramc.figura.util.FiguraMatrixStack;
import me.jellysquid.mods.sodium.client.render.immediate.model.EntityRenderer;
import me.jellysquid.mods.sodium.client.render.immediate.model.ModelCuboid;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.ArrayUtils;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Basically just mimics ModelPartRenderMixin, because the logic is essentially the same.
 */
@IfModLoaded("sodium")
@Mixin(EntityRenderer.class)
public class SodiumEntityRendererMixin {

    @Unique private static Avatar<?> currentAvatar = null;
    @Unique private static VanillaRootModelPart currentModelPart = null;
    @Unique private static VanillaParts currentVanillaParts = null;
    @Unique private static FiguraMatrixStack tempMatrixStack = new FiguraMatrixStack();
    @Unique private static Quaternionf tempQuat = new Quaternionf();

    @Inject(method = "render", at = @At("HEAD"))
    private static void preRender(PoseStack matrixStack, VertexBufferWriter writer, ModelPart part, int light, int overlay, int color, CallbackInfo ci) {
        if (FiguraModClient.AVATAR_RENDERING_STACK.isEmpty()) { resetVars(); return; }
        Avatar<?> peeked = FiguraModClient.AVATAR_RENDERING_STACK.peek();
        if (peeked == null) { resetVars(); return; }
        VanillaParts vanillaParts = peeked.getComponent(VanillaParts.class);
        if (vanillaParts == null) { resetVars(); return; }
        VanillaRootModelPart modelPart = vanillaParts.partMap.get(part);
        if (modelPart == null) { resetVars(); return; }
        currentAvatar = peeked;
        currentModelPart = modelPart;
        currentVanillaParts = vanillaParts;
    }

    @Unique
    private static void resetVars() {
        currentAvatar = null;
        currentModelPart = null;
        currentVanillaParts = null;
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/apache/commons/lang3/ArrayUtils;isEmpty([Ljava/lang/Object;)Z"
            )
    )
    private static boolean cubesNeverEmpty(Object[] array) {
        if (currentModelPart != null)
            return false;
        return ArrayUtils.isEmpty(array);
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/geom/ModelPart;translateAndRotate(Lcom/mojang/blaze3d/vertex/PoseStack;)V"
            )
    )
    private static void maybeOverrideTransforms(ModelPart instance, PoseStack poseStack) {
        if (currentModelPart != null) {

            // Fetch the values into storage:
            if (currentVanillaParts.isLivingEntityRenderer) {
                currentModelPart.storedVanillaOrigin.x = -instance.x;
                currentModelPart.storedVanillaOrigin.y = -instance.y;
                currentModelPart.storedVanillaRotation.x = -instance.xRot;
                currentModelPart.storedVanillaRotation.y = -instance.yRot;
            } else {
                currentModelPart.storedVanillaOrigin.x = instance.x;
                currentModelPart.storedVanillaOrigin.y = instance.y;
                currentModelPart.storedVanillaRotation.x = instance.xRot;
                currentModelPart.storedVanillaRotation.y = instance.yRot;
            }
            currentModelPart.storedVanillaOrigin.z = instance.z;
            currentModelPart.storedVanillaRotation.z = instance.zRot;
            currentModelPart.storedVanillaScale.set(instance.xScale, instance.yScale, instance.zScale);

            // Run the callbacks:
            try {
                for (ScriptCallback callback : currentModelPart.vanillaRenderCallbacks)
                    callback.call();
            } catch (ScriptError ex) {
                currentAvatar.error(Component.literal("Error inside vanilla part callback"), ex); // Todo translate
                resetVars();
                return;
            }

            // Fetch the current matrices:
            tempMatrixStack.setFromVanilla(poseStack);
            float inv = 1f; // Whether to invert X and Y pos and rot
            float yOffset = 0f; // Extra offset on the y-axis for our renderer
            if (currentVanillaParts.isLivingEntityRenderer) {
                tempMatrixStack.translate(0, 1.5f, 0);
                tempMatrixStack.scale(-1, -1, 1);
                inv = -1f;
                yOffset = 1.5f;
            }

            // Apply transforms!

            // Origin:
            Vector3f origin = currentModelPart.vanillaTransform.getOrigin();
            if (currentModelPart.cancelVanillaOrigin) {
                // Affect only by offsets
                poseStack.translate(origin.x * inv / 16, origin.y * inv / 16, origin.z / 16);
                tempMatrixStack.translate(origin.x / 16, origin.y / 16 + yOffset, origin.z / 16);
            } else {
                // Affect by vanilla AND offsets
                poseStack.translate((instance.x + origin.x * inv) / 16, (instance.y + origin.y * inv) / 16, (instance.z + origin.z) / 16);
                tempMatrixStack.translate((instance.x * inv + origin.x) / 16, (instance.y * inv + origin.y) / 16 + yOffset, (instance.z + origin.z) / 16);
            }
            // Rotation:
            Vector3f rotation = currentModelPart.vanillaTransform.getEulerRad();
            if (currentModelPart.cancelVanillaRotation) {
                // Affect only by offset
                poseStack.mulPose(tempQuat.rotationZYX(rotation.z, rotation.y * inv, rotation.x * inv));
                tempMatrixStack.rotate(tempQuat.rotationZYX(rotation.z, rotation.y, rotation.x));
            } else {
                // Affect by vanilla AND offsets
                poseStack.mulPose(tempQuat.rotationZYX(instance.zRot + rotation.z, instance.yRot + rotation.y * inv, instance.xRot + rotation.x * inv));
                tempMatrixStack.rotate(tempQuat.rotationZYX(instance.zRot + rotation.z, instance.yRot * inv + rotation.y, instance.xRot * inv + rotation.x));
            }
            // Scale:
            Vector3f scale = currentModelPart.vanillaTransform.getScale();
            if (currentModelPart.cancelVanillaScale) {
                // Affect only by multiplier
                poseStack.scale(scale.x, scale.y, scale.z);
                tempMatrixStack.scale(scale.x, scale.y, scale.z);
            } else {
                // Affect by vanilla AND multiplier
                poseStack.scale(instance.xScale * scale.x, instance.yScale * scale.y, instance.zScale * scale.z);
                tempMatrixStack.scale(instance.xScale * scale.x, instance.yScale * scale.y, instance.zScale * scale.z);
            }
            // Position (has no vanilla analogue to cancel):
            Vector3f position = currentModelPart.vanillaTransform.getPosition();
            poseStack.translate(position.x * inv, position.y * inv, position.z);
            tempMatrixStack.translate(position.x, position.y, position.z);

        } else {
            // Otherwise, just do the usual
            instance.translateAndRotate(poseStack);
        }
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/immediate/model/EntityRenderer;renderCuboids(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/caffeinemc/mods/sodium/api/vertex/buffer/VertexBufferWriter;[Lme/jellysquid/mods/sodium/client/render/immediate/model/ModelCuboid;III)V",
                    shift = At.Shift.AFTER
            )
    )
    private static void renderModelPart(PoseStack matrixStack, VertexBufferWriter writer, ModelPart part, int light, int overlay, int color, CallbackInfo ci) {
        if (currentModelPart != null)
            DeferredVanillaPartRenderQueue.insert(currentAvatar, currentModelPart, tempMatrixStack.peekPosition(), tempMatrixStack.peekNormal(), light, overlay);
    }

    @Inject(
            method = "renderCuboids",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void maybeCancelVanillaRender(PoseStack.Pose matrices, VertexBufferWriter writer, ModelCuboid[] cuboids, int light, int overlay, int color, CallbackInfo ci) {
        // If the model part's vanilla transform is invisible, cancel out!
        if (currentModelPart != null && !currentModelPart.vanillaTransform.getVisible())
            ci.cancel();
    }

}
