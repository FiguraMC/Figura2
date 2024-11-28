package org.figuramc.figura.mixin.client.vanilla_parts.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.figuramc.figura.FiguraModClient;
import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.components.VanillaParts;
import org.figuramc.figura.model.part.VanillaRootModelPart;
import org.figuramc.figura.script_hooks.ScriptError;
import org.figuramc.figura.script_hooks.ScriptFunction;
import org.figuramc.figura.util.DeferredVanillaPartRenderQueue;
import org.figuramc.figura.util.FiguraMatrixStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.network.chat.Component;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ModelPart.class)
public class ModelPartRenderMixin {

    @Shadow public float x, y, z, xRot, yRot, zRot, xScale, yScale, zScale;
    @Shadow private PartPose initialPose;

    @Unique private static Avatar<?> currentAvatar = null;
    @Unique private static VanillaRootModelPart currentModelPart = null;
    @Unique private static VanillaParts currentVanillaParts = null;
    @Unique private static final FiguraMatrixStack tempMatrixStack = new FiguraMatrixStack();
    @Unique private static final Quaternionf tempQuat = new Quaternionf();

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            at = @At("HEAD")
    )
    public void preRender(PoseStack poseStack, VertexConsumer vertexConsumer, int light, int overlay, int color, CallbackInfo ci) {
        // At the start of the render, fetch the corresponding Avatar if there is one
        if (FiguraModClient.AVATAR_RENDERING_STACK.isEmpty()) { resetVars(); return; }
        Avatar<?> peeked = FiguraModClient.AVATAR_RENDERING_STACK.peek();
        if (peeked == null) { resetVars(); return; }
        VanillaParts vanillaParts = peeked.getComponent(VanillaParts.class);
        if (vanillaParts == null) { resetVars(); return; }
        VanillaRootModelPart modelPart = vanillaParts.partMap.get(this);
        if (modelPart == null) { resetVars(); return; }
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
                for (ScriptFunction callback : currentModelPart.vanillaRenderCallbacks)
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
            if (currentModelPart.getCancelVanillaOrigin()) {
                // Affect only by offsets
                poseStack.translate(currentModelPart.originOffset.x * inv / 16, currentModelPart.originOffset.y * inv / 16, currentModelPart.originOffset.z / 16);
                tempMatrixStack.translate(currentModelPart.originOffset.x / 16, currentModelPart.originOffset.y / 16 + yOffset, currentModelPart.originOffset.z / 16);
            } else {
                // Affect by vanilla AND offsets
                poseStack.translate((this.x + currentModelPart.originOffset.x * inv) / 16, (this.y + currentModelPart.originOffset.y * inv) / 16, (this.z + currentModelPart.originOffset.z) / 16);
                tempMatrixStack.translate((this.x * inv + currentModelPart.originOffset.x) / 16, (this.y * inv + currentModelPart.originOffset.y) / 16 + yOffset, (this.z + currentModelPart.originOffset.z) / 16);
            }
            // Rotation:
            if (currentModelPart.getCancelVanillaRotation()) {
                // Affect only by offset
                poseStack.mulPose(tempQuat.rotationZYX(currentModelPart.rotationOffset.z, currentModelPart.rotationOffset.y * inv, currentModelPart.rotationOffset.x * inv));
                tempMatrixStack.rotate(tempQuat.rotationZYX(currentModelPart.rotationOffset.z, currentModelPart.rotationOffset.y, currentModelPart.rotationOffset.x));
            } else {
                // Affect by vanilla AND offsets
                poseStack.mulPose(tempQuat.rotationZYX(this.zRot + currentModelPart.rotationOffset.z, this.yRot + currentModelPart.rotationOffset.y * inv, this.xRot + currentModelPart.rotationOffset.x * inv));
                tempMatrixStack.rotate(tempQuat.rotationZYX(this.zRot + currentModelPart.rotationOffset.z, this.yRot * inv + currentModelPart.rotationOffset.y, this.xRot * inv + currentModelPart.rotationOffset.x));
            }
            // Scale:
            if (currentModelPart.getCancelVanillaScale()) {
                // Affect only by multiplier
                poseStack.scale(currentModelPart.scaleMultiplier.x, currentModelPart.scaleMultiplier.y, currentModelPart.scaleMultiplier.z);
                tempMatrixStack.scale(currentModelPart.scaleMultiplier.x, currentModelPart.scaleMultiplier.y, currentModelPart.scaleMultiplier.z);
            } else {
                // Affect by vanilla AND multiplier
                poseStack.scale(this.xScale * currentModelPart.scaleMultiplier.x, this.yScale * currentModelPart.scaleMultiplier.y, this.zScale * currentModelPart.scaleMultiplier.z);
                tempMatrixStack.scale(this.xScale * currentModelPart.scaleMultiplier.x, this.yScale * currentModelPart.scaleMultiplier.y, this.zScale * currentModelPart.scaleMultiplier.z);
            }
            // Position (has no vanilla analogue to cancel):
            poseStack.translate(currentModelPart.positionOffset.x * inv, currentModelPart.positionOffset.y * inv, currentModelPart.positionOffset.z);
            tempMatrixStack.translate(currentModelPart.positionOffset.x, currentModelPart.positionOffset.y, currentModelPart.positionOffset.z);

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
        // If the model part's vanilla transform is invisible, cancel out!
        if (currentModelPart != null && !currentModelPart.getVanillaVisible())
            ci.cancel();
    }

}
