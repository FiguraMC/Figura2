package org.figuramc.figura.vanillamodel;

import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.figuramc.figura.FiguraMod;
import org.jetbrains.annotations.Contract;

import java.util.*;

import static org.figuramc.figura.vanillamodel.ModelPartAlias.Group.*;

// Kept in a separate file for cleanliness.

// Most operations in this file are not type-safe, because generics are extremely annoying to make work with all this, so I just gave up. Use with caution.
// Suppress these throughout the entire file, because it gets BAD in here with all the unchecked generics and types...
@SuppressWarnings({"unchecked", "rawtypes", "deprecation"})
public class ModelPartAliases {

    // Mappings from renderers/model classes to their aliasers.
    public static final Map<Class<? extends EntityRenderer>, RendererAliaser> RENDERER_ALIASERS = new HashMap<>();
    public static final Map<Class<? extends EntityModel>, EntityModelAliaser> MODEL_ALIASERS = new HashMap<>();

    public static <T extends EntityRenderer> void register(Class<T> clazz, RendererAliaser<? extends T> aliaser) {
        RENDERER_ALIASERS.put(clazz, aliaser);
    }

    public static <T extends EntityModel> void register(Class<T> clazz, EntityModelAliaser<? extends T> aliaser) {
        MODEL_ALIASERS.put(clazz, aliaser);
    }

    // Loop through renderer classes from subclass to superclass.
    // Aliases given by subclass have higher priority.
    public static void genAliases(EntityRenderer renderer) {
        Class<?> clazz = renderer.getClass();
        while (EntityRenderer.class.isAssignableFrom(clazz)) {
            RendererAliaser aliaser = RENDERER_ALIASERS.get(clazz);
            if (aliaser != null) aliaser.alias(renderer, new AliasModifier());
            clazz = clazz.getSuperclass();
        }
    }


    /**
     * Alias an EntityRenderer and add its mappings to the output map.
     */
    @FunctionalInterface
    public interface RendererAliaser<T extends EntityRenderer> {
        void alias(T renderer, AliasModifier output);
    }

    /**
     * Alias an EntityModel and add its mappings to the output map, with given prefix/suffix.
     * No need to interact with superclass/subclass here, because aliasModel() will loop through superclasses.
     */
    @FunctionalInterface
    public interface EntityModelAliaser<T extends EntityModel> {
        void alias(T model, AliasModifier output);
    }

    // Fill in the mappings.
    static {
        // Renderers
        register(LivingEntityRenderer.class, ModelPartAliases::aliasLivingEntityRenderer);
        register(PlayerRenderer.class, ModelPartAliases::aliasPlayerRenderer);
        register(AgeableMobRenderer.class, ModelPartAliases::aliasAgeableMobRenderer);

        // Models
        register(HumanoidModel.class, ModelPartAliases::aliasHumanoid);
        register(PlayerModel.class, ModelPartAliases::aliasPlayer);
        register(QuadrupedModel.class, ModelPartAliases::aliasQuadruped);
        register(FoxModel.class, ModelPartAliases::aliasFox);
    }

    // -------------
    // | UTILITIES |
    // -------------

    private static void aliasModel(EntityModel<?> model, AliasModifier output) {
        Class<?> clazz = model.getClass();
        while (clazz != null) {
            EntityModelAliaser aliaser = MODEL_ALIASERS.get(model.getClass());
            if (aliaser != null) aliaser.alias(model, output);
            clazz = clazz.getSuperclass();
        }
    }

    // Immutable, keeps modifications. Can extend with more prefix/suffix/groups.
    public record AliasModifier(String prefix, String suffix, EnumSet<ModelPartAlias.Group> groups) {
        public AliasModifier() {
            this("", "", EnumSet.noneOf(ModelPartAlias.Group.class));
        }
        @Contract(pure = true)
        public AliasModifier withGroups(ModelPartAlias.Group addedGroup) {
            EnumSet<ModelPartAlias.Group> newGroups = groups.clone();
            newGroups.add(addedGroup);
            return new AliasModifier(prefix, suffix, newGroups);
        }
        @Contract(pure = true)
        public AliasModifier withGroups(ModelPartAlias.Group... addedGroups) {
            EnumSet<ModelPartAlias.Group> newGroups = groups.clone();
            newGroups.addAll(List.of(addedGroups));
            return new AliasModifier(prefix, suffix, newGroups);
        }
        @Contract(pure = true)
        public AliasModifier withPrefix(String prefix) {
            return new AliasModifier(prefix + "_" + this.prefix, suffix, groups);
        }
        @Contract(pure = true)
        public AliasModifier withSuffix(String suffix) {
            return new AliasModifier(prefix, this.suffix + "_" + suffix, groups);
        }

        // Sets an alias only if there wasn't already an existing alias, and warns if something unexpected occurs.
        // Example of why this is helpful:
        // - PlayerRenderer aliaser generates aliases for its model parts, with some groups declared.
        // - Follow superclasses...
        // - EntityRenderer aliaser generates aliases for the model parts, without any groups declared.
        //   These should be ignored, because the PlayerRenderer aliaser already gave these parts better aliases.
        public void setAlias(ModelPart part, String name) {
            ModelPartAlias newAlias = new ModelPartAlias(prefix + name + suffix, groups);
            ModelPartAlias oldAlias = ModelPartTracker.getAlias(part);
            // If there's no previous alias, just set.
            if (oldAlias == null) ModelPartTracker.setAlias(part, newAlias);
            // If there was an old alias, but the names are different, issue a warning.
            else if (!oldAlias.name().equals(newAlias.name()))
                FiguraMod.LOGGER.warn("Conflicting alias names for the same model part: {} and {}", oldAlias.name(), newAlias.name());
            // If the new alias has groups not present in the old alias, issue a warning.
            else if (!oldAlias.groups().containsAll(newAlias.groups()))
                FiguraMod.LOGGER.warn("Second alias for part {} has groups not present in first alias: {} -> {}", oldAlias.name(), oldAlias.groups(), newAlias.groups());
        }
    }


    // ----------------------------
    // | VARIOUS ENTITY RENDERERS |
    // ----------------------------

    private static void aliasLivingEntityRenderer(LivingEntityRenderer<?, ?, ?> living, AliasModifier mods) {
        aliasModel(living.getModel(), mods);
        for (var layer : living.layers)
            aliasRenderLayer(layer, mods);
    }

    private static void aliasPlayerRenderer(PlayerRenderer player, AliasModifier mods) {
        aliasPlayer(player.getModel(), mods.withGroups(OUTER_LAYER));
        aliasHumanoid(player.getModel(), mods);
    }

    private static void aliasAgeableMobRenderer(AgeableMobRenderer<?, ?, ?> ageable, AliasModifier mods) {
        aliasModel(ageable.adultModel, mods);
        aliasModel(ageable.babyModel, mods.withGroups(BABY).withPrefix("BABY"));
    }

    // -----------------
    // | RENDER LAYERS |
    // -----------------

    // Inner model is for leggings, outer model is everything else.
    private static void aliasArmor(HumanoidModel<?> innerModel, HumanoidModel<?> outerModel, AliasModifier mods) {
        mods.setAlias(outerModel.head, "HELMET");
        mods.setAlias(outerModel.body, "CHESTPLATE_BODY");
        mods.setAlias(outerModel.leftArm, "CHESTPLATE_LEFT_ARM");
        mods.setAlias(outerModel.rightArm, "CHESTPLATE_RIGHT_ARM");
        mods.setAlias(innerModel.body, "LEGGINGS_BODY");
        mods.setAlias(innerModel.leftLeg, "LEGGINGS_LEFT_LEG");
        mods.setAlias(innerModel.rightLeg, "LEGGINGS_RIGHT_LEG");
        mods.setAlias(outerModel.leftLeg, "BOOTS_LEFT_LEG");
        mods.setAlias(outerModel.rightLeg, "BOOTS_RIGHT_LEG");
    }

    private static void aliasElytra(ElytraModel model, AliasModifier mods) {
        mods.setAlias(model.leftWing, "LEFT_ELYTRA");
        mods.setAlias(model.rightWing, "RIGHT_ELYTRA");
    }

    private static void aliasArrowModel(ArrowModel model, AliasModifier mods) {
        mods.setAlias(model.root().getChild("cross_1"), "ARROW_1");
        mods.setAlias(model.root().getChild("cross_2"), "ARROW_2");
        mods.setAlias(model.root().getChild("back"), "ARROW_BACK");
    }

    // Render layers! Need a lot of access wideners for this one.
    private static void aliasRenderLayer(RenderLayer<?, ?> layer, AliasModifier mods) {
        switch (layer) {
            case CapeLayer cape -> mods.withGroups(CAPE).setAlias(((PlayerCapeModel<?>) cape.model).cape, "CAPE");
            case WingsLayer<?, ?> elytra -> {
                aliasElytra(elytra.elytraModel, mods.withGroups(ELYTRA));
                if (elytra.elytraModel != elytra.elytraBabyModel)
                    aliasElytra(elytra.elytraBabyModel, mods.withGroups(ELYTRA, BABY).withPrefix("BABY"));
            }
            case SpinAttackEffectLayer spin -> {
                ModelPart[] boxes = spin.model.boxes;
                for (int i = 0; i < boxes.length; i++) {
                    mods.withGroups(TRIDENT_SPIN_ATTACK).setAlias(boxes[i], "SPIN_ATTACK_" + (i + 1));
                }
            }
            case ArrowLayer<?> arrows -> aliasArrowModel((ArrowModel) arrows.model, mods.withPrefix("STUCK").withGroups(STUCK_ARROW));
            case BeeStingerLayer<?> beeStingers -> {
                mods.withGroups(STUCK_BEE_STINGER).setAlias(beeStingers.model.root().getChild("cross_1"), "STUCK_BEE_STINGER_1");
                mods.withGroups(STUCK_BEE_STINGER).setAlias(beeStingers.model.root().getChild("cross_2"), "STUCK_BEE_STINGER_2");
            }
            case HumanoidArmorLayer<?,?,?> armor -> {
                aliasArmor(armor.innerModel, armor.outerModel, mods.withGroups(ARMOR));
                if (armor.innerModel != armor.innerModelBaby || armor.outerModel != armor.outerModelBaby)
                    aliasArmor(armor.innerModelBaby, armor.outerModelBaby, mods.withGroups(ARMOR, BABY).withPrefix("BABY"));
            }
            default -> {} // Don't do anything
        }
    }

    // ------------------------------
    // | VARIOUS VANILLA MOB MODELS |
    // ------------------------------

    private static void aliasHumanoid(HumanoidModel<?> humanoid, AliasModifier mods) {
        mods.setAlias(humanoid.head, "HEAD");
        mods.setAlias(humanoid.hat, "HAT");
        mods.setAlias(humanoid.body, "BODY");
        mods.setAlias(humanoid.leftArm, "LEFT_ARM");
        mods.setAlias(humanoid.rightArm, "RIGHT_ARM");
        mods.setAlias(humanoid.leftLeg, "LEFT_LEG");
        mods.setAlias(humanoid.rightLeg, "RIGHT_LEG");
    }

    private static void aliasPlayer(PlayerModel player, AliasModifier mods) {
        mods.setAlias(player.hat, "HAT");
        mods.setAlias(player.jacket, "JACKET");
        mods.setAlias(player.leftSleeve, "LEFT_SLEEVE");
        mods.setAlias(player.rightSleeve, "RIGHT_SLEEVE");
        mods.setAlias(player.leftPants, "LEFT_PANTS");
        mods.setAlias(player.rightPants, "RIGHT_PANTS");
    }

    private static void aliasQuadruped(QuadrupedModel<?> quadruped, AliasModifier mods) {
        mods.setAlias(quadruped.head, "HEAD");
        mods.setAlias(quadruped.body, "BODY");
        mods.setAlias(quadruped.leftHindLeg, "LEFT_HIND_LEG");
        mods.setAlias(quadruped.rightHindLeg, "RIGHT_HIND_LEG");
        mods.setAlias(quadruped.leftFrontLeg, "LEFT_FRONT_LEG");
        mods.setAlias(quadruped.rightFrontLeg, "RIGHT_FRONT_LEG");
    }

    private static void aliasFox(FoxModel fox, AliasModifier mods) {
        mods.setAlias(fox.head, "HEAD");
        mods.setAlias(fox.head.getChild("left_ear"), "LEFT_EAR");
        mods.setAlias(fox.head.getChild("right_ear"), "RIGHT_EAR");
        mods.setAlias(fox.head.getChild("nose"), "NOSE");
        mods.setAlias(fox.body, "BODY");
        mods.setAlias(fox.leftHindLeg, "LEFT_HIND_LEG");
        mods.setAlias(fox.rightHindLeg, "RIGHT_HIND_LEG");
        mods.setAlias(fox.leftFrontLeg, "LEFT_FRONT_LEG");
        mods.setAlias(fox.rightFrontLeg, "RIGHT_FRONT_LEG");
        mods.setAlias(fox.tail, "TAIL");
    }


}
