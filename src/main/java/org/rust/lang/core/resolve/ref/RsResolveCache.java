/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.AnyPsiChangeListener;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.containers.ConcurrentWeakKeySoftValueHashMap;
import com.intellij.util.containers.HashingStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    public RsResolveCache(@NotNull Project project) {
        RsPsiManager rustPsiManager = RsPsiManagerUtil.getRustPsiManager(project);
        com.intellij.util.messages.MessageBusConnection connection = project.getMessageBus().connect(this);
        rustPsiManager.subscribeRustStructureChange(connection, new RustStructureChangeListener() {
            @Override
            public void rustStructureChanged(@Nullable PsiFile file, @Nullable PsiElement changedElement) {
                onRustStructureChanged();
            }
        });
        connection.subscribe(PsiManagerImpl.ANY_PSI_CHANGE_TOPIC, new AnyPsiChangeListener() {
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
            public void rustPsiChanged(@NotNull PsiFile file, @NotNull PsiElement element, boolean isStructureModification) {
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
        @NotNull K key,
        @NotNull ResolveCacheDependency dep,
        @NotNull Function<K, V> resolver
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
    public Object getCached(@NotNull PsiElement key, @NotNull ResolveCacheDependency dep) {
        return getCacheFor(key, refineDependency(key, dep)).get(key);
    }

    @NotNull
    private ResolveCacheDependency refineDependency(@NotNull PsiElement key, @NotNull ResolveCacheDependency dep) {
        com.intellij.openapi.vfs.VirtualFile vf = key.getContainingFile().getVirtualFile();
        if (vf == null || vf instanceof VirtualFileWindow) {
            return ResolveCacheDependency.ANY_PSI_CHANGE;
        }
        return dep;
    }

    @NotNull
    private ConcurrentMap<PsiElement, Object> getCacheFor(@NotNull PsiElement element, @NotNull ResolveCacheDependency dep) {
        switch (dep) {
            case LOCAL:
            case LOCAL_AND_RUST_STRUCTURE: {
                RsModificationTrackerOwner owner = org.rust.lang.core.psi.ext.RsModificationTrackerOwnerUtil.findModificationTrackerOwner(element, false);
                if (owner != null) {
                    if (dep == ResolveCacheDependency.LOCAL) {
                        return CachedValuesManager.getCachedValue(owner, LOCAL_CACHE_KEY, () ->
                            CachedValueProvider.Result.create(
                                createWeakMap(),
                                owner.getModificationTracker()
                            )
                        );
                    } else {
                        return CachedValuesManager.getCachedValue(owner, LOCAL_CACHE_KEY2, () ->
                            CachedValueProvider.Result.create(
                                createWeakMap(),
                                RsPsiManagerUtil.getRustStructureModificationTracker(owner.getProject()),
                                owner.getModificationTracker()
                            )
                        );
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

    @NotNull
    private ConcurrentMap<PsiElement, Object> getRustStructureDependentCache() {
        return getOrCreateMap(myRustStructureDependentCache);
    }

    @NotNull
    private ConcurrentMap<PsiElement, Object> getAnyPsiChangeDependentCache() {
        return getOrCreateMap(myAnyPsiChangeDependentCache);
    }

    @SuppressWarnings("unchecked")
    private <K extends PsiElement, V> void cache(@NotNull ConcurrentMap<PsiElement, Object> map, @NotNull K element, @Nullable V result) {
        Object cached = map.get(element);
        if (cached != null && cached == result) return;
        map.put(element, result != null ? result : NULL_RESULT);
    }

    private void onRustStructureChanged() {
        Testmarks.RustStructureDependentCacheCleared.hit();
        myRustStructureDependentCache.set(null);
    }

    private void onRustPsiChanged(@NotNull PsiElement element) {
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

    @NotNull
    public static RsResolveCache getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, RsResolveCache.class);
    }

    @NotNull
    private static ConcurrentMap<PsiElement, Object> getOrCreateMap(@NotNull AtomicReference<ConcurrentMap<PsiElement, Object>> ref) {
        while (true) {
            ConcurrentMap<PsiElement, Object> existing = ref.get();
            if (existing != null) return existing;
            ConcurrentMap<PsiElement, Object> map = createWeakMap();
            if (ref.compareAndSet(null, map)) return map;
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @NotNull
    private static <K, V> ConcurrentMap<K, V> createWeakMap() {
        return new ConcurrentWeakKeySoftValueHashMap<K, V>(
            100,
            0.75f,
            Runtime.getRuntime().availableProcessors(),
            HashingStrategy.canonical()
        ) {
            @Override
            protected ValueReference<K, V> createValueReference(V value, ReferenceQueue<? super V> queue) {
                boolean isTrivialValue = value == NULL_RESULT ||
                    (value instanceof Object[] && ((Object[]) value).length == 0) ||
                    (value instanceof List && ((List<?>) value).isEmpty());
                if (isTrivialValue) {
                    return new StrongValueReference<>(value);
                }
                return super.createValueReference(value, queue);
            }

            @Override
            public V get(Object key) {
                V v = super.get(key);
                if (v == NULL_RESULT) return null;
                return v;
            }
        };
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

    @SuppressWarnings("UnstableApiUsage")
    private static class StrongValueReference<K, V> implements ConcurrentWeakKeySoftValueHashMap.ValueReference<K, V> {
        private final V myValue;

        StrongValueReference(V value) {
            myValue = value;
        }

        @Override
        public ConcurrentWeakKeySoftValueHashMap.KeyReference<K, V> getKeyReference() {
            throw new UnsupportedOperationException();
        }

        @Override
        public V get() {
            return myValue;
        }
    }

    public static class Testmarks {
        public static final Testmark RustStructureDependentCacheCleared = new Testmark();
        public static final Testmark RemoveChangedElement = new Testmark();
    }
}
