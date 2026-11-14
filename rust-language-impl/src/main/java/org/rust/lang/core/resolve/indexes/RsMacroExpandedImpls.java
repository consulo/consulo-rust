/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.indexes;

import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.StubIndex;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.RsMacro;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.lang.core.psi.RsModItem;
import org.rust.lang.core.psi.RsTypeParameter;
import org.rust.lang.core.psi.RsTypeReference;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.impl.RsDocAndAttributeOwnerUtil;
import org.rust.lang.core.psi.ext.impl.RsGenericDeclarationUtil;
import org.rust.lang.core.psi.ext.impl.RsItemsOwnerUtil;
import org.rust.lang.core.psi.ext.impl.RsMacroCallUtil;
import org.rust.lang.core.psi.impl.RsPsiManagerUtil;
import org.rust.lang.core.types.TyFingerprint;
import org.rust.lang.utils.evaluation.ThreeValuedLogic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import consulo.application.util.CachedValue;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwner;

/**
 * The impls that macro calls expand to, by the fingerprint of their self type. {@link RsImplIndex} sees only the impls
 * written in files, but much of the standard library and of crates like tokio implements traits from macros
 * ({@code impl Ord for usize} in {@code core::cmp}, everything in {@code cfg_io_util! { .. }}). Expansions live in
 * memory, so for one fingerprint only the module-level macro calls that may produce it are expanded: the calls with an
 * impl for the type written in them, the calls of the {@code macro_rules!} that write one, the calls that name the type
 * in their arguments and pass it to a macro that takes the self type of an impl from a metavariable, and the calls that
 * include a file (see {@link RsImplMacroKeys}). Empty while indexing, when no macro can be expanded.
 */
public final class RsMacroExpandedImpls {
    private static final Key<CachedValue<Map<TyFingerprint, List<RsImplItem>>>> KEY =
        Key.create("org.rust.lang.core.resolve.indexes.RsMacroExpandedImpls");
    private static final Key<CachedValue<Map<RsMacroCall, Map<TyFingerprint, List<RsImplItem>>>>> EXPANDED_CALLS_KEY =
        Key.create("org.rust.lang.core.resolve.indexes.RsMacroExpandedImpls.expandedCalls");
    private static final Key<CachedValue<Set<String>>> METAVARIABLE_MACROS_KEY =
        Key.create("org.rust.lang.core.resolve.indexes.RsMacroExpandedImpls.metavariableMacros");

    private RsMacroExpandedImpls() {
    }

    @Nonnull
    public static List<RsImplItem> findPotentialImpls(@Nonnull Project project, @Nonnull TyFingerprint tyf) {
        if (DumbService.isDumb(project)) return Collections.emptyList();
        Map<TyFingerprint, List<RsImplItem>> cache = CachedValuesManager.getManager(project).getCachedValue(project, KEY, () ->
            CachedValueProvider.Result.create(new ConcurrentHashMap<>(), RsPsiManagerUtil.getRustStructureModificationTracker(project)), false);
        List<RsImplItem> cached = cache.get(tyf);
        if (cached != null) return cached;
        // not computeIfAbsent: expanding a macro resolves names, which may look up the impls of another type
        List<RsImplItem> impls = collect(project, tyf);
        List<RsImplItem> previous = cache.putIfAbsent(tyf, impls);
        return previous != null ? previous : impls;
    }

    @Nonnull
    private static List<RsImplItem> collect(@Nonnull Project project, @Nonnull TyFingerprint tyf) {
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        Set<String> keys = new LinkedHashSet<>(RsImplMacroKeys.forFingerprint(tyf));
        keys.add(RsImplMacroKeys.INCLUDE);
        Set<RsMacroCall> calls = new LinkedHashSet<>();
        Set<String> macroNames = new HashSet<>();
        for (String key : keys) {
            // an impl for the type written in the call, or in the definition of the macro it calls
            calls.addAll(StubIndex.getElements(RsImplMacroCallIndex.KEY, key, project, scope, RsMacroCall.class));
            for (RsMacro macro : StubIndex.getElements(RsImplMacroDefIndex.KEY, key, project, scope, RsMacro.class)) {
                if (macro.getName() != null) macroNames.add(macro.getName());
            }
        }
        for (String name : macroNames) {
            calls.addAll(StubIndex.getElements(RsImplMacroCallIndex.KEY, RsImplMacroKeys.callOf(name), project, scope, RsMacroCall.class));
        }
        // the type in the arguments of a call that passes it on to the self type of an impl
        Set<String> metavariableMacros = metavariableSelfTypeMacros(project);
        for (String key : RsImplMacroKeys.forFingerprint(tyf)) {
            for (RsMacroCall call : StubIndex.getElements(RsImplMacroCallIndex.KEY, RsImplMacroKeys.argument(key), project, scope, RsMacroCall.class)) {
                if (callsAnyOf(call, metavariableMacros)) calls.add(call);
            }
        }
        Map<RsMacroCall, Map<TyFingerprint, List<RsImplItem>>> expanded = expandedCalls(project);
        List<RsImplItem> result = new ArrayList<>();
        for (RsMacroCall call : calls) {
            Map<TyFingerprint, List<RsImplItem>> implsOfCall = expanded.get(call);
            if (implsOfCall == null) {
                Collector collector = new Collector();
                collector.visitMacroCall(call);
                implsOfCall = collector.impls;
                expanded.putIfAbsent(call, implsOfCall);
            }
            List<RsImplItem> impls = implsOfCall.get(tyf);
            if (impls != null) result.addAll(impls);
        }
        return result;
    }

    /** The impls each macro call expanded so far adds, by fingerprint: a call is expanded once for all the types it may serve. */
    @Nonnull
    private static Map<RsMacroCall, Map<TyFingerprint, List<RsImplItem>>> expandedCalls(@Nonnull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(project, EXPANDED_CALLS_KEY, () ->
            CachedValueProvider.Result.create(new ConcurrentHashMap<>(), RsPsiManagerUtil.getRustStructureModificationTracker(project)), false);
    }

    /** The names of the {@code macro_rules!} whose impls take their self type from a metavariable. */
    @Nonnull
    private static Set<String> metavariableSelfTypeMacros(@Nonnull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(project, METAVARIABLE_MACROS_KEY, () -> {
            Set<String> names = new HashSet<>();
            for (RsMacro macro : StubIndex.getElements(RsImplMacroDefIndex.KEY, RsImplMacroKeys.METAVARIABLE_SELF_TYPE, project,
                GlobalSearchScope.allScope(project), RsMacro.class)) {
                if (macro.getName() != null) names.add(macro.getName());
            }
            return CachedValueProvider.Result.create(names, RsPsiManagerUtil.getRustStructureModificationTracker(project));
        }, false);
    }

    /** Whether {@code call} calls one of {@code macros}, itself or in its arguments. */
    private static boolean callsAnyOf(@Nonnull RsMacroCall call, @Nonnull Set<String> macros) {
        if (macros.contains(RsMacroCallUtil.getMacroName(call))) return true;
        String body = RsMacroCallUtil.getMacroBody(call);
        if (body == null) return false;
        for (String name : RsImplMacroKeys.calledMacros(body)) {
            if (macros.contains(name)) return true;
        }
        return false;
    }

    private static final class Collector {
        final Map<TyFingerprint, List<RsImplItem>> impls = new HashMap<>();
        private final Set<PsiElement> visited = new HashSet<>();

        void visitMacroCall(@Nonnull RsMacroCall call) {
            if (!isEnabledByCfg(call)) return;
            RsMacroCallUtil.processExpansionRecursively(call, expanded -> {
                visitExpanded(expanded);
                return false;
            });
        }

        /** An element of an expansion: every impl in it is one the index does not know. */
        private void visitExpanded(@Nonnull PsiElement element) {
            if (element instanceof RsImplItem impl) {
                if (isEnabledByCfg(impl)) add(impl);
            }
            else if (element instanceof RsModItem module && isEnabledByCfg(module)) {
                if (!visited.add(module)) return;
                for (RsElement child : RsItemsOwnerUtil.getItemsAndMacros(module)) {
                    if (child instanceof RsMacroCall call) visitMacroCall(call);
                    else visitExpanded(child);
                }
            }
        }

        private void add(@Nonnull RsImplItem impl) {
            RsTypeReference typeReference = impl.getTypeReference();
            if (typeReference == null) return;
            List<String> typeParameterNames = new ArrayList<>();
            for (RsTypeParameter parameter : RsGenericDeclarationUtil.getTypeParameters(impl)) {
                if (parameter.getName() != null) typeParameterNames.add(parameter.getName());
            }
            for (TyFingerprint fingerprint : TyFingerprint.create(typeReference, typeParameterNames)) {
                impls.computeIfAbsent(fingerprint, k -> new ArrayList<>()).add(impl);
            }
        }

        private static boolean isEnabledByCfg(@Nonnull PsiElement element) {
            return !(element instanceof RsDocAndAttributeOwner owner)
                || RsDocAndAttributeOwnerUtil.evaluateCfg(owner, null) != ThreeValuedLogic.False;
        }
    }
}
