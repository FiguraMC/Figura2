package org.figuramc.figura.vanillamodel;

import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.apache.commons.lang3.mutable.MutableInt;
import org.figuramc.figura.ducks.client.ModelPartTrackingAccess;
import org.figuramc.figura.util.ListUtils;
import org.figuramc.figura.util.ReflectionUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Deals with tracking the various model parts and other rendering objects in the game.
 */
@SuppressWarnings("SameParameterValue")
public class ModelPartTracker {

    /**
     * Various mappings from MC's model/rendering classes to collections
     * of ModelPart that are (potentially) used by said model.
     * The main point of this class is to fill in these maps and keep
     * them updated.
     */
    private static final ConcurrentHashMap<EntityRenderer<?,?>, List<ModelPart>> MODEL_PARTS_BY_RENDERER = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<RenderLayer<?, ?>, List<ModelPart>> MODEL_PARTS_BY_RENDER_LAYER = new ConcurrentHashMap<>();
    private static final Map<EntityRenderer<?,?>, HashMap<String, ModelPart>> MODEL_PARTS_BY_NAME_BY_RENDERER = new ConcurrentHashMap<>();

    /**
     * Helpers to get stored values of vanilla parts, if they have any.
     */
    public static @Nullable String getName(ModelPart vanillaPart) {
        return ((ModelPartTrackingAccess) (Object) vanillaPart).figura$getName();
    }
    public static void setName(ModelPart vanillaPart, String name) {
        ((ModelPartTrackingAccess) (Object) vanillaPart).figura$setName(name);
    }
    public static @Nullable String getFullName(ModelPart vanillaPart) {
        String s = getName(vanillaPart);
        if (s == null) return null;
        if (getParent(vanillaPart) == null) return s;
        String parentName = getFullName(getParent(vanillaPart));
        if (parentName == null) return s;
        return parentName + "/" + s;
    }
    public static @Nullable String getAlias(ModelPart vanillaPart) {
        return ((ModelPartTrackingAccess) (Object) vanillaPart).figura$getAlias();
    }
    public static void setAlias(ModelPart vanillaPart, String alias) {
        ((ModelPartTrackingAccess) (Object) vanillaPart).figura$setAlias(alias);
    }
    public static @Nullable ModelPart getParent(ModelPart vanillaPart) {
        return ((ModelPartTrackingAccess) (Object) vanillaPart).figura$getParent();
    }

    /**
     * ModelPart instances, as well as instances of EntityRenderer and RenderLayer,
     * are changed out when the resource manager is reloaded. Therefore, this cache
     * should also be cleared out when that happens.
     */
    public static void clearCaches() {
        MODEL_PARTS_BY_RENDERER.clear();
        MODEL_PARTS_BY_RENDER_LAYER.clear();
        MODEL_PARTS_BY_NAME_BY_RENDERER.clear();
    }

    /**
     * Gets the cached list of model parts for this entity renderer if it's
     * already been calculated, or calculates it if it hasn't.
     */
    public static List<ModelPart> traceEntityRenderer(EntityRenderer<?,?> entityRenderer) {
        if (!MODEL_PARTS_BY_RENDERER.containsKey(entityRenderer)) {
            // Traverse all fields and fetch model parts from each one
            Collection<ModelPart> foundModelParts = new HashSet<>();
            int[] childrenModelIndex = new int[] {1};
            for (Object obj : ReflectionUtils.getAllFieldValues(entityRenderer.getClass(), entityRenderer))
                getModelParts(obj, foundModelParts, childrenModelIndex);
            // Also do the render layers, if it has any:
            if (entityRenderer instanceof LivingEntityRenderer<?,?,?> ler) {
                for (RenderLayer<?,?> layer : ler.layers) {
                    // Get the layer's name
                    String layerName = RENDER_LAYER_ALIASES.get(layer.getClass());
                    if (layerName == null) layerName = layer.getClass().getSimpleName();
                    // Check cases:
                    List<ModelPart> layerParts = traceRenderLayer(layer);
                    if (layerParts.size() == 1 && getName(layerParts.getFirst()) == null) {
                        // One part with no name: Set its name to something and add it
                        setName(layerParts.getFirst(), layerName);
                        foundModelParts.add(layerParts.getFirst());
                    } else if (!layerParts.isEmpty()) {
                        // Multiple parts: Create a new part, make these the children
                        // Give the children names if they don't already have them
                        MutableInt childIndex = new MutableInt(0);
                        ModelPart newPart = new ModelPart(List.of(), ListUtils.associateBy(layerParts, part -> {
                            if (getName(part) == null)
                                setName(part, "part" + childIndex);
                            childIndex.increment();
                            return getName(part);
                        }));
                        setName(newPart, layerName);
                        foundModelParts.add(newPart);
                    }
                    // If there are no model parts, do nothing
                }
            }
            // Simplify and store in caches
            foundModelParts = simplifyFoundModelParts(foundModelParts);
            MODEL_PARTS_BY_RENDERER.put(entityRenderer, new ArrayList<>(foundModelParts));

            // Get by name, and also add aliases for this renderer. (And parent renderers!)
            HashMap<String, ModelPart> modelPartsByName = new HashMap<>();
            foundModelParts.forEach(part -> part.getAllParts().forEach(part2 -> modelPartsByName.put(ModelPartTracker.getFullName(part2), part2)));
            Class<?> clazz = entityRenderer.getClass();
            while (EntityRenderer.class.isAssignableFrom(clazz)) {
                var func = MODEL_PART_ALIASES.get(clazz);
                if (func != null) {
                    Map<String, ModelPart> aliases = func.apply(entityRenderer);
                    for (var entry : aliases.entrySet())
                        setAlias(entry.getValue(), entry.getKey()); // Store aliases other way too, for blockbench export
                    modelPartsByName.putAll(func.apply(entityRenderer));
                }
                clazz = clazz.getSuperclass();
            }
            // Store map in cache
            MODEL_PARTS_BY_NAME_BY_RENDERER.put(entityRenderer, modelPartsByName);
        }
        return MODEL_PARTS_BY_RENDERER.get(entityRenderer);
    }

    // Gets model part by name. Names include both auto-generated names and aliases.
    public static @Nullable ModelPart getModelPartByName(EntityRenderer<?,?> renderer, String name) {
        traceEntityRenderer(renderer);
        return MODEL_PARTS_BY_NAME_BY_RENDERER.get(renderer).get(name);
    }

    public static List<ModelPart> traceRenderLayer(RenderLayer<?, ?> renderLayer) {
        if (!MODEL_PARTS_BY_RENDER_LAYER.containsKey(renderLayer)) {
            // Traverse all fields and fetch model parts from each one, like we do for entity
            // renderers.
            Collection<ModelPart> foundModelParts = new HashSet<>();
            int[] childrenModelIndex = new int[] {1};
            for (Object obj : ReflectionUtils.getAllFieldValues(renderLayer.getClass(), renderLayer))
                getModelParts(obj, foundModelParts, childrenModelIndex);
            foundModelParts = simplifyFoundModelParts(foundModelParts);

            // If there's only one model part, and its name is of the form "Model<number>", add its children directly.
            if (foundModelParts.size() == 1) {
                ModelPart part = foundModelParts.iterator().next();
                String name = getName(part);
                if (name != null && name.matches("^Model\\d+$")) {
                    foundModelParts = new HashSet<>(part.children.values());
                }
            }

            // Cache
            MODEL_PARTS_BY_RENDER_LAYER.put(renderLayer, new ArrayList<>(foundModelParts));
        }
        return MODEL_PARTS_BY_RENDER_LAYER.get(renderLayer);
    }

    /**
     * Simplify the set of found model parts by tracing the parent-child trees.
     * If an element of foundModelParts is a child of another element, remove
     * that first element.
     * <p>
     * Not the most efficient implementation, but should only happen once per
     * entity renderer per resource manager reload, so it's alright.
     */
    private static Collection<ModelPart> simplifyFoundModelParts(Collection<ModelPart> foundModelParts) {
        Set<ModelPart> result = new HashSet<>();
        outer:
        for (ModelPart testing : foundModelParts) {
            // Check if the part was already added in some way.
            // Iterate over all the other found parts:
            for (ModelPart found : foundModelParts) {
                if (found == testing) continue;
                // If "found" is a parent of "testing", don't add "testing" to result.
                ModelPart cur = testing;
                while (cur != null) {
                    if (found == cur) continue outer;
                    cur = getParent(cur);
                }
            }
            // If it wasn't otherwise reachable, add it to result list.
            result.add(testing);
        }
        // Return
        return result;
    }

    /**
     * Get the model parts contained in this Object and add them to the list,
     * if there are any.
     */
    private static void getModelParts(Object fieldValue, Collection<ModelPart> foundModelParts, int[] modelIndex) {
        if (fieldValue instanceof Model model)
            foundModelParts.add(traceModel(model, modelIndex));
        else if (fieldValue instanceof ModelPart part) {
            foundModelParts.add(part);
        } else if (fieldValue instanceof Collection<?> collection)
            for (Object o : collection) getModelParts(o, foundModelParts, modelIndex);
        else if (fieldValue instanceof ModelPart[] array)
            Collections.addAll(foundModelParts, array);
    }

    /**
     * Attempt to reflectively trace a Model object to uncover its ModelPart instances.
     * Then it will coalesce the found parts into a single model part, with an incrementing name.
     */
    private static ModelPart traceModel(Model model, int[] modelIndex) {
        int index = modelIndex[0]++;
        List<ModelPart> modelChildren = new ArrayList<>();
        int[] childrenModelIndex = new int[] {1};
        for (Object obj : ReflectionUtils.getAllFieldValues(model.getClass(), model))
            getModelParts(obj, modelChildren, childrenModelIndex);
        // Simplify the children
        Collection<ModelPart> simplifiedModelChildren = simplifyFoundModelParts(modelChildren);
        // Create a new model part with the simplifiedModelChildren as its children
        ModelPart res = new ModelPart(List.of(), ListUtils.associateBy(List.copyOf(simplifiedModelChildren), part -> {
            String name = getName(part);
            return name == null ? "root" : name; // Default to "root" as the key
        }));
        setName(res, "Model" + (index == 1 ? "" : index)); // First is Model, second is Model2, etc.
        return res;
    }

    @SuppressWarnings("rawtypes")
    public static final Map<Class<? extends RenderLayer>, String> RENDER_LAYER_ALIASES = new HashMap<>() {{
        // The anonymous classes are mixed into, and they add their own class to this map.
        // Roughly categorized...

        // Player / other humanoids
        put(CustomHeadLayer.class, "HeadItem");
        put(WingsLayer.class, "Elytra");
        put(SpinAttackEffectLayer.class, "SpinAttack");
        put(CapeLayer.class, "Cape"); // Note: Does not contain the actual cape model part
        put(HumanoidArmorLayer.class, "Armor");
        put(ParrotOnShoulderLayer.class, "ParrotOnShoulder");
        put(ArrowLayer.class, "StuckArrows");
        put(BeeStingerLayer.class, "StuckBeeStingers");
        put(Deadmau5EarsLayer.class, "Deadmau5Ears"); // Note: Does not contain the actual ears model part

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
        put(HorseArmorLayer.class, "HorseArmor");
        put(HorseMarkingLayer.class, "HorseMarkingLayer");
        put(SaddleLayer.class, "Saddle");
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

    // Manually added aliases which can optionally be used in place of the generated ones.
    @SuppressWarnings("rawtypes")
    private static final Map<Class<? extends EntityRenderer>, Function<EntityRenderer, Map<String, ModelPart>>> MODEL_PART_ALIASES = new HashMap<>();
    // Helper method to add at least a tiny bit of type safety...
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends EntityRenderer> void addModelPartAliases(Class<? super T> clazz, Function<T, Map<String, ModelPart>> func) {
        if (!EntityRenderer.class.isAssignableFrom(clazz)) throw new IllegalArgumentException("Class argument must extend EntityRenderer");
        MODEL_PART_ALIASES.put((Class<? extends EntityRenderer>) clazz, (Function<EntityRenderer, Map<String, ModelPart>>) func);
    }


    // Add the aliases.
    // Ideally have each alias string only appear one time, for easier changes.
    static {
        // Living entities have render layers
        addModelPartAliases(LivingEntityRenderer.class, (LivingEntityRenderer<?,?,?> living) -> {
            HashMap<String, ModelPart> res = new HashMap<>();
            for (RenderLayer<?,?> layer : living.layers)
                aliasRenderLayer(layer, res);
            return res;
        });

        // Humanoid mobs
        addModelPartAliases(HumanoidMobRenderer.class, (HumanoidMobRenderer<?,?,?> humanoid) -> {
            HashMap<String, ModelPart> res = new HashMap<>();
            aliasHumanoidModel(humanoid.getModel(), "", "", res);
            return res;
        });

        // Players are not humanoid mobs because... reasons
        addModelPartAliases(PlayerRenderer.class, player -> {
            Map<String, ModelPart> res = new HashMap<>();
            PlayerModel model = player.getModel();
            aliasHumanoidModel(model, "", "", res); // Trace humanoid parts
            // Trace other parts
            res.put("FIGURA_JACKET", model.jacket);
            res.put("FIGURA_LEFT_SLEEVE", model.leftSleeve);
            res.put("FIGURA_RIGHT_SLEEVE", model.rightSleeve);
            res.put("FIGURA_LEFT_PANTS", model.leftPants);
            res.put("FIGURA_RIGHT_PANTS", model.rightPants);
            return res;
        });
    }

    // Helpers for DRY.
    private static void aliasHumanoidModel(HumanoidModel<?> model, String prefix, String suffix, Map<String, ModelPart> output) {
        output.put("FIGURA_" + prefix + "HEAD" + suffix, model.head);
        output.put("FIGURA_" + prefix + "HAT" + suffix, model.hat);
        output.put("FIGURA_" + prefix + "BODY" + suffix, model.body);
        output.put("FIGURA_" + prefix + "LEFT_ARM" + suffix, model.leftArm);
        output.put("FIGURA_" + prefix + "RIGHT_ARM" + suffix, model.rightArm);
        output.put("FIGURA_" + prefix + "LEFT_LEG" + suffix, model.leftLeg);
        output.put("FIGURA_" + prefix + "RIGHT_LEG" + suffix, model.rightLeg);
    }

    private static void aliasArmor(HumanoidModel<?> model, String suffix, Map<String, ModelPart> output) {
        aliasHumanoidModel(model, "ARMOR_", suffix, output);
    }

    private static void aliasArrowModel(ArrowModel model, String prefix, Map<String, ModelPart> output) {
        output.put("FIGURA_" + prefix + "ARROW_1", model.root().getChild("cross_1"));
        output.put("FIGURA_" + prefix + "ARROW_2", model.root().getChild("cross_2"));
        output.put("FIGURA_" + prefix + "ARROW_BACK", model.root().getChild("back"));
    }

    // Render layers... need a lot of access wideners for this one.
    private static void aliasRenderLayer(RenderLayer<?, ?> layer, Map<String, ModelPart> output) {
        switch (layer) {
            case CapeLayer cape -> output.put("FIGURA_CAPE", ((PlayerCapeModel<?>) cape.model).cape);
            case WingsLayer<?, ?> elytra -> {
                output.put("FIGURA_LEFT_ELYTRA", elytra.elytraModel.leftWing);
                output.put("FIGURA_RIGHT_ELYTRA", elytra.elytraModel.rightWing);
            }
            case SpinAttackEffectLayer spin -> {
                ModelPart[] boxes = spin.model.boxes;
                for (int i = 0; i < boxes.length; i++) {
                    output.put("FIGURA_SPIN_ATTACK_" + (i + 1), boxes[i]);
                }
            }
            case ArrowLayer<?> arrows -> aliasArrowModel((ArrowModel) arrows.model, "STUCK_", output);
            case BeeStingerLayer<?> beeStingers -> {
                output.put("FIGURA_STUCK_BEE_STINGER_1", beeStingers.model.root().getChild("cross_1"));
                output.put("FIGURA_STUCK_BEE_STINGER_2", beeStingers.model.root().getChild("cross_2"));
            }
            case HumanoidArmorLayer<?,?,?> armor -> {
                aliasArmor(armor.innerModel, "_INNER", output);
                aliasArmor(armor.outerModel, "_OUTER", output);
                if (armor.innerModelBaby != armor.innerModel) aliasArmor(armor.innerModelBaby, "_INNER_BABY", output);
                if (armor.outerModelBaby != armor.outerModel) aliasArmor(armor.outerModelBaby, "_OUTER_BABY", output);
            }
            default -> {} // Don't do anything
        }
    }

}
