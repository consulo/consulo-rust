/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.util.lang.ThreeState;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.rust.lang.RsConstants;
import org.rust.lang.core.psi.ext.RsAbstractable;
import org.rust.lang.core.psi.ext.RsMacroDefinitionBase;
import org.rust.lang.core.psi.ext.RsMacroUtil;
import org.rust.lang.core.psi.ext.RsMandatoryReferenceElement;
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement;
import org.rust.lang.core.psi.ext.RsTraitOrImpl;
import org.rust.lang.core.resolve.ref.RsPathReference;
import org.rust.lang.core.resolve.ref.RsPathReferenceImpl;
import org.rust.lang.core.resolve2.CrateDefMap;
import org.rust.lang.core.resolve2.FacadeResolve;
import org.rust.lang.core.resolve2.ModData;
import org.rust.lang.core.resolve2.RsModInfo;
import org.rust.lang.core.stubs.index.RsNamedElementIndex;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.infer.Autoderef;
import org.rust.lang.core.types.ty.TyAdt;
import org.rust.lang.core.crate.CrateGraphService;
import org.rust.lang.core.psi.RsBinaryExpr;
import org.rust.lang.core.psi.RsCondition;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsIfExpr;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.RsLetExpr;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.psi.RsTypeReference;
import org.rust.lang.core.psi.RsUseSpeck;
import org.rust.lang.core.psi.RsWhileExpr;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsGenericDeclaration;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.lang.core.psi.ext.RsTypeDeclarationElement;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.resolve.KnownDerivableTrait;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.resolve.indexes.RsMacroIndex;
import org.rust.lang.core.resolve2.ItemProcessingMode;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.SubstitutionUtil;
import org.rust.lang.core.types.infer.FoldUtil;
import org.rust.lang.core.types.infer.TypeInferenceUtil;
import org.rust.lang.core.types.ty.TyInfer;
import org.rust.lang.core.types.ty.TyTypeParameter;
import org.rust.lang.core.types.ty.TyUnknown;

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
    public static Pair<String, String> splitAbsolutePath(@Nonnull String path) {
        if (path.startsWith("::")) {
            throw new IllegalStateException("splitAbsolutePath received path starting with ::");
        }
        int idx = path.indexOf("::");
        if (idx < 0) return null;
        return new Pair<>(path.substring(0, idx), path.substring(idx + 2));
    }

    @Nullable
    public static RsFile findDependencyCrateByNamePath(@Nonnull RsElement context, @Nonnull RsPath path) {
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
    public static RsFile findDependencyCrateByName(@Nonnull RsElement context, @Nonnull String name) {
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
    public static RsFile findDependencyCrateByName(@Nonnull RsElement context, @Nonnull String name, @Nullable Object extra) {
        return findDependencyCrateByName(context, name);
    }

    @Nullable
    public static Crate resolveDollarCrateIdentifier(@Nonnull RsPath path) {
        // Falls back to the containing crate, which is correct
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
        @Nonnull String path,
        @Nonnull CargoWorkspace workspace,
        @Nonnull Project project,
        @Nonnull ThreeState isStd
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
        @Nonnull String path,
        @Nonnull CargoWorkspace workspace,
        @Nonnull Project project
    ) {
        return resolveStringPath(path, workspace, project, ThreeState.UNSURE);
    }

    /**
     * Compute the allowed namespace mask for a path based on its syntactic position
     * (imports allow all 3, trait refs require TYPES, path expressions require VALUES, etc.).
     *
     */
    @Nonnull
    public static Set<Namespace> allowedNamespaces(@Nonnull RsPath path) {
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
        @Nonnull ImplLookup lookup,
        @Nonnull Ty receiverType,
        @Nonnull RsElement context,
        @Nonnull RsResolveProcessorBase<DotExprResolveVariant> processor
    ) {
        if (processFieldExprResolveVariants(lookup, receiverType, fieldToDotWrapper(processor))) return true;
        return processMethodCallExprResolveVariants(lookup, receiverType, context, methodToDotWrapper(processor));
    }

    /**
     * Resolve a field access on {@code receiverType}: walk the auto-deref coercion sequence and
     * for each struct ADT in that chain emit its fields as {@link FieldResolveVariant}s.
     */
    public static boolean processFieldExprResolveVariants(
        @Nonnull ImplLookup lookup,
        @Nonnull Ty receiverType,
        @Nonnull RsResolveProcessorBase<FieldResolveVariant> originalProcessor
    ) {
        Autoderef autoderef = lookup.coercionSequence(receiverType);
        for (Ty ty : autoderef) {
            if (!(ty instanceof TyAdt)) continue;
            RsStructOrEnumItemElement item = ((TyAdt) ty).getItem();
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
        @Nonnull RsStructOrEnumItemElement owner,
        @Nonnull RsStructItem struct,
        @Nonnull java.util.function.Function<ScopeEntry, Boolean> sink
    ) {
        for (RsFieldDecl decl : struct.getFields()) {
            String name = decl.getName();
            if (name == null) continue;
            if (sink.apply(new SimpleScopeEntry(name, decl, Namespace.VALUES))) return true;
        }
        return false;
    }

    private static RsResolveProcessorBase<FieldResolveVariant> fieldToDotWrapper(
        @Nonnull RsResolveProcessorBase<DotExprResolveVariant> processor
    ) {
        return new RsResolveProcessorBase<FieldResolveVariant>() {
            @Override
            public boolean process(@Nonnull FieldResolveVariant entry) {
                return processor.process(entry);
            }

            @Override
            public Set<String> getNames() {
                return processor.getNames();
            }
        };
    }

    private static RsResolveProcessorBase<MethodResolveVariant> methodToDotWrapper(
        @Nonnull RsResolveProcessorBase<DotExprResolveVariant> processor
    ) {
        return new RsResolveProcessorBase<MethodResolveVariant>() {
            @Override
            public boolean process(@Nonnull MethodResolveVariant entry) {
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
        @Nonnull RsStructLiteralField field,
        boolean isCompletion,
        @Nonnull RsResolveProcessor processor
    ) {
        RsStructLiteral literal = RsStructLiteralFieldUtil.getParentStructLiteral(field);
        PsiElement resolved = literal != null && literal.getPath() != null && literal.getPath().getReference() != null
            ? RsPathReferenceImpl.deepResolve(
                (RsPathReference) literal.getPath().getReference())
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
        @Nonnull RsPatFieldFull field,
        @Nonnull RsResolveProcessor processor
    ) {
        RsPatStruct pat = RsPatFieldFullUtil.getParentStructPattern(field);
        if (pat == null || pat.getPath() == null || pat.getPath().getReference() == null) return false;
        PsiElement resolved = RsPathReferenceImpl.deepResolve(
            (RsPathReference) pat.getPath().getReference());
        if (!(resolved instanceof RsFieldsOwner)) return false;
        return processFieldDeclarations((RsFieldsOwner) resolved, processor);
    }

    private static boolean processFieldDeclarations(@Nonnull RsFieldsOwner struct, @Nonnull RsResolveProcessor processor) {
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
        @Nonnull ImplLookup lookup,
        @Nonnull Ty receiverType,
        @Nonnull RsElement context,
        @Nonnull RsResolveProcessorBase<MethodResolveVariant> processor
    ) {
        Autoderef autoderef = lookup.coercionSequence(receiverType);
        int derefIndex = 0;
        for (Ty ty : autoderef) {
            for (TraitImplSource source : lookup.findImplsAndTraits(ty)) {
                RsTraitOrImpl implSite = source.getValue();
                if (implSite == null) continue;
                for (RsAbstractable member :
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
        @Nonnull RsModDeclItem modDecl,
        @Nonnull RsResolveProcessor processor
    ) {
        PsiManager psiMgr = PsiManager.getInstance(modDecl.getProject());
        RsMod containingMod = modDecl.getContainingMod();
        PsiDirectory ownedDirectory = containingMod.getOwnedDirectory();
        PsiFile contextualFile = RsElementExtUtil.getContextualFile(modDecl);
        VirtualFile originalFile = contextualFile.getOriginalFile().getVirtualFile();
        boolean inModRs = RsConstants.MOD_RS_FILE.equals(contextualFile.getName());

        String explicitPath = RsModDeclItemUtil.getPathAttribute(modDecl);
        if (explicitPath != null) {
            PsiDirectory dir = containingMod instanceof RsFile
                ? contextualFile.getParent()
                : ownedDirectory;
            if (dir == null) return false;
            VirtualFile vFile = dir.getVirtualFile().findFileByRelativePath(
                FileUtil.toSystemIndependentName(explicitPath));
            if (vFile == null) return false;
            RsFile mod = RsFileUtil.getRustFile(psiMgr.findFile(vFile));
            if (mod == null) return false;
            String name = modDecl.getName();
            if (name == null) return false;
            return Processors.processEntry(processor, name, Namespace.TYPES, mod);
        }
        if (ownedDirectory == null) return false;
        if (RsModDeclItemUtil.isLocal(modDecl)) return false;

        String modDeclName = ((RsMandatoryReferenceElement) modDecl).getReferenceName();
        if (modDeclName == null) return false;

        java.util.List<VirtualFile> dirs = new java.util.ArrayList<>();
        java.util.List<VirtualFile> files = new java.util.ArrayList<>();
        for (VirtualFile child : ownedDirectory.getVirtualFile().getChildren()) {
            if (child.isDirectory()) dirs.add(child); else files.add(child);
        }

        for (VirtualFile vFile : files) {
            String rawFileName = vFile.getName();
            if (vFile.equals(originalFile) || RsConstants.MOD_RS_FILE.equals(rawFileName)) continue;
            String fileName = modDeclFileName(rawFileName, modDeclName);
            RsFile rf = RsFileUtil.getRustFile(psiMgr.findFile(vFile));
            if (rf != null && Processors.processEntry(processor, fileName, Namespace.TYPES, rf)) return true;
        }

        for (VirtualFile vDir : dirs) {
            VirtualFile modFile = vDir.findChild(RsConstants.MOD_RS_FILE);
            if (modFile != null) {
                RsFile rf = RsFileUtil.getRustFile(psiMgr.findFile(modFile));
                if (rf != null && Processors.processEntry(processor, vDir.getName(), Namespace.TYPES, rf)) return true;
            }
            // `mod foo;` in `mod.rs` doesn't search submodule files
            if (inModRs) continue;
            // Submodule in crate root dir already handled above
            if (containingMod.isCrateRoot()) continue;

            if (vDir.getName().equals(containingMod.getModName())) {
                for (VirtualFile vFile : vDir.getChildren()) {
                    if (vFile.isDirectory()) continue;
                    String rawFileName = vFile.getName();
                    if (RsConstants.MOD_RS_FILE.equals(rawFileName)) continue;
                    String fileName = modDeclFileName(rawFileName, modDeclName);
                    RsFile rf = RsFileUtil.getRustFile(psiMgr.findFile(vFile));
                    if (rf != null && Processors.processEntry(processor, fileName, Namespace.TYPES, rf)) return true;
                }
            }
        }
        return false;
    }

    private static String modDeclFileName(@Nonnull String rawName, @Nonnull String modDeclName) {
        String fileName = FileUtil.getNameWithoutExtension(rawName);
        return modDeclName.equalsIgnoreCase(fileName) ? modDeclName : fileName;
    }

    /** Convenience overload that includes the current crate as {@code "crate"}. */
    public static boolean processExternCrateResolveVariants(
        @Nonnull RsElement element,
        boolean isCompletion,
        @Nonnull RsResolveProcessor processor
    ) {
        return processExternCrateResolveVariants(element, isCompletion, true, processor);
    }

    /**
     * Enumerate extern-crate dependencies of the current crate (optionally with the current
     * crate as {@code "crate"}) and feed each one to the processor.
     */
    public static boolean processExternCrateResolveVariants(
        @Nonnull RsElement element,
        boolean isCompletion,
        boolean withSelf,
        @Nonnull RsResolveProcessor processor
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
        @Nonnull RsPath path,
        boolean isCompletion,
        boolean processAssocItems,
        @Nonnull RsResolveProcessor processor
    ) {
        PathResolutionContext ctx = new PathResolutionContext(path, isCompletion, processAssocItems, lookup);
        RsPathResolveKind pathKind = ctx.classifyPath(path);
        return processPathResolveVariants(ctx, pathKind, processor);
    }

    public static boolean processPathResolveVariants(
        @Nonnull PathResolutionContext ctx,
        @Nonnull RsPathResolveKind pathKind,
        @Nonnull RsResolveProcessor processor
    ) {
        if (processor.getNames() != null && processor.getNames().contains("std")) {
        }
        if (pathKind instanceof RsPathResolveKind.UnqualifiedPath) {
            Set<Namespace> ns = ((RsPathResolveKind.UnqualifiedPath) pathKind).getNs();
            if (processSelfSuperCrate(ns, ctx, processor)) return true;
            return processNestedScopesUpwards(ctx.getContext(), ns, ctx, processor);
        }
        if (pathKind instanceof RsPathResolveKind.QualifiedPath) {
            RsPathResolveKind.QualifiedPath q = (RsPathResolveKind.QualifiedPath) pathKind;
            return processQualifiedPathResolveVariants(ctx, q.getNs(), q.getQualifier(), q.getPath(), q.getParent(), processor);
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
        @Nonnull Set<Namespace> ns,
        @Nonnull PathResolutionContext ctx,
        @Nonnull RsResolveProcessor processor
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
        @Nonnull PathResolutionContext ctx,
        @Nonnull Set<Namespace> ns,
        @Nonnull RsPath qualifier,
        @Nonnull RsPath path,
        @Nullable PsiElement parent,
        @Nonnull RsResolveProcessor processor
    ) {
        if (qualifier.getReference() == null) return false;
        PsiElement resolved = RsPathReferenceImpl.deepResolve(
            (RsPathReference) qualifier.getReference());
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
        // `Foo::bar` where Foo names a type: the associated item lives in a detached `impl` block,
        // so the qualifier has to be lowered to a type and the impls looked up through it.
        if (resolved instanceof RsTypeDeclarationElement
            && !(parent instanceof RsUseSpeck)
            && ctx.isProcessAssocItems()) {
            Ty rawBaseTy;
            if (RsPathUtil.getHasCself(qualifier)) {
                // `Self::bar()` inside an impl or trait names the implementing type.
                if (resolved instanceof RsImplItem) {
                    RsTypeReference typeRef =
                        ((RsImplItem) resolved).getTypeReference();
                    rawBaseTy = typeRef != null
                        ? ExtensionsUtil.getRawType(typeRef)
                        : TyUnknown.INSTANCE;
                }
                else if (resolved instanceof RsTraitItem) {
                    rawBaseTy = TyTypeParameter.self((RsTraitItem) resolved);
                }
                else {
                    rawBaseTy = TyUnknown.INSTANCE;
                }
            }
            else {
                Substitution subst = SubstitutionUtil.EMPTY;
                // Without explicit type arguments the generics stay open, so they become inference
                // variables - otherwise a generic type never matches its own impl.
                if (qualifier.getTypeArgumentList() == null
                    && resolved instanceof RsGenericDeclaration) {
                    java.util.Map<TyTypeParameter, Ty> typeSubst =
                        new java.util.HashMap<>();
                    for (TyTypeParameter gen
                        : TypeInferenceUtil.getGenerics(
                            (RsGenericDeclaration) resolved)) {
                        typeSubst.put(gen, new TyInfer.TyVar(gen));
                    }
                    subst = new Substitution(typeSubst);
                }
                rawBaseTy = FoldUtil.substituteOrUnknown(
                    ((RsTypeDeclarationElement) resolved).getDeclaredType(), subst);
            }
            Ty baseTy =
                ctx.getImplLookup().getCtx().normalizeAssociatedTypesIn(rawBaseTy).getValue();
            return processAssociatedItems(ctx.getImplLookup(), baseTy, ns, ctx.getContext(), processor);
        }
        return false;
    }

    /**
     * Emits the associated items reachable through {@code type} - the members of its inherent impls
     * first, then of the traits it implements, an inherent member hiding a trait member of the
     * same name.
     */
    private static boolean processAssociatedItems(
        @Nonnull ImplLookup lookup,
        @Nonnull Ty type,
        @Nonnull Set<Namespace> ns,
        @Nonnull RsElement context,
        @Nonnull RsResolveProcessor processor
    ) {
        java.util.function.Predicate<RsAbstractable> nsFilter = assocMembersNsFilter(ns);
        if (nsFilter == null) return false;

        Substitution selfSubst = SubstitutionUtil.toTypeSubst(
            java.util.Collections.singletonMap(TyTypeParameter.self(), type));

        java.util.Map<String, RsAbstractable> visitedInherent = new java.util.HashMap<>();
        for (TraitImplSource source : lookup.findImplsAndTraits(type)) {
            boolean isInherent = source.isInherent();
            for (java.util.Map.Entry<String, java.util.List<RsAbstractable>> entry
                : source.getImplAndTraitExpandedMembers().entrySet()) {
                String name = entry.getKey();
                for (RsAbstractable member : entry.getValue()) {
                    if (!nsFilter.test(member)) continue;
                    if (isInherent) {
                        visitedInherent.put(name, member);
                    }
                    else if (visitedInherent.containsKey(name)) {
                        continue;
                    }
                    Set<Namespace> namespaces = member instanceof RsTypeAlias
                        ? Namespace.TYPES : Namespace.VALUES;
                    if (processor.process(new AssocItemScopeEntry(
                        name, member, namespaces, selfSubst, type, source))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Associated types live in the type namespace, everything else in the value namespace. */
    @Nullable
    private static java.util.function.Predicate<RsAbstractable> assocMembersNsFilter(
        @Nonnull Set<Namespace> ns
    ) {
        boolean types = ns.contains(Namespace.Types);
        boolean values = ns.contains(Namespace.Values);
        if (types && values) return m -> true;
        if (types) return m -> m instanceof RsTypeAlias;
        if (values) return m -> !(m instanceof RsTypeAlias);
        return null;
    }

    private static boolean processExplicitTypeQualifiedPathResolveVariants(
        @Nonnull PathResolutionContext ctx,
        @Nonnull Set<Namespace> ns,
        @Nonnull RsTypeQual typeQual,
        @Nonnull RsResolveProcessor processor
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
        @Nonnull RsPath path,
        @Nonnull RsResolveProcessor processor
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
        @Nonnull RsPatBinding binding,
        boolean isCompletion,
        @Nonnull RsResolveProcessor originalProcessor
    ) {
        if (binding.getParent() instanceof RsPatField) {
            PsiElement pp = binding.getParent().getParent();
            if (pp instanceof RsPatStruct) {
                RsPath path = ((RsPatStruct) pp).getPath();
                PsiElement resolved = path != null && path.getReference() != null
                    ? RsPathReferenceImpl.deepResolve(
                        (RsPathReference) path.getReference())
                    : null;
                if (resolved instanceof RsFieldsOwner) {
                    if (processFieldDeclarations((RsFieldsOwner) resolved, originalProcessor)) return true;
                    if (isCompletion) return false;
                }
            }
        }

        final boolean completion = isCompletion;
        @SuppressWarnings("unchecked")
        RsResolveProcessor filtered = Processors.asResolveProcessor(Processors.wrapWithFilter(originalProcessor, entry -> {
            if (!originalProcessor.acceptsName(entry.getName())) return false;
            RsElement element = entry.getElement();
            boolean isConstant = RsElementUtil.isConstantLike(element);
            boolean isPathOrDestructurable =
                element instanceof RsMod
                    || element instanceof RsEnumItem
                    || element instanceof RsEnumVariant
                    || element instanceof RsStructItem;
            return isConstant || (completion && isPathOrDestructurable);
        }));
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
        @Nonnull RsLabel label,
        @Nonnull RsResolveProcessor processor
    ) {
        return processLabelResolveVariants(label, processor, false);
    }

    public static boolean processLabelResolveVariants(
        @Nonnull RsLabel label,
        @Nonnull RsResolveProcessor processor,
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

    @Nonnull
    public static List<RsElement> resolveLabelReference(@Nonnull RsLabel element) {
        return resolveLabelReference(element, false);
    }

    @Nonnull
    public static List<RsElement> resolveLabelReference(@Nonnull RsLabel element, boolean processBeyondLabelBarriers) {
        String name = element.getReferenceName();
        if (name == null) return Collections.emptyList();
        return Processors.collectResolveVariants(name, p -> processLabelResolveVariants(element, p, processBeyondLabelBarriers));
    }

    /**
     * Walk outward looking for enclosing {@link RsGenericDeclaration}s and feed each lifetime
     * parameter to the processor.
     */
    public static boolean processLifetimeResolveVariants(
        @Nonnull RsLifetime lifetime,
        @Nonnull RsResolveProcessor processor
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
        @Nonnull RsElement place,
        @Nonnull Consumer<RsPatBinding> processor
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

    private static void collectPatBindings(@Nonnull PsiElement pat, @Nonnull Consumer<RsPatBinding> sink) {
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
        @Nonnull RsMacroReference ref,
        @Nonnull RsResolveProcessor processor
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

    private static <T extends PsiElement> List<T> collectDescendantsOfType(@Nullable PsiElement root, @Nonnull Class<T> clazz) {
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
        @Nonnull RsPath path,
        @Nonnull RsResolveProcessor originalProcessor,
        boolean isCompletion
    ) {
        RsResolveProcessor filtered = Processors.asResolveProcessor(Processors.wrapWithFilter(originalProcessor,
            e -> !(e.getElement() instanceof RsMacroDefinitionBase)));
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
        @Nonnull RsPath element,
        @Nonnull String traitName,
        @Nonnull RsResolveProcessor processor
    ) {
        if (processNestedScopesUpwards(element, Namespace.MACROS, processor)) return true;
        KnownDerivableTrait known =
            KnownItems.getKNOWN_DERIVABLE_TRAITS().get(traitName);
        if (known != null) {
            RsTraitItem hardcoded = known.findTrait(KnownItems.getKnownItems(element));
            if (hardcoded != null) {
                return Processors.processEntry(processor, traitName, Namespace.TYPES, hardcoded);
            }
        }
        java.util.Collection<RsNamedElement> found =
            RsNamedElementIndex.findElementsByName(element.getProject(), traitName);
        return Processors.processAll(processor,
            new java.util.ArrayList<>(filterTraits(found)), Namespace.TYPES);
    }

    @Nonnull
    private static java.util.List<RsNamedElement> filterTraits(@Nonnull java.util.Collection<RsNamedElement> elements) {
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
        @Nonnull RsBinaryOp element,
        @Nonnull OverloadableBinaryOperator operator,
        @Nonnull RsResolveProcessor processor
    ) {
        RsBinaryExpr binaryExpr = PsiElementUtil.ancestorStrict(element, RsBinaryExpr.class);
        if (binaryExpr == null) return false;
        RsExpr right = binaryExpr.getRight();
        if (right == null) return false;
        Ty rhsType = ExtensionsUtil.getType(right);
        Ty lhsType = ExtensionsUtil.getType(binaryExpr.getLeft());
        ImplLookup lookup = ImplLookup.relativeTo(element);
        RsTraitOrImpl impl = lookup.findOverloadedOpImpl(lhsType, rhsType, operator);
        if (impl == null) return false;
        for (RsAbstractable m : RsTraitOrImplUtil.getExpandedMembers(impl)) {
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
        @Nonnull RsAssocTypeBinding element,
        @Nonnull RsResolveProcessor processor
    ) {
        RsPath path = PsiElementUtil.ancestorStrict(element, RsPath.class);
        if (path == null) return false;
        if (path.getReference() == null) return false;
        PsiElement resolved = path.getReference().resolve();
        if (!(resolved instanceof RsTraitItem)) return false;
        return processAssocTypeVariants((RsTraitItem) resolved, processor);
    }

    public static boolean processAssocTypeVariants(
        @Nonnull RsTraitItem trait,
        @Nonnull RsResolveProcessor processor
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
        @Nonnull RsPath path,
        boolean isCompletion,
        @Nonnull RsResolveProcessor processor
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
        @Nonnull RsPath path,
        boolean ignoreLegacyMacros,
        @Nonnull RsResolveProcessor processor
    ) {
        java.util.Set<String> seen = new java.util.HashSet<>();
        PsiElement scope = path.getContext();
        while (scope != null) {
            if (scope instanceof RsItemsOwner) {
                // The def map knows the macros a scope can see, including those a `use` brought in and
                // the standard library macro prelude. A module's answer is final; an inner scope that
                // finds nothing just keeps the walk going outward.
                boolean fromDefMap = FacadeResolve.processMacros(
                    (RsItemsOwner) scope, processor, path);
                if (fromDefMap) return true;
                // A module ends the lexical walk, but the injected standard library macros below are
                // still in scope, so fall out of the loop rather than out of the method.
                if (scope instanceof RsMod) break;

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

        // `println!` and friends are not declared anywhere in this crate: the standard library is
        // injected as if by `#[macro_use] extern crate std`, so its exported macros are the last
        // place an unqualified macro name can come from.
        RsFile crateRoot = path.getCrateRoot() instanceof RsFile ? (RsFile) path.getCrateRoot() : null;
        if (crateRoot == null) return false;
        RsFile stdlibCrateRoot = implicitStdlibCrateRoot(crateRoot);
        if (stdlibCrateRoot == null) return false;
        return processExportedMacros(stdlibCrateRoot, processor);
    }

    /**
     * The crate root of the standard library crate injected into {@code scope} - {@code std}, or
     * {@code core} under {@code #![no_std]}, or nothing under {@code #![no_core]}.
     */
    @Nullable
    private static RsFile implicitStdlibCrateRoot(@Nonnull RsFile scope) {
        String name = scope.getStdlibAttributes().getAutoInjectedCrate();
        return name == null ? null : findDependencyCrateByName(scope, name);
    }

    /**
     * The {@code #[macro_export]} macros of a crate. They are taken from the stub index rather than
     * from the crate root's own items: an exported macro is callable by the crate's name wherever in
     * the crate it happens to be declared, and in the standard library they are not in the root file.
     */
    private static boolean processExportedMacros(
        @Nonnull RsFile crateRoot,
        @Nonnull RsResolveProcessor processor
    ) {
        java.util.Map<RsMod, java.util.List<RsMacro>> exported =
            RsMacroIndex.allExportedMacros(crateRoot.getProject());
        java.util.List<RsMacro> macros = exported.get(crateRoot);
        if (macros == null) return false;
        for (RsMacro macro : macros) {
            String name = macro.getName();
            if (name == null) continue;
            if (Processors.processEntry(processor, name, Namespace.MACROS, macro)) return true;
        }
        return false;
    }

    /** Walk from the given element outward, feeding every visible name in the requested namespaces to the processor. */
    public static boolean processNestedScopesUpwards(
        @Nonnull RsElement scopeStart,
        @Nonnull Set<Namespace> ns,
        @Nonnull RsResolveProcessor processor
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
        @Nonnull RsElement scopeStart,
        @Nonnull Set<Namespace> ns,
        @Nullable PathResolutionContext ctx,
        @Nonnull RsResolveProcessor processor
    ) {
        java.util.Set<String> seen = new java.util.HashSet<>();
        PsiElement cameFrom = scopeStart;
        PsiElement scope = scopeStart.getContext();
        while (scope != null) {
            if (scope instanceof RsMod) {
                // A module is the end of the walk: Rust does not make the items of an enclosing
                // module visible unqualified, and everything that *is* visible here - own items,
                // imports, extern crates and the prelude - comes from the crate's def map.
                return processModScope((RsMod) scope, ns, seen, processor);
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
            } else if (scope instanceof RsIfExpr) {
                // `if let Some(x) = e { .. }` binds x for the body only. A scope that binds nothing here
                // is simply skipped - the walk must carry on outward, not stop.
                RsIfExpr ifExpr = (RsIfExpr) scope;
                if (ifExpr.getBlock() == cameFrom
                    && processLetExprs(conditionExpr(ifExpr.getCondition()), cameFrom, ns, seen, processor)) {
                    return true;
                }
            } else if (scope instanceof RsWhileExpr) {
                RsWhileExpr whileExpr = (RsWhileExpr) scope;
                if (whileExpr.getBlock() == cameFrom
                    && processLetExprs(conditionExpr(whileExpr.getCondition()), cameFrom, ns, seen, processor)) {
                    return true;
                }
            } else if (scope instanceof RsBinaryExpr) {
                // A let-chain: `if let A = a && let B = b`, where the left operand binds for the right.
                RsBinaryExpr binary = (RsBinaryExpr) scope;
                if (binary.getRight() == cameFrom
                    && processLetExprs(binary.getLeft(), cameFrom, ns, seen, processor)) {
                    return true;
                }
            }
            cameFrom = scope;
            scope = scope.getContext();
        }
        return false;
    }

    /**
     * Names visible in a module: its own items, whatever its {@code use} declarations bring in, the
     * extern prelude and finally the standard library prelude. All of it is held by the crate's def
     * map, so the module is asked through {@code resolve2} rather than by walking its items - a PSI
     * walk sees neither imports nor either prelude.
     */
    @Nullable
    private static RsExpr conditionExpr(
        @Nullable RsCondition condition
    ) {
        return condition == null ? null : condition.getExpr();
    }

    /**
     * The bindings introduced by the {@code let} expressions of a condition, including a let-chain
     * joined by {@code &&}. Only the {@code let}s to the left of where the walk came from are in
     * scope, which is what {@code cameFrom} settles.
     */
    private static boolean processLetExprs(
        @Nullable RsExpr expr,
        @Nullable PsiElement cameFrom,
        @Nonnull Set<Namespace> ns,
        @Nonnull java.util.Set<String> seen,
        @Nonnull RsResolveProcessor processor
    ) {
        if (expr == null || expr == cameFrom) return false;

        if (expr instanceof RsLetExpr) {
            RsPat pat = ((RsLetExpr) expr).getPat();
            return pat != null && processPatternBindings(pat, ns, seen, processor);
        }

        if (expr instanceof RsBinaryExpr) {
            RsBinaryExpr binary = (RsBinaryExpr) expr;
            if (processLetExprs(binary.getRight(), cameFrom, ns, seen, processor)) return true;
            return processLetExprs(binary.getLeft(), cameFrom, ns, seen, processor);
        }

        return false;
    }

    private static boolean processModScope(
        @Nonnull RsMod mod,
        @Nonnull Set<Namespace> ns,
        @Nonnull java.util.Set<String> seen,
        @Nonnull RsResolveProcessor processor
    ) {
        RsModInfo modInfo = FacadeResolve.getModInfo(mod);
        if (processor.getNames() != null && processor.getNames().contains("std")) {
        }
        if (modInfo == null) {
            return processModScopeFromPsi(mod, ns, seen, processor);
        }

        RsResolveProcessor shadowing = shadowingProcessor(processor, seen);
        if (FacadeResolve.processItemDeclarationsUsingModInfo(
            true, modInfo, ns, shadowing,
            ItemProcessingMode.WITH_PRIVATE_IMPORTS_N_EXTERN_CRATES)) {
            return true;
        }

        RsModInfo preludeInfo = findPreludeUsingModInfo(modInfo);
        if (preludeInfo == null) return false;
        return FacadeResolve.processItemDeclarationsUsingModInfo(
            true, preludeInfo, ns, shadowingProcessor(processor, seen),
            ItemProcessingMode.WITHOUT_PRIVATE_IMPORTS);
    }

    /** Used for a module the def map does not cover, such as one inside a code fragment. */
    private static boolean processModScopeFromPsi(
        @Nonnull RsMod mod,
        @Nonnull Set<Namespace> ns,
        @Nonnull java.util.Set<String> seen,
        @Nonnull RsResolveProcessor processor
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

    /** Drops names an inner scope already bound, so that a local shadows an item of the same name. */
    @Nonnull
    private static RsResolveProcessor shadowingProcessor(
        @Nonnull RsResolveProcessor processor,
        @Nonnull java.util.Set<String> seen
    ) {
        return Processors.asResolveProcessor(
            Processors.wrapWithFilter(processor, entry -> seen.add(entry.getName())));
    }

    /**
     * The standard library prelude of the crate {@code info} belongs to, as a module info of its own.
     */
    @Nullable
    private static RsModInfo findPreludeUsingModInfo(
        @Nonnull RsModInfo info
    ) {
        ModData preludeModData = info.getDefMap().getPrelude();
        if (preludeModData == null) return null;
        Crate preludeCrate =
            CrateGraphService.crateGraph(info.getProject())
                .findCrateById(preludeModData.getCrate());
        if (preludeCrate == null) return null;
        CrateDefMap preludeDefMap =
            info.getDefMap().getDefMap(preludeModData.getCrate());
        if (preludeDefMap == null) return null;
        return new RsModInfo(
            info.getProject(), preludeDefMap, preludeModData, preludeCrate, null);
    }

    private static boolean processBlockScope(
        @Nonnull RsBlock block,
        @Nonnull PsiElement cameFrom,
        @Nonnull Set<Namespace> ns,
        @Nonnull java.util.Set<String> seen,
        @Nonnull RsResolveProcessor processor
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

        // A block can hold `use` items, which are not named elements and so contribute nothing to the
        // loop above; their imports only exist in the def map built for the block.
        return FacadeResolve.processItemDeclarations(
            block, ns, shadowingProcessor(processor, seen),
            ItemProcessingMode.WITH_PRIVATE_IMPORTS);
    }

    private static boolean processFunctionScope(
        @Nonnull RsFunction fn,
        @Nonnull Set<Namespace> ns,
        @Nonnull java.util.Set<String> seen,
        @Nonnull RsResolveProcessor processor
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
        @Nonnull RsLambdaExpr lambda,
        @Nonnull Set<Namespace> ns,
        @Nonnull java.util.Set<String> seen,
        @Nonnull RsResolveProcessor processor
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
        @Nonnull RsGenericDeclaration decl,
        @Nonnull Set<Namespace> ns,
        @Nonnull java.util.Set<String> seen,
        @Nonnull RsResolveProcessor processor
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
        @Nonnull PsiElement pat,
        @Nonnull Set<Namespace> ns,
        @Nonnull java.util.Set<String> seen,
        @Nonnull RsResolveProcessor processor
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

    private static boolean anyNsMatches(@Nonnull RsItemElement item, @Nonnull Set<Namespace> ns) {
        for (Namespace candidate : elementNamespaces(item)) {
            if (ns.contains(candidate)) return true;
        }
        return false;
    }

    @Nonnull
    private static Set<Namespace> elementNamespaces(@Nonnull RsItemElement item) {
        if (item instanceof RsNamedElement) return Namespace.getNamespaces((RsNamedElement) item);
        return Namespace.TYPES_N_VALUES;
    }

    /**
     * Returns the prelude module for the enclosing mod. Uses the resolve2
     * {@link FacadeResolve#getModInfo} + {@code defMap.prelude}
     * where available; returns {@code null} otherwise.
     */
    @Nullable
    public static RsMod findPrelude(@Nonnull RsElement element) {
        RsMod containing = element.getContainingMod();
        if (containing == null) return null;
        RsModInfo info = FacadeResolve.getModInfo(containing);
        if (info == null) return null;
        ModData prelude = info.getDefMap().getPrelude();
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
        @Nonnull RsElement context,
        @Nonnull Consumer<RsUseSpeck> processor
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
        @Nonnull PathResolutionContext ctx,
        @Nonnull RsResolveProcessor processor
    ) {
        RsModInfo info = ctx.getContainingModInfo();
        if (info == null) return false;
        Set<String> wantedNames = processor.getNames();
        for (Map.Entry<String, CrateDefMap> entry :
            info.getDefMap().getExternPrelude().entrySet()) {
            if (wantedNames != null && !wantedNames.contains(entry.getKey())) continue;
            RsMod externCrateRoot = entry.getValue().rootAsRsMod(info.getProject());
            if (externCrateRoot == null) continue;
            if (Processors.processEntry(processor, entry.getKey(), Namespace.TYPES, externCrateRoot)) return true;
        }
        return false;
    }

    /** Resolve a path to its candidates via {@link Processors#collectPathResolveVariants}. */
    @Nonnull
    public static List<RsPathResolveResult<RsElement>> resolvePath(@Nonnull PathResolutionContext ctx, @Nonnull RsPath path, @Nonnull Object kind) {
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
    public static RsNamedElement findInScope(@Nonnull RsElement scope, @Nonnull String name, @Nonnull Set<Namespace> ns) {
        RsElement[] found = new RsElement[1];
        processNestedScopesUpwards(scope, ns, new RsResolveProcessor() {
            @Override
            public boolean process(@Nonnull ScopeEntry entry) {
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

    @Nonnull
    public static List<RsElement> collectResolveVariants(@Nullable String referenceName, @Nonnull Consumer<RsResolveProcessor> f) {
        return Processors.collectResolveVariants(referenceName, f);
    }

    @Nullable
    public static RsElement pickFirstResolveVariant(@Nullable String referenceName, @Nonnull Consumer<RsResolveProcessor> f) {
        return Processors.pickFirstResolveVariant(referenceName, f);
    }

    @Nonnull
    public static <T extends ScopeEntry> List<T> collectResolveVariantsAsScopeEntries(
        @Nullable String referenceName,
        @Nonnull Consumer<RsResolveProcessorBase<T>> f
    ) {
        return Processors.collectResolveVariantsAsScopeEntries(referenceName, f);
    }

    @Nonnull
    public static List<RsPathResolveResult<RsElement>> collectPathResolveVariants(
        @Nonnull PathResolutionContext ctx,
        @Nonnull RsPath path,
        @Nonnull Consumer<RsResolveProcessor> f
    ) {
        return Processors.collectPathResolveVariants(ctx, path, f);
    }

    @Nonnull
    public static Map<RsPath, List<RsPathResolveResult<RsElement>>> collectMultiplePathResolveVariants(
        @Nonnull PathResolutionContext ctx,
        @Nonnull List<RsPath> paths,
        @Nonnull Consumer<RsResolveProcessor> f
    ) {
        return Processors.collectMultiplePathResolveVariants(ctx, paths, f);
    }

    // -------------------------------------------------------------------------
    // Constants + namespace shortcuts.
    // -------------------------------------------------------------------------

    public static final int DEFAULT_RECURSION_LIMIT = 128;

    public static final Set<Namespace> TYPES_N_VALUES_N_MACROS = Namespace.TYPES_N_VALUES_N_MACROS;

    @Nonnull public static Set<Namespace> getTYPES() { return Namespace.TYPES; }
    @Nonnull public static Set<Namespace> getVALUES() { return Namespace.VALUES; }
    @Nonnull public static Set<Namespace> getTYPES_N_VALUES_N_MACROS() { return Namespace.TYPES_N_VALUES_N_MACROS; }
    @Nonnull public static Set<Namespace> getENUM_VARIANT_NS() { return Namespace.ENUM_VARIANT_NS; }
}
