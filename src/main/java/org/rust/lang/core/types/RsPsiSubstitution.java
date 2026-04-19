/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.ty.Ty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Similar to {@link Substitution}, but maps PSI to PSI instead of Ty to Ty */
public class RsPsiSubstitution {
    @NotNull
    private final Map<RsTypeParameter, Value<TypeValue, TypeDefault>> myTypeSubst;
    @NotNull
    private final Map<RsLifetimeParameter, Value<RsLifetime, ?>> myRegionSubst;
    @NotNull
    private final Map<RsConstParameter, Value<RsElement, RsExpr>> myConstSubst;
    @NotNull
    private final Map<RsTypeAlias, AssocValue> myAssoc;

    public RsPsiSubstitution() {
        this(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    public RsPsiSubstitution(
        @NotNull Map<RsTypeParameter, Value<TypeValue, TypeDefault>> typeSubst,
        @NotNull Map<RsLifetimeParameter, Value<RsLifetime, ?>> regionSubst,
        @NotNull Map<RsConstParameter, Value<RsElement, RsExpr>> constSubst,
        @NotNull Map<RsTypeAlias, AssocValue> assoc
    ) {
        myTypeSubst = typeSubst;
        myRegionSubst = regionSubst;
        myConstSubst = constSubst;
        myAssoc = assoc;
    }

    @NotNull
    public Map<RsTypeParameter, Value<TypeValue, TypeDefault>> getTypeSubst() {
        return myTypeSubst;
    }

    @NotNull
    public Map<RsLifetimeParameter, Value<RsLifetime, ?>> getRegionSubst() {
        return myRegionSubst;
    }

    @NotNull
    public Map<RsConstParameter, Value<RsElement, RsExpr>> getConstSubst() {
        return myConstSubst;
    }

    @NotNull
    public Map<RsTypeAlias, AssocValue> getAssoc() {
        return myAssoc;
    }

    public static abstract class Value<P, D> {
        private Value() {}

        public static final class RequiredAbsent<P, D> extends Value<P, D> {
            @SuppressWarnings("rawtypes")
            public static final RequiredAbsent INSTANCE = new RequiredAbsent();

            @SuppressWarnings("unchecked")
            public static <P, D> RequiredAbsent<P, D> instance() { return INSTANCE; }
        }

        public static final class OptionalAbsent<P, D> extends Value<P, D> {
            @SuppressWarnings("rawtypes")
            public static final OptionalAbsent INSTANCE = new OptionalAbsent();

            @SuppressWarnings("unchecked")
            public static <P, D> OptionalAbsent<P, D> instance() { return INSTANCE; }
        }

        public static final class Present<P, D> extends Value<P, D> {
            @NotNull
            private final P myValue;

            public Present(@NotNull P value) {
                myValue = value;
            }

            @NotNull
            public P getValue() {
                return myValue;
            }
        }

        public static final class DefaultValue<P, D> extends Value<P, D> {
            @NotNull
            private final D myValue;

            public DefaultValue(@NotNull D value) {
                myValue = value;
            }

            @NotNull
            public D getValue() {
                return myValue;
            }
        }
    }

    public static abstract class TypeValue {
        private TypeValue() {}

        public static final class InAngles extends TypeValue {
            @NotNull
            private final RsTypeReference myValue;

            public InAngles(@NotNull RsTypeReference value) {
                myValue = value;
            }

            @NotNull
            public RsTypeReference getValue() {
                return myValue;
            }
        }

        public static final class FnSugar extends TypeValue {
            @NotNull
            private final List<RsTypeReference> myInputArgs;

            public FnSugar(@NotNull List<RsTypeReference> inputArgs) {
                myInputArgs = inputArgs;
            }

            @NotNull
            public List<RsTypeReference> getInputArgs() {
                return myInputArgs;
            }
        }
    }

    public static class TypeDefault {
        @NotNull
        private final RsTypeReference myValue;
        @Nullable
        private final Ty mySelfTy;

        public TypeDefault(@NotNull RsTypeReference value, @Nullable Ty selfTy) {
            myValue = value;
            mySelfTy = selfTy;
        }

        @NotNull
        public RsTypeReference getValue() {
            return myValue;
        }

        @Nullable
        public Ty getSelfTy() {
            return mySelfTy;
        }
    }

    public static abstract class AssocValue {
        private AssocValue() {}

        public static final class Present extends AssocValue {
            @NotNull
            private final RsTypeReference myValue;

            public Present(@NotNull RsTypeReference value) {
                myValue = value;
            }

            @NotNull
            public RsTypeReference getValue() {
                return myValue;
            }
        }

        public static final class FnSugarImplicitRet extends AssocValue {
            public static final FnSugarImplicitRet INSTANCE = new FnSugarImplicitRet();
        }
    }
}
