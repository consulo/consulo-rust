/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsPsiManagerUtil;
import org.rust.lang.core.psi.RustStructureChangeListener;
import org.rust.lang.core.resolve.indexes.RsAliasIndex;
import org.rust.lang.core.resolve.indexes.RsImplIndex;
import org.rust.lang.core.types.TyFingerprint;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
public final class RsImplIndexAndTypeAliasCache implements Disposable {
    private final Project project;

    // strong key -> soft value maps
    private final AtomicReference<ConcurrentMap<TyFingerprint, List<RsCachedImplItem>>> implIndexCache = new AtomicReference<>(null);
    private final AtomicReference<ConcurrentMap<TyFingerprint, List<String>>> typeAliasShallowIndexCache = new AtomicReference<>(null);
    private final AtomicReference<ConcurrentMap<TyFingerprint, List<String>>> typeAliasTransitiveIndexCache = new AtomicReference<>(null);

    /**
     * This map is actually used as a Set (the value is always placeholder).
     * The only purpose of this set is holding links to PsiFiles, so as to retain them in memory.
     */
    private final ConcurrentMap<PsiFile, Object> usedPsiFiles = ContainerUtil.createConcurrentSoftMap();
    private static final Object PLACEHOLDER = new Object();

    public RsImplIndexAndTypeAliasCache(@NotNull Project project) {
        this.project = project;

        var rustPsiManager = RsPsiManagerUtil.getRustPsiManager(project);
        var connection = project.getMessageBus().connect(this);
        rustPsiManager.subscribeRustStructureChange(connection, new RustStructureChangeListener() {
            @Override
            public void rustStructureChanged(PsiFile file, PsiElement changedElement) {
                implIndexCache.getAndSet(null);
                typeAliasShallowIndexCache.getAndSet(null);
                typeAliasTransitiveIndexCache.getAndSet(null);
            }
        });
    }

    @NotNull
    public List<RsCachedImplItem> findPotentialImpls(@NotNull TyFingerprint tyf) {
        ConcurrentMap<TyFingerprint, List<RsCachedImplItem>> cache = getOrCreateMap(implIndexCache);
        return cache.computeIfAbsent(tyf, key -> {
            List<RsCachedImplItem> result = new ArrayList<>();
            for (RsCachedImplItem item : RsImplIndex.findPotentialImpls(project, key)) {
                retainPsi(item.getImpl().getContainingFile());
                if (item.isValid()) {
                    result.add(item);
                }
            }
            return result;
        });
    }

    @NotNull
    private List<String> shallowFindPotentialAliases(@NotNull TyFingerprint tyf) {
        ConcurrentMap<TyFingerprint, List<String>> cache = getOrCreateMap(typeAliasShallowIndexCache);
        return cache.computeIfAbsent(tyf, key -> RsAliasIndex.findPotentialAliases(project, key));
    }

    @NotNull
    public List<String> findPotentialAliases(@NotNull TyFingerprint tyf) {
        ConcurrentMap<TyFingerprint, List<String>> cache = getOrCreateMap(typeAliasTransitiveIndexCache);
        return cache.computeIfAbsent(tyf, key -> {
            Set<String> result = new HashSet<>();
            result.add(key.getName());
            List<String> queue = new ArrayList<>(shallowFindPotentialAliases(key));
            while (!queue.isEmpty()) {
                String alias = queue.remove(queue.size() - 1);
                if (result.add(alias)) {
                    queue.addAll(shallowFindPotentialAliases(new TyFingerprint(alias)));
                }
            }
            List<String> filtered = new ArrayList<>(result.size());
            for (String s : result) {
                if (!s.equals(key.getName())) {
                    filtered.add(s);
                }
            }
            return filtered;
        });
    }

    private void retainPsi(@NotNull PsiFile containingFile) {
        usedPsiFiles.put(containingFile, PLACEHOLDER);
    }

    @Override
    public void dispose() {
    }

    @NotNull
    public static RsImplIndexAndTypeAliasCache getInstance(@NotNull Project project) {
        return project.getService(RsImplIndexAndTypeAliasCache.class);
    }

    @NotNull
    private static <T> ConcurrentMap<TyFingerprint, T> getOrCreateMap(@NotNull AtomicReference<ConcurrentMap<TyFingerprint, T>> ref) {
        while (true) {
            ConcurrentMap<TyFingerprint, T> existing = ref.get();
            if (existing != null) return existing;
            ConcurrentMap<TyFingerprint, T> map = ContainerUtil.createConcurrentSoftValueMap();
            if (ref.compareAndSet(null, map)) return map;
        }
    }
}
