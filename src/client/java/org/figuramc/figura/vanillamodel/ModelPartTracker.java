package org.figuramc.figura.vanillamodel;

import org.figuramc.figura.ducks.client.ModelPartTrackingAccess;
import org.figuramc.figura.util.ListUtils;
import org.figuramc.figura.util.ReflectionUtils;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.*;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deals with tracking the various
 */
public class ModelPartTracker {

    /**
     * Various mappings from MC's model/rendering classes to collections
     * of ModelPart that are (potentially) used by said model.
     * The main point of this class is to fill in these maps and keep
     * them updated.
     */
    private static final ConcurrentHashMap<EntityRenderer<?>, List<ModelPart>> MODEL_PARTS_BY_RENDERER = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<RenderLayer<?, ?>, List<ModelPart>> MODEL_PARTS_BY_RENDER_LAYER = new ConcurrentHashMap<>();
    private static final Map<EntityRenderer<?>, Map<String, ModelPart>> MODEL_PARTS_BY_NAME_BY_RENDERER = new ConcurrentHashMap<>();

    /**
     * Helpers to get name/parent of a vanilla part, if it has one.
     * Returns null if the part is unnamed, or has no parent, respectively.
     * Note: A part has a name if it has a parent, or if we manually gave it a name through setName().
     */
    public static @Nullable String getName(ModelPart vanillaPart) {
        return ((ModelPartTrackingAccess) (Object) vanillaPart).figura$getName();
    }
    public static void setName(ModelPart vanillaPart, String name) {
        ((ModelPartTrackingAccess) (Object) vanillaPart).figura$setName(name);
    }
    public static @Nullable String getFullName(ModelPart vanillaPart, String separator) {
        String s = getName(vanillaPart);
        if (s == null) return null;
        if (getParent(vanillaPart) == null) return s;
        String parentName = getFullName(getParent(vanillaPart), separator);
        if (parentName == null) return s;
        return parentName + separator + s;
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
    }

    /**
     * Gets the cached list of model parts for this entity renderer if it's
     * already been calculated, or calculates it if it hasn't.
     */
    public static List<ModelPart> traceEntityRenderer(EntityRenderer<?> entityRenderer) {
        if (!MODEL_PARTS_BY_RENDERER.containsKey(entityRenderer)) {
            // Traverse all fields and fetch model parts from each one
            Collection<ModelPart> foundModelParts = new HashSet<>();
            int[] childrenModelIndex = new int[] {1};
            for (Object obj : ReflectionUtils.getAllFieldValues(entityRenderer.getClass(), entityRenderer))
                getModelParts(obj, foundModelParts, childrenModelIndex);
            // Also do the render layers, if it has any:
            if (entityRenderer instanceof LivingEntityRenderer<?,?> ler) {
                for (RenderLayer<?, ?> layer : ler.layers) {
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
                    } else {
                        // If there were no model parts, do nothing here
                    }
                }
            }
            // Simplify and store in caches
            foundModelParts = simplifyFoundModelParts(foundModelParts);
            MODEL_PARTS_BY_RENDERER.put(entityRenderer, new ArrayList<>(foundModelParts));
            MODEL_PARTS_BY_NAME_BY_RENDERER.put(entityRenderer, ListUtils.associateBy(foundModelParts, part -> getFullName(part, "/")));
        }
        return MODEL_PARTS_BY_RENDERER.get(entityRenderer);
    }

    public static @Nullable ModelPart getModelPartByName(EntityRenderer<?> renderer, String name) {
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
                if (getName(part) != null && getName(part).matches("^Model\\d+$")) {
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
     *
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

    public static Map<Class<? extends RenderLayer>, String> RENDER_LAYER_ALIASES = new HashMap<>() {{
        // The anonymous classes are mixed into, and they add their own class to this map.
        // Roughly categorized...

        // Player / other humanoids
        put(CustomHeadLayer.class, "HeadItem");
        put(ElytraLayer.class, "Elytra");
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
        put(SheepFurLayer.class, "SheepFur");
        put(MushroomCowMushroomLayer.class, "MooshroomMushrooms");
        put(BreezeWindLayer.class, "BreezeWind");
        put(LlamaDecorLayer.class, "LlamaDecor");
        put(WardenEmissiveLayer.class, "WardenGlow");
        put(VillagerProfessionLayer.class, "VillagerProfessionLayer");
        put(ShulkerHeadLayer.class, "ShulkerHead");
        put(SnowGolemHeadLayer.class, "SnowGolemHead");
        put(IronGolemFlowerLayer.class, "IronGolemFlower");
        put(IronGolemCrackinessLayer.class, "IronGolemCracks");
        put(EnderEyesLayer.class, "EndermanEyes");
        put(SpiderEyesLayer.class, "SpiderEyes");
        put(PhantomEyesLayer.class, "PhantomEyes");
        put(BreezeEyesLayer.class, "BreezeEyes");
    }};
}
