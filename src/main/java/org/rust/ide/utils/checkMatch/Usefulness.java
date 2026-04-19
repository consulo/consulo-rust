/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils.checkMatch;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsEnumItem;
import org.rust.lang.core.types.ty.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A witness of non-exhaustiveness.
 * At the end of exhaustiveness checking, the witness will have length 1,
 * but in the middle of the algorithm, it can contain multiple patterns.
 */
final class Witness {
    @NotNull
    private final List<Pattern> patterns;

    public Witness() {
        this.patterns = new ArrayList<>();
    }

    public Witness(@NotNull List<Pattern> patterns) {
        this.patterns = new ArrayList<>(patterns);
    }

    @NotNull
    public List<Pattern> getPatterns() {
        return patterns;
    }

    @NotNull
    public Witness cloneWitness() {
        return new Witness(new ArrayList<>(patterns));
    }

    @NotNull
    public Witness pushWildConstructor(@NotNull Constructor constructor, @NotNull Ty type) {
        List<Ty> subPatternTypes = constructor.subTypes(type);
        for (Ty ty : subPatternTypes) {
            patterns.add(Pattern.wild(ty));
        }
        return applyConstructor(constructor, type);
    }

    @NotNull
    public Witness applyConstructor(@NotNull Constructor constructor, @NotNull Ty type) {
        int arity = constructor.arity(type);
        int len = patterns.size();
        List<Pattern> oldPatterns = patterns.subList(len - arity, len);
        List<Pattern> pats = new ArrayList<>(oldPatterns);
        Collections.reverse(pats);
        oldPatterns.clear();

        PatternKind kind;
        if (type instanceof TyAdt) {
            TyAdt adt = (TyAdt) type;
            if (adt.getItem() instanceof RsEnumItem) {
                kind = new PatternKind.Variant(
                    (RsEnumItem) adt.getItem(),
                    ((Constructor.Variant) constructor).getVariant(),
                    pats
                );
            } else {
                kind = new PatternKind.Leaf(pats);
            }
        } else if (type instanceof TyTuple) {
            kind = new PatternKind.Leaf(pats);
        } else if (type instanceof TyReference) {
            kind = new PatternKind.Deref(pats.get(0));
        } else if (type instanceof TySlice || type instanceof TyArray) {
            throw new UnsupportedOperationException("TODO: Slice/Array witness");
        } else if (constructor instanceof Constructor.ConstantValue) {
            kind = new PatternKind.Const(((Constructor.ConstantValue) constructor).getValue());
        } else {
            kind = PatternKind.Wild;
        }
        patterns.add(new Pattern(type, kind));
        return this;
    }

    @Override
    public String toString() {
        return patterns.toString();
    }
}
