/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Visibility {

    public static final Visibility PUBLIC = Public.INSTANCE;
    public static final Visibility INVISIBLE = Invisible.INSTANCE;
    public static final Visibility CFG_DISABLED = CfgDisabled.INSTANCE;

    private Visibility() {}

    public boolean isVisibleFromOtherCrate() {
        return this == PUBLIC;
    }

    public boolean isVisibleFromMod(@NotNull ModData mod) {
        if (this == PUBLIC) return true;
        if (this instanceof Restricted) {
            ModData inMod = ((Restricted) this).getInMod();
            return inMod.getPath().isSubPathOf(mod.getPath());
        }
        return false;
    }

    public boolean isStrictlyMorePermissive(@NotNull Visibility other) {
        if (this instanceof Restricted && other instanceof Restricted) {
            Restricted thisRestricted = (Restricted) this;
            Restricted otherRestricted = (Restricted) other;
            if (thisRestricted.getInMod().getCrate() != otherRestricted.getInMod().getCrate()) return false;
            if (thisRestricted.getInMod() == otherRestricted.getInMod()) return false;
            for (ModData parent : otherRestricted.getInMod().getParentsIterable()) {
                if (parent == thisRestricted.getInMod()) return true;
            }
            return false;
        }
        if (this == PUBLIC) return !(other instanceof Public);
        if (this instanceof Restricted) return other == INVISIBLE || other == CFG_DISABLED;
        if (this == INVISIBLE) return other == CFG_DISABLED;
        return false;
    }

    @NotNull
    public Visibility intersect(@NotNull Visibility other) {
        return isStrictlyMorePermissive(other) ? other : this;
    }

    @NotNull
    public VisibilityType getType() {
        if (this == PUBLIC || this instanceof Restricted) return VisibilityType.Normal;
        if (this == INVISIBLE) return VisibilityType.Invisible;
        return VisibilityType.CfgDisabled;
    }

    public boolean isInvisible() {
        return this == INVISIBLE || this == CFG_DISABLED;
    }

    @Override
    @NotNull
    public String toString() {
        if (this == PUBLIC) return "Public";
        if (this instanceof Restricted) return "Restricted(in " + ((Restricted) this).getInMod().getPath() + ")";
        if (this == INVISIBLE) return "Invisible";
        return "CfgDisabled";
    }

    public static final class Public extends Visibility {
        public static final Public INSTANCE = new Public();
        private Public() {}
    }

    /**
     * Includes private visibility.
     * Constructor is private because ModData.visibilityInSelf must be used instead.
     */
    public static final class Restricted extends Visibility {
        @NotNull
        private final ModData inMod;

        private Restricted(@NotNull ModData inMod) {
            this.inMod = inMod;
        }

        @NotNull
        public static Restricted create(@NotNull ModData inMod) {
            return new Restricted(inMod);
        }

        @NotNull
        public ModData getInMod() {
            return inMod;
        }
    }

    /**
     * Means that we have import to private item.
     * So normally we should ignore such VisItem (it is not accessible)
     * But we record it for completion, etc.
     */
    public static final class Invisible extends Visibility {
        public static final Invisible INSTANCE = new Invisible();
        private Invisible() {}
    }

    public static final class CfgDisabled extends Visibility {
        public static final CfgDisabled INSTANCE = new CfgDisabled();
        private CfgDisabled() {}
    }
}
