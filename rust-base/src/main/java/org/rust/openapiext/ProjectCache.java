/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class ProjectCache<T, R> {
    private static final Set<String> registered = ContainerUtil.newConcurrentSet();

    private final Key<CachedValue<ConcurrentMap<T, R>>> cacheKey;
    private final Function<Project, Object> dependencyGetter;

    public ProjectCache(@Nonnull String cacheName, @Nonnull Function<Project, Object> dependencyGetter) {
        if (!registered.add(cacheName)) {
            throw new IllegalStateException(
                "ProjectCache `" + cacheName + "` is already registered.\n" +
                    "Make sure ProjectCache is static, that is, put it inside companion object."
            );
        }
        this.cacheKey = Key.create(cacheName);
        this.dependencyGetter = dependencyGetter;
    }

    @Nonnull
    public R getOrPut(@Nonnull Project project, @Nonnull T key, @Nonnull Supplier<R> defaultValue) {
        ConcurrentMap<T, R> cache = CachedValuesManager.getManager(project)
            .getCachedValue(project, cacheKey, () ->
                CachedValueProvider.Result.create(
                    new ConcurrentHashMap<>(),
                    dependencyGetter.apply(project)
                ), false);
        return cache.computeIfAbsent(key, k -> defaultValue.get());
    }
}
