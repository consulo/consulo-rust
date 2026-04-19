/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import com.intellij.codeInsight.completion.CompletionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.ext.RsTraitOrImpl;
import org.rust.lang.core.psi.RsTypeParameter;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.KindUtil;
import org.rust.lang.core.types.regions.Region;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TyTypeParameter extends Ty {
    @NotNull
    private final TypeParameter myParameter;

    private TyTypeParameter(@NotNull TypeParameter parameter) {
        super(KindUtil.HAS_TY_TYPE_PARAMETER_MASK);
        myParameter = parameter;
    }

    @NotNull
    public TypeParameter getParameter() {
        return myParameter;
    }

    /**
     * Extracts lifetime bounds from this type parameter (e.g. the {@code 'a} in {@code T: 'a}).
     * builtin (no-PSI) parameters this is always empty.
     */
    @NotNull
    public List<Region> getRegionBounds() {
        if (!(myParameter instanceof Named)) return Collections.emptyList();
        org.rust.lang.core.psi.RsTypeParameter rsParam = ((Named) myParameter).getParameter();
        org.rust.lang.core.psi.RsTypeParamBounds bounds = rsParam.getTypeParamBounds();
        if (bounds == null) return Collections.emptyList();
        java.util.List<Region> result = new java.util.ArrayList<>();
        for (org.rust.lang.core.psi.RsPolybound polybound : bounds.getPolyboundList()) {
            org.rust.lang.core.psi.RsLifetime lifetime = polybound.getBound().getLifetime();
            if (lifetime == null) continue;
            Region region = org.rust.lang.core.types.infer.TyLowering.resolveLifetime(lifetime);
            if (!(region instanceof org.rust.lang.core.types.regions.ReUnknown)) {
                result.add(region);
            }
        }
        return result;
    }

    @NotNull
    public static TyTypeParameter named(@NotNull RsTypeParameter parameter) {
        return new TyTypeParameter(new Named(CompletionUtil.getOriginalOrSelf(parameter)));
    }

    @NotNull
    public static TyTypeParameter self() {
        return new TyTypeParameter(Self.INSTANCE);
    }

    @NotNull
    public static TyTypeParameter self(@NotNull RsTraitOrImpl traitOrImpl) {
        return new TyTypeParameter(new Self(traitOrImpl));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TyTypeParameter that = (TyTypeParameter) o;
        return Objects.equals(myParameter, that.myParameter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myParameter);
    }

    public interface TypeParameter {}

    public static class Named implements TypeParameter {
        @NotNull
        private final RsTypeParameter myParameter;

        public Named(@NotNull RsTypeParameter parameter) {
            myParameter = parameter;
        }

        @NotNull
        public RsTypeParameter getParameter() {
            return myParameter;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Named named = (Named) o;
            return Objects.equals(myParameter, named.myParameter);
        }

        @Override
        public int hashCode() {
            return myParameter.hashCode();
        }

        @Override
        public String toString() {
            String name = myParameter.getName();
            return name != null ? name : "<unknown>";
        }
    }

    public static class Self implements TypeParameter {
        public static final Self INSTANCE = new Self(null);

        @Nullable
        private final RsTraitOrImpl myTraitOrImpl;

        public Self(@Nullable RsTraitOrImpl traitOrImpl) {
            myTraitOrImpl = traitOrImpl;
        }

        @Nullable
        public RsTraitOrImpl getTraitOrImpl() {
            return myTraitOrImpl;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Self self = (Self) o;
            return Objects.equals(myTraitOrImpl, self.myTraitOrImpl);
        }

        @Override
        public int hashCode() {
            return myTraitOrImpl != null ? myTraitOrImpl.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "Self";
        }
    }
}
