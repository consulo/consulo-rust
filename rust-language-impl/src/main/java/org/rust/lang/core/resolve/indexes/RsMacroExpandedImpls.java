/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.indexes;

import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.language.psi.PsiElement;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.crate.CrateGraphService;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.lang.core.psi.RsModItem;
import org.rust.lang.core.psi.RsTypeParameter;
import org.rust.lang.core.psi.RsTypeReference;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsItemsOwner;
import org.rust.lang.core.psi.ext.impl.RsDocAndAttributeOwnerUtil;
import org.rust.lang.core.psi.ext.impl.RsGenericDeclarationUtil;
import org.rust.lang.core.psi.ext.impl.RsItemsOwnerUtil;
import org.rust.lang.core.psi.ext.impl.RsMacroCallUtil;
import org.rust.lang.core.psi.impl.RsFile;
import org.rust.lang.core.psi.impl.RsPsiManagerUtil;
import org.rust.lang.core.types.TyFingerprint;
import org.rust.lang.utils.evaluation.ThreeValuedLogic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The impls that macro calls expand to, by the fingerprint of their self type. {@link RsImplIndex} sees only the impls
 * written in files, but much of the standard library and of crates like tokio implements traits from macros
 * ({@code impl Ord for usize} in {@code core::cmp}, everything in {@code cfg_io_util! { .. }}). Expansions live in
 * memory, so they are found by expanding the module-level macro calls of every file of every crate.
 * Empty while indexing, when no macro can be expanded.
 */
public final class RsMacroExpandedImpls {
    private static final Key<consulo.application.util.CachedValue<Map<TyFingerprint, List<RsImplItem>>>> KEY =
        Key.create("org.rust.lang.core.resolve.indexes.RsMacroExpandedImpls");

    private RsMacroExpandedImpls() {
    }

    @Nonnull
    public static List<RsImplItem> findPotentialImpls(@Nonnull Project project, @Nonnull TyFingerprint tyf) {
        if (DumbService.isDumb(project)) return Collections.emptyList();
        Map<TyFingerprint, List<RsImplItem>> impls = CachedValuesManager.getManager(project).getCachedValue(project, KEY, () ->
            CachedValueProvider.Result.create(collect(project), RsPsiManagerUtil.getRustStructureModificationTracker(project)), false);
        List<RsImplItem> result = impls.get(tyf);
        return result != null ? result : Collections.emptyList();
    }

    @Nonnull
    private static Map<TyFingerprint, List<RsImplItem>> collect(@Nonnull Project project) {
        Collector collector = new Collector();
        consulo.language.psi.PsiManager psiManager = consulo.language.psi.PsiManager.getInstance(project);
        consulo.virtualFileSystem.ManagingFS fs = consulo.virtualFileSystem.ManagingFS.getInstance();
        for (Crate crate : CrateGraphService.crateGraph(project).getTopSortedCrates()) {
            Integer crateId = crate.getId();
            if (crateId == null) continue;
            // the def map knows every file of the crate - also the modules declared inside macro calls,
            // which a PSI walk could not follow, since an expansion has no directory to resolve `mod x;` in
            org.rust.lang.core.resolve2.CrateDefMap defMap = org.rust.lang.core.resolve2.FacadeUpdateDefMapUtil.getOrUpdateIfNeeded(crate);
            if (defMap == null) continue;
            for (Integer fileId : new ArrayList<>(defMap.getFileInfos().keySet())) {
                consulo.virtualFileSystem.VirtualFile file = fs.findFileById(fileId);
                if (file != null && psiManager.findFile(file) instanceof RsFile rsFile) {
                    collector.visitModule(rsFile);
                }
            }
        }
        return collector.impls;
    }

    private static final class Collector {
        final Map<TyFingerprint, List<RsImplItem>> impls = new HashMap<>();
        private final Set<PsiElement> visited = new HashSet<>();

        /** A module as written: only its macro calls can add impls the index does not know. */
        void visitModule(@Nonnull RsItemsOwner module) {
            if (!visited.add(module)) return;
            for (RsElement element : RsItemsOwnerUtil.getItemsAndMacros(module)) {
                if (element instanceof RsMacroCall call) {
                    visitMacroCall(call);
                }
                else if (element instanceof RsModItem inlineModule && isEnabledByCfg(inlineModule)) {
                    visitModule(inlineModule);
                }
            }
        }

        private void visitMacroCall(@Nonnull RsMacroCall call) {
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
            return !(element instanceof org.rust.lang.core.psi.ext.RsDocAndAttributeOwner owner)
                || RsDocAndAttributeOwnerUtil.evaluateCfg(owner, null) != ThreeValuedLogic.False;
        }
    }
}
