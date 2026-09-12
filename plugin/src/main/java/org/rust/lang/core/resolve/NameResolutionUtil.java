/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import consulo.project.Project;
import consulo.util.lang.ThreeState;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.psi.ext.OverloadableBinaryOperator;
import org.rust.lang.core.types.ty.Ty;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.rust.lang.core.resolve.ref.DotExprResolveVariant;
import org.rust.lang.core.resolve.ref.FieldResolveVariant;
import org.rust.lang.core.resolve.ref.MethodResolveVariant;
import consulo.util.lang.Pair;

/**
 * Utility class providing static access to name resolution functions.
 * Delegates to {@link NameResolution} and {@link Processors}.
 */
public final class NameResolutionUtil {
    private NameResolutionUtil() {}

    // --- Delegates to NameResolution ---

    public static boolean processDotExprResolveVariants(
        @Nonnull ImplLookup lookup, @Nonnull Ty receiverType,
        @Nonnull RsElement context, @Nonnull RsResolveProcessorBase<DotExprResolveVariant> processor
    ) {
        return NameResolution.processDotExprResolveVariants(lookup, receiverType, context, processor);
    }

    public static boolean processFieldExprResolveVariants(
        @Nonnull ImplLookup lookup, @Nonnull Ty receiverType,
        @Nonnull RsResolveProcessorBase<FieldResolveVariant> processor
    ) {
        return NameResolution.processFieldExprResolveVariants(lookup, receiverType, processor);
    }

    public static boolean processStructLiteralFieldResolveVariants(
        @Nonnull RsStructLiteralField field, boolean isCompletion,
        @Nonnull RsResolveProcessor processor
    ) {
        return NameResolution.processStructLiteralFieldResolveVariants(field, isCompletion, processor);
    }

    public static boolean processStructPatternFieldResolveVariants(
        @Nonnull RsPatFieldFull field, @Nonnull RsResolveProcessor processor
    ) {
        return NameResolution.processStructPatternFieldResolveVariants(field, processor);
    }

    public static boolean processMethodCallExprResolveVariants(
        @Nonnull ImplLookup lookup, @Nonnull Ty receiverType,
        @Nonnull RsElement context, @Nonnull RsResolveProcessorBase<MethodResolveVariant> processor
    ) {
        return NameResolution.processMethodCallExprResolveVariants(lookup, receiverType, context, processor);
    }

    public static boolean processModDeclResolveVariants(
        @Nonnull RsModDeclItem modDecl, @Nonnull RsResolveProcessor processor
    ) {
        return NameResolution.processModDeclResolveVariants(modDecl, processor);
    }

    public static boolean processExternCrateResolveVariants(
        @Nonnull RsElement element, boolean isCompletion, @Nonnull RsResolveProcessor processor
    ) {
        return NameResolution.processExternCrateResolveVariants(element, isCompletion, processor);
    }

    public static boolean processExternCrateResolveVariants(
        @Nonnull RsElement element, boolean isCompletion, boolean withSelf,
        @Nonnull RsResolveProcessor processor
    ) {
        return NameResolution.processExternCrateResolveVariants(element, isCompletion, withSelf, processor);
    }

    @Nullable
    public static RsFile findDependencyCrateByNamePath(@Nonnull RsElement context, @Nonnull RsPath path) {
        return NameResolution.findDependencyCrateByNamePath(context, path);
    }

    @Nullable
    public static RsFile findDependencyCrateByName(@Nonnull RsElement context, @Nonnull String name) {
        return NameResolution.findDependencyCrateByName(context, name);
    }

    public static boolean processPathResolveVariants(
        @Nullable ImplLookup lookup, @Nonnull RsPath path,
        boolean isCompletion, boolean processAssocItems,
        @Nonnull RsResolveProcessor processor
    ) {
        return NameResolution.processPathResolveVariants(lookup, path, isCompletion, processAssocItems, processor);
    }

    public static boolean processPathResolveVariants(
        @Nonnull PathResolutionContext ctx, @Nonnull RsPathResolveKind pathKind,
        @Nonnull RsResolveProcessor processor
    ) {
        return NameResolution.processPathResolveVariants(ctx, pathKind, processor);
    }

    @Nullable
    public static Crate resolveDollarCrateIdentifier(@Nonnull RsPath path) {
        return NameResolution.resolveDollarCrateIdentifier(path);
    }

    public static boolean processPatBindingResolveVariants(
        @Nonnull RsPatBinding binding, boolean isCompletion,
        @Nonnull RsResolveProcessor processor
    ) {
        return NameResolution.processPatBindingResolveVariants(binding, isCompletion, processor);
    }

    public static boolean processLabelResolveVariants(
        @Nonnull RsLabel label, @Nonnull RsResolveProcessor processor
    ) {
        return NameResolution.processLabelResolveVariants(label, processor);
    }

    public static boolean processLabelResolveVariants(
        @Nonnull RsLabel label, @Nonnull RsResolveProcessor processor,
        boolean processBeyondLabelBarriers
    ) {
        return NameResolution.processLabelResolveVariants(label, processor, processBeyondLabelBarriers);
    }

    @Nonnull
    public static List<RsElement> resolveLabelReference(@Nonnull RsLabel element) {
        return NameResolution.resolveLabelReference(element);
    }

    @Nonnull
    public static List<RsElement> resolveLabelReference(@Nonnull RsLabel element, boolean processBeyondLabelBarriers) {
        return NameResolution.resolveLabelReference(element, processBeyondLabelBarriers);
    }

    public static boolean processLifetimeResolveVariants(
        @Nonnull RsLifetime lifetime, @Nonnull RsResolveProcessor processor
    ) {
        return NameResolution.processLifetimeResolveVariants(lifetime, processor);
    }

    public static void processLocalVariables(
        @Nonnull RsElement place, @Nonnull Consumer<RsPatBinding> processor
    ) {
        NameResolution.processLocalVariables(place, processor);
    }

    @Nullable
    public static consulo.util.lang.Pair<RsNamedElement, CargoWorkspace.Package> resolveStringPath(
        @Nonnull String path, @Nonnull CargoWorkspace workspace,
        @Nonnull Project project, @Nonnull ThreeState isStd
    ) {
        return NameResolution.resolveStringPath(path, workspace, project, isStd);
    }

    @Nullable
    public static consulo.util.lang.Pair<RsNamedElement, CargoWorkspace.Package> resolveStringPath(
        @Nonnull String path, @Nonnull CargoWorkspace workspace,
        @Nonnull Project project
    ) {
        return NameResolution.resolveStringPath(path, workspace, project);
    }

    @Nullable
    public static consulo.util.lang.Pair<String, String> splitAbsolutePath(@Nonnull String path) {
        return NameResolution.splitAbsolutePath(path);
    }

    public static boolean processMacroReferenceVariants(
        @Nonnull RsMacroReference ref, @Nonnull RsResolveProcessor processor
    ) {
        return NameResolution.processMacroReferenceVariants(ref, processor);
    }

    public static boolean processProcMacroResolveVariants(
        @Nonnull RsPath path, @Nonnull RsResolveProcessor processor, boolean isCompletion
    ) {
        return NameResolution.processProcMacroResolveVariants(path, processor, isCompletion);
    }

    public static boolean processDeriveTraitResolveVariants(
        @Nonnull RsPath element, @Nonnull String traitName,
        @Nonnull RsResolveProcessor processor
    ) {
        return NameResolution.processDeriveTraitResolveVariants(element, traitName, processor);
    }

    public static boolean processBinaryOpVariants(
        @Nonnull RsBinaryOp element, @Nonnull OverloadableBinaryOperator operator,
        @Nonnull RsResolveProcessor processor
    ) {
        return NameResolution.processBinaryOpVariants(element, operator, processor);
    }

    public static boolean processAssocTypeVariants(
        @Nonnull RsAssocTypeBinding element, @Nonnull RsResolveProcessor processor
    ) {
        return NameResolution.processAssocTypeVariants(element, processor);
    }

    public static boolean processAssocTypeVariants(
        @Nonnull RsTraitItem trait, @Nonnull RsResolveProcessor processor
    ) {
        return NameResolution.processAssocTypeVariants(trait, processor);
    }

    public static boolean processMacroCallPathResolveVariants(
        @Nonnull RsPath path, boolean isCompletion,
        @Nonnull RsResolveProcessor processor
    ) {
        return NameResolution.processMacroCallPathResolveVariants(path, isCompletion, processor);
    }

    public static boolean processMacroCallVariantsInScope(
        @Nonnull RsPath path, boolean ignoreLegacyMacros,
        @Nonnull RsResolveProcessor processor
    ) {
        return NameResolution.processMacroCallVariantsInScope(path, ignoreLegacyMacros, processor);
    }

    public static boolean processNestedScopesUpwards(
        @Nonnull RsElement scopeStart, @Nonnull Set<Namespace> ns,
        @Nonnull RsResolveProcessor processor
    ) {
        return NameResolution.processNestedScopesUpwards(scopeStart, ns, processor);
    }

    public static boolean processNestedScopesUpwards(
        @Nonnull RsElement scopeStart, @Nonnull Set<Namespace> ns,
        @Nullable PathResolutionContext ctx, @Nonnull RsResolveProcessor processor
    ) {
        return NameResolution.processNestedScopesUpwards(scopeStart, ns, ctx, processor);
    }

    @Nullable
    public static RsMod findPrelude(@Nonnull RsElement element) {
        return NameResolution.findPrelude(element);
    }

    public static void processUnresolvedImports(
        @Nonnull RsElement context, @Nonnull Consumer<RsUseSpeck> processor
    ) {
        NameResolution.processUnresolvedImports(context, processor);
    }

    public static boolean processExternPreludeResolveVariants(
        @Nonnull PathResolutionContext ctx, @Nonnull RsResolveProcessor processor
    ) {
        return NameResolution.processExternPreludeResolveVariants(ctx, processor);
    }

    @Nonnull
    public static Set<Namespace> allowedNamespaces(@Nonnull RsPath path) {
        return NameResolution.allowedNamespaces(path);
    }

    @Nonnull
    public static List<RsPathResolveResult<RsElement>> resolvePath(
        @Nonnull PathResolutionContext ctx, @Nonnull RsPath path, @Nonnull Object kind
    ) {
        return NameResolution.resolvePath(ctx, path, kind);
    }

    // --- Delegates to Processors ---

    @Nonnull
    public static List<RsElement> collectResolveVariants(
        @Nullable String referenceName, @Nonnull Consumer<RsResolveProcessor> f
    ) {
        return Processors.collectResolveVariants(referenceName, f);
    }

    @Nonnull
    public static <T extends ScopeEntry> List<T> collectResolveVariantsAsScopeEntries(
        @Nullable String referenceName, @Nonnull Consumer<RsResolveProcessorBase<T>> f
    ) {
        return Processors.collectResolveVariantsAsScopeEntries(referenceName, f);
    }

    @Nonnull
    public static List<RsPathResolveResult<RsElement>> collectPathResolveVariants(
        @Nonnull PathResolutionContext ctx, @Nonnull RsPath path,
        @Nonnull Consumer<RsResolveProcessor> f
    ) {
        return Processors.collectPathResolveVariants(ctx, path, f);
    }

    @Nonnull
    public static Map<RsPath, List<RsPathResolveResult<RsElement>>> collectMultiplePathResolveVariants(
        @Nonnull PathResolutionContext ctx, @Nonnull List<RsPath> paths,
        @Nonnull Consumer<RsResolveProcessor> f
    ) {
        return Processors.collectMultiplePathResolveVariants(ctx, paths, f);
    }
}
