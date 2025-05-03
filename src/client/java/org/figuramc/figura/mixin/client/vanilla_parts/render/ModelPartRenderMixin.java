package org.figuramc.figura.mixin.client.vanilla_parts.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.FiguraModClient;
import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.avatars.components.VanillaRendering;
import org.figuramc.figura.ducks.client.ModelPartTrackingAccess;
import org.figuramc.figura.script_hooks.ScriptCallback;
import org.figuramc.figura.script_hooks.ScriptError;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * When the model part is rendered, find the current avatar and current part data.
 * Store the data so avatars can access it, and also read the avatar's data to modify the part's visual.
 */
@SuppressWarnings({"UnreachableCode", "DataFlowIssue"}) // IntelliJ doesn't like our accessor casts
@Mixin(ModelPart.class)
public class ModelPartRenderMixin {

    @Shadow public float x, y, z, xRot, yRot, zRot, xScale, yScale, zScale;
    @Shadow public boolean visible = true;

    @Shadow private PartPose initialPose;
    @Unique private boolean vanillaVisible;
    @Unique private static Avatar<?> currentAvatar = null;
    @Unique private static VanillaRendering currentComponent = null;
    @Unique private static VanillaRendering.VanillaPart currentPart = null;

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            at = @At("HEAD")
    )
    public void preRender(PoseStack poseStack, VertexConsumer vertexConsumer, int light, int overlay, int color, CallbackInfo ci) {
        // At the start of the render, find out information about the current part.
        // If there is none, cancel out.
        Avatar<?> peeked = FiguraModClient.AVATAR_RENDERING_STACK.peek();
        if (peeked == null) { return; }
        VanillaRendering vanillaRendering = peeked.getComponent(VanillaRendering.ID);
        if (vanillaRendering == null) { return; }
        VanillaRendering.VanillaPart scriptPart = vanillaRendering.partMap.get((ModelPart) (Object) this);
        if (scriptPart == null) { return; }
        // All checks are passed, let's go
        currentAvatar = peeked;
        currentComponent = vanillaRendering;
        currentPart = scriptPart;
        // If this is false, Minecraft will skip all parts of rendering. However, Figura may want to run things anyway,
        // so we force it to true. We also store what the value was previously, so we can restore it at the end of the mixin.
        vanillaVisible = visible;
        visible = true;
    }

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            at = @At("RETURN")
    )
    public void postRender(PoseStack poseStack, VertexConsumer vertexConsumer, int light, int overlay, int color, CallbackInfo ci) {
        // Reset variables after rendering
        if (currentAvatar != null) {
            currentAvatar = null;
            currentComponent = null;
            currentPart = null;
            visible = vanillaVisible; // Restore the visible variable
        }
    }

    // Rendering is skipped if there are no cubes and no children, but Figura may wish to run things anyway.
    // Therefore, we force the render method to continue.
    @Redirect(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z")
    )
    public boolean maybePretendCubesNotEmpty(List instance) {
        if (currentPart != null) { return false; }
        return instance.isEmpty();
    }

    @Inject(method = "translateAndRotate", at = @At("HEAD"), cancellable = true)
    public void maybeOverrideTransforms(PoseStack poseStack, CallbackInfo ci) {
        // If we're not doing anything Figura-related, return immediately.
        if (currentPart == null) return;
        // Otherwise, perform Figura tasks, then cancel.
        copyOutValues();
        if (runCallbacks()) return;
        applyTransforms(poseStack);
        ci.cancel();
    }

    // Copy values out of the part and store them in the current part.
    // Cancel out the initial pose's origin and rotation as well
    @Unique private void copyOutValues() {
        // Certain values need to be negated if we're dealing with a living entity
        if (currentComponent.entityRenderer instanceof LivingEntityRenderer<?,?,?>) {
            currentPart.storedVanillaOrigin.x = -(this.x - initialPose.x());
            currentPart.storedVanillaOrigin.y = -(this.y - initialPose.y());
            currentPart.storedVanillaRotation.x = -(this.xRot - initialPose.xRot());
            currentPart.storedVanillaRotation.y = -(this.yRot - initialPose.yRot());
        } else {
            currentPart.storedVanillaOrigin.x = this.x - initialPose.x();
            currentPart.storedVanillaOrigin.y = this.y - initialPose.y();
            currentPart.storedVanillaRotation.x = this.xRot - initialPose.xRot();
            currentPart.storedVanillaRotation.y = this.yRot - initialPose.yRot();
        }
        currentPart.storedVanillaOrigin.z = this.z - initialPose.z();
        currentPart.storedVanillaRotation.z = this.zRot - initialPose.zRot();
        currentPart.storedVanillaScale.set(this.xScale, this.yScale, this.zScale);
    }

    // Return true if error occurred
    @Unique private boolean runCallbacks() {
        try {
            for (ScriptCallback callback : currentPart.vanillaRenderCallbacks)
                callback.call();
        } catch (ScriptError ex) {
            Component partName;
            String str = ((ModelPartTrackingAccess) this).figura$getName();
            if (str == null) partName = Component.translatable("figura.error.runtime.unnamed_vanilla_part");
            else partName = Component.literal(str);
            currentAvatar.error(new AvatarError("figura.error.runtime.vanilla_part_callback", ex, true, partName));
            return true;
        }
        return false;
    }

    // Scratch quaternion
    @Unique private static final Quaternionf tempQuat = new Quaternionf();

    // Apply transforms to the pose stack, much the same way vanilla would.
    @Unique private void applyTransforms(PoseStack poseStack) {
        // Detect if we need to invert X/Y pos/rot because of living-entity-ness:
        float inv = currentComponent.entityRenderer instanceof LivingEntityRenderer<?,?,?> ? -1f : 1f;

        // For each property, either:
        // - Cancel the vanilla transform, only applying the figura transform
        // - Apply both the vanilla transform and the figura transform
        // Depending on whether the entity is cancelling the vanilla transform.

        // Origin:
        Vector3fc origin = currentPart.figuraTransform.getOrigin();
        if (currentPart.cancelVanillaOrigin) poseStack.translate(origin.x() * inv / 16, origin.y() * inv / 16, origin.z() / 16);
        else poseStack.translate((this.x + origin.x() * inv) / 16, (this.y + origin.y() * inv) / 16, (this.z + origin.z()) / 16);

        // Rotation:
        Vector3fc rotation = currentPart.figuraTransform.getEulerRad();
        if (currentPart.cancelVanillaRotation) poseStack.mulPose(tempQuat.rotationZYX(rotation.z(), rotation.y() * inv, rotation.x() * inv));
        else poseStack.mulPose(tempQuat.rotationZYX(this.zRot + rotation.z(), this.yRot + rotation.y() * inv, this.xRot + rotation.x() * inv));

        // Scale:
        Vector3fc scale = currentPart.figuraTransform.getScale();
        if (currentPart.cancelVanillaScale) poseStack.scale(scale.x(), scale.y(), scale.z());
        else poseStack.scale(this.xScale * scale.x(), this.yScale * scale.y(), this.zScale * scale.z());

        // Position has no vanilla analogue to cancel, so only apply Figura version:
        Vector3fc position = currentPart.figuraTransform.getPosition();
        poseStack.translate(position.x() * inv, position.y() * inv, position.z());
    }

    // If the avatar says so, don't draw the vanilla part.
    @Inject(method = "compile", at = @At("HEAD"), cancellable = true)
    private void maybeCancelVanillaRendering(PoseStack.Pose pose, VertexConsumer vertexConsumer, int i, int j, int k, CallbackInfo ci) {
        if (currentPart == null) return; // If Figura is not involved here, let the normal method do its thing.
        if (!vanillaVisible) return; // If vanilla is hiding this model part, cancel.
        if (currentComponent.hideAllModelParts) { ci.cancel(); return; } // If we're hiding all model parts, cancel.
        if (!currentPart.figuraTransform.getVisible()) { ci.cancel(); return; } // If we're hiding this specific model part, cancel.
    }

}
