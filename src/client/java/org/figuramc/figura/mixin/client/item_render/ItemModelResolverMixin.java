package org.figuramc.figura.mixin.client.item_render;

import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.figuramc.figura.ducks.client.ItemStackRenderStateAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Need to store the ItemStack instance in the ItemStackRenderState.
 */
@Mixin(ItemModelResolver.class)
public class ItemModelResolverMixin {

    @Inject(method = "updateForTopItem", at = @At(value = "FIELD", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;isLeftHand:Z"))
    public void updateItemStackField(ItemStackRenderState itemStackRenderState, ItemStack itemStack, ItemDisplayContext itemDisplayContext, boolean bl, Level level, LivingEntity livingEntity, int i, CallbackInfo ci) {
        ((ItemStackRenderStateAccess) itemStackRenderState).figura$setItemStack(itemStack);
    }

}
