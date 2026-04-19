/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.macros.proc.ProcMacroApplicationService;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve2.DefMapService;
import org.rust.lang.core.stubs.RsAttributeOwnerStub;
import org.rust.lang.core.stubs.common.RsAttrProcMacroOwnerPsiOrStub;
import org.rust.lang.core.stubs.common.RsMetaItemPsiOrStub;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The class helps to check whether some item is a procedural macro call or not.
 */
public abstract class ProcMacroAttribute<T extends RsMetaItemPsiOrStub> {
    @Nullable
    public abstract T getAttr();

    /**
     * The item has no attribute procedural macros, but may have custom derive attributes
     */
    public static class Derive<T extends RsMetaItemPsiOrStub> extends ProcMacroAttribute<T> {
        @NotNull
        private final Iterable<T> myDerives;

        public Derive(@NotNull Iterable<T> derives) {
            myDerives = derives;
        }

        @NotNull
        public Iterable<T> getDerives() {
            return myDerives;
        }

        @Nullable
        @Override
        public T getAttr() {
            return null;
        }
    }

    /**
     * The item has attribute procedural macro call
     */
    public static class Attr<T extends RsMetaItemPsiOrStub> extends ProcMacroAttribute<T> {
        @NotNull
        private final T myAttr;
        private final int myIndex;

        public Attr(@NotNull T attr, int index) {
            myAttr = attr;
            myIndex = index;
        }

        @NotNull
        @Override
        public T getAttr() {
            return myAttr;
        }

        public int getIndex() {
            return myIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Attr<?> attr = (Attr<?>) o;
            return myIndex == attr.myIndex && Objects.equals(myAttr, attr.myAttr);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myAttr, myIndex);
        }
    }

    // ---- Static factory methods ----

    @NotNull
    public static <T extends RsMetaItemPsiOrStub> List<ProcMacroAttribute<T>> getProcMacroAttributeWithoutResolve(
        @NotNull RsAttrProcMacroOwnerPsiOrStub<T> owner,
        @Nullable RsAttributeOwnerStub stub,
        @Nullable Crate explicitCrate,
        boolean withDerives,
        @Nullable CustomAttributes explicitCustomAttributes,
        boolean ignoreProcMacrosDisabled
    ) {
        if (!ignoreProcMacrosDisabled && !ProcMacroApplicationService.isAnyEnabled()) return Collections.emptyList();
        if (stub != null) {
            if (!stub.getMayHaveCustomAttrs()) {
                if (stub.getMayHaveCustomDerive() && RsProcMacroPsiUtil.canOwnDeriveAttrs(owner)) {
                    if (!ignoreProcMacrosDisabled && !ProcMacroApplicationService.isDeriveEnabled()) return Collections.emptyList();
                    if (withDerives) {
                        QueryAttributes<T> queryAttributes = new QueryAttributes<>(owner.getRawOuterMetaItems());
                        return Collections.singletonList(new Derive<>(queryAttributes.getCustomDeriveMetaItems()));
                    } else {
                        return Collections.singletonList(new Derive<>(Collections.emptyList()));
                    }
                } else {
                    return Collections.emptyList();
                }
            }
        }

        Crate crate = explicitCrate;
        if (crate == null) {
            crate = Crate.asNotFake(RsElementUtil.getContainingCrate((RsElement) owner));
            if (crate == null) return Collections.emptyList();
        }

        if (crate.getOrigin() == PackageOrigin.STDLIB || crate.getOrigin() == PackageOrigin.STDLIB_DEPENDENCY) {
            return Collections.emptyList();
        }

        CustomAttributes customAttributes = explicitCustomAttributes != null
            ? explicitCustomAttributes
            : CustomAttributes.fromCrate(crate);

        QueryAttributes<T> queryAttributes = new QueryAttributes<>(owner.getRawMetaItems());
        List<ProcMacroAttribute<T>> result = new ArrayList<>();
        int index = 0;
        for (T meta : queryAttributes.getMetaItems()) {
            if ("derive".equals(meta.getName())) {
                if (RsProcMacroPsiUtil.canOwnDeriveAttrs(owner)) {
                    if (!ignoreProcMacrosDisabled && !ProcMacroApplicationService.isDeriveEnabled()) break;
                    if (withDerives) {
                        result.add(new Derive<>(queryAttributes.getCustomDeriveMetaItems()));
                    } else {
                        result.add(new Derive<>(Collections.emptyList()));
                    }
                }
                break;
            }
            if (RsProcMacroPsiUtil.canBeProcMacroAttributeCallWithoutContextCheck(meta, customAttributes)
                && (ignoreProcMacrosDisabled || ProcMacroApplicationService.isAttrEnabled())) {
                result.add(new Attr<>(meta, index));
            }
            index++;
        }
        return result;
    }

    @NotNull
    public static <T extends RsMetaItemPsiOrStub> List<ProcMacroAttribute<T>> getAllPossibleProcMacroAttributes(
        @NotNull RsAttrProcMacroOwnerPsiOrStub<T> owner,
        @Nullable RsAttributeOwnerStub stub,
        @NotNull Crate crate
    ) {
        return getProcMacroAttributeWithoutResolve(owner, stub, crate, true, null, false);
    }

    @Nullable
    public static ProcMacroAttribute<RsMetaItem> getProcMacroAttribute(
        @NotNull RsAttrProcMacroOwner owner
    ) {
        return getProcMacroAttribute(owner, org.rust.lang.core.psi.ext.RsDocAndAttributeOwnerUtil.getAttributeStub(owner), null, false, false);
    }

    @Nullable
    public static ProcMacroAttribute<RsMetaItem> getProcMacroAttribute(
        @NotNull RsAttrProcMacroOwner owner,
        @Nullable RsAttributeOwnerStub stub,
        @Nullable Crate explicitCrate,
        boolean withDerives,
        boolean ignoreProcMacrosDisabled
    ) {
        return getProcMacroAttributeWithHardcoded(owner, stub, explicitCrate, withDerives, ignoreProcMacrosDisabled, null);
    }

    @NotNull
    public static List<KnownProcMacroKind> getHardcodedProcMacroAttributes(@NotNull RsAttrProcMacroOwner owner) {
        List<KnownProcMacroKind> ignoredAttributes = new ArrayList<>();
        getProcMacroAttributeWithHardcoded(owner, org.rust.lang.core.psi.ext.RsDocAndAttributeOwnerUtil.getAttributeStub(owner), null, false, false, ignoredAttributes);
        return ignoredAttributes;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static ProcMacroAttribute<RsMetaItem> getProcMacroAttributeWithHardcoded(
        @NotNull RsAttrProcMacroOwner owner,
        @Nullable RsAttributeOwnerStub stub,
        @Nullable Crate explicitCrate,
        boolean withDerives,
        boolean ignoreProcMacrosDisabled,
        @Nullable List<KnownProcMacroKind> outIgnoredAttributes
    ) {
        List<ProcMacroAttribute<RsMetaItem>> attrs =
            (List<ProcMacroAttribute<RsMetaItem>>) (List<?>) getProcMacroAttributeWithoutResolve(
                owner, stub, explicitCrate, withDerives, null, ignoreProcMacrosDisabled);

        if (!RsProcMacroPsiUtil.canFallBackAttrMacroToOriginalItem(owner)) {
            return attrs.isEmpty() ? null : attrs.get(0);
        }

        ProcMacroAttribute<RsMetaItem> firstSeenAttrMacro = null;
        for (ProcMacroAttribute<RsMetaItem> attr : attrs) {
            if (attr instanceof Derive) {
                return firstSeenAttrMacro != null ? firstSeenAttrMacro : attr;
            } else if (attr instanceof Attr) {
                if (firstSeenAttrMacro == null) {
                    firstSeenAttrMacro = attr;
                }
                RsMetaItem metaItem = ((Attr<RsMetaItem>) attr).getAttr();
                org.rust.lang.core.resolve2.ProcMacroDefInfo defInfo = org.rust.lang.core.resolve2.FacadeResolve.resolveToProcMacroWithoutPsi(metaItem, false);
                KnownProcMacroKind kind = defInfo != null ? defInfo.getKind() : null;
                if (kind != null && outIgnoredAttributes != null) {
                    outIgnoredAttributes.add(kind);
                }
                if (kind == null || !kind.getTreatAsBuiltinAttr()) {
                    return firstSeenAttrMacro;
                }
            }
        }
        return null;
    }
}
