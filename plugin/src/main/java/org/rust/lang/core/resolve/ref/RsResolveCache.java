/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import org.rust.lang.core.psi.ext.RsElementUtil;
import consulo.language.file.inject.VirtualFileWindow;
import consulo.disposer.Disposable;
import com.intellij.openapi.components.Service;
import consulo.ide.ServiceManager;
import consulo.application.progress.ProgressManager;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.application.util.RecursionGuard;
import consulo.application.util.RecursionManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.ResolveResult;
import consulo.language.psi.AnyPsiChangeListener;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.language.psi.PsiUtilCore;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Maps;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsModificationTrackerOwner;
import org.rust.lang.core.psi.ext.RsReferenceElement;
import org.rust.openapiext.Testmark;

import java.lang.ref.ReferenceQueue;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * The implementation is inspired by Intellij platform's {@link com.intellij.psi.impl.source.resolve.ResolveCache}.
 * The main difference from the platform one: we invalidate the cache depends on {@link ResolveCacheDependency}, when
 * platform cache invalidates on any PSI change.
 */
@Service
public final class RsResolveCache implements Disposable {
    /** The cache is cleared on rustStructureModificationTracker increment */
    private final AtomicReference<ConcurrentMap<PsiElement, Object>> myRustStructureDependentCache = new AtomicReference<>(null);

    /** The cache is cleared on ANY_PSI_CHANGE_TOPIC event */
    private final AtomicReference<ConcurrentMap<PsiElement, Object>> myAnyPsiChangeDependentCache = new AtomicReference<>(null);

    private final RecursionGuard<PsiElement> myGuard = RecursionManager.createGuard("RsResolveCache");

    private static final Key<CachedValue<ConcurrentMap<PsiElement, Object>>> LOCAL_CACHE_KEY = Key.create("LOCAL_CACHE_KEY");
    private static final Key<CachedValue<ConcurrentMap<PsiElement, Object>>> LOCAL_CACHE_KEY2 = Key.create("LOCAL_CACHE_KEY2");

    private static final Object NULL_RESULT = new Object();

    public RsResolveCache(@Nonnull Project project) {
        RsPsiManager rustPsiManager = RsPsiManagerUtil.getRustPsiManager(project);
        consulo.component.messagebus.MessageBusConnection connection = project.getMessageBus().connect(this);
        rustPsiManager.subscribeRustStructureChange(connection, new RustStructureChangeListener() {
            @Override
            public void rustStructureChanged(@Nullable PsiFile file, @Nullable PsiElement changedElement) {
                onRustStructureChanged();
            }
        });
        connection.subscribe(AnyPsiChangeListener.class, new AnyPsiChangeListener() {
            @Override
            public void afterPsiChanged(boolean isPhysical) {
                myAnyPsiChangeDependentCache.set(null);
            }

            @Override
            public void beforePsiChanged(boolean isPhysical) {
            }
        });
        rustPsiManager.subscribeRustPsiChange(connection, new RustPsiChangeListener() {
            @Override
            public void rustPsiChanged(@Nonnull PsiFile file, @Nonnull PsiElement element, boolean isStructureModification) {
                onRustPsiChanged(element);
            }
        });
    }

    @Override
    public void dispose() {
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <K extends PsiElement, V> V resolveWithCaching(
        @Nonnull K key,
        @Nonnull ResolveCacheDependency dep,
        @Nonnull Function<K, V> resolver
    ) {
        ProgressManager.checkCanceled();
        ResolveCacheDependency refinedDep = refineDependency(key, dep);
        ConcurrentMap<PsiElement, Object> map = getCacheFor(key, refinedDep);
        Object cached = map.get(key);
        if (cached != null) {
            return cached == NULL_RESULT ? null : (V) cached;
        }
        V result = myGuard.doPreventingRecursion(key, true, () -> resolver.apply(key));
        ensureValidResult(result);

        cache(map, key, result);
        return result;
    }

    @Nullable
    public Object getCached(@Nonnull PsiElement key, @Nonnull ResolveCacheDependency dep) {
        Object cached = getCacheFor(key, refineDependency(key, dep)).get(key);
        return cached == NULL_RESULT ? null : cached;
    }

    @Nonnull
    private ResolveCacheDependency refineDependency(@Nonnull PsiElement key, @Nonnull ResolveCacheDependency dep) {
        consulo.virtualFileSystem.VirtualFile vf = key.getContainingFile().getVirtualFile();
        if (vf == null || vf instanceof VirtualFileWindow) {
            return ResolveCacheDependency.ANY_PSI_CHANGE;
        }
        return dep;
    }

    @Nonnull
    private ConcurrentMap<PsiElement, Object> getCacheFor(@Nonnull PsiElement element, @Nonnull ResolveCacheDependency dep) {
        switch (dep) {
            case LOCAL:
            case LOCAL_AND_RUST_STRUCTURE: {
                RsModificationTrackerOwner owner = org.rust.lang.core.psi.ext.RsModificationTrackerOwnerUtil.findModificationTrackerOwner(element, false);
                if (owner != null) {
                    if (dep == ResolveCacheDependency.LOCAL) {
                        return CachedValuesManager.getManager(owner.getProject()).getCachedValue(owner, LOCAL_CACHE_KEY, () ->
                            CachedValueProvider.Result.create(
                                createWeakMap(),
                                owner.getModificationTracker()
                            ), false);
                    } else {
                        return CachedValuesManager.getManager(owner.getProject()).getCachedValue(owner, LOCAL_CACHE_KEY2, () ->
                            CachedValueProvider.Result.create(
                                createWeakMap(),
                                RsPsiManagerUtil.getRustStructureModificationTracker(owner.getProject()),
                                owner.getModificationTracker()
                            ), false);
                    }
                }
                return getRustStructureDependentCache();
            }
            case RUST_STRUCTURE:
                return getRustStructureDependentCache();
            case ANY_PSI_CHANGE:
                return getAnyPsiChangeDependentCache();
            default:
                throw new IllegalStateException("Unknown dependency: " + dep);
        }
    }

    @Nonnull
    private ConcurrentMap<PsiElement, Object> getRustStructureDependentCache() {
        return getOrCreateMap(myRustStructureDependentCache);
    }

    @Nonnull
    private ConcurrentMap<PsiElement, Object> getAnyPsiChangeDependentCache() {
        return getOrCreateMap(myAnyPsiChangeDependentCache);
    }

    @SuppressWarnings("unchecked")
    private <K extends PsiElement, V> void cache(@Nonnull ConcurrentMap<PsiElement, Object> map, @Nonnull K element, @Nullable V result) {
        Object cached = map.get(element);
        if (cached != null && cached == result) return;
        map.put(element, result != null ? result : NULL_RESULT);
    }

    private void onRustStructureChanged() {
        Testmarks.RustStructureDependentCacheCleared.hit();
        myRustStructureDependentCache.set(null);
    }

    private void onRustPsiChanged(@Nonnull PsiElement element) {
        PsiElement parent = element.getParent();
        if (!(parent instanceof RsReferenceElement)) return;
        RsReferenceElement referenceElement = (RsReferenceElement) parent;
        PsiElement referenceNameElement = referenceElement.getReferenceNameElement();
        if (referenceNameElement == element) {
            Testmarks.RemoveChangedElement.hit();
            PsiElement current = referenceElement;
            while (current != null) {
                if (current instanceof RsReferenceElement) {
                    getRustStructureDependentCache().remove(current);
                }
                current = current.getParent();
            }
        }
    }

    @Nonnull
    public static RsResolveCache getInstance(@Nonnull Project project) {
        return ServiceManager.getService(project, RsResolveCache.class);
    }

    @Nonnull
    private static ConcurrentMap<PsiElement, Object> getOrCreateMap(@Nonnull AtomicReference<ConcurrentMap<PsiElement, Object>> ref) {
        while (true) {
            ConcurrentMap<PsiElement, Object> existing = ref.get();
            if (existing != null) return existing;
            ConcurrentMap<PsiElement, Object> map = createWeakMap();
            if (ref.compareAndSet(null, map)) return map;
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    /**
     * Upstream subclassed {@code ConcurrentWeakKeySoftValueHashMap} to (a) hold trivial values
     * (the NULL_RESULT sentinel, empty arrays/lists) behind strong rather than soft references and
     * (b) translate NULL_RESULT to {@code null} inside {@code get}. That class is platform-internal;
     * the public factory returns a plain {@link ConcurrentMap} with no subclass hooks.
     * <p>
     * (a) was a memory optimisation only — trivial values may now be softly collected and
     * recomputed. (b) moved to the single call site that needed it, {@link #getCached}
     * ({@code resolveWithCaching} already translated the sentinel itself).
     */
    @Nonnull
    private static <K, V> ConcurrentMap<K, V> createWeakMap() {
        return Maps.newConcurrentWeakKeySoftValueHashMap(
            100,
            0.75f,
            Runtime.getRuntime().availableProcessors(),
            HashingStrategy.canonical()
        );
    }

    private static void ensureValidResult(@Nullable Object result) {
        if (result instanceof ResolveResult) {
            PsiElement element = ((ResolveResult) result).getElement();
            if (element != null) PsiUtilCore.ensureValid(element);
        } else if (result instanceof Object[]) {
            for (Object item : (Object[]) result) {
                ensureValidResult(item);
            }
        } else if (result instanceof List) {
            for (Object item : (List<?>) result) {
                ensureValidResult(item);
            }
        } else if (result instanceof PsiElement) {
            PsiUtilCore.ensureValid((PsiElement) result);
        }
    }

    public static class Testmarks {
        public static final Testmark RustStructureDependentCacheCleared = new Testmark();
        public static final Testmark RemoveChangedElement = new Testmark();
    }
}
