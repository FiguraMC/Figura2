package org.figuramc.figura.avatars;

import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.util.ListUtils;
import org.figuramc.figura.avatars.components.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Different types of Avatar which have a preset group of components!
 */
public class AvatarTemplate<K> {

    public final ArrayList<Supplier<AvatarComponent>> components = new ArrayList<>();

    @SafeVarargs
    public AvatarTemplate(Supplier<AvatarComponent>... components) {
        Collections.addAll(this.components, components);
    }

    public static final AvatarTemplate<UUID> LOCAL_PLAYER_AVATAR = new AvatarTemplate<>(Textures::new, EntityRoot::new, EntityUser::new, VanillaParts::new, CustomItems::new, Scripts::new);
    public static final AvatarTemplate<UUID> CEM_AVATAR = new AvatarTemplate<>(Textures::new, EntityRoot::new, EntityUser::new, VanillaParts::new, CustomItems::new, Scripts::new);


    public Avatar<K> construct(K user, AvatarMaterials materials) throws AvatarLoadingException {
        return new Avatar<>(user, materials, ListUtils.map(components, Supplier::get).toArray(new AvatarComponent[0]));
    }

    public void addComponent(Supplier<AvatarComponent> componentSupplier) {
        this.components.add(componentSupplier);
    }


}
