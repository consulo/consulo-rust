/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils.checkMatch;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.presentation.RsPsiRenderingUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsFieldsOwner;
import org.rust.lang.core.psi.ext.RsNamedElement;
import org.rust.lang.core.psi.ext.RsStubbedElementKindUtil;
import org.rust.lang.core.resolve.NameResolution;
import org.rust.lang.core.types.ty.*;

import java.util.*;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsEnumVariantUtil;
import org.rust.ide.presentation.TypeRendering;

public final class Pattern {
    @NotNull
    private final Ty ty;
    @NotNull
    private final PatternKind kind;

    public Pattern(@NotNull Ty ty, @NotNull PatternKind kind) {
        this.ty = ty;
        this.kind = kind;
    }

    @NotNull
    public Ty getTy() {
        return ty;
    }

    @NotNull
    public PatternKind getKind() {
        return kind;
    }

    @NotNull
    public String text(@Nullable RsElement ctx) {
        if (kind == PatternKind.Wild) {
            return "_";
        }
        if (kind instanceof PatternKind.Binding) {
            return ((PatternKind.Binding) kind).getName();
        }
        if (kind instanceof PatternKind.Variant) {
            PatternKind.Variant v = (PatternKind.Variant) kind;
            String variantName = v.getVariant().getName() != null ? v.getVariant().getName() : "";
            String itemName = TypeRendering.renderInsertionSafe(ty, false, false, false);

            RsNamedElement variantInScope = null;
            if (ctx != null) {
                RsNamedElement found = NameResolution.findInScope(ctx, variantName, NameResolution.getVALUES());
                if (found instanceof RsEnumVariant) {
                    RsEnumVariant ev = (RsEnumVariant) found;
                    if (RsEnumVariantUtil.getParentEnum(ev).equals(v.getItem())) {
                        variantInScope = found;
                    }
                }
            }

            String name;
            if (variantInScope != null) {
                name = RsRawIdentifiers.escapeIdentifierIfNeeded(variantName);
            } else {
                name = RsRawIdentifiers.escapeIdentifierIfNeeded(itemName) + "::" + RsRawIdentifiers.escapeIdentifierIfNeeded(variantName);
            }
            String initializer = initializerForFieldsOwner(v.getVariant(), v.getSubPatterns(), ctx);
            return name + initializer;
        }
        if (kind instanceof PatternKind.Leaf) {
            PatternKind.Leaf leaf = (PatternKind.Leaf) kind;
            List<Pattern> subPatterns = leaf.getSubPatterns();
            if (ty instanceof TyTuple) {
                return subPatterns.stream().map(p -> p.text(ctx)).collect(Collectors.joining(", ", "(", ")"));
            }
            if (ty instanceof TyAdt) {
                TyAdt adt = (TyAdt) ty;
                String rawName = null;
                if (adt.getItem() instanceof RsStructItem) {
                    rawName = ((RsStructItem) adt.getItem()).getName();
                } else if (adt.getItem() instanceof RsEnumVariant) {
                    rawName = ((RsEnumVariant) adt.getItem()).getName();
                }
                String escapedName = RsRawIdentifiers.escapeIdentifierIfNeeded(rawName != null ? rawName : "");
                String initializer = initializerForFieldsOwner((RsFieldsOwner) adt.getItem(), subPatterns, ctx);
                return escapedName + initializer;
            }
            return "";
        }
        if (kind instanceof PatternKind.Range) {
            PatternKind.Range range = (PatternKind.Range) kind;
            return range.getLc().toString() + (range.isInclusive() ? ".." : "..=") + range.getRc().toString();
        }
        if (kind instanceof PatternKind.Deref) {
            return "&" + ((PatternKind.Deref) kind).getSubPattern().text(ctx);
        }
        if (kind instanceof PatternKind.Const) {
            return ((PatternKind.Const) kind).getValue().toString();
        }
        if (kind instanceof PatternKind.Slice) {
            throw new UnsupportedOperationException("TODO: Slice pattern text");
        }
        if (kind instanceof PatternKind.Array) {
            throw new UnsupportedOperationException("TODO: Array pattern text");
        }
        return "";
    }

    @Nullable
    public List<Constructor> getConstructors() {
        if (kind == PatternKind.Wild || kind instanceof PatternKind.Binding) {
            return null;
        }
        if (kind instanceof PatternKind.Variant) {
            return Collections.singletonList(new Constructor.Variant(((PatternKind.Variant) kind).getVariant()));
        }
        if (kind instanceof PatternKind.Leaf || kind instanceof PatternKind.Deref) {
            return Collections.singletonList(Constructor.Single.INSTANCE);
        }
        if (kind instanceof PatternKind.Const) {
            return Collections.singletonList(new Constructor.ConstantValue(((PatternKind.Const) kind).getValue()));
        }
        if (kind instanceof PatternKind.Range) {
            PatternKind.Range r = (PatternKind.Range) kind;
            return Collections.singletonList(new Constructor.ConstantRange(r.getLc(), r.getRc(), r.isInclusive()));
        }
        if (kind instanceof PatternKind.Slice) {
            throw new UnsupportedOperationException("TODO: Slice constructors");
        }
        if (kind instanceof PatternKind.Array) {
            throw new UnsupportedOperationException("TODO: Array constructors");
        }
        return null;
    }

    /**
     * Returns the type of the pattern suitable for generating constructors.
     * Returns dereferenced ty when ty is a (multi)reference to enum.
     * Returns ty in other cases.
     */
    @NotNull
    public Ty getErgonomicType() {
        Ty referencedBase = ty;
        while (referencedBase instanceof TyReference) {
            referencedBase = ((TyReference) referencedBase).getReferenced();
        }
        if (referencedBase instanceof TyAdt && ((TyAdt) referencedBase).getItem() instanceof RsEnumItem) {
            return referencedBase;
        }
        return ty;
    }

    @NotNull
    public static Pattern wild(@NotNull Ty ty) {
        return new Pattern(ty, PatternKind.Wild);
    }

    @NotNull
    public static Pattern wild() {
        return new Pattern(TyUnknown.INSTANCE, PatternKind.Wild);
    }

    @NotNull
    private static String initializerForFieldsOwner(@NotNull RsFieldsOwner owner, @NotNull List<Pattern> subPatterns, @Nullable RsElement ctx) {
        if (owner.getBlockFields() != null) {
            boolean allWild = subPatterns.stream().allMatch(p -> p.getKind() == PatternKind.Wild);
            if (allWild) {
                return "{ .. }";
            }
            StringBuilder sb = new StringBuilder("{");
            List<RsNamedFieldDecl> fields = owner.getBlockFields().getNamedFieldDeclList();
            for (int i = 0; i < subPatterns.size(); i++) {
                if (i > 0) sb.append(",");
                String fieldName = fields.get(i).getName();
                sb.append(RsRawIdentifiers.escapeIdentifierIfNeeded(fieldName != null ? fieldName : ""));
                sb.append(": ");
                sb.append(subPatterns.get(i).text(ctx));
            }
            sb.append("}");
            return sb.toString();
        }
        if (owner.getTupleFields() != null) {
            return subPatterns.stream().map(p -> p.text(ctx)).collect(Collectors.joining(",", "(", ")"));
        }
        return "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pattern)) return false;
        Pattern pattern = (Pattern) o;
        return ty.equals(pattern.ty) && kind.equals(pattern.kind);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ty, kind);
    }

    @Override
    public String toString() {
        return "Pattern(ty=" + ty + ", kind=" + kind + ")";
    }
}
