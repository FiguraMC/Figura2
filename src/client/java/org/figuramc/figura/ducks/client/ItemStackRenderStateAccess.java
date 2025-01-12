package org.figuramc.figura.ducks.client;

import net.minecraft.world.item.ItemStack;

/**
 * Access the new fields in ItemStackRenderStateMixin
 */
public interface ItemStackRenderStateAccess {
    ItemStack figura$getItemStack();
    void figura$setItemStack(ItemStack itemStack);
}
