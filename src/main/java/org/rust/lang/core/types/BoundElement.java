/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types;

import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsGenericDeclaration;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.consts.CtConstParameter;
import org.rust.lang.core.types.infer.TypeFoldable;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.infer.TypeVisitor;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyTypeParameter;
import org.rust.lang.core.types.ty.TyUnknown;
import org.rust.stdext.CollectionsUtil;

import java.util.*;

/**
 * Represents a potentially generic Psi Element, like `fn make_t<T>() { }`,
 * together with actual type arguments, like `T := i32` (subst) and
 * associated type values (assoc).
 */
public class BoundElement<E extends RsElement> implements ResolveResult, TypeFoldable<BoundElement<E>> {
    @NotNull
    private final E myElement;
    @NotNull
    private final Substitution mySubst;
    @NotNull
    private final Map<RsTypeAlias, Ty> myAssoc;

    public BoundElement(@NotNull E element) {
        this(element, SubstitutionUtil.EMPTY_SUBSTITUTION, Collections.emptyMap());
    }

    public BoundElement(@NotNull E element, @NotNull Substitution subst) {
        this(element, subst, Collections.emptyMap());
    }

    public BoundElement(@NotNull E element, @NotNull Substitution subst, @NotNull Map<RsTypeAlias, Ty> assoc) {
        myElement = element;
        mySubst = subst;
        myAssoc = assoc;
    }

    @NotNull
    public E element() {
        return myElement;
    }

    @NotNull
    public Substitution getSubst() {
        return mySubst;
    }

    @NotNull
    public Map<RsTypeAlias, Ty> getAssoc() {
        return myAssoc;
    }

    @Override
    @NotNull
    public PsiElement getElement() {
        return myElement;
    }

    /**
     * Returns the typed element. Use this instead of getElement() when
     * you need the element with its proper type parameter.
     */
    @NotNull
    public E getTypedElement() {
        return myElement;
    }

    @Override
    public boolean isValidResult() {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends RsElement> BoundElement<T> downcast(@NotNull Class<T> clazz) {
        if (clazz.isInstance(myElement)) {
            return new BoundElement<>((T) myElement, mySubst, myAssoc);
        }
        return null;
    }

    @Override
    @NotNull
    public BoundElement<E> superFoldWith(@NotNull TypeFolder folder) {
        Map<RsTypeAlias, Ty> newAssoc = new HashMap<>();
        for (Map.Entry<RsTypeAlias, Ty> entry : myAssoc.entrySet()) {
            newAssoc.put(entry.getKey(), entry.getValue().foldWith(folder));
        }
        return new BoundElement<>(myElement, mySubst.foldValues(folder), newAssoc);
    }

    @Override
    public boolean superVisitWith(@NotNull TypeVisitor visitor) {
        for (Ty value : myAssoc.values()) {
            if (visitor.visitTy(value)) return true;
        }
        return mySubst.visitValues(visitor);
    }

    public <T extends RsElement> boolean isEquivalentTo(@NotNull BoundElement<T> other) {
        if (!myElement.equals(other.myElement)) return false;

        List<com.intellij.openapi.util.Pair<Ty, Ty>> typePairs = mySubst.zipTypeValues(other.mySubst);
        for (com.intellij.openapi.util.Pair<Ty, Ty> pair : typePairs) {
            if (!pair.getFirst().isEquivalentTo(pair.getSecond())) return false;
        }
        if (!mySubst.getConstSubst().equals(other.mySubst.getConstSubst())) return false;
        List<com.intellij.openapi.util.Pair<Ty, Ty>> assocPairs = CollectionsUtil.zipValues(myAssoc, other.myAssoc);
        for (com.intellij.openapi.util.Pair<Ty, Ty> pair : assocPairs) {
            if (!pair.getFirst().isEquivalentTo(pair.getSecond())) return false;
        }
        return true;
    }

    @NotNull
    public static List<Ty> positionalTypeArguments(@NotNull BoundElement<? extends RsGenericDeclaration> element) {
        List<Ty> result = new ArrayList<>();
        for (org.rust.lang.core.psi.RsTypeParameter param : element.element().getTypeParameters()) {
            Ty ty = element.getSubst().get(param);
            result.add(ty != null ? ty : TyTypeParameter.named(param));
        }
        return result;
    }

    @NotNull
    public static List<Const> positionalConstArguments(@NotNull BoundElement<? extends RsGenericDeclaration> element) {
        List<Const> result = new ArrayList<>();
        for (org.rust.lang.core.psi.RsConstParameter param : element.element().getConstParameters()) {
            Const c = element.getSubst().get(param);
            result.add(c != null ? c : new CtConstParameter(param));
        }
        return result;
    }

    @NotNull
    public static Ty singleParamValue(@NotNull BoundElement<? extends RsGenericDeclaration> element) {
        List<org.rust.lang.core.psi.RsTypeParameter> params = element.element().getTypeParameters();
        if (params.size() == 1) {
            Ty ty = element.getSubst().get(params.get(0));
            if (ty != null) return ty;
        }
        return TyUnknown.INSTANCE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BoundElement<?> that = (BoundElement<?>) o;
        return Objects.equals(myElement, that.myElement)
            && Objects.equals(mySubst, that.mySubst)
            && Objects.equals(myAssoc, that.myAssoc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myElement, mySubst, myAssoc);
    }

    /**
     * Creates a new BoundElement with type substitutions derived from positional type arguments.
     */
    @NotNull
    public BoundElement<E> withSubst(@NotNull Ty... subst) {
        if (!(myElement instanceof RsGenericDeclaration)) {
            return this;
        }
        RsGenericDeclaration genDecl = (RsGenericDeclaration) myElement;
        List<org.rust.lang.core.psi.RsTypeParameter> typeParams = genDecl.getTypeParameters();
        Substitution newSubst = SubstitutionUtil.EMPTY_SUBSTITUTION;
        if (subst.length > 0 && !typeParams.isEmpty()) {
            Map<TyTypeParameter, Ty> typeSubst = new HashMap<>();
            for (int i = 0; i < Math.min(subst.length, typeParams.size()); i++) {
                typeSubst.put(TyTypeParameter.named(typeParams.get(i)), subst[i]);
            }
            newSubst = new Substitution(typeSubst, Collections.emptyMap(), Collections.emptyMap());
        }
        return new BoundElement<>(myElement, newSubst, myAssoc);
    }

    /**
     * Creates a new BoundElement with substitution from a Substitution object.
     */
    @NotNull
    public BoundElement<E> withSubst(@NotNull Substitution subst) {
        return new BoundElement<>(myElement, subst, myAssoc);
    }

    /**
     * Creates a new BoundElement with default type substitutions.
     */
    @NotNull
    public BoundElement<E> withDefaultSubst() {
        if (!(myElement instanceof RsGenericDeclaration)) {
            return this;
        }
        RsGenericDeclaration genDecl = (RsGenericDeclaration) myElement;
        List<org.rust.lang.core.psi.RsTypeParameter> typeParams = genDecl.getTypeParameters();
        Map<TyTypeParameter, Ty> typeSubst = new HashMap<>();
        for (org.rust.lang.core.psi.RsTypeParameter param : typeParams) {
            typeSubst.put(TyTypeParameter.named(param), TyTypeParameter.named(param));
        }
        Substitution newSubst = new Substitution(typeSubst, Collections.emptyMap(), Collections.emptyMap());
        return new BoundElement<>(myElement, newSubst, myAssoc);
    }

    @Override
    public String toString() {
        return "BoundElement(element=" + myElement + ", subst=" + mySubst + ", assoc=" + myAssoc + ")";
    }
}
