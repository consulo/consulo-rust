/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils.imports;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;
import org.rust.lang.core.types.ty.Ty;

import java.util.Collection;
import java.util.Set;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.infer.TypeVisitor;
import org.rust.lang.core.types.ty.TyAdt;
import org.rust.lang.core.types.ty.TyAnon;
import org.rust.lang.core.types.ty.TyProjection;
import org.rust.lang.core.types.ty.TyTraitObject;

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

    public static void importTypeReferencesFromElement(@Nonnull RsElement context, @Nonnull RsElement element) {
        ImportBridge.importTypeReferencesFromElement(context, element);
    }

    public static void importTypeReferencesFromTys(
        @Nonnull RsElement context,
        @Nonnull Collection<Ty> tys
    ) {
        ImportBridge.importTypeReferencesFromTys(context, tys);
    }

    public static void importTypeReferencesFromTy(
        @Nonnull RsElement context,
        @Nonnull Ty ty
    ) {
        ImportBridge.importTypeReferencesFromTy(context, ty);
    }

    public static void importElement(@Nonnull RsElement context, @Nonnull RsQualifiedNamedElement element) {
        ImportBridge.importElement(context, element);
    }

    public static void importElements(@Nonnull RsElement context, @Nonnull Set<RsQualifiedNamedElement> elements) {
        for (RsQualifiedNamedElement element : elements) {
            ImportBridge.importElement(context, element);
        }
    }

    /**
     * Finds the import path to the given element from the given context.
     * Returns a qualified path string, or null if no path can be found.
     */
    @Nullable
    public static String findPath(@Nonnull RsElement context, @Nonnull RsQualifiedNamedElement target) {
        // Try to get the qualified name relative to the context's module
        String qualifiedName = target.getQualifiedName();
        if (qualifiedName != null) return qualifiedName;
        String crateRelative = target.getCrateRelativePath();
        if (crateRelative != null) return "crate" + crateRelative;
        return null;
    }

    /**
     * Collects the concrete items (ADTs, traits, projections, anon-impls) referenced by each
     * {@link Ty}, then splits them into {@code toImport} (resolvable via
     * {@link ImportCandidatesCollector}) and {@code toQualify} (cannot be imported — must use a
     * fully-qualified path). Alias handling and default-generic-argument elision from the
     */
    /**
     * Collects the items referenced by the unqualified paths inside {@code element}, then splits them
     * into {@code toImport} and {@code toQualify} the same way {@link #getTypeReferencesInfoFromTys} does.
     */
    @Nonnull
    public static TypeReferencesInfo getTypeReferencesInfoFromElement(@Nonnull RsElement context, @Nonnull RsElement element) {
        java.util.Set<RsQualifiedNamedElement> raw = new java.util.LinkedHashSet<>();
        collectImportSubjectsFromTypeReferences(element, raw);
        return processRawImportSubjects(context, raw);
    }

    private static void collectImportSubjectsFromTypeReferences(
        @Nonnull RsElement element,
        @Nonnull java.util.Set<RsQualifiedNamedElement> out
    ) {
        element.accept(new RsVisitor() {
            @Override
            public void visitPath(@Nonnull RsPath path) {
                if (path.getPath() == null) {
                    RsReference reference = path.getReference();
                    RsElement resolved = reference != null ? reference.resolve() : null;
                    if (resolved instanceof RsQualifiedNamedElement) {
                        out.add((RsQualifiedNamedElement) resolved);
                    }
                }
                super.visitPath(path);
            }

            @Override
            public void visitElement(@Nonnull RsElement e) {
                e.acceptChildren(this);
            }
        });
    }

    @Nonnull
    public static TypeReferencesInfo getTypeReferencesInfoFromTys(@Nonnull RsElement context, @Nonnull Ty... tys) {
        java.util.Set<RsQualifiedNamedElement> raw = new java.util.LinkedHashSet<>();
        for (Ty ty : tys) {
            collectImportSubjectsFromTy(ty, raw);
        }
        return processRawImportSubjects(context, raw);
    }

    private static void collectImportSubjectsFromTy(@Nonnull Ty ty, @Nonnull java.util.Set<RsQualifiedNamedElement> out) {
        ty.visitWith(new org.rust.lang.core.types.infer.TypeVisitor() {
            @Override
            public boolean visitTy(@Nonnull Ty t) {
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

    @Nonnull
    private static TypeReferencesInfo processRawImportSubjects(
        @Nonnull RsElement context,
        @Nonnull java.util.Set<RsQualifiedNamedElement> raw
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
