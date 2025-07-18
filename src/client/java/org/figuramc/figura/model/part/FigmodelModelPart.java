package org.figuramc.figura.model.part;

import org.figuramc.figura.animation.Animation;
import org.figuramc.figura.animation.AnimationInstance;
import org.figuramc.figura.avatars.components.Textures;
import org.figuramc.figura.avatars.components.VanillaRendering;
import org.figuramc.figura.data.ModuleMaterials;
import org.figuramc.figura.model.texture.AvatarTexture;
import org.figuramc.figura.script_hooks.mem_count.MemoryCounter;
import org.figuramc.figura.util.MapUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * A model part which corresponds to a ".figmodel" file in the hierarchy.
 * It acts as a regular model part, but also has additional information captured from the model file.
 */
public class FigmodelModelPart extends FiguraModelPart {

    // Animations are pre-bound, because they come bundled with the figmodel!
    private final Map<String, AnimationInstance> animations;
    // Map to textures; these may be the same object reference as other textures.
    private final Map<String, AvatarTexture> textures;
    private final long namesSize; // Store size of names

    public FigmodelModelPart(ModuleMaterials.FigmodelMaterials materials, @Nullable FiguraModelPart parent, int moduleIndex, Textures texturesComponent, @Nullable VanillaRendering vanillaComponent) {
        super(materials, parent, moduleIndex, texturesComponent, vanillaComponent);
        animations = MapUtils.mapValues(materials.animations, animMats -> new AnimationInstance(new Animation(animMats), this));
        textures = MapUtils.mapValues(materials.textures, texIndex -> texturesComponent.getTexture(moduleIndex, texIndex));
        namesSize =
                animations.keySet().stream().mapToInt(String::length).sum() * CHAR_SIZE +
                textures.keySet().stream().mapToInt(String::length).sum() * CHAR_SIZE;
    }

    public @Nullable AnimationInstance animation(String name) { return animations.get(name); }
    public @Nullable AvatarTexture texture(String name) { return textures.get(name); }

    @Override
    protected long traceNoMark(MemoryCounter counter, int depth) {
        for (AnimationInstance anim : animations.values()) counter.trace(anim, depth);
        for (AvatarTexture tex : textures.values()) counter.trace(tex, depth);
        return namesSize + super.traceNoMark(counter, depth);
    }
}
