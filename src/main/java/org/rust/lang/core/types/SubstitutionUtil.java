/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.consts.CtConstParameter;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyTypeParameter;

import java.util.Map;

public final class SubstitutionUtil {
    @NotNull
    public static final Substitution EMPTY_SUBSTITUTION = new Substitution();

    /** Alias for code that references SubstitutionUtil.EMPTY */
    @NotNull
    public static final Substitution EMPTY = EMPTY_SUBSTITUTION;

    private SubstitutionUtil() {
    }

    @NotNull
    public static Substitution emptySubstitution() {
        return EMPTY_SUBSTITUTION;
    }

    @NotNull
    public static Substitution getEmptySubstitution() {
        return EMPTY_SUBSTITUTION;
    }

    @NotNull
    public static Substitution toTypeSubst(@NotNull Map<TyTypeParameter, Ty> map) {
        return new Substitution(map);
    }

    @NotNull
    public static Substitution toConstSubst(@NotNull Map<CtConstParameter, Const> map) {
        return new Substitution(java.util.Collections.emptyMap(), java.util.Collections.emptyMap(), map);
    }
}
