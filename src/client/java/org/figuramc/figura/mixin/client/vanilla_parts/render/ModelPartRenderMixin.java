package org.figuramc.figura.mixin.client.vanilla_parts.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.figuramc.figura.FiguraModClient;
import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.avatars.components.VanillaParts;
import org.figuramc.figura.model.part.VanillaRootModelPart;
import org.figuramc.figura.script_hooks.ScriptError;
import org.figuramc.figura.util.DeferredVanillaPartRenderQueue;
import org.figuramc.figura.util.FiguraTransformStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.vanillamodel.ModelPartTracker;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@SuppressWarnings({"UnreachableCode", "SuspiciousMethodCalls"})
@Mixin(ModelPart.class)
public class ModelPartRenderMixin {

    @Shadow public float x, y, z, xRot, yRot, zRot, xScale, yScale, zScale;

    @Unique private static Avatar<?> currentAvatar = null;
    @Unique private static VanillaRootModelPart currentModelPart = null;
    @Unique private static VanillaParts currentVanillaParts = null;
    @Unique private static final FiguraTransformStack tempMatrixStack = new FiguraTransformStack();
    @Unique private static final Quaternionf tempQuat = new Quaternionf();

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            at = @At("HEAD")
    )
    public void preRender(PoseStack poseStack, VertexConsumer vertexConsumer, int light, int overlay, int color, CallbackInfo ci) {
        // At the start of the render, fetch the corresponding Avatar if there is one
        Avatar<?> peeked = FiguraModClient.AVATAR_RENDERING_STACK.peek();
        if (peeked == null) { resetVars(); return; }
        VanillaParts vanillaParts = peeked.getComponent(VanillaParts.class);
        if (vanillaParts == null) { resetVars(); return; }
        VanillaRootModelPart modelPart = vanillaParts.partMap.get(this);
        if (!vanillaParts.cancelAllModelParts && modelPart == null) { resetVars(); return; }
        // All checks are passed, let's go
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

    // Rendering skips if the cubes are empty, but the Avatar model might want to render things anyway.
    // So if there's a FiguraModelPart currently rendering, pretend the list of cubes is not empty,
    // and continue the render operation.
    @Redirect(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;isEmpty()Z"
            )
    )
    public boolean cubesNeverEmpty(List<?> instance) {
        if (currentModelPart != null)
            return false;
        return instance.isEmpty();
    }

    @Inject(
            method = "translateAndRotate",
            at = @At("HEAD"),
            cancellable = true
    )
    public void maybeOverrideTransforms(PoseStack poseStack, CallbackInfo ci) {
        if (currentModelPart != null) {

            // Fetch the values into storage:
            if (currentVanillaParts.isLivingEntityRenderer) {
                currentModelPart.storedVanillaOrigin.x = -this.x;
                currentModelPart.storedVanillaOrigin.y = -this.y;
                currentModelPart.storedVanillaRotation.x = -this.xRot;
                currentModelPart.storedVanillaRotation.y = -this.yRot;
            } else {
                currentModelPart.storedVanillaOrigin.x = this.x;
                currentModelPart.storedVanillaOrigin.y = this.y;
                currentModelPart.storedVanillaRotation.x = this.xRot;
                currentModelPart.storedVanillaRotation.y = this.yRot;
            }
            currentModelPart.storedVanillaOrigin.z = this.z;
            currentModelPart.storedVanillaRotation.z = this.zRot;
            currentModelPart.storedVanillaScale.set(this.xScale, this.yScale, this.zScale);

            // Run the callbacks:

            try {
                currentModelPart.invokeCallbacks(currentModelPart.vanillaRenderCallbacks);
            } catch (ScriptError ex) {
                // Get vanilla part's name in a component, for informational purposes
                Component partName;
                String str = ModelPartTracker.getAliasOrFullName((ModelPart) (Object) this);
                if (str == null) partName = Component.translatable("figura.error.runtime.unnamed_vanilla_part");
                else partName = Component.literal(str);
                // Error out
                currentAvatar.error(new AvatarError("figura.error.runtime.vanilla_part_callback", ex, true, partName));
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
                poseStack.translate((this.x + origin.x * inv) / 16, (this.y + origin.y * inv) / 16, (this.z + origin.z) / 16);
                tempMatrixStack.translate((this.x * inv + origin.x) / 16, (this.y * inv + origin.y) / 16 + yOffset, (this.z + origin.z) / 16);
            }
            // Rotation:
            Vector3f rotation = currentModelPart.vanillaTransform.getEulerRad();
            if (currentModelPart.cancelVanillaRotation) {
                // Affect only by offset
                poseStack.mulPose(tempQuat.rotationZYX(rotation.z, rotation.y * inv, rotation.x * inv));
                tempMatrixStack.rotate(tempQuat.rotationZYX(rotation.z, rotation.y, rotation.x));
            } else {
                // Affect by vanilla AND offsets
                poseStack.mulPose(tempQuat.rotationZYX(this.zRot + rotation.z, this.yRot + rotation.y * inv, this.xRot + rotation.x * inv));
                tempMatrixStack.rotate(tempQuat.rotationZYX(this.zRot + rotation.z, this.yRot * inv + rotation.y, this.xRot * inv + rotation.x));
            }
            // Scale:
            Vector3f scale = currentModelPart.vanillaTransform.getScale();
            if (currentModelPart.cancelVanillaScale) {
                // Affect only by multiplier
                poseStack.scale(scale.x, scale.y, scale.z);
                tempMatrixStack.scale(scale.x, scale.y, scale.z);
            } else {
                // Affect by vanilla AND multiplier
                poseStack.scale(this.xScale * scale.x, this.yScale * scale.y, this.zScale * scale.z);
                tempMatrixStack.scale(this.xScale * scale.x, this.yScale * scale.y, this.zScale * scale.z);
            }
            // Position (has no vanilla analogue to cancel):
            Vector3f position = currentModelPart.vanillaTransform.getPosition();
            poseStack.translate(position.x * inv, position.y * inv, position.z);
            tempMatrixStack.translate(position.x, position.y, position.z);

            // Cancel the vanilla code
            ci.cancel();
        }
    }

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/geom/ModelPart;compile(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
                    shift = At.Shift.AFTER
            )
    )
    public void renderModelPart(PoseStack poseStack, VertexConsumer vertexConsumer, int light, int overlay, int color, CallbackInfo ci) {
        if (currentModelPart != null)
            DeferredVanillaPartRenderQueue.insert(currentAvatar, currentModelPart, tempMatrixStack.peekPosition(), tempMatrixStack.peekNormal(), light, overlay);
    }

    @Inject(
            method = "compile",
            at = @At("HEAD"),
            cancellable = true
    )
    private void maybeCancelVanillaRender(PoseStack.Pose pose, VertexConsumer vertexConsumer, int i, int j, int k, CallbackInfo ci) {
        // If all model parts are hidden, or the model part's vanilla transform is invisible, cancel out!
        if (currentVanillaParts != null && (currentVanillaParts.cancelAllModelParts || (currentModelPart != null && !currentModelPart.vanillaTransform.getVisible())))
            ci.cancel();
    }

}
