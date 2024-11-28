package org.figuramc.figura.mixin.client.vanilla_parts;

import net.minecraft.client.model.PlayerModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Evil mixin
 *
 * JUSTIFICATION:
 * The "default" position as given in the createMesh method is inaccurate. It lists the default position
 * as 2.5 for the slim skin, and 2.0 for the non-slim skin. However, in HumanoidEntityModel, the pivot is
 * unconditionally set to exactly 2 in all cases, no matter the skin type.
 *
 * Additionally, the Blockbench preset for the player model has the pivot point as 2 in both skin types, meaning
 * some kind of workaround would be necessary if I wish to support the "Minecraft Skin" preset from Blockbench.
 * I chose this, as it was the easiest, and keeps the code the cleanest that I could think of.
 *
 * If anyone else's mod depends on these incorrect default transforms provided by Minecraft, then I apologize.
 */
@Mixin(PlayerModel.class)
public class PlayerModelMixin {

    @Unique
    private static float adjust(float origOffset) {
        // Just ensuring I did this mixin right:
        if (origOffset != 2.5f)
            throw new IllegalStateException("Expected offset to be 2.5? Mixin error!");
        return 2f;
    }

    @ModifyArg(method = "createMesh", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/geom/PartPose;offset(FFF)Lnet/minecraft/client/model/geom/PartPose;",
            ordinal = 1
    ), index = 1)
    private static float adjustSlimOffset1(float offset) { return adjust(offset); }

    @ModifyArg(method = "createMesh", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/geom/PartPose;offset(FFF)Lnet/minecraft/client/model/geom/PartPose;",
            ordinal = 2
    ), index = 1)
    private static float adjustSlimOffset2(float offset) { return adjust(offset); }

    @ModifyArg(method = "createMesh", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/geom/PartPose;offset(FFF)Lnet/minecraft/client/model/geom/PartPose;",
            ordinal = 3
    ), index = 1)
    private static float adjustSlimOffset3(float offset) { return adjust(offset); }

    @ModifyArg(method = "createMesh", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/geom/PartPose;offset(FFF)Lnet/minecraft/client/model/geom/PartPose;",
            ordinal = 4
    ), index = 1)
    private static float adjustSlimOffset4(float offset) { return adjust(offset); }

}
