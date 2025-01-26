package org.figuramc.figura.avatars.components;

import net.minecraft.core.registries.BuiltInRegistries;
import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.AvatarComponent;
import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.model.part.CustomItemModelPart;
import org.figuramc.figura.model.part.RootModelPart;
import org.figuramc.figura.util.ListUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * A component that handles rendering custom items instead of vanilla ones.
 */
public class CustomItems implements AvatarComponent {

    /**
     * The custom items which were determined by file names in the "items/" folder!
     */
    private List<PartEntry> customItems;

    @Override
    public void initialize(AvatarMaterials materials, Avatar<?> self) {
        // Depend on textures, needed for creating model parts
        Textures texturesComponent = self.assertDependency(Textures.class, getClass());

        // Create the custom items
        customItems = ListUtils.mapNonNull(materials.customItemRoots().entrySet(), entry -> {
            // Convert the String pattern to a Matcher:
            String pattern = entry.getKey();
            if (pattern.isEmpty()) return null;
            Matcher matcher;
            if (pattern.charAt(0) == '$') {
                String endsWith = pattern.substring(1);
                matcher = new Matcher.EndsWithMatcher(endsWith);
            } else {
                ResourceLocation exactLocation = ResourceLocation.parse(pattern.replace('$', ':'));
                matcher = new Matcher.ExactMatcher(exactLocation);
            }


            // Convert the materials to a CustomItemModelPart
            CustomItemModelPart mainPart = entry.getValue().modelPartMaterials() != null ? new CustomItemModelPart(entry.getValue().modelPartMaterials(), entry.getValue().transforms(), texturesComponent.textures) : null;

            // Convert the texture index to a RootModelPart
            RootModelPart flatPart = entry.getValue().textureIndex() != -1 ? new RootModelPart(pattern, texturesComponent.textures.get(entry.getValue().textureIndex())) : null;
            // Return the entry
            return new PartEntry(matcher, mainPart, flatPart);
        });
        Collections.sort(customItems); // Sort them, ensuring more specific ones are checked first (Exact matchers)
    }

    public @Nullable RootModelPart getModelPart(ItemStack stack, ItemDisplayContext context) {
        // TODO: Once the API exists, try any custom callbacks before this for loop
        for (PartEntry entry : customItems) {
            if (!entry.matcher.matches(stack))
                continue;
            if (entry.mainPart == null)
                return entry.flatPart;
            if (entry.flatPart == null)
                return entry.mainPart;
            if (context == ItemDisplayContext.GUI || context == ItemDisplayContext.GROUND || context == ItemDisplayContext.FIXED)
                return entry.flatPart;
            return entry.mainPart;
        }
        return null;
    }

    private record PartEntry(Matcher matcher, @Nullable CustomItemModelPart mainPart, @Nullable RootModelPart flatPart) implements Comparable<PartEntry> {
        @Override
        public int compareTo(@NotNull CustomItems.PartEntry o) {
            return this.matcher.compareTo(o.matcher);
        }
    }

    /**
     * Built-in matchers.
     * If the file name starts with $, then it's an EndsWithMatcher.
     * Otherwise, it's exact, with $ acting as the namespace separator.
     * Examples:
     * - $_sword.bbmodel matches anything whose id ends with "_sword"
     * - golden_axe.bbmodel matches specifically "minecraft:golden_axe"
     * - silly_mod$titanium_axe.bbmodel matches specifically "silly_mod:titanium_axe"
     */
    private sealed interface Matcher extends Comparable<Matcher> {
        boolean matches(ItemStack item);

        record ExactMatcher(ResourceLocation location) implements Matcher {
            @Override
            public boolean matches(ItemStack item) {
                return BuiltInRegistries.ITEM.getKey(item.getItem()).equals(location);
            }

            @Override
            public int compareTo(@NotNull CustomItems.Matcher o) {
                if (o instanceof EndsWithMatcher) return -1;
                return this.location.compareTo(((ExactMatcher) o).location);
            }
        }
        record EndsWithMatcher(String ending) implements Matcher {
            @Override
            public boolean matches(ItemStack item) {
                return BuiltInRegistries.ITEM.getKey(item.getItem()).getPath().endsWith(ending);
            }
            @Override
            public int compareTo(@NotNull CustomItems.Matcher o) {
                if (o instanceof ExactMatcher) return 1;
                int lenCompare = ((EndsWithMatcher) o).ending.length() - this.ending.length();
                if (lenCompare != 0) return lenCompare;
                return this.ending.compareTo(((EndsWithMatcher) o).ending);
            }
        }
    }

}
