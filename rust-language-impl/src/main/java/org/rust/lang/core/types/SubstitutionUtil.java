/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.consts.CtConstParameter;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyTypeParameter;

import java.util.Map;

public final class SubstitutionUtil {
    @Nonnull
    public static final Substitution EMPTY_SUBSTITUTION = new Substitution();

    /** Alias for code that references SubstitutionUtil.EMPTY */
    @Nonnull
    public static final Substitution EMPTY = EMPTY_SUBSTITUTION;

    private SubstitutionUtil() {
    }

    @Nonnull
    public static Substitution emptySubstitution() {
        return EMPTY_SUBSTITUTION;
    }

    @Nonnull
    public static Substitution getEmptySubstitution() {
        return EMPTY_SUBSTITUTION;
    }

    @Nonnull
    public static Substitution toTypeSubst(@Nonnull Map<TyTypeParameter, Ty> map) {
        return new Substitution(map);
    }

    @Nonnull
    public static Substitution toConstSubst(@Nonnull Map<CtConstParameter, Const> map) {
        return new Substitution(java.util.Collections.emptyMap(), java.util.Collections.emptyMap(), map);
    }
}
