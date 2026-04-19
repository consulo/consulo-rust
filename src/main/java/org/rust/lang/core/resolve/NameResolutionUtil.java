/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import com.intellij.openapi.project.Project;
import com.intellij.util.ThreeState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

/**
 * Utility class providing static access to name resolution functions.
 * Delegates to {@link NameResolution} and {@link Processors}.
 */
public final class NameResolutionUtil {
    private NameResolutionUtil() {}

    // --- Delegates to NameResolution ---

    public static boolean processDotExprResolveVariants(
        @NotNull ImplLookup lookup, @NotNull Ty receiverType,
        @NotNull RsElement context, @NotNull RsResolveProcessorBase<DotExprResolveVariant> processor
    ) {
        return NameResolution.processDotExprResolveVariants(lookup, receiverType, context, processor);
    }

    public static boolean processFieldExprResolveVariants(
        @NotNull ImplLookup lookup, @NotNull Ty receiverType,
        @NotNull RsResolveProcessorBase<FieldResolveVariant> processor
    ) {
        return NameResolution.processFieldExprResolveVariants(lookup, receiverType, processor);
    }

    public static boolean processStructLiteralFieldResolveVariants(
        @NotNull RsStructLiteralField field, boolean isCompletion,
        @NotNull RsResolveProcessor processor
    ) {
        return NameResolution.processStructLiteralFieldResolveVariants(field, isCompletion, processor);
    }

    public static boolean processStructPatternFieldResolveVariants(
        @NotNull RsPatFieldFull field, @NotNull RsResolveProcessor processor
    ) {
        return NameResolution.processStructPatternFieldResolveVariants(field, processor);
    }

    public static boolean processMethodCallExprResolveVariants(
        @NotNull ImplLookup lookup, @NotNull Ty receiverType,
        @NotNull RsElement context, @NotNull RsResolveProcessorBase<MethodResolveVariant> processor
    ) {
        return NameResolution.processMethodCallExprResolveVariants(lookup, receiverType, context, processor);
    }

    public static boolean processModDeclResolveVariants(
        @NotNull RsModDeclItem modDecl, @NotNull RsResolveProcessor processor
    ) {
        return NameResolution.processModDeclResolveVariants(modDecl, processor);
    }

    public static boolean processExternCrateResolveVariants(
        @NotNull RsElement element, boolean isCompletion, @NotNull RsResolveProcessor processor
    ) {
        return NameResolution.processExternCrateResolveVariants(element, isCompletion, processor);
    }

    public static boolean processExternCrateResolveVariants(
        @NotNull RsElement element, boolean isCompletion, boolean withSelf,
        @NotNull RsResolveProcessor processor
    ) {
        return NameResolution.processExternCrateResolveVariants(element, isCompletion, withSelf, processor);
    }

    @Nullable
    public static RsFile findDependencyCrateByNamePath(@NotNull RsElement context, @NotNull RsPath path) {
        return NameResolution.findDependencyCrateByNamePath(context, path);
    }

    @Nullable
    public static RsFile findDependencyCrateByName(@NotNull RsElement context, @NotNull String name) {
        return NameResolution.findDependencyCrateByName(context, name);
    }

    public static boolean processPathResolveVariants(
        @Nullable ImplLookup lookup, @NotNull RsPath path,
        boolean isCompletion, boolean processAssocItems,
        @NotNull RsResolveProcessor processor
    ) {
        return NameResolution.processPathResolveVariants(lookup, path, isCompletion, processAssocItems, processor);
    }

    public static boolean processPathResolveVariants(
        @NotNull PathResolutionContext ctx, @NotNull RsPathResolveKind pathKind,
        @NotNull RsResolveProcessor processor
    ) {
        return NameResolution.processPathResolveVariants(ctx, pathKind, processor);
    }

    @Nullable
    public static Crate resolveDollarCrateIdentifier(@NotNull RsPath path) {
        return NameResolution.resolveDollarCrateIdentifier(path);
    }

    public static boolean processPatBindingResolveVariants(
        @NotNull RsPatBinding binding, boolean isCompletion,
        @NotNull RsResolveProcessor processor
    ) {
        return NameResolution.processPatBindingResolveVariants(binding, isCompletion, processor);
    }

    public static boolean processLabelResolveVariants(
        @NotNull RsLabel label, @NotNull RsResolveProcessor processor
    ) {
        return NameResolution.processLabelResolveVariants(label, processor);
    }

    public static boolean processLabelResolveVariants(
        @NotNull RsLabel label, @NotNull RsResolveProcessor processor,
        boolean processBeyondLabelBarriers
    ) {
        return NameResolution.processLabelResolveVariants(label, processor, processBeyondLabelBarriers);
    }

    @NotNull
    public static List<RsElement> resolveLabelReference(@NotNull RsLabel element) {
        return NameResolution.resolveLabelReference(element);
    }

    @NotNull
    public static List<RsElement> resolveLabelReference(@NotNull RsLabel element, boolean processBeyondLabelBarriers) {
        return NameResolution.resolveLabelReference(element, processBeyondLabelBarriers);
    }

    public static boolean processLifetimeResolveVariants(
        @NotNull RsLifetime lifetime, @NotNull RsResolveProcessor processor
    ) {
        return NameResolution.processLifetimeResolveVariants(lifetime, processor);
    }

    public static void processLocalVariables(
        @NotNull RsElement place, @NotNull Consumer<RsPatBinding> processor
    ) {
        NameResolution.processLocalVariables(place, processor);
    }

    @Nullable
    public static com.intellij.openapi.util.Pair<RsNamedElement, CargoWorkspace.Package> resolveStringPath(
        @NotNull String path, @NotNull CargoWorkspace workspace,
        @NotNull Project project, @NotNull ThreeState isStd
    ) {
        return NameResolution.resolveStringPath(path, workspace, project, isStd);
    }

    @Nullable
    public static com.intellij.openapi.util.Pair<RsNamedElement, CargoWorkspace.Package> resolveStringPath(
        @NotNull String path, @NotNull CargoWorkspace workspace,
        @NotNull Project project
    ) {
        return NameResolution.resolveStringPath(path, workspace, project);
    }

    @Nullable
    public static com.intellij.openapi.util.Pair<String, String> splitAbsolutePath(@NotNull String path) {
        return NameResolution.splitAbsolutePath(path);
    }

    public static boolean processMacroReferenceVariants(
        @NotNull RsMacroReference ref, @NotNull RsResolveProcessor processor
    ) {
        return NameResolution.processMacroReferenceVariants(ref, processor);
    }

    public static boolean processProcMacroResolveVariants(
        @NotNull RsPath path, @NotNull RsResolveProcessor processor, boolean isCompletion
    ) {
        return NameResolution.processProcMacroResolveVariants(path, processor, isCompletion);
    }

    public static boolean processDeriveTraitResolveVariants(
        @NotNull RsPath element, @NotNull String traitName,
        @NotNull RsResolveProcessor processor
    ) {
        return NameResolution.processDeriveTraitResolveVariants(element, traitName, processor);
    }

    public static boolean processBinaryOpVariants(
        @NotNull RsBinaryOp element, @NotNull OverloadableBinaryOperator operator,
        @NotNull RsResolveProcessor processor
    ) {
        return NameResolution.processBinaryOpVariants(element, operator, processor);
    }

    public static boolean processAssocTypeVariants(
        @NotNull RsAssocTypeBinding element, @NotNull RsResolveProcessor processor
    ) {
        return NameResolution.processAssocTypeVariants(element, processor);
    }

    public static boolean processAssocTypeVariants(
        @NotNull RsTraitItem trait, @NotNull RsResolveProcessor processor
    ) {
        return NameResolution.processAssocTypeVariants(trait, processor);
    }

    public static boolean processMacroCallPathResolveVariants(
        @NotNull RsPath path, boolean isCompletion,
        @NotNull RsResolveProcessor processor
    ) {
        return NameResolution.processMacroCallPathResolveVariants(path, isCompletion, processor);
    }

    public static boolean processMacroCallVariantsInScope(
        @NotNull RsPath path, boolean ignoreLegacyMacros,
        @NotNull RsResolveProcessor processor
    ) {
        return NameResolution.processMacroCallVariantsInScope(path, ignoreLegacyMacros, processor);
    }

    public static boolean processNestedScopesUpwards(
        @NotNull RsElement scopeStart, @NotNull Set<Namespace> ns,
        @NotNull RsResolveProcessor processor
    ) {
        return NameResolution.processNestedScopesUpwards(scopeStart, ns, processor);
    }

    public static boolean processNestedScopesUpwards(
        @NotNull RsElement scopeStart, @NotNull Set<Namespace> ns,
        @Nullable PathResolutionContext ctx, @NotNull RsResolveProcessor processor
    ) {
        return NameResolution.processNestedScopesUpwards(scopeStart, ns, ctx, processor);
    }

    @Nullable
    public static RsMod findPrelude(@NotNull RsElement element) {
        return NameResolution.findPrelude(element);
    }

    public static void processUnresolvedImports(
        @NotNull RsElement context, @NotNull Consumer<RsUseSpeck> processor
    ) {
        NameResolution.processUnresolvedImports(context, processor);
    }

    public static boolean processExternPreludeResolveVariants(
        @NotNull PathResolutionContext ctx, @NotNull RsResolveProcessor processor
    ) {
        return NameResolution.processExternPreludeResolveVariants(ctx, processor);
    }

    @NotNull
    public static Set<Namespace> allowedNamespaces(@NotNull RsPath path) {
        return NameResolution.allowedNamespaces(path);
    }

    @NotNull
    public static List<RsPathResolveResult<RsElement>> resolvePath(
        @NotNull PathResolutionContext ctx, @NotNull RsPath path, @NotNull Object kind
    ) {
        return NameResolution.resolvePath(ctx, path, kind);
    }

    // --- Delegates to Processors ---

    @NotNull
    public static List<RsElement> collectResolveVariants(
        @Nullable String referenceName, @NotNull Consumer<RsResolveProcessor> f
    ) {
        return Processors.collectResolveVariants(referenceName, f);
    }

    @NotNull
    public static <T extends ScopeEntry> List<T> collectResolveVariantsAsScopeEntries(
        @Nullable String referenceName, @NotNull Consumer<RsResolveProcessorBase<T>> f
    ) {
        return Processors.collectResolveVariantsAsScopeEntries(referenceName, f);
    }

    @NotNull
    public static List<RsPathResolveResult<RsElement>> collectPathResolveVariants(
        @NotNull PathResolutionContext ctx, @NotNull RsPath path,
        @NotNull Consumer<RsResolveProcessor> f
    ) {
        return Processors.collectPathResolveVariants(ctx, path, f);
    }

    @NotNull
    public static Map<RsPath, List<RsPathResolveResult<RsElement>>> collectMultiplePathResolveVariants(
        @NotNull PathResolutionContext ctx, @NotNull List<RsPath> paths,
        @NotNull Consumer<RsResolveProcessor> f
    ) {
        return Processors.collectMultiplePathResolveVariants(ctx, paths, f);
    }
}
