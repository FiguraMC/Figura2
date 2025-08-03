package org.figuramc.figura.mixin.client.vanilla_parts.render;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.figuramc.figura.FiguraModClient;
import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.components.VanillaRendering;
import org.figuramc.figura.script_hooks.callback.ScriptCallback;
import org.figuramc.figura.script_hooks.callback.items.CallbackItem;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.Map;

/**
 * When the model part is rendered, find the current avatar and current part data.
 * Store the data so avatars can access it, and also read the avatar's data to modify the part's visual.
 */
@SuppressWarnings({"UnreachableCode", "DataFlowIssue"}) // IntelliJ doesn't like our accessor casts
@Mixin(ModelPart.class)
public abstract class ModelPartRenderMixin {

    @Shadow public float x, y, z, xRot, yRot, zRot, xScale, yScale, zScale;
    @Shadow public boolean visible = true;

    @Shadow private PartPose initialPose;
    @Shadow public boolean skipDraw;

    @Shadow protected abstract void compile(PoseStack.Pose pose, VertexConsumer vertexConsumer, int i, int j, int k);

    @Shadow @Final public Map<String, ModelPart> children;
    @Unique private static Avatar<?> currentAvatar = null;
    @Unique private static VanillaRendering currentComponent = null;
    @Unique private static VanillaRendering.VanillaPart currentPart = null;

    @WrapMethod(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V")
    public void wrapRender(PoseStack poseStack, VertexConsumer vertexConsumer, int i, int j, int k, Operation<Void> original) {
        // We make a custom render impl if Figura is involved with the part...
        // this could maybe cause compat issues, TODO look into any such problems
        if (checkFiguraInvolvement()) {
            figuraCustomRender(poseStack, vertexConsumer, i, j, k);
            resetFiguraInvolvement();
        } else {
            original.call(poseStack, vertexConsumer, i, j, k);
        }
    }

    // Custom re-implementation of render() method which respects Figura
    @Unique private void figuraCustomRender(PoseStack poseStack, VertexConsumer vertexConsumer, int i, int j, int k) {
        if (this.visible) {
            poseStack.pushPose();
            runCallbacks(currentPart.vanillaRenderCallbacks);
            applyTransforms(poseStack);
            if (currentPart.figuraTransform.getVisible() && !currentComponent.hideAllModelParts && !this.skipDraw) {
                this.compile(poseStack.last(), vertexConsumer, i, j, k);
            }
            for (ModelPart part : this.children.values()) {
                currentPart = currentComponent.partMap.get(part); // Update currentPart
                if (currentPart != null)
                    ((ModelPartRenderMixin) (Object) part).figuraCustomRender(poseStack, vertexConsumer, i, j, k);
                else
                    part.render(poseStack, vertexConsumer, i, j, k);
            }
            poseStack.popPose();
        }
    }

    // Cause translateAndRotate() to respect Figura's customizations
    @WrapMethod(method = "translateAndRotate")
    public void wrapTranslateAndRotate(PoseStack poseStack, Operation<Void> original) {
        if (checkFiguraInvolvement()) {
            applyTransforms(poseStack);
            resetFiguraInvolvement();
        } else {
            original.call(poseStack);
        }
    }

    // Return true if Figura is involved in the current part;
    // If this returns true, then the "current" static fields will be set.
    @Unique private boolean checkFiguraInvolvement() {
        if (currentPart != null) return true;
        Avatar<?> peeked = FiguraModClient.AVATAR_RENDERING_STACK.peek();
        if (peeked == null) return false;
        VanillaRendering vanillaRendering = peeked.getComponent(VanillaRendering.TYPE);
        if (vanillaRendering == null) return false;
        VanillaRendering.VanillaPart scriptPart = vanillaRendering.partMap.get((ModelPart) (Object) this);
        if (scriptPart == null) return false;
        currentAvatar = peeked;
        currentComponent = vanillaRendering;
        currentPart = scriptPart;
        return true;
    }

    @Unique private static void resetFiguraInvolvement() {
        currentAvatar = null;
        currentComponent = null;
        currentPart = null;
    }

    @Unique private void runCallbacks(List<ScriptCallback<CallbackItem.Unit, CallbackItem.Unit>> callbacks) {
        // TODO try to provide info to error messages of which VanillaPart an error occurred on
        for (ScriptCallback<CallbackItem.Unit, CallbackItem.Unit> callback : callbacks)
            callback.call(CallbackItem.Unit.INSTANCE);
    }

    // Scratch math objects
    @Unique private static final Quaternionf TEMPQUAT = new Quaternionf();
    @Unique private static final Vector3f TEMPVEC = new Vector3f();

    // Apply transforms to the pose stack, much the same way vanilla would.
    @Unique private void applyTransforms(PoseStack poseStack) {
        // Detect if we need to invert X/Y pos/rot because of living-entity-ness:
        float inv = currentComponent.entityRenderer instanceof LivingEntityRenderer<?,?,?> ? -1f : 1f;

        // For each property, either:
        // - Cancel the vanilla transform, only applying the figura transform
        // - Apply both the vanilla transform and the figura transform
        // Depending on whether the entity is cancelling the vanilla transform.

        // Origin:
        TEMPVEC.set(currentPart.figuraTransform.getOrigin()); // Figura-applied amount
        currentPart.storedVanillaOrigin.set(TEMPVEC);
        TEMPVEC.mul(inv, inv, 1); // Apply inversions
        if (currentPart.cancelVanillaOrigin) {
            TEMPVEC.add(initialPose.x(), initialPose.y(), initialPose.z());
        } else {
            TEMPVEC.add(x, y, z);
            currentPart.storedVanillaOrigin.add(inv * (x - initialPose.x()), inv * (y - initialPose.y()), z - initialPose.z());
        }
        TEMPVEC.mul(1.0f / 16);
        poseStack.translate(TEMPVEC.x, TEMPVEC.y, TEMPVEC.z);

        // Rotation:
        TEMPVEC.set(currentPart.figuraTransform.getEulerRad()); // Figura-applied amount
        currentPart.storedVanillaRotation.set(TEMPVEC);
        TEMPVEC.mul(inv, inv, 1); // Apply inversions
        if (currentPart.cancelVanillaRotation) {
            TEMPVEC.add(initialPose.xRot(), initialPose.yRot(), initialPose.zRot());
        } else {
            TEMPVEC.add(xRot, yRot, zRot);
            currentPart.storedVanillaRotation.add(inv * (xRot - initialPose.xRot()), inv * (yRot - initialPose.yRot()), zRot - initialPose.zRot());
        }
        TEMPQUAT.rotationZYX(TEMPVEC.z, TEMPVEC.y, TEMPVEC.x);
        poseStack.mulPose(TEMPQUAT);

        // Scale:
        TEMPVEC.set(currentPart.figuraTransform.getScale()); // Figura-applied amount
        currentPart.storedVanillaScale.set(TEMPVEC);
        if (currentPart.cancelVanillaScale) {
            TEMPVEC.mul(initialPose.xScale(), initialPose.yScale(), initialPose.zScale());
        } else {
            TEMPVEC.mul(xScale, yScale, zScale);
            currentPart.storedVanillaScale.mul(xScale, yScale, zScale);
        }
        poseStack.scale(TEMPVEC.x, TEMPVEC.y, TEMPVEC.z);

        // Position (only comes from Figura):
        TEMPVEC.set(currentPart.figuraTransform.getPosition());
        currentPart.storedVanillaPosition.set(TEMPVEC);
        TEMPVEC.mul(inv, inv, 1);
        poseStack.translate(TEMPVEC.x / 16f, TEMPVEC.y / 16f, TEMPVEC.z / 16f);
    }

}
