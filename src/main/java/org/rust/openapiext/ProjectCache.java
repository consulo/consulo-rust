/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsPsiManagerUtil;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class ProjectCache<T, R> {
    private static final Set<String> registered = ContainerUtil.newConcurrentSet();

    private final Key<CachedValue<ConcurrentMap<T, R>>> cacheKey;
    private final Function<Project, Object> dependencyGetter;

    public ProjectCache(@NotNull String cacheName) {
        this(cacheName, project -> RsPsiManagerUtil.getRustStructureModificationTracker(project));
    }

    public ProjectCache(@NotNull String cacheName, @NotNull Function<Project, Object> dependencyGetter) {
        if (!registered.add(cacheName)) {
            throw new IllegalStateException(
                "ProjectCache `" + cacheName + "` is already registered.\n" +
                    "Make sure ProjectCache is static, that is, put it inside companion object."
            );
        }
        this.cacheKey = Key.create(cacheName);
        this.dependencyGetter = dependencyGetter;
    }

    @NotNull
    public R getOrPut(@NotNull Project project, @NotNull T key, @NotNull Supplier<R> defaultValue) {
        ConcurrentMap<T, R> cache = CachedValuesManager.getManager(project)
            .getCachedValue(project, cacheKey, () ->
                CachedValueProvider.Result.create(
                    new ConcurrentHashMap<>(),
                    dependencyGetter.apply(project)
                ), false);
        return cache.computeIfAbsent(key, k -> defaultValue.get());
    }
}
