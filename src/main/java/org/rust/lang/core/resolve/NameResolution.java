/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.ThreeState;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.macros.decl.DeclMacroConstantsUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.ref.DotExprResolveVariant;
import org.rust.lang.core.resolve.ref.FieldResolveVariant;
import org.rust.lang.core.resolve.ref.MethodResolveVariant;
import org.rust.lang.core.types.ty.Ty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Rust name resolution algorithm. Top-level entry points for resolving paths, method calls,
 * field references, pattern bindings, labels, lifetimes, macros, and every other identifier
 * scope used by the IDE.
 */
public final class NameResolution {

    private NameResolution() {}

    /**
     * {@code 'crate::mod1::mod2' -> ('crate', 'mod1::mod2')}. Null if no {@code ::} is found.
     * Throws if the path begins with {@code ::}.
     */
    @Nullable
    public static Pair<String, String> splitAbsolutePath(@NotNull String path) {
        if (path.startsWith("::")) {
            throw new IllegalStateException("splitAbsolutePath received path starting with ::");
        }
        int idx = path.indexOf("::");
        if (idx < 0) return null;
        return new Pair<>(path.substring(0, idx), path.substring(idx + 2));
    }

    @Nullable
    public static RsFile findDependencyCrateByNamePath(@NotNull RsElement context, @NotNull RsPath path) {
        String referenceName = path.getReferenceName();
        if (referenceName == null) return null;
        if (DeclMacroConstantsUtil.MACRO_DOLLAR_CRATE_IDENTIFIER.equals(referenceName)) {
            Crate c = resolveDollarCrateIdentifier(path);
            if (c == null) return null;
            RsFile root = c.getRootMod();
            return root;
        }
        if ("crate".equals(referenceName)) {
            RsMod crateRoot = context.getCrateRoot();
            return crateRoot instanceof RsFile ? (RsFile) crateRoot : null;
        }
        return findDependencyCrateByName(context, referenceName);
    }

    @Nullable
    public static RsFile findDependencyCrateByName(@NotNull RsElement context, @NotNull String name) {
        // Scan all transitive crate dependencies for a crate whose normName matches.
        Crate myCrate = RsElementUtil.getContainingCrate(context);
        if (myCrate == null) return null;
        for (Crate dep : myCrate.getFlatDependencies()) {
            if (name.equals(dep.getNormName())) {
                RsFile root = dep.getRootMod();
                if (root != null) return root;
            }
        }
        return null;
    }

    @Nullable
    public static RsFile findDependencyCrateByName(@NotNull RsElement context, @NotNull String name, @Nullable Object extra) {
        return findDependencyCrateByName(context, name);
    }

    @Nullable
    public static Crate resolveDollarCrateIdentifier(@NotNull RsPath path) {
        // crate-id mapping ported, we fall back to the containing crate which is correct
        // in non-macro contexts.
        return RsElementUtil.getContainingCrate(path);
    }

    /**
     * {@code resolveStringPath(path, workspace, project, isStd)} — absolute-path resolution
     * used by {@code #[lang = "std::iter::Iterator"]} attributes and similar. We locate the
     * package, then walk a synthetic crate-relative path through the code-fragment factory.
     */
    @Nullable
    public static Pair<RsNamedElement, CargoWorkspace.Package> resolveStringPath(
        @NotNull String path,
        @NotNull CargoWorkspace workspace,
        @NotNull Project project,
        @NotNull ThreeState isStd
    ) {
        Pair<String, String> split = splitAbsolutePath(path);
        if (split == null) return null;
        CargoWorkspace.Package pkg = workspace.findPackageByName(split.getFirst(), isStd);
        if (pkg == null) return null;

        RsCodeFragmentFactory factory = new RsCodeFragmentFactory(project);
        for (CargoWorkspace.Target target : pkg.getTargets()) {
            RsPath crateRelPath = factory.createCrateRelativePath(split.getSecond(), target);
            if (crateRelPath == null) continue;
            PsiElement crateRootOwner = crateRelPath.getContainingFile().getContext();
            if (!(crateRootOwner instanceof RsFile)) continue;
            Crate containing = RsElementUtil.getContainingCrate((RsFile) crateRootOwner);
            Crate notFake = containing != null ? Crate.asNotFake(containing) : null;
            if (notFake == null || notFake.getId() == null) continue;
            if (crateRelPath.getReference() == null) continue;
            PsiElement resolved = crateRelPath.getReference().resolve();
            if (resolved instanceof RsNamedElement) return new Pair<>((RsNamedElement) resolved, pkg);
        }
        return null;
    }

    @Nullable
    public static Pair<RsNamedElement, CargoWorkspace.Package> resolveStringPath(
        @NotNull String path,
        @NotNull CargoWorkspace workspace,
        @NotNull Project project
    ) {
        return resolveStringPath(path, workspace, project, ThreeState.UNSURE);
    }

    /**
     * Compute the allowed namespace mask for a path based on its syntactic position
     * (imports allow all 3, trait refs require TYPES, path expressions require VALUES, etc.).
     *
     */
    @NotNull
    public static Set<Namespace> allowedNamespaces(@NotNull RsPath path) {
        PsiElement parent = path.getParent();
        if (parent instanceof RsPath) {
            // Qualifier: allow Types only (the tail segment refines further).
            return Namespace.TYPES;
        }
        if (parent instanceof RsTraitRef || parent instanceof RsPathType) return Namespace.TYPES;
        if (parent instanceof RsPathExpr || parent instanceof RsPatTupleStruct || parent instanceof RsPatStruct) {
            return Namespace.TYPES_N_VALUES;
        }
        if (parent instanceof RsMacroCall || parent instanceof RsMetaItem) return Namespace.MACROS;
        if (parent instanceof RsUseSpeck) return Namespace.TYPES_N_VALUES_N_MACROS;
        // Default: treat as general resolution — types and values.
        return Namespace.TYPES_N_VALUES_N_MACROS;
    }

    // -------------------------------------------------------------------------
    // Large scope-walking APIs — see individual docstrings.
    // -------------------------------------------------------------------------

    /** Resolve a {@code x.name} dot-expression — tries field resolution first, then methods. */
    public static boolean processDotExprResolveVariants(
        @NotNull ImplLookup lookup,
        @NotNull Ty receiverType,
        @NotNull RsElement context,
        @NotNull RsResolveProcessorBase<DotExprResolveVariant> processor
    ) {
        if (processFieldExprResolveVariants(lookup, receiverType, fieldToDotWrapper(processor))) return true;
        return processMethodCallExprResolveVariants(lookup, receiverType, context, methodToDotWrapper(processor));
    }

    /**
     * Resolve a field access on {@code receiverType}: walk the auto-deref coercion sequence and
     * for each struct ADT in that chain emit its fields as {@link FieldResolveVariant}s.
     */
    public static boolean processFieldExprResolveVariants(
        @NotNull ImplLookup lookup,
        @NotNull Ty receiverType,
        @NotNull RsResolveProcessorBase<FieldResolveVariant> originalProcessor
    ) {
        org.rust.lang.core.types.infer.Autoderef autoderef = lookup.coercionSequence(receiverType);
        for (Ty ty : autoderef) {
            if (!(ty instanceof org.rust.lang.core.types.ty.TyAdt)) continue;
            org.rust.lang.core.psi.ext.RsStructOrEnumItemElement item = ((org.rust.lang.core.types.ty.TyAdt) ty).getItem();
            if (!(item instanceof RsStructItem)) continue;
            final Ty currentTy = ty;
            if (processStructFieldsAs(item, (RsStructItem) item, entry ->
                originalProcessor.process(new FieldResolveVariant(
                    entry.getName(),
                    entry.getElement(),
                    currentTy,
                    autoderef.steps(),
                    autoderef.obligations())))) {
                return true;
            }
        }
        return false;
    }

    private static boolean processStructFieldsAs(
        @NotNull org.rust.lang.core.psi.ext.RsStructOrEnumItemElement owner,
        @NotNull RsStructItem struct,
        @NotNull java.util.function.Function<ScopeEntry, Boolean> sink
    ) {
        for (RsFieldDecl decl : struct.getFields()) {
            String name = decl.getName();
            if (name == null) continue;
            if (sink.apply(new SimpleScopeEntry(name, decl, Namespace.VALUES))) return true;
        }
        return false;
    }

    private static RsResolveProcessorBase<FieldResolveVariant> fieldToDotWrapper(
        @NotNull RsResolveProcessorBase<DotExprResolveVariant> processor
    ) {
        return new RsResolveProcessorBase<FieldResolveVariant>() {
            @Override
            public boolean process(@NotNull FieldResolveVariant entry) {
                return processor.process(entry);
            }

            @Override
            public Set<String> getNames() {
                return processor.getNames();
            }
        };
    }

    private static RsResolveProcessorBase<MethodResolveVariant> methodToDotWrapper(
        @NotNull RsResolveProcessorBase<DotExprResolveVariant> processor
    ) {
        return new RsResolveProcessorBase<MethodResolveVariant>() {
            @Override
            public boolean process(@NotNull MethodResolveVariant entry) {
                return processor.process(entry);
            }

            @Override
            public Set<String> getNames() {
                return processor.getNames();
            }
        };
    }

    /** Enumerate declared fields of the struct-literal target; fall back to in-scope values for shorthand. */
    public static boolean processStructLiteralFieldResolveVariants(
        @NotNull RsStructLiteralField field,
        boolean isCompletion,
        @NotNull RsResolveProcessor processor
    ) {
        RsStructLiteral literal = RsStructLiteralFieldUtil.getParentStructLiteral(field);
        PsiElement resolved = literal != null && literal.getPath() != null && literal.getPath().getReference() != null
            ? org.rust.lang.core.resolve.ref.RsPathReferenceImpl.deepResolve(
                (org.rust.lang.core.resolve.ref.RsPathReference) literal.getPath().getReference())
            : null;
        if (resolved instanceof RsFieldsOwner
            && processFieldDeclarations((RsFieldsOwner) resolved, processor)) {
            return true;
        }
        if (!isCompletion && field.getExpr() == null) {
            processNestedScopesUpwards(field, Namespace.VALUES, processor);
        }
        return false;
    }

    /** Enumerate fields of the struct pattern's target. */
    public static boolean processStructPatternFieldResolveVariants(
        @NotNull RsPatFieldFull field,
        @NotNull RsResolveProcessor processor
    ) {
        RsPatStruct pat = RsPatFieldFullUtil.getParentStructPattern(field);
        if (pat == null || pat.getPath() == null || pat.getPath().getReference() == null) return false;
        PsiElement resolved = org.rust.lang.core.resolve.ref.RsPathReferenceImpl.deepResolve(
            (org.rust.lang.core.resolve.ref.RsPathReference) pat.getPath().getReference());
        if (!(resolved instanceof RsFieldsOwner)) return false;
        return processFieldDeclarations((RsFieldsOwner) resolved, processor);
    }

    private static boolean processFieldDeclarations(@NotNull RsFieldsOwner struct, @NotNull RsResolveProcessor processor) {
        for (RsFieldDecl decl : struct.getFields()) {
            String name = decl.getName();
            if (name == null) continue;
            if (Processors.processEntry(processor, name, Namespace.VALUES, decl)) return true;
        }
        return false;
    }

    /**
     * Resolve a {@code x.method()} method call on a value of type {@code receiverType}. Walks
     * the auto-deref coercion sequence and, for each step, enumerates every impl / trait
     * reachable from {@link ImplLookup#findImplsAndTraits} and emits each function member of
     * those impls / traits as a {@link MethodResolveVariant}.
     */
    public static boolean processMethodCallExprResolveVariants(
        @NotNull ImplLookup lookup,
        @NotNull Ty receiverType,
        @NotNull RsElement context,
        @NotNull RsResolveProcessorBase<MethodResolveVariant> processor
    ) {
        org.rust.lang.core.types.infer.Autoderef autoderef = lookup.coercionSequence(receiverType);
        int derefIndex = 0;
        for (Ty ty : autoderef) {
            for (TraitImplSource source : lookup.findImplsAndTraits(ty)) {
                org.rust.lang.core.psi.ext.RsTraitOrImpl implSite = source.getValue();
                if (implSite == null) continue;
                for (org.rust.lang.core.psi.ext.RsAbstractable member :
                    RsTraitOrImplUtil.getExpandedMembers(implSite)) {
                    if (!(member instanceof RsFunction)) continue;
                    RsFunction fn = (RsFunction) member;
                    if (!RsFunctionUtil.isMethod(fn)) continue;
                    String name = fn.getName();
                    if (name == null) continue;
                    MethodResolveVariant variant = new MethodResolveVariant(name, fn, ty, derefIndex, source);
                    if (processor.process(variant)) return true;
                }
            }
            derefIndex++;
        }
        return false;
    }

    /**
     * Resolve a {@code mod foo;} declaration to the corresponding file. Honours the
     * {@code #[path = "..."]} attribute, and otherwise searches the owned directory for
     * {@code foo.rs} / {@code foo/mod.rs}.
     */
    public static boolean processModDeclResolveVariants(
        @NotNull RsModDeclItem modDecl,
        @NotNull RsResolveProcessor processor
    ) {
        com.intellij.psi.PsiManager psiMgr = com.intellij.psi.PsiManager.getInstance(modDecl.getProject());
        RsMod containingMod = modDecl.getContainingMod();
        com.intellij.psi.PsiDirectory ownedDirectory = containingMod.getOwnedDirectory();
        com.intellij.psi.PsiFile contextualFile = RsElementExtUtil.getContextualFile(modDecl);
        com.intellij.openapi.vfs.VirtualFile originalFile = contextualFile.getOriginalFile().getVirtualFile();
        boolean inModRs = org.rust.lang.RsConstants.MOD_RS_FILE.equals(contextualFile.getName());

        String explicitPath = RsModDeclItemUtil.getPathAttribute(modDecl);
        if (explicitPath != null) {
            com.intellij.psi.PsiDirectory dir = containingMod instanceof RsFile
                ? contextualFile.getParent()
                : ownedDirectory;
            if (dir == null) return false;
            com.intellij.openapi.vfs.VirtualFile vFile = dir.getVirtualFile().findFileByRelativePath(
                com.intellij.openapi.util.io.FileUtil.toSystemIndependentName(explicitPath));
            if (vFile == null) return false;
            RsFile mod = RsFileUtil.getRustFile(psiMgr.findFile(vFile));
            if (mod == null) return false;
            String name = modDecl.getName();
            if (name == null) return false;
            return Processors.processEntry(processor, name, Namespace.TYPES, mod);
        }
        if (ownedDirectory == null) return false;
        if (RsModDeclItemUtil.isLocal(modDecl)) return false;

        String modDeclName = ((org.rust.lang.core.psi.ext.RsMandatoryReferenceElement) modDecl).getReferenceName();
        if (modDeclName == null) return false;

        java.util.List<com.intellij.openapi.vfs.VirtualFile> dirs = new java.util.ArrayList<>();
        java.util.List<com.intellij.openapi.vfs.VirtualFile> files = new java.util.ArrayList<>();
        for (com.intellij.openapi.vfs.VirtualFile child : ownedDirectory.getVirtualFile().getChildren()) {
            if (child.isDirectory()) dirs.add(child); else files.add(child);
        }

        for (com.intellij.openapi.vfs.VirtualFile vFile : files) {
            String rawFileName = vFile.getName();
            if (vFile.equals(originalFile) || org.rust.lang.RsConstants.MOD_RS_FILE.equals(rawFileName)) continue;
            String fileName = modDeclFileName(rawFileName, modDeclName);
            RsFile rf = RsFileUtil.getRustFile(psiMgr.findFile(vFile));
            if (rf != null && Processors.processEntry(processor, fileName, Namespace.TYPES, rf)) return true;
        }

        for (com.intellij.openapi.vfs.VirtualFile vDir : dirs) {
            com.intellij.openapi.vfs.VirtualFile modFile = vDir.findChild(org.rust.lang.RsConstants.MOD_RS_FILE);
            if (modFile != null) {
                RsFile rf = RsFileUtil.getRustFile(psiMgr.findFile(modFile));
                if (rf != null && Processors.processEntry(processor, vDir.getName(), Namespace.TYPES, rf)) return true;
            }
            // `mod foo;` in `mod.rs` doesn't search submodule files
            if (inModRs) continue;
            // Submodule in crate root dir already handled above
            if (containingMod.isCrateRoot()) continue;

            if (vDir.getName().equals(containingMod.getModName())) {
                for (com.intellij.openapi.vfs.VirtualFile vFile : vDir.getChildren()) {
                    if (vFile.isDirectory()) continue;
                    String rawFileName = vFile.getName();
                    if (org.rust.lang.RsConstants.MOD_RS_FILE.equals(rawFileName)) continue;
                    String fileName = modDeclFileName(rawFileName, modDeclName);
                    RsFile rf = RsFileUtil.getRustFile(psiMgr.findFile(vFile));
                    if (rf != null && Processors.processEntry(processor, fileName, Namespace.TYPES, rf)) return true;
                }
            }
        }
        return false;
    }

    private static String modDeclFileName(@NotNull String rawName, @NotNull String modDeclName) {
        String fileName = com.intellij.openapi.util.io.FileUtil.getNameWithoutExtension(rawName);
        return modDeclName.equalsIgnoreCase(fileName) ? modDeclName : fileName;
    }

    /** Convenience overload that includes the current crate as {@code "crate"}. */
    public static boolean processExternCrateResolveVariants(
        @NotNull RsElement element,
        boolean isCompletion,
        @NotNull RsResolveProcessor processor
    ) {
        return processExternCrateResolveVariants(element, isCompletion, true, processor);
    }

    /**
     * Enumerate extern-crate dependencies of the current crate (optionally with the current
     * crate as {@code "crate"}) and feed each one to the processor.
     */
    public static boolean processExternCrateResolveVariants(
        @NotNull RsElement element,
        boolean isCompletion,
        boolean withSelf,
        @NotNull RsResolveProcessor processor
    ) {
        Crate containing = RsElementUtil.getContainingCrate(element);
        if (containing == null) return false;
        if (withSelf) {
            RsFile selfRoot = containing.getRootMod();
            if (selfRoot != null && "crate".length() > 0) {
                if (Processors.processEntry(processor, "crate", Namespace.TYPES, selfRoot)) return true;
            }
        }
        for (Crate dep : containing.getFlatDependencies()) {
            String name = dep.getNormName();
            if (name == null) continue;
            RsFile root = dep.getRootMod();
            if (root == null) continue;
            if (Processors.processEntry(processor, name, Namespace.TYPES, root)) return true;
        }
        return false;
    }

    /**
     * Resolve a path expression end-to-end: classify the path, walk the correct scopes
     * (unqualified → lexical scopes + self/super/crate; qualified → the qualifier's module;
     * crate-relative → the crate root; extern-crate → the extern prelude).
     */
    public static boolean processPathResolveVariants(
        @Nullable ImplLookup lookup,
        @NotNull RsPath path,
        boolean isCompletion,
        boolean processAssocItems,
        @NotNull RsResolveProcessor processor
    ) {
        PathResolutionContext ctx = new PathResolutionContext(path, isCompletion, processAssocItems, lookup);
        RsPathResolveKind pathKind = ctx.classifyPath(path);
        return processPathResolveVariants(ctx, pathKind, processor);
    }

    public static boolean processPathResolveVariants(
        @NotNull PathResolutionContext ctx,
        @NotNull RsPathResolveKind pathKind,
        @NotNull RsResolveProcessor processor
    ) {
        if (pathKind instanceof RsPathResolveKind.UnqualifiedPath) {
            Set<Namespace> ns = ((RsPathResolveKind.UnqualifiedPath) pathKind).getNs();
            if (processSelfSuperCrate(ns, ctx, processor)) return true;
            return processNestedScopesUpwards(ctx.getContext(), ns, ctx, processor);
        }
        if (pathKind instanceof RsPathResolveKind.QualifiedPath) {
            RsPathResolveKind.QualifiedPath q = (RsPathResolveKind.QualifiedPath) pathKind;
            return processQualifiedPathResolveVariants(ctx, q.getNs(), q.getQualifier(), q.getPath(), processor);
        }
        if (pathKind instanceof RsPathResolveKind.ExplicitTypeQualifiedPath) {
            RsPathResolveKind.ExplicitTypeQualifiedPath e = (RsPathResolveKind.ExplicitTypeQualifiedPath) pathKind;
            return processExplicitTypeQualifiedPathResolveVariants(ctx, e.getNs(), e.getTypeQual(), processor);
        }
        if (pathKind instanceof RsPathResolveKind.MacroDollarCrateIdentifier) {
            return processMacroDollarCrateResolveVariants(
                ((RsPathResolveKind.MacroDollarCrateIdentifier) pathKind).getPath(), processor);
        }
        if (pathKind instanceof RsPathResolveKind.CrateRelativePath) {
            RsPathResolveKind.CrateRelativePath cr = (RsPathResolveKind.CrateRelativePath) pathKind;
            if (!cr.getHasColonColon() && processSelfSuperCrate(cr.getNs(), ctx, processor)) return true;
            RsMod crateRoot = ctx.getCrateRoot();
            if (crateRoot == null) return false;
            return processModScope(crateRoot, cr.getNs(), new java.util.HashSet<>(), processor);
        }
        if (pathKind instanceof RsPathResolveKind.ExternCratePath) {
            return processExternPreludeResolveVariants(ctx, processor);
        }
        if (pathKind instanceof RsPathResolveKind.AssocTypeBindingPath) {
            return processAssocTypeVariants(((RsPathResolveKind.AssocTypeBindingPath) pathKind).getParentBinding(), processor);
        }
        return false;
    }

    /** {@code self}, {@code super} and {@code crate} keywords available at any unqualified path. */
    private static boolean processSelfSuperCrate(
        @NotNull Set<Namespace> ns,
        @NotNull PathResolutionContext ctx,
        @NotNull RsResolveProcessor processor
    ) {
        if (!ns.contains(Namespace.Types)) return false;
        RsMod containingMod = ctx.getContext().getContainingMod();
        if (containingMod == null) return false;
        if (Processors.processEntry(processor, "self", Namespace.TYPES, containingMod)) return true;
        RsMod superMod = containingMod.getSuper();
        if (superMod != null && Processors.processEntry(processor, "super", Namespace.TYPES, superMod)) return true;
        RsMod crateRoot = ctx.getCrateRoot();
        if (crateRoot != null && Processors.processEntry(processor, "crate", Namespace.TYPES, crateRoot)) return true;
        return false;
    }

    /** Resolve a qualified {@code A::B} path by resolving the qualifier then looking up the tail name in it. */
    private static boolean processQualifiedPathResolveVariants(
        @NotNull PathResolutionContext ctx,
        @NotNull Set<Namespace> ns,
        @NotNull RsPath qualifier,
        @NotNull RsPath path,
        @NotNull RsResolveProcessor processor
    ) {
        if (qualifier.getReference() == null) return false;
        PsiElement resolved = org.rust.lang.core.resolve.ref.RsPathReferenceImpl.deepResolve(
            (org.rust.lang.core.resolve.ref.RsPathReference) qualifier.getReference());
        if (resolved instanceof RsMod) {
            return processModScope((RsMod) resolved, ns, new java.util.HashSet<>(), processor);
        }
        if (resolved instanceof RsEnumItem) {
            RsEnumBody body = ((RsEnumItem) resolved).getEnumBody();
            if (body == null) return false;
            for (RsEnumVariant variant : body.getEnumVariantList()) {
                String name = variant.getName();
                if (name == null) continue;
                if (Processors.processEntry(processor, name, Namespace.ENUM_VARIANT_NS, variant)) return true;
            }
            return false;
        }
        if (resolved instanceof RsTraitItem) {
            return processAssocTypeVariants((RsTraitItem) resolved, processor);
        }
        return false;
    }

    private static boolean processExplicitTypeQualifiedPathResolveVariants(
        @NotNull PathResolutionContext ctx,
        @NotNull Set<Namespace> ns,
        @NotNull RsTypeQual typeQual,
        @NotNull RsResolveProcessor processor
    ) {
        // The qualifier form `<T as Trait>::X` resolves X inside Trait.
        RsTraitRef traitRef = typeQual.getTraitRef();
        if (traitRef == null) return false;
        if (traitRef.getPath().getReference() == null) return false;
        PsiElement resolved = traitRef.getPath().getReference().resolve();
        if (!(resolved instanceof RsTraitItem)) return false;
        return processAssocTypeVariants((RsTraitItem) resolved, processor);
    }

    private static boolean processMacroDollarCrateResolveVariants(
        @NotNull RsPath path,
        @NotNull RsResolveProcessor processor
    ) {
        Crate c = resolveDollarCrateIdentifier(path);
        if (c == null) return false;
        RsFile root = c.getRootMod();
        if (root == null) return false;
        return Processors.processEntry(processor, DeclMacroConstantsUtil.MACRO_DOLLAR_CRATE_IDENTIFIER, Namespace.TYPES, root);
    }

    /**
     * Name resolution for a pattern binding. In a struct-pattern shorthand (e.g. {@code Point { x, y }})
     * we enumerate the struct's fields; otherwise we walk the lexical scope for constants
     * (and, on completion, for paths/destructurable items).
     */
    public static boolean processPatBindingResolveVariants(
        @NotNull RsPatBinding binding,
        boolean isCompletion,
        @NotNull RsResolveProcessor originalProcessor
    ) {
        if (binding.getParent() instanceof RsPatField) {
            PsiElement pp = binding.getParent().getParent();
            if (pp instanceof RsPatStruct) {
                RsPath path = ((RsPatStruct) pp).getPath();
                PsiElement resolved = path != null && path.getReference() != null
                    ? org.rust.lang.core.resolve.ref.RsPathReferenceImpl.deepResolve(
                        (org.rust.lang.core.resolve.ref.RsPathReference) path.getReference())
                    : null;
                if (resolved instanceof RsFieldsOwner) {
                    if (processFieldDeclarations((RsFieldsOwner) resolved, originalProcessor)) return true;
                    if (isCompletion) return false;
                }
            }
        }

        final boolean completion = isCompletion;
        @SuppressWarnings("unchecked")
        RsResolveProcessor filtered = (RsResolveProcessor) Processors.wrapWithFilter(originalProcessor, entry -> {
            if (!originalProcessor.acceptsName(entry.getName())) return false;
            RsElement element = entry.getElement();
            boolean isConstant = RsElementUtil.isConstantLike(element);
            boolean isPathOrDestructurable =
                element instanceof RsMod
                    || element instanceof RsEnumItem
                    || element instanceof RsEnumVariant
                    || element instanceof RsStructItem;
            return isConstant || (completion && isPathOrDestructurable);
        });
        return processNestedScopesUpwards(
            binding,
            isCompletion ? Namespace.TYPES_N_VALUES : Namespace.VALUES,
            filtered);
    }

    /**
     * Walk outward looking for enclosing {@link RsLabeledExpression}s and feed each declared
     * label to the processor.
     */
    public static boolean processLabelResolveVariants(
        @NotNull RsLabel label,
        @NotNull RsResolveProcessor processor
    ) {
        return processLabelResolveVariants(label, processor, false);
    }

    public static boolean processLabelResolveVariants(
        @NotNull RsLabel label,
        @NotNull RsResolveProcessor processor,
        boolean processBeyondLabelBarriers
    ) {
        PsiElement ctx = label.getContext();
        while (ctx != null) {
            if (ctx instanceof RsLabeledExpression) {
                RsLabelDecl decl = ((RsLabeledExpression) ctx).getLabelDecl();
                if (decl != null) {
                    String name = decl.getName();
                    if (name != null
                        && Processors.processEntry(processor, name, Namespace.LIFETIMES, decl)) {
                        return true;
                    }
                }
            }
            if (!processBeyondLabelBarriers
                && (ctx instanceof RsFunction || ctx instanceof RsLambdaExpr || ctx instanceof RsConstant)) {
                break;
            }
            ctx = ctx.getContext();
        }
        return false;
    }

    @NotNull
    public static List<RsElement> resolveLabelReference(@NotNull RsLabel element) {
        return resolveLabelReference(element, false);
    }

    @NotNull
    public static List<RsElement> resolveLabelReference(@NotNull RsLabel element, boolean processBeyondLabelBarriers) {
        String name = element.getReferenceName();
        if (name == null) return Collections.emptyList();
        return Processors.collectResolveVariants(name, p -> processLabelResolveVariants(element, p, processBeyondLabelBarriers));
    }

    /**
     * Walk outward looking for enclosing {@link RsGenericDeclaration}s and feed each lifetime
     * parameter to the processor.
     */
    public static boolean processLifetimeResolveVariants(
        @NotNull RsLifetime lifetime,
        @NotNull RsResolveProcessor processor
    ) {
        PsiElement ctx = lifetime.getContext();
        while (ctx != null) {
            if (ctx instanceof RsGenericDeclaration) {
                for (RsLifetimeParameter p : RsGenericDeclarationUtil.getLifetimeParameters((RsGenericDeclaration) ctx)) {
                    String name = p.getName();
                    if (name == null) continue;
                    if (Processors.processEntry(processor, name, Namespace.LIFETIMES, p)) return true;
                }
            }
            ctx = ctx.getContext();
        }
        return false;
    }

    /**
     * Walk the expression's ancestors collecting {@link RsPatBinding}s in scope.
     */
    public static void processLocalVariables(
        @NotNull RsElement place,
        @NotNull Consumer<RsPatBinding> processor
    ) {
        PsiElement scope = place;
        while (scope != null) {
            if (scope instanceof RsBlock) {
                for (PsiElement child = scope.getFirstChild(); child != null; child = child.getNextSibling()) {
                    if (child instanceof RsLetDecl) {
                        RsPat pat = ((RsLetDecl) child).getPat();
                        if (pat != null) collectPatBindings(pat, processor);
                    }
                }
            }
            if (scope instanceof RsFunction) {
                RsValueParameterList plist = ((RsFunction) scope).getValueParameterList();
                if (plist != null) {
                    for (RsValueParameter p : plist.getValueParameterList()) {
                        RsPat pat = p.getPat();
                        if (pat != null) collectPatBindings(pat, processor);
                    }
                }
                break;
            }
            if (scope instanceof RsLambdaExpr) {
                RsValueParameterList plist = ((RsLambdaExpr) scope).getValueParameterList();
                if (plist != null) {
                    for (RsValueParameter p : plist.getValueParameterList()) {
                        RsPat pat = p.getPat();
                        if (pat != null) collectPatBindings(pat, processor);
                    }
                }
            }
            PsiElement next = scope.getContext();
            scope = next;
        }
    }

    private static void collectPatBindings(@NotNull PsiElement pat, @NotNull Consumer<RsPatBinding> sink) {
        if (pat instanceof RsPatBinding) {
            sink.accept((RsPatBinding) pat);
            return;
        }
        for (PsiElement child = pat.getFirstChild(); child != null; child = child.getNextSibling()) {
            collectPatBindings(child, sink);
        }
    }

    /**
     * {@code processMacroReferenceVariants} — walks a {@code macro_rules!} macro-case pattern
     * and feeds the {@link RsMacroBinding}s it contains to the processor.
     */
    public static boolean processMacroReferenceVariants(
        @NotNull RsMacroReference ref,
        @NotNull RsResolveProcessor processor
    ) {
        RsMacroCase definition = PsiElementUtil.ancestorStrict(ref, RsMacroCase.class);
        if (definition == null) return false;
        for (RsMacroBinding binding : collectDescendantsOfType(definition.getMacroPattern(), RsMacroBinding.class)) {
            String name = binding.getName();
            if (name == null) continue;
            if (Processors.processEntry(processor, name, Namespace.TYPES, binding)) return true;
        }
        return false;
    }

    private static <T extends PsiElement> List<T> collectDescendantsOfType(@Nullable PsiElement root, @NotNull Class<T> clazz) {
        List<T> result = new ArrayList<>();
        if (root == null) return result;
        for (PsiElement child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (clazz.isInstance(child)) result.add(clazz.cast(child));
            result.addAll(collectDescendantsOfType(child, clazz));
        }
        return result;
    }

    /**
     * Resolve a proc-macro path: scope-walk for unqualified names, qualified path resolve
     * otherwise. Declarative (bang) macro definitions are filtered out.
     */
    @SuppressWarnings("unchecked")
    public static boolean processProcMacroResolveVariants(
        @NotNull RsPath path,
        @NotNull RsResolveProcessor originalProcessor,
        boolean isCompletion
    ) {
        RsResolveProcessor filtered = (RsResolveProcessor) Processors.wrapWithFilter(originalProcessor,
            e -> !(e.getElement() instanceof org.rust.lang.core.psi.ext.RsMacroDefinitionBase));
        if (path.getPath() == null) {
            return processMacroCallVariantsInScope(path, true, filtered);
        }
        return processMacroCallPathResolveVariants(path, isCompletion, filtered);
    }

    /**
     * Resolve a trait name inside {@code #[derive(...)]}. Tries the known-derivable-trait
     * shortcut first; otherwise falls back to a by-name index lookup.
     */
    public static boolean processDeriveTraitResolveVariants(
        @NotNull RsPath element,
        @NotNull String traitName,
        @NotNull RsResolveProcessor processor
    ) {
        if (processNestedScopesUpwards(element, Namespace.MACROS, processor)) return true;
        org.rust.lang.core.resolve.KnownDerivableTrait known =
            org.rust.lang.core.resolve.KnownItems.getKNOWN_DERIVABLE_TRAITS().get(traitName);
        if (known != null) {
            RsTraitItem hardcoded = known.findTrait(org.rust.lang.core.resolve.KnownItems.getKnownItems(element));
            if (hardcoded != null) {
                return Processors.processEntry(processor, traitName, Namespace.TYPES, hardcoded);
            }
        }
        java.util.Collection<RsNamedElement> found =
            org.rust.lang.core.stubs.index.RsNamedElementIndex.findElementsByName(element.getProject(), traitName);
        return Processors.processAll(processor,
            new java.util.ArrayList<>(filterTraits(found)), Namespace.TYPES);
    }

    @NotNull
    private static java.util.List<RsNamedElement> filterTraits(@NotNull java.util.Collection<RsNamedElement> elements) {
        java.util.List<RsNamedElement> result = new java.util.ArrayList<>();
        for (RsNamedElement el : elements) {
            if (el instanceof RsTraitItem) result.add(el);
        }
        return result;
    }

    /**
     * Resolve an overloaded binary-op (+, -, *, …) to the {@code std::ops::X} trait method
     * that implements it for the operand types.
     */
    public static boolean processBinaryOpVariants(
        @NotNull RsBinaryOp element,
        @NotNull OverloadableBinaryOperator operator,
        @NotNull RsResolveProcessor processor
    ) {
        RsBinaryExpr binaryExpr = PsiElementUtil.ancestorStrict(element, RsBinaryExpr.class);
        if (binaryExpr == null) return false;
        RsExpr right = binaryExpr.getRight();
        if (right == null) return false;
        Ty rhsType = org.rust.lang.core.types.ExtensionsUtil.getType(right);
        Ty lhsType = org.rust.lang.core.types.ExtensionsUtil.getType(binaryExpr.getLeft());
        ImplLookup lookup = ImplLookup.relativeTo(element);
        org.rust.lang.core.psi.ext.RsTraitOrImpl impl = lookup.findOverloadedOpImpl(lhsType, rhsType, operator);
        if (impl == null) return false;
        for (org.rust.lang.core.psi.ext.RsAbstractable m : RsTraitOrImplUtil.getExpandedMembers(impl)) {
            if (m instanceof RsFunction && operator.getFnName().equals(((RsFunction) m).getName())) {
                return Processors.processEntry(processor, operator.getFnName(), Namespace.VALUES, m);
            }
        }
        return false;
    }

    /**
     * Emit associated-type names from the enclosing trait of an assoc-type binding to the
     * processor.
     */
    public static boolean processAssocTypeVariants(
        @NotNull RsAssocTypeBinding element,
        @NotNull RsResolveProcessor processor
    ) {
        RsPath path = PsiElementUtil.ancestorStrict(element, RsPath.class);
        if (path == null) return false;
        if (path.getReference() == null) return false;
        PsiElement resolved = path.getReference().resolve();
        if (!(resolved instanceof RsTraitItem)) return false;
        return processAssocTypeVariants((RsTraitItem) resolved, processor);
    }

    public static boolean processAssocTypeVariants(
        @NotNull RsTraitItem trait,
        @NotNull RsResolveProcessor processor
    ) {
        for (RsAbstractable m : RsTraitOrImplUtil.getExpandedMembers(trait)) {
            if (!(m instanceof RsTypeAlias)) continue;
            String name = ((RsTypeAlias) m).getName();
            if (name == null) continue;
            if (Processors.processEntry(processor, name, Namespace.TYPES, m)) return true;
        }
        return false;
    }

    /**
     * Resolve a macro-call path. {@code foo!()} walks enclosing scopes for a {@link RsMacro} or
     * proc-macro function; {@code krate::foo!()} resolves {@code krate} to a dependency and
     * enumerates its exported macros.
     */
    public static boolean processMacroCallPathResolveVariants(
        @NotNull RsPath path,
        boolean isCompletion,
        @NotNull RsResolveProcessor processor
    ) {
        RsPath qualifier = path.getPath();
        if (qualifier == null) {
            return processMacroCallVariantsInScope(path, false, processor);
        }
        if (qualifier.getPath() != null) return false;
        RsFile crateRoot = findDependencyCrateByNamePath(path, qualifier);
        if (crateRoot == null) return false;
        return processExportedMacros(crateRoot, processor);
    }

    /**
     * Walk outward from {@code path} collecting {@link RsMacro} / {@link RsMacro2} items and
     * proc-macro function definitions.
     */
    public static boolean processMacroCallVariantsInScope(
        @NotNull RsPath path,
        boolean ignoreLegacyMacros,
        @NotNull RsResolveProcessor processor
    ) {
        java.util.Set<String> seen = new java.util.HashSet<>();
        PsiElement scope = path.getContext();
        while (scope != null) {
            if (scope instanceof RsItemsOwner) {
                for (RsItemElement item : RsItemsOwnerUtil.getExpandedItemsExceptImplsAndUses((RsItemsOwner) scope)) {
                    if (item instanceof RsMacro || item instanceof RsMacro2) {
                        String name = ((RsNamedElement) item).getName();
                        if (name == null || !seen.add(name)) continue;
                        if (Processors.processEntry(processor, name, Namespace.MACROS, item)) return true;
                    } else if (item instanceof RsFunction
                        && RsFunctionUtil.isProcMacroDef((RsFunction) item)) {
                        String name = ((RsFunction) item).getName();
                        if (name == null || !seen.add(name)) continue;
                        if (Processors.processEntry(processor, name, Namespace.MACROS, item)) return true;
                    }
                }
            }
            scope = scope.getContext();
        }
        return false;
    }

    private static boolean processExportedMacros(
        @NotNull RsFile crateRoot,
        @NotNull RsResolveProcessor processor
    ) {
        for (RsItemElement item : RsItemsOwnerUtil.getExpandedItemsExceptImplsAndUses(crateRoot)) {
            if (item instanceof RsMacro
                && org.rust.lang.core.psi.ext.RsMacroUtil.getHasMacroExport((RsMacro) item)) {
                String name = ((RsNamedElement) item).getName();
                if (name == null) continue;
                if (Processors.processEntry(processor, name, Namespace.MACROS, item)) return true;
            }
        }
        return false;
    }

    /** Walk from the given element outward, feeding every visible name in the requested namespaces to the processor. */
    public static boolean processNestedScopesUpwards(
        @NotNull RsElement scopeStart,
        @NotNull Set<Namespace> ns,
        @NotNull RsResolveProcessor processor
    ) {
        return processNestedScopesUpwards(scopeStart, ns, null, processor);
    }

    /**
     * Walk from {@code scopeStart} outward. At each scope, feed the names visible in that
     * scope (lexical bindings for blocks/functions, item declarations for modules, generic
     * parameters for generic declarations) that belong to one of the requested namespaces to
     * the processor. Stops when the processor signals to stop or when the whole outer chain
     * has been exhausted.
     */
    public static boolean processNestedScopesUpwards(
        @NotNull RsElement scopeStart,
        @NotNull Set<Namespace> ns,
        @Nullable PathResolutionContext ctx,
        @NotNull RsResolveProcessor processor
    ) {
        java.util.Set<String> seen = new java.util.HashSet<>();
        PsiElement cameFrom = scopeStart;
        PsiElement scope = scopeStart.getContext();
        while (scope != null) {
            if (scope instanceof RsMod) {
                if (processModScope((RsMod) scope, ns, seen, processor)) return true;
                // Also walk the `super` chain — RsMod's getContext stops at the file boundary.
                if (scope instanceof RsFile) {
                    RsMod superMod = ((RsFile) scope).getSuper();
                    if (superMod != null && processNestedScopesUpwards(superMod, ns, ctx, processor)) return true;
                }
            } else if (scope instanceof RsBlock) {
                if (processBlockScope((RsBlock) scope, cameFrom, ns, seen, processor)) return true;
            } else if (scope instanceof RsFunction) {
                if (processFunctionScope((RsFunction) scope, ns, seen, processor)) return true;
            } else if (scope instanceof RsLambdaExpr) {
                if (processLambdaScope((RsLambdaExpr) scope, ns, seen, processor)) return true;
            } else if (scope instanceof RsGenericDeclaration) {
                if (processGenericParamsScope((RsGenericDeclaration) scope, ns, seen, processor)) return true;
            } else if (scope instanceof RsForExpr) {
                RsPat pat = ((RsForExpr) scope).getPat();
                if (pat != null && processPatternBindings(pat, ns, seen, processor)) return true;
            } else if (scope instanceof RsMatchArm) {
                RsPat pat = ((RsMatchArm) scope).getPat();
                if (pat != null && processPatternBindings(pat, ns, seen, processor)) return true;
            } else if (scope instanceof RsLetDecl) {
                // Let pattern bindings are in scope for the initializer only when `let-chain`.
                RsPat pat = ((RsLetDecl) scope).getPat();
                if (pat != null && processPatternBindings(pat, ns, seen, processor)) return true;
            }
            cameFrom = scope;
            scope = scope.getContext();
        }
        return false;
    }

    private static boolean processModScope(
        @NotNull RsMod mod,
        @NotNull Set<Namespace> ns,
        @NotNull java.util.Set<String> seen,
        @NotNull RsResolveProcessor processor
    ) {
        for (RsItemElement item : RsItemsOwnerUtil.getExpandedItemsExceptImplsAndUses(mod)) {
            if (!(item instanceof RsNamedElement)) continue;
            String name = ((RsNamedElement) item).getName();
            if (name == null || !seen.add(name)) continue;
            if (!anyNsMatches(item, ns)) continue;
            if (Processors.processEntry(processor, name, elementNamespaces(item), item)) return true;
        }
        return false;
    }

    private static boolean processBlockScope(
        @NotNull RsBlock block,
        @NotNull PsiElement cameFrom,
        @NotNull Set<Namespace> ns,
        @NotNull java.util.Set<String> seen,
        @NotNull RsResolveProcessor processor
    ) {
        // Items declared anywhere in the block are visible from everywhere; let-bindings only
        // from later statements.
        for (PsiElement child = block.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof RsItemElement && child instanceof RsNamedElement) {
                String name = ((RsNamedElement) child).getName();
                if (name == null || !seen.add(name)) continue;
                if (!anyNsMatches((RsItemElement) child, ns)) continue;
                if (Processors.processEntry(processor, name, elementNamespaces((RsItemElement) child), (RsItemElement) child)) return true;
            }
        }
        if (ns.contains(Namespace.Values)) {
            // Let bindings: only those declared textually before cameFrom.
            for (PsiElement child = block.getFirstChild(); child != null && child != cameFrom; child = child.getNextSibling()) {
                if (child instanceof RsLetDecl) {
                    RsPat pat = ((RsLetDecl) child).getPat();
                    if (pat != null && processPatternBindings(pat, ns, seen, processor)) return true;
                }
            }
        }
        return false;
    }

    private static boolean processFunctionScope(
        @NotNull RsFunction fn,
        @NotNull Set<Namespace> ns,
        @NotNull java.util.Set<String> seen,
        @NotNull RsResolveProcessor processor
    ) {
        if (ns.contains(Namespace.Values)) {
            RsValueParameterList plist = fn.getValueParameterList();
            if (plist != null) {
                for (RsValueParameter p : plist.getValueParameterList()) {
                    RsPat pat = p.getPat();
                    if (pat != null && processPatternBindings(pat, ns, seen, processor)) return true;
                }
            }
            RsSelfParameter self = fn.getSelfParameter();
            if (self != null) {
                String name = "self";
                if (seen.add(name)
                    && Processors.processEntry(processor, name, Namespace.VALUES, self)) return true;
            }
        }
        return processGenericParamsScope(fn, ns, seen, processor);
    }

    private static boolean processLambdaScope(
        @NotNull RsLambdaExpr lambda,
        @NotNull Set<Namespace> ns,
        @NotNull java.util.Set<String> seen,
        @NotNull RsResolveProcessor processor
    ) {
        if (!ns.contains(Namespace.Values)) return false;
        RsValueParameterList plist = lambda.getValueParameterList();
        if (plist == null) return false;
        for (RsValueParameter p : plist.getValueParameterList()) {
            RsPat pat = p.getPat();
            if (pat != null && processPatternBindings(pat, ns, seen, processor)) return true;
        }
        return false;
    }

    private static boolean processGenericParamsScope(
        @NotNull RsGenericDeclaration decl,
        @NotNull Set<Namespace> ns,
        @NotNull java.util.Set<String> seen,
        @NotNull RsResolveProcessor processor
    ) {
        if (ns.contains(Namespace.Types)) {
            for (RsTypeParameter tp : RsGenericDeclarationUtil.getTypeParameters(decl)) {
                String name = tp.getName();
                if (name == null || !seen.add(name)) continue;
                if (Processors.processEntry(processor, name, Namespace.TYPES, tp)) return true;
            }
        }
        if (ns.contains(Namespace.Values)) {
            for (RsConstParameter cp : RsGenericDeclarationUtil.getConstParameters(decl)) {
                String name = cp.getName();
                if (name == null || !seen.add(name)) continue;
                if (Processors.processEntry(processor, name, Namespace.VALUES, cp)) return true;
            }
        }
        if (ns.contains(Namespace.Lifetimes)) {
            for (RsLifetimeParameter lp : RsGenericDeclarationUtil.getLifetimeParameters(decl)) {
                String name = lp.getName();
                if (name == null || !seen.add(name)) continue;
                if (Processors.processEntry(processor, name, Namespace.LIFETIMES, lp)) return true;
            }
        }
        return false;
    }

    private static boolean processPatternBindings(
        @NotNull PsiElement pat,
        @NotNull Set<Namespace> ns,
        @NotNull java.util.Set<String> seen,
        @NotNull RsResolveProcessor processor
    ) {
        if (!ns.contains(Namespace.Values)) return false;
        final boolean[] stop = {false};
        collectPatBindings(pat, binding -> {
            if (stop[0]) return;
            String name = binding.getName();
            if (name == null || !seen.add(name)) return;
            if (Processors.processEntry(processor, name, Namespace.VALUES, binding)) stop[0] = true;
        });
        return stop[0];
    }

    private static boolean anyNsMatches(@NotNull RsItemElement item, @NotNull Set<Namespace> ns) {
        for (Namespace candidate : elementNamespaces(item)) {
            if (ns.contains(candidate)) return true;
        }
        return false;
    }

    @NotNull
    private static Set<Namespace> elementNamespaces(@NotNull RsItemElement item) {
        if (item instanceof RsNamedElement) return Namespace.getNamespaces((RsNamedElement) item);
        return Namespace.TYPES_N_VALUES;
    }

    /**
     * Returns the prelude module for the enclosing mod. Uses the resolve2
     * {@link org.rust.lang.core.resolve2.FacadeResolve#getModInfo} + {@code defMap.prelude}
     * where available; returns {@code null} otherwise.
     */
    @Nullable
    public static RsMod findPrelude(@NotNull RsElement element) {
        RsMod containing = element.getContainingMod();
        if (containing == null) return null;
        org.rust.lang.core.resolve2.RsModInfo info = org.rust.lang.core.resolve2.FacadeResolve.getModInfo(containing);
        if (info == null) return null;
        org.rust.lang.core.resolve2.ModData prelude = info.getDefMap().getPrelude();
        if (prelude == null) return null;
        List<RsMod> resolved = prelude.toRsMod(info.getProject());
        return resolved.size() == 1 ? resolved.get(0) : null;
    }

    /**
     * Walks from {@code context} outward and collects every {@link RsUseSpeck} whose path
     * currently fails to resolve, feeding each one to the consumer — used by the auto-import
     * feature to know which imports to offer.
     */
    public static void processUnresolvedImports(
        @NotNull RsElement context,
        @NotNull Consumer<RsUseSpeck> processor
    ) {
        PsiElement scope = context;
        while (scope != null) {
            if (scope instanceof RsItemsOwner) {
                for (RsUseItem importItem : RsItemsOwnerUtil.getExpandedItemsCached((RsItemsOwner) scope).getImports()) {
                    if (!RsDocAndAttributeOwnerUtil.existsAfterExpansionSelf(importItem)) continue;
                    RsUseSpeck root = importItem.getUseSpeck();
                    if (root == null) continue;
                    RsUseSpeckUtil.forEachLeafSpeck(root, speck -> {
                        if (RsUseSpeckUtil.isStarImport(speck)) return;
                        RsPath p = speck.getPath();
                        if (p == null || p.getReference() == null) return;
                        if (p.getReference().multiResolve().isEmpty()) {
                            processor.accept(speck);
                        }
                    });
                }
            }
            scope = scope.getContext();
        }
    }

    /**
     * Iterate the extern-prelude (i.e. the extern-crate names visible in this module, including
     * any {@code extern crate X as Y} renames) and feed each one to the processor.
     */
    public static boolean processExternPreludeResolveVariants(
        @NotNull PathResolutionContext ctx,
        @NotNull RsResolveProcessor processor
    ) {
        org.rust.lang.core.resolve2.RsModInfo info = ctx.getContainingModInfo();
        if (info == null) return false;
        Set<String> wantedNames = processor.getNames();
        for (Map.Entry<String, org.rust.lang.core.resolve2.CrateDefMap> entry :
            info.getDefMap().getExternPrelude().entrySet()) {
            if (wantedNames != null && !wantedNames.contains(entry.getKey())) continue;
            RsMod externCrateRoot = entry.getValue().rootAsRsMod(info.getProject());
            if (externCrateRoot == null) continue;
            if (Processors.processEntry(processor, entry.getKey(), Namespace.TYPES, externCrateRoot)) return true;
        }
        return false;
    }

    /** Resolve a path to its candidates via {@link Processors#collectPathResolveVariants}. */
    @NotNull
    public static List<RsPathResolveResult<RsElement>> resolvePath(@NotNull PathResolutionContext ctx, @NotNull RsPath path, @NotNull Object kind) {
        RsPathResolveKind pathKind = kind instanceof RsPathResolveKind
            ? (RsPathResolveKind) kind
            : ctx.classifyPath(path);
        return Processors.collectPathResolveVariants(ctx, path, processor ->
            processPathResolveVariants(ctx, pathKind, processor));
    }

    /**
     * Simple by-name lookup: uses {@link #processNestedScopesUpwards} to enumerate in-scope
     * names and returns the first match with one of the requested namespaces. Returns
     * {@code null} if the scope-walking subsystem is deferred (so callers get consistent
     * "not found" semantics instead of crashing).
     */
    @Nullable
    public static RsNamedElement findInScope(@NotNull RsElement scope, @NotNull String name, @NotNull Set<Namespace> ns) {
        RsElement[] found = new RsElement[1];
        processNestedScopesUpwards(scope, ns, new RsResolveProcessor() {
            @Override
            public boolean process(@NotNull ScopeEntry entry) {
                if (name.equals(entry.getName())) {
                    RsElement element = entry.getElement();
                    if (element instanceof RsNamedElement) {
                        found[0] = element;
                        return true;
                    }
                }
                return false;
            }

            @Override
            public Set<String> getNames() {
                return Collections.singleton(name);
            }
        });
        return (RsNamedElement) found[0];
    }

    // -------------------------------------------------------------------------
    // Delegating "collect*" methods live in {@link Processors}.
    // -------------------------------------------------------------------------

    @NotNull
    public static List<RsElement> collectResolveVariants(@Nullable String referenceName, @NotNull Consumer<RsResolveProcessor> f) {
        return Processors.collectResolveVariants(referenceName, f);
    }

    @Nullable
    public static RsElement pickFirstResolveVariant(@Nullable String referenceName, @NotNull Consumer<RsResolveProcessor> f) {
        return Processors.pickFirstResolveVariant(referenceName, f);
    }

    @NotNull
    public static <T extends ScopeEntry> List<T> collectResolveVariantsAsScopeEntries(
        @Nullable String referenceName,
        @NotNull Consumer<RsResolveProcessorBase<T>> f
    ) {
        return Processors.collectResolveVariantsAsScopeEntries(referenceName, f);
    }

    @NotNull
    public static List<RsPathResolveResult<RsElement>> collectPathResolveVariants(
        @NotNull PathResolutionContext ctx,
        @NotNull RsPath path,
        @NotNull Consumer<RsResolveProcessor> f
    ) {
        return Processors.collectPathResolveVariants(ctx, path, f);
    }

    @NotNull
    public static Map<RsPath, List<RsPathResolveResult<RsElement>>> collectMultiplePathResolveVariants(
        @NotNull PathResolutionContext ctx,
        @NotNull List<RsPath> paths,
        @NotNull Consumer<RsResolveProcessor> f
    ) {
        return Processors.collectMultiplePathResolveVariants(ctx, paths, f);
    }

    // -------------------------------------------------------------------------
    // Constants + namespace shortcuts.
    // -------------------------------------------------------------------------

    public static final int DEFAULT_RECURSION_LIMIT = 128;

    public static final Set<Namespace> TYPES_N_VALUES_N_MACROS = Namespace.TYPES_N_VALUES_N_MACROS;

    @NotNull public static Set<Namespace> getTYPES() { return Namespace.TYPES; }
    @NotNull public static Set<Namespace> getVALUES() { return Namespace.VALUES; }
    @NotNull public static Set<Namespace> getTYPES_N_VALUES_N_MACROS() { return Namespace.TYPES_N_VALUES_N_MACROS; }
    @NotNull public static Set<Namespace> getENUM_VARIANT_NS() { return Namespace.ENUM_VARIANT_NS; }
}
