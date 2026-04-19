/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.jetbrains.annotations.NotNull;

public class Import {
    @NotNull
    private final ModData containingMod;
    @NotNull
    private final String[] usePath;
    @NotNull
    private final String nameInScope;
    @NotNull
    private final Visibility visibility;
    private final boolean isGlob;
    private final boolean isExternCrate;
    private final boolean isPrelude;

    @NotNull
    private PartialResolvedImport status = PartialResolvedImport.Unresolved.INSTANCE;

    public Import(
        @NotNull ModData containingMod,
        @NotNull String[] usePath,
        @NotNull String nameInScope,
        @NotNull Visibility visibility,
        boolean isGlob,
        boolean isExternCrate,
        boolean isPrelude
    ) {
        this.containingMod = containingMod;
        this.usePath = usePath;
        this.nameInScope = nameInScope;
        this.visibility = visibility;
        this.isGlob = isGlob;
        this.isExternCrate = isExternCrate;
        this.isPrelude = isPrelude;
    }

    public Import(
        @NotNull ModData containingMod,
        @NotNull String[] usePath,
        @NotNull String nameInScope,
        @NotNull Visibility visibility,
        boolean isExternCrate
    ) {
        this(containingMod, usePath, nameInScope, visibility, false, isExternCrate, false);
    }

    public Import(
        @NotNull ModData containingMod,
        @NotNull String[] usePath,
        @NotNull String nameInScope,
        @NotNull Visibility visibility
    ) {
        this(containingMod, usePath, nameInScope, visibility, false, false, false);
    }

    @NotNull
    public ModData getContainingMod() {
        return containingMod;
    }

    @NotNull
    public String[] getUsePath() {
        return usePath;
    }

    @NotNull
    public String getNameInScope() {
        return nameInScope;
    }

    @NotNull
    public Visibility getVisibility() {
        return visibility;
    }

    public boolean isGlob() {
        return isGlob;
    }

    public boolean isExternCrate() {
        return isExternCrate;
    }

    public boolean isPrelude() {
        return isPrelude;
    }

    @NotNull
    public PartialResolvedImport getStatus() {
        return status;
    }

    public void setStatus(@NotNull PartialResolvedImport status) {
        this.status = status;
    }

    @Override
    @NotNull
    public String toString() {
        String vis;
        if (visibility == Visibility.PUBLIC) {
            vis = "pub ";
        } else if (visibility instanceof Visibility.Restricted) {
            Visibility.Restricted restricted = (Visibility.Restricted) visibility;
            if (restricted.getInMod() == containingMod) {
                vis = "";
            } else if (restricted.getInMod().isCrateRoot()) {
                vis = "pub(crate) ";
            } else {
                vis = "pub(in " + restricted.getInMod() + ") ";
            }
        } else if (visibility == Visibility.INVISIBLE) {
            vis = "invisible ";
        } else {
            vis = "#[cfg(false)] ";
        }
        String use = isExternCrate ? "extern crate" : "use";
        String glob = isGlob ? "::*" : "";
        String alias = (usePath[usePath.length - 1].equals(nameInScope) || isGlob) ? "" : " as " + nameInScope;
        return "`" + vis + use + " " + String.join("::", usePath) + glob + alias + ";` in " + containingMod.getPath();
    }
}
