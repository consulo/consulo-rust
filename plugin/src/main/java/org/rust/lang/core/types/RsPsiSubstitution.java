/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.ty.Ty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Similar to {@link Substitution}, but maps PSI to PSI instead of Ty to Ty */
public class RsPsiSubstitution {
    @Nonnull
    private final Map<RsTypeParameter, Value<TypeValue, TypeDefault>> myTypeSubst;
    @Nonnull
    private final Map<RsLifetimeParameter, Value<RsLifetime, ?>> myRegionSubst;
    @Nonnull
    private final Map<RsConstParameter, Value<RsElement, RsExpr>> myConstSubst;
    @Nonnull
    private final Map<RsTypeAlias, AssocValue> myAssoc;

    public RsPsiSubstitution() {
        this(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    public RsPsiSubstitution(
        @Nonnull Map<RsTypeParameter, Value<TypeValue, TypeDefault>> typeSubst,
        @Nonnull Map<RsLifetimeParameter, Value<RsLifetime, ?>> regionSubst,
        @Nonnull Map<RsConstParameter, Value<RsElement, RsExpr>> constSubst,
        @Nonnull Map<RsTypeAlias, AssocValue> assoc
    ) {
        myTypeSubst = typeSubst;
        myRegionSubst = regionSubst;
        myConstSubst = constSubst;
        myAssoc = assoc;
    }

    @Nonnull
    public Map<RsTypeParameter, Value<TypeValue, TypeDefault>> getTypeSubst() {
        return myTypeSubst;
    }

    @Nonnull
    public Map<RsLifetimeParameter, Value<RsLifetime, ?>> getRegionSubst() {
        return myRegionSubst;
    }

    @Nonnull
    public Map<RsConstParameter, Value<RsElement, RsExpr>> getConstSubst() {
        return myConstSubst;
    }

    @Nonnull
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
            @Nonnull
            private final P myValue;

            public Present(@Nonnull P value) {
                myValue = value;
            }

            @Nonnull
            public P getValue() {
                return myValue;
            }
        }

        public static final class DefaultValue<P, D> extends Value<P, D> {
            @Nonnull
            private final D myValue;

            public DefaultValue(@Nonnull D value) {
                myValue = value;
            }

            @Nonnull
            public D getValue() {
                return myValue;
            }
        }
    }

    public static abstract class TypeValue {
        private TypeValue() {}

        public static final class InAngles extends TypeValue {
            @Nonnull
            private final RsTypeReference myValue;

            public InAngles(@Nonnull RsTypeReference value) {
                myValue = value;
            }

            @Nonnull
            public RsTypeReference getValue() {
                return myValue;
            }
        }

        public static final class FnSugar extends TypeValue {
            @Nonnull
            private final List<RsTypeReference> myInputArgs;

            public FnSugar(@Nonnull List<RsTypeReference> inputArgs) {
                myInputArgs = inputArgs;
            }

            @Nonnull
            public List<RsTypeReference> getInputArgs() {
                return myInputArgs;
            }
        }
    }

    public static class TypeDefault {
        @Nonnull
        private final RsTypeReference myValue;
        @Nullable
        private final Ty mySelfTy;

        public TypeDefault(@Nonnull RsTypeReference value, @Nullable Ty selfTy) {
            myValue = value;
            mySelfTy = selfTy;
        }

        @Nonnull
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
            @Nonnull
            private final RsTypeReference myValue;

            public Present(@Nonnull RsTypeReference value) {
                myValue = value;
            }

            @Nonnull
            public RsTypeReference getValue() {
                return myValue;
            }
        }

        public static final class FnSugarImplicitRet extends AssocValue {
            public static final FnSugarImplicitRet INSTANCE = new FnSugarImplicitRet();
        }
    }
}
