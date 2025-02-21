package org.figuramc.figura.vanillamodel;

import java.util.EnumSet;

// The part will only be exported if ALL of its groups are selected.
// For example a baby elytra part with groups = [Baby, Elytra] will only show up if both Baby and Elytra are selected.
public record ModelPartAlias(String name, EnumSet<Group> groups) {

    // Groups of model parts
    public enum Group {
        BABY,

        OUTER_LAYER,
        CAPE,
        ARMOR,
        ELYTRA,
        STUCK_ARROW,
        STUCK_BEE_STINGER,
        TRIDENT_SPIN_ATTACK
    }
}
