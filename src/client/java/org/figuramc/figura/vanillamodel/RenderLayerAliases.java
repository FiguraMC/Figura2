package org.figuramc.figura.vanillamodel;

import net.minecraft.client.renderer.entity.layers.*;

import java.util.HashMap;
import java.util.Map;

// Split into its own class for cleanliness

@SuppressWarnings("rawtypes")
public class RenderLayerAliases {
    public static final Map<Class<? extends RenderLayer>, String> RENDER_LAYER_ALIASES = new HashMap<>() {{
        // Anonymous classes are mixed into, and they add their own class to this map.
        // Roughly categorized...

        // Player / other humanoids
        put(CustomHeadLayer.class, "HeadItem");
        put(WingsLayer.class, "Elytra");
        put(SpinAttackEffectLayer.class, "SpinAttack");
        put(CapeLayer.class, "Cape");
        put(HumanoidArmorLayer.class, "Armor");
        put(ParrotOnShoulderLayer.class, "ParrotOnShoulder");
        put(ArrowLayer.class, "StuckArrows");
        put(BeeStingerLayer.class, "StuckBeeStingers");
        put(Deadmau5EarsLayer.class, "Deadmau5Ears");

        // Held items (Not shown here: EvokerItem, IllusionerItem, VindicatorItem)
        put(ItemInHandLayer.class, "MobItem");
        put(PlayerItemInHandLayer.class, "PlayerItem");
        put(CarriedBlockLayer.class, "EndermanItem");
        put(FoxHeldItemLayer.class, "FoxItem");
        put(PandaHoldsItemLayer.class, "PandaItem");
        put(CrossedArmsItemLayer.class, "VillagerItem");
        put(WitchItemLayer.class, "WitchItem");
        put(DolphinCarryingItemLayer.class, "DolphinItem");

        // Various random animal/mob layers
        put(CreeperPowerLayer.class, "CreeperPower");
        put(WitherArmorLayer.class, "WitherArmor");
        put(SlimeOuterLayer.class, "SlimeOuterLayer");
        put(DrownedOuterLayer.class, "DrownedOuterLayer");
        put(SkeletonClothingLayer.class, "SkeletonClothing");
        put(TropicalFishPatternLayer.class, "TropicalFishPatternLayer");
        put(WolfArmorLayer.class, "WolfArmor");
        put(WolfCollarLayer.class, "WolfCollar");
        put(CatCollarLayer.class, "CatCollar");
        put(SimpleEquipmentLayer.class, "Equipment");
        put(HorseMarkingLayer.class, "HorseMarkingLayer");
        put(SheepWoolLayer.class, "SheepFur");
        put(MushroomCowMushroomLayer.class, "MooshroomMushrooms");
        put(BreezeWindLayer.class, "BreezeWind");
        put(LlamaDecorLayer.class, "LlamaDecor");
        put(LivingEntityEmissiveLayer.class, "EntityGlow");
        put(VillagerProfessionLayer.class, "VillagerProfessionLayer");
//        put(ShulkerHeadLayer.class, "ShulkerHead");
        put(SnowGolemHeadLayer.class, "SnowGolemHead");
        put(IronGolemFlowerLayer.class, "IronGolemFlower");
        put(IronGolemCrackinessLayer.class, "IronGolemCracks");
        put(EnderEyesLayer.class, "EndermanEyes");
        put(SpiderEyesLayer.class, "SpiderEyes");
        put(PhantomEyesLayer.class, "PhantomEyes");
        put(BreezeEyesLayer.class, "BreezeEyes");
    }};
}
