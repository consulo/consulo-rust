/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils.imports;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;
import org.rust.lang.core.types.ty.Ty;

import java.util.Collection;
import java.util.Set;

/**
 *
 * lives in the 'org.rust.ide.utils.import' package (Java reserved keyword),
 * (RsImportHelperBridge) that are already defined in the 'imports' (plural) package.
 *
 * Key methods:
 * - importTypeReferencesFromElement: imports type references from a PSI element
 * - importTypeReferencesFromTys: imports types from a collection of Ty
 * - importTypeReferencesFromTy: imports types from a single Ty
 * - importElement: imports a single named element
 * - importElements: imports multiple named elements
 * - findPath: finds path to element from context
 * - getTypeReferencesInfoFromTys: collects unresolved type references
 */
public final class RsImportHelper {
    private RsImportHelper() {
    }

    public static void importTypeReferencesFromElement(@NotNull RsElement context, @NotNull RsElement element) {
        ImportBridge.importTypeReferencesFromElement(context, element);
    }

    public static void importTypeReferencesFromTys(
        @NotNull RsElement context,
        @NotNull Collection<Ty> tys
    ) {
        ImportBridge.importTypeReferencesFromTys(context, tys);
    }

    public static void importTypeReferencesFromTy(
        @NotNull RsElement context,
        @NotNull Ty ty
    ) {
        ImportBridge.importTypeReferencesFromTy(context, ty);
    }

    public static void importElement(@NotNull RsElement context, @NotNull RsQualifiedNamedElement element) {
        ImportBridge.importElement(context, element);
    }

    public static void importElements(@NotNull RsElement context, @NotNull Set<RsQualifiedNamedElement> elements) {
        for (RsQualifiedNamedElement element : elements) {
            ImportBridge.importElement(context, element);
        }
    }

    /**
     * Finds the import path to the given element from the given context.
     * Returns a qualified path string, or null if no path can be found.
     */
    @Nullable
    public static String findPath(@NotNull RsElement context, @NotNull RsQualifiedNamedElement target) {
        // Try to get the qualified name relative to the context's module
        String qualifiedName = target.getQualifiedName();
        if (qualifiedName != null) return qualifiedName;
        String crateRelative = target.getCrateRelativePath();
        if (crateRelative != null) return "crate" + crateRelative;
        return null;
    }

    /**
     * <p>
     * Collects the concrete items (ADTs, traits, projections, anon-impls) referenced by each
     * {@link Ty}, then splits them into {@code toImport} (resolvable via
     * {@link ImportCandidatesCollector}) and {@code toQualify} (cannot be imported — must use a
     * fully-qualified path). Alias handling and default-generic-argument elision from the
     */
    @NotNull
    public static TypeReferencesInfo getTypeReferencesInfoFromTys(@NotNull RsElement context, @NotNull Ty... tys) {
        java.util.Set<RsQualifiedNamedElement> raw = new java.util.LinkedHashSet<>();
        for (Ty ty : tys) {
            collectImportSubjectsFromTy(ty, raw);
        }
        return processRawImportSubjects(context, raw);
    }

    private static void collectImportSubjectsFromTy(@NotNull Ty ty, @NotNull java.util.Set<RsQualifiedNamedElement> out) {
        ty.visitWith(new org.rust.lang.core.types.infer.TypeVisitor() {
            @Override
            public boolean visitTy(@NotNull Ty t) {
                if (t instanceof org.rust.lang.core.types.ty.TyAdt) {
                    out.add(((org.rust.lang.core.types.ty.TyAdt) t).getItem());
                } else if (t instanceof org.rust.lang.core.types.ty.TyAnon) {
                    for (org.rust.lang.core.types.BoundElement<?> tr : ((org.rust.lang.core.types.ty.TyAnon) t).getTraits()) {
                        if (tr.getElement() instanceof RsQualifiedNamedElement) {
                            out.add((RsQualifiedNamedElement) tr.getElement());
                        }
                    }
                } else if (t instanceof org.rust.lang.core.types.ty.TyTraitObject) {
                    for (org.rust.lang.core.types.BoundElement<?> tr : ((org.rust.lang.core.types.ty.TyTraitObject) t).getTraits()) {
                        if (tr.getElement() instanceof RsQualifiedNamedElement) {
                            out.add((RsQualifiedNamedElement) tr.getElement());
                        }
                    }
                } else if (t instanceof org.rust.lang.core.types.ty.TyProjection) {
                    org.rust.lang.core.types.ty.TyProjection proj = (org.rust.lang.core.types.ty.TyProjection) t;
                    if (proj.getTrait().getElement() instanceof RsQualifiedNamedElement) {
                        out.add((RsQualifiedNamedElement) proj.getTrait().getElement());
                    }
                    if (proj.getTarget().getElement() instanceof RsQualifiedNamedElement) {
                        out.add((RsQualifiedNamedElement) proj.getTarget().getElement());
                    }
                }
                return t.superVisitWith(this);
            }
        });
    }

    @NotNull
    private static TypeReferencesInfo processRawImportSubjects(
        @NotNull RsElement context,
        @NotNull java.util.Set<RsQualifiedNamedElement> raw
    ) {
        java.util.Set<RsQualifiedNamedElement> toImport = new java.util.HashSet<>();
        java.util.Set<RsQualifiedNamedElement> toQualify = new java.util.HashSet<>();
        ImportContext ctx = ImportContext.from(context, ImportContext.Type.OTHER);
        for (RsQualifiedNamedElement item : raw) {
            ImportCandidate candidate = ctx != null
                ? ImportCandidatesCollector.findImportCandidate(ctx, item)
                : null;
            if (candidate != null) {
                toImport.add(item);
            } else {
                toQualify.add(item);
            }
        }
        return new TypeReferencesInfo(toImport, toQualify);
    }
}
