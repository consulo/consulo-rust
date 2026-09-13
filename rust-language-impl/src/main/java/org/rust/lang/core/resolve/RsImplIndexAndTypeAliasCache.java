/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.impl.RsPsiManagerUtil;
import org.rust.lang.core.psi.RustStructureChangeListener;
import org.rust.lang.core.resolve.indexes.RsAliasIndex;
import org.rust.lang.core.resolve.indexes.RsImplIndex;
import org.rust.lang.core.types.TyFingerprint;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import jakarta.inject.Inject;

@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
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

    /**
     * Index lookups answer nothing while indexing is still running, and those answers must not be kept:
     * the maps below would otherwise hold an empty result for a fingerprint until the next Rust
     * structure change. {@link consulo.project.DumbService#getModificationTracker()} advances whenever
     * indexing finishes, so a change of this count means every cached lookup was taken against a
     * different index state and has to be recomputed.
     */
    private final AtomicLong indexStateStamp = new AtomicLong(-1);
    private static final Object PLACEHOLDER = new Object();

    @Inject

    public RsImplIndexAndTypeAliasCache(@Nonnull Project project) {
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

    @Nonnull
    public List<RsCachedImplItem> findPotentialImpls(@Nonnull TyFingerprint tyf) {
        dropCachesIfIndexStateChanged();
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

    @Nonnull
    private List<String> shallowFindPotentialAliases(@Nonnull TyFingerprint tyf) {
        dropCachesIfIndexStateChanged();
        ConcurrentMap<TyFingerprint, List<String>> cache = getOrCreateMap(typeAliasShallowIndexCache);
        return cache.computeIfAbsent(tyf, key -> RsAliasIndex.findPotentialAliases(project, key));
    }

    @Nonnull
    public List<String> findPotentialAliases(@Nonnull TyFingerprint tyf) {
        dropCachesIfIndexStateChanged();
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

    private void retainPsi(@Nonnull PsiFile containingFile) {
        usedPsiFiles.put(containingFile, PLACEHOLDER);
    }

    @Override
    public void dispose() {
    }

    @Nonnull
    public static RsImplIndexAndTypeAliasCache getInstance(@Nonnull Project project) {
        return project.getService(RsImplIndexAndTypeAliasCache.class);
    }

    @Nonnull
    /** Clears every cached lookup when indexing has progressed since they were taken. */
    private void dropCachesIfIndexStateChanged() {
        long current = consulo.project.DumbService.getInstance(project).getModificationTracker().getModificationCount();
        long previous = indexStateStamp.getAndSet(current);
        if (previous != current) {
            implIndexCache.set(null);
            typeAliasShallowIndexCache.set(null);
            typeAliasTransitiveIndexCache.set(null);
        }
    }

    private static <T> ConcurrentMap<TyFingerprint, T> getOrCreateMap(@Nonnull AtomicReference<ConcurrentMap<TyFingerprint, T>> ref) {
        while (true) {
            ConcurrentMap<TyFingerprint, T> existing = ref.get();
            if (existing != null) return existing;
            ConcurrentMap<TyFingerprint, T> map = ContainerUtil.createConcurrentSoftValueMap();
            if (ref.compareAndSet(null, map)) return map;
        }
    }
}
