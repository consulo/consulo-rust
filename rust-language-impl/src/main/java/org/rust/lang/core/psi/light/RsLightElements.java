/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.light;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** The light elements of a project that stand for a source element, so that the usages of the source can include theirs. */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public final class RsLightElements {
    private final Map<PsiElement, List<WeakReference<RsLightElement<?>>>> byOrigin = new WeakHashMap<>();

    @Nonnull
    public static RsLightElements getInstance(@Nonnull Project project) {
        return project.getInstance(RsLightElements.class);
    }

    void register(@Nonnull RsLightElement<?> element) {
        synchronized (byOrigin) {
            byOrigin.computeIfAbsent(element.getOrigin(), key -> new ArrayList<>()).add(new WeakReference<>(element));
        }
    }

    /** The valid light elements generated for {@code origin}. */
    @Nonnull
    public List<RsLightElement<?>> forOrigin(@Nonnull PsiElement origin) {
        synchronized (byOrigin) {
            List<WeakReference<RsLightElement<?>>> references = byOrigin.get(origin);
            if (references == null) return Collections.emptyList();
            List<RsLightElement<?>> result = new ArrayList<>();
            references.removeIf(reference -> {
                RsLightElement<?> element = reference.get();
                if (element == null || !element.isValid()) return true;
                result.add(element);
                return false;
            });
            return result;
        }
    }
}
