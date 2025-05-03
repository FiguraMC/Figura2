package org.figuramc.figura.vanillamodel;

import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The job of this class is simply to map EntityRenderer -> Map<String, Model>.
 * It also handles correspondence of Model and VanillaModel.
 */
@SuppressWarnings({"rawtypes", "unchecked"}) // These are used out of necessity, but only privately, not in the public API
public class ModelNames {

    // For each entity renderer class, provide a mapping from root -> name of said root.
    private static final Map<Class, Function<EntityRenderer, Map<Model, String>>> RENDERER_MAPPERS = new HashMap<>();

    // Public, type-safe API with generics. Other mods can call this on startup without worrying.
    // It is acceptable for two mappers to reference the same model with different names; for example, we could have
    // LivingEntityRenderer --> [main model => "entity_root"]
    // PlayerRenderer --> [main model => "player_root"]
    // If this happens, the SUBCLASS's name choice will have priority, because it can be more specific!
    public static <T> void registerRendererMapper(Class<? extends T> clazz, Function<T, Map<Model, String>> mapper) {
        RENDERER_MAPPERS.put(clazz, (Function) mapper); // Cast
    }

    // Weak cache
    private static final Map<EntityRenderer, Map<String, Model>> MODELS_CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    public static Map<String, Model> getModelsByName(@NotNull EntityRenderer<?, ?> renderer) {
        return MODELS_CACHE.computeIfAbsent(renderer, ModelNames::roots);
    }

    private static Map<String, Model> roots(EntityRenderer<?, ?> renderer) {
        return Stream.iterate((Class) renderer.getClass(), Objects::nonNull, Class::getSuperclass)
                .map(RENDERER_MAPPERS::get)
                .filter(Objects::nonNull)
                .flatMap(mapper -> mapper.apply(renderer).entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey, (subclassName, superclassName) -> subclassName, HashMap::new));
    }

    // Our own model fetchers:
    static {
        // Living entity renderer special, keeps render layers
        registerRendererMapper(LivingEntityRenderer.class, badler -> {
            LivingEntityRenderer<?, ?, ?> ler = (LivingEntityRenderer<?, ?, ?>) badler; // Just cast it i dont care anymore
            Map<Model, String> map = new HashMap<>();
            // Main entity
            map.put(ler.getModel(), "ENTITY");
            // Render layers:
            for (RenderLayer<?, ?> layer : ler.layers) {
                switch (layer) {
                    case CapeLayer cape -> map.put(cape.model, "CAPE_ROOT");
                    case WingsLayer<?, ?> wings -> {
                        map.put(wings.elytraModel, "ELYTRA");
                        if (wings.elytraBabyModel != wings.elytraModel) map.put(wings.elytraBabyModel, "BABY_ELYTRA");
                    }
                    case SpinAttackEffectLayer spin -> map.put(spin.model, "TRIDENT_SPIN_ATTACK");
                    case ArrowLayer<?> arrows -> map.put(arrows.model, "STUCK_ARROW");
                    case BeeStingerLayer<?> stingers -> map.put(stingers.model, "STUCK_BEE_STINGER");
                    case HumanoidArmorLayer<?, ?, ?> armor -> {
                        map.put(armor.outerModel, "MAIN_ARMOR");
                        map.put(armor.innerModel, "LEGGINGS_ARMOR");
                        if (armor.outerModel != armor.outerModelBaby) map.put(armor.outerModelBaby, "BABY_MAIN_ARMOR");
                        if (armor.innerModel != armor.innerModelBaby) map.put(armor.innerModelBaby, "BABY_LEGGINGS_ARMOR");
                    }
                    default -> {}
                }
            }
            return map;
        });
        // Adult is default, baby is special
        registerRendererMapper(AgeableMobRenderer.class, amr -> Map.of(amr.adultModel, "ENTITY", amr.babyModel, "BABY"));
    }

}
